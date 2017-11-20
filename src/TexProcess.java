import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ConstantConditions")
class TexProcess {
    private static final Pattern filenamePattern = Pattern.compile("^(\\s*\\\\includegraphics\\[(width|height)\\s*=\\s*)(\\S+)(\\]\\{)(\\S+)(\\}\\S*\\s*)$");
    private static final Pattern sizePattern = Pattern.compile("/size(\\d+)/");
    private static final Pattern chapterPattern = Pattern.compile("^(\\s*\\\\chapter)\\{([\\s\\S]+)\\}(\\s*)$");
    private static final Pattern sectionPattern = Pattern.compile("^(\\s*\\\\section)\\{(\\W+)\\}(\\s*)$");
    private static final Pattern subsectionPattern = Pattern.compile("^(\\s*\\\\subsection)\\{(\\W+)\\}(\\s*)$");
    private static final Pattern tikzlibararyPattern = Pattern.compile("^\\\\usetikzlibrary\\{([\\s\\S]+)\\}");
    private final Logger log;
    private int warningCount = 0;
    private List<File> inputRawTexFiles;
    private File mainFile;
    private File figureFolder;
    private File headerFile;
    private List<File> partFolders;
    private Set<String> tikzLibraries = new HashSet<>();

    TexProcess(List<File> inputRawTexFiles, File mainFile, File figureFolder, File headerFile,
               List<File> partFolders) {
        this.inputRawTexFiles = inputRawTexFiles;
        this.mainFile = mainFile;
        this.figureFolder = figureFolder;
        this.log = Logger.getLog();
        this.headerFile = headerFile;
        this.partFolders = partFolders;
        for (File folder : partFolders) {
            if (!folder.exists()) folder.mkdir();
        }
    }

    /**
     * Execute the process
     */
    public void process() {
        log.println("============================================Merge start============================================");
        // categorize input files by their prefix
        Map<File, List<File>> rawTexMap = categorizeRawTexFiles(inputRawTexFiles);
        for (File folder : rawTexMap.keySet()) {
            for (File texFile : rawTexMap.get(folder)) {
                processTexFile(folder, texFile);
            }
        }
        // decorate trimmed files
        Map<File, List<File>> trimmedTexMap = new HashMap<>();
        for (File folder : partFolders) {
            trimmedTexMap.put(folder, getTrimmedTexFileInFolder(folder));
            for (File trimmedFile : trimmedTexMap.get(folder)) {
                decorateTrimmedFile(trimmedFile);
            }
        }
        // generate main file
        generateMainFile(trimmedTexMap);
        // output completion info in log
        log.print("All done");
        if (warningCount != 0) {
            log.print(" with " + warningCount + " warning(s)");
        } else {
            log.print(" without warnings");
        }
        log.println(". ");
    }

    /**
     * categorize the input tex files by their filename.
     *
     * @param inputRawTexFiles input tex files
     * @return a map describe the result of categorization.
     */
    private Map<File, List<File>> categorizeRawTexFiles(List<File> inputRawTexFiles) {
        Map<File, List<File>> map = new HashMap<>();
        for (File folder : partFolders) {
            map.put(folder, new ArrayList<>());
        }
        for (File texFile : inputRawTexFiles) {
            List<File> currentList;
            switch (texFile.getName().substring(0, 6)) {
                case "Differ":
                    currentList = map.get(partFolders.get(0));
                    break;
                case "Integr":
                    currentList = map.get(partFolders.get(1));
                    break;
                case "Series":
                    currentList = map.get(partFolders.get(2));
                    break;
                default:
                    currentList = map.get(partFolders.get(3));
            }
            currentList.add(texFile);
        }
        return map;
    }

    /**
     * Decorate a String of certain length.
     *
     * @param string string need decorate
     * @return decorated string.
     */
    private String decorateSection(String string) {
        String separator;
        switch (string.length()) {
            case 2:
                separator = "\\hskip 2em ";
                break;
            case 3:
                separator = "\\hskip 1em ";
                break;
            case 4:
                separator = "\\ ";
                break;
            case 5:
                separator = "\\hskip 0.25em ";
                break;
            default:
                separator = null;
        }
        if (separator == null) return string;
        StringBuilder sb = new StringBuilder();
        sb.append(string.charAt(0));
        for (int i = 1; i < string.length(); i++) {
            sb.append(separator).append(string.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Count how many of '}' at the end of the given String.
     *
     * @param path file path which may contains ending braces
     * @return the count of '}' at the end of path.
     */
    private int countEndingBraces(String path) {
        int pos = path.length() - 1;
        int braceCount = 0;
        while (path.charAt(pos) == '}') {
            braceCount++;
            pos--;
        }
        return braceCount;
    }

    /**
     * Recursively find the file with certain name in the given folder, return the result in a list of Files.
     *
     * @param folder   the folder is to be searched
     * @param filename the target file name
     * @return a list of found files
     */
    @SuppressWarnings("ConstantConditions")
    private List<File> findByFileName(File folder, String filename) {
        return findByFileName(folder, filename, true);
    }

    private boolean isLegalFolderName(File file, boolean ignoreIllegalFolders) {
        if (ignoreIllegalFolders) {
            return file.getName().matches("^size([\\d]+)$");
        }
        return true;
    }

    private List<File> findByFileName(File folder, String filename, boolean ignoreIllegalFolders) {
        List<File> foundList = new ArrayList<>();
        for (File file : folder.listFiles()) {
            if (file.isDirectory() && isLegalFolderName(file, ignoreIllegalFolders)) {
                foundList.addAll(findByFileName(file, filename, ignoreIllegalFolders));
            } else {
                if (file.getName().equals(filename)) {
                    foundList.add(file);
                }
            }
        }
        return foundList;
    }

    /**
     * A formula to calculate the width of figure with certain size.
     *
     * @param size size of figure
     * @return the width of figure in centimeter
     */
    private String getWidth(int size) {
        double width = 13.0 / 500 * size;
        return String.format("%.2f", width);
    }

    /**
     * Returns a sorted list of trimmed tex files in the given folder.
     * A trimmed tex file is a file whose full name ended with "-trim.tex".
     * The list is sorted by the ordinal number which is contained in the filename.
     *
     * @param folder file instance of a folder
     * @return a list of trimmed tex files (whose filename ends with "-trim.tex")
     */
    private List<File> getTrimmedTexFileInFolder(File folder) {
        List<File> files = new ArrayList<>();
        Pattern pattern = Pattern.compile("^\\S*-(\\d{2})-\\S*");
        for (File trimmedFile : folder.listFiles()) {
            Matcher matcher = pattern.matcher(trimmedFile.getName());
            if (trimmedFile.getName().endsWith("-trim.tex") && matcher.find()) {
                files.add(trimmedFile);
            }
        }
        files.sort((File file1, File file2) -> {
            Matcher matcher1 = pattern.matcher(file1.getName());
            Matcher matcher2 = pattern.matcher(file2.getName());
            if (matcher1.find() && matcher2.find()) {
                return matcher1.group(1).compareTo(matcher2.group(1));
            } else {
                return file1.getName().compareTo(file2.getName());
            }
        });
        return files;
    }

    /**
     * Reader the content of a tex file, transfer the main part of it to the trimmed tex file.
     *
     * @param folder  the corresponding folder of the trimmed file
     * @param texFile the raw tex file.
     */
    private void processTexFile(File folder, File texFile) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        String texFileName = texFile.getName();
        String trimmedFileName = texFileName.substring(0, texFileName.length() - 4).replace(' ', '_') + "-trim.tex";
        File trimmedFile = new File(folder.getPath() + File.separator + trimmedFileName);
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(texFile), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trimmedFile), "UTF-8"));
            String line;
            try {
                boolean flag = false;
                boolean titled = false;
                while ((line = reader.readLine()) != null) {
                    // process tikz library inputting
                    Matcher tikzlibraryMatcher = tikzlibararyPattern.matcher(line.trim());
                    boolean isTikzFound = tikzlibraryMatcher.find();
                    if (isTikzFound) {
                        String libraries = tikzlibraryMatcher.group(1);
                        for (String library : libraries.split(",")) {
                            tikzLibraries.add(library.trim());
                        }
                    }
                    if (!titled && line.trim().startsWith("\\title")) {
                        titled = true;
                        writer.write(line.trim().replaceFirst("title", "chapter"));
                        writer.newLine();
                    }
                    if (flag && !line.trim().startsWith("\\end{document}") && !isTikzFound) {
                        writer.write(line);
                        writer.newLine();
                    }
                    if (line.trim().startsWith("\\maketitle")) flag = true;
                }
            } catch (IOException e) {
                log.printStackTrace(e);
            }
        } catch (IOException e) {
            log.printStackTrace(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.printStackTrace(e);
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.printStackTrace(e);
                }
            }
        }
    }

    /**
     * Tweak the trimmed tex file content.
     * First tweak the chapter title, remove the illegal characters.
     * Second tweak the section and subsection title (if needed), adding separator between every pair of adjacent characters.
     * Third tweak the {@code includegraphics} line. Ensure the existence of the figure intended to include,
     * calculate and correct the width of the figure. Print warnings if the figure file does not exist or the
     * figure file duplicates.
     *
     * @param trimmedFile current file
     */
    private void decorateTrimmedFile(File trimmedFile) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        StringBuilder content = new StringBuilder();
        StringBuilder chapterInfo = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(trimmedFile), "UTF-8"));
            String line;
            try {
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    // decorate the chapter line
                    Matcher chapterMatcher = chapterPattern.matcher(line);
                    if (chapterMatcher.find()) {
                        String title = chapterMatcher.group(2);
                        String newTitle = null;
                        int idx = title.lastIndexOf("\\,");
                        if (idx != -1) {
                            newTitle = title.substring(idx + 2);
                        } else {
                            idx = title.lastIndexOf("—");
                            if (idx != -1) {
                                newTitle = title.substring(idx + 1);
                            }
                        }
                        if (newTitle != null) {
                            newTitle = newTitle.trim();
                            StringBuilder newChapterSB = new StringBuilder();
                            newChapterSB.append(chapterMatcher.group(1));
                            if (newTitle.contains("\\\\")) {
                                newChapterSB.append("[").append(newTitle.replace("\\\\", "")).append("]");
                            }
                            newChapterSB.append("{").append(newTitle).append("}");
                            line = newChapterSB.toString();
                        } else {
                            log.println("INFO--title error (ignore this if title exists) at line " + lineNumber
                                    + " of file " + trimmedFile.getPath(), Logger.MEDIUM);
                        }
                        chapterInfo.append(line).append("\n").append("\\input{")
                                .append(modifyPath(headerFile.getAbsolutePath())).append("}\n");
                    } else {
                        // decorate section title
                        Matcher sectionMatcher = sectionPattern.matcher(line);
                        if (sectionMatcher.find()) {
                            line = sectionMatcher.group(1) + "{" + decorateSection(sectionMatcher.group(2)) + "}"
                                    + sectionMatcher.group(3);
                        }
                        // decorate subsection title
                        Matcher subsectionMatcher = subsectionPattern.matcher(line);
                        if (subsectionMatcher.find()) {
                            line = subsectionMatcher.group(1) + "{" + decorateSection(subsectionMatcher.group(2)) + "}"
                                    + subsectionMatcher.group(3);
                        }
                        // extract the figure filename and its width or height
                        Matcher filenameMatcher = filenamePattern.matcher(line);
                        if (filenameMatcher.find()) {
                            String picFilePath = filenameMatcher.group(5);
                            String postfix = filenameMatcher.group(6);
                            int braceCount = countEndingBraces(picFilePath);
                            if (braceCount != 0) {
                                postfix = picFilePath.substring(picFilePath.length() - braceCount) + postfix;
                                picFilePath = picFilePath.substring(0, picFilePath.length() - braceCount);
                            }
                            File picFile = new File(picFilePath);
                            List<File> picList = findByFileName(figureFolder, picFile.getName());
                            if (picList.size() != 1) {
                                if (picList.isEmpty())
                                    log.println(
                                            "WARNING--picture file not found: " + picFile.getName()
                                                    + " at line " + lineNumber + " of file " + trimmedFile.getPath());
                                else {
                                    log.println("WARNING--duplicated picture file: " + picFile.getName()
                                            + " at line " + lineNumber + " of file " + trimmedFile.getPath());
                                }
                                warningCount++;
                            } else {
                                StringBuilder newline = new StringBuilder();
                                File newPicFile = picList.get(0);
                                String newFilePath = modifyPath(newPicFile.getAbsolutePath());
                                String newSize;
                                Matcher sizeMatcher = sizePattern.matcher(newFilePath);
                                // get the size information for the picture file
                                if (sizeMatcher.find()) {
                                    // this file has size info
                                    newSize = getWidth(Integer.valueOf(sizeMatcher.group(1))) + "cm";
                                } else {
                                    // this file does not have size info
                                    log.println("WARNING--picture file: " + picFile.getName()
                                            + " does not have size info at line " + lineNumber
                                            + " of file " + trimmedFile.getPath());
                                    warningCount++;
                                    newSize = filenameMatcher.group(3);
                                }
                                newline.append(filenameMatcher.group(1)).append(newSize).append(filenameMatcher.group(4))
                                        .append(newFilePath).append(postfix);
                                line = newline.toString();
                            }
                        }
                        // process tikz library inputting
                        Matcher tikzlibraryMatcher = tikzlibararyPattern.matcher(line.trim());
                        boolean isTikzFound = tikzlibraryMatcher.find();
                        if (isTikzFound) {
                            String libraries = tikzlibraryMatcher.group(1);
                            for (String library : libraries.split(",")) {
                                tikzLibraries.add(library.trim());
                            }
                        }
                        if (!line.trim().startsWith("\\input") && !isTikzFound) {
                            content.append(line).append("\n");
                        }
                    }
                    lineNumber++;
                }
            } catch (IOException e) {
                log.printStackTrace(e);
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trimmedFile), "UTF-8"));
            content = chapterInfo.append(content);
            writer.write(content.toString());
        } catch (IOException e) {
            log.printStackTrace(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.printStackTrace(e);
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.printStackTrace(e);
                }
            }
        }
    }

    /**
     * Generate the main tex file using the map of part name and the corresponding list of tex files.
     * Inject the file names into the main file at certain position.
     *
     * @param trimmedTexMap trimmed files stored in a map by the folder it lies.
     */
    private void generateMainFile(Map<File, List<File>> trimmedTexMap) {
        // get the contents that is to injected
        StringBuilder injectContent = new StringBuilder();
        for (File folder : partFolders) {
            for (File trimmedTexFile : trimmedTexMap.get(folder)) {
                injectContent.append("\\input{").append(modifyPath(trimmedTexFile.getAbsolutePath()))
                        .append("}\n");
                log.println("File: " + trimmedTexFile.getPath() + " injected into main file.", Logger.HIGH);
            }
        }
        // get all the tikz libraries that will be used in sub-files
        StringBuilder usetikzlibrary = new StringBuilder();
        if (!tikzLibraries.isEmpty()) {
            String libraries = tikzLibraries.toString().replace("[", "").replace("]", "");
            usetikzlibrary.append("\\usetikzlibrary{").append(libraries).append("}");
        }
        BufferedReader reader = null;
        BufferedWriter writer = null;
        StringBuilder content = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(mainFile), "UTF-8"));
            String line;
            try {
                boolean injectionFlag = true;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("%!!!ContentEnd")) {
                        injectionFlag = true;
                    }
                    if (injectionFlag) { // other content in main file goes here
                        Matcher tikzLibraryMatcher = tikzlibararyPattern.matcher(line.trim());
                        if (!tikzLibraryMatcher.find())
                            content.append(line).append("\n"); // ignore the line "\\usetikzlibrary"
                        if (line.trim().startsWith("\\begin{document}")) {
                            content.append(usetikzlibrary).append("\n");
                        }
                    }
                    if (line.trim().startsWith("%!!!ContentStart")) {
                        content.append(injectContent);
                        injectionFlag = false;
                    }
                }
            } catch (IOException e) {
                log.printStackTrace(e);
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mainFile), "UTF-8"));
            writer.write(content.toString());
        } catch (IOException e1) {
            log.printStackTrace(e1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    log.printStackTrace(e1);
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    log.printStackTrace(e1);
                }
            }
        }
    }

    /**
     * Since the tex system do not accept a path with any spaces, this method intends to trim any path before the
     * main file's folder which will be replaced by "."
     *
     * @param path The full path may contains spaces.
     * @return the relative path to the position of the main file (which is guaranteed that will not contains any
     * spaces).
     */
    private String modifyPath(String path) {
        String pathPrefix = mainFile.getAbsolutePath().replace(mainFile.getName(), "");
        return path.replace(pathPrefix, "./").replace(" ", "_").replace("\\", "/");
    }
}
