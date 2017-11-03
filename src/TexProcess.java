import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TexProcess {
    private File mainFile;
    private File figureFolder;
    private File partFolder;
    private Logger log;
    private File headerFile;
    private int warningCount = 0;
    private int logLevel;
    private ArrayList<File> inputRawTexFiles;
    private ArrayList<File> partFolders;

    TexProcess(File mainFile, File figureFolder, File headerFile, JTextArea logField, int logLevel) {
        this.mainFile = mainFile;
        this.figureFolder = figureFolder;
        log = Logger.getLog(logField);
        this.logLevel = logLevel;
        this.headerFile = headerFile;
    }

    TexProcess(File mainFile, File figureFolder, File headerFile, ArrayList<File> inputRawTexFiles, JTextArea logField, int logLevel) {
        this.mainFile = mainFile;
        this.figureFolder = figureFolder;
        this.inputRawTexFiles = inputRawTexFiles;
        log = Logger.getLog(logField);
        this.logLevel = logLevel;
        this.headerFile = headerFile;
        partFolder = new File(mainFile.getAbsolutePath().replace(mainFile.getName(), "parts"));
        partFolders = new ArrayList<>();
        partFolders.add(new File(partFolder.getAbsolutePath() + File.separator + "Differential-01"));
        partFolders.add(new File(partFolder.getAbsolutePath() + File.separator + "Integral-02"));
        partFolders.add(new File(partFolder.getAbsolutePath() + File.separator + "Series-03"));
        partFolders.add(new File(partFolder.getAbsolutePath() + File.separator + "UnCategorized"));
        for (File folder : partFolders) {
            if (!folder.exists()) folder.mkdir();
        }
    }

    TexProcess(File mainFile, File figureFolder, JTextArea logField) {
        this(mainFile, figureFolder, new File("." + File.separator + "parts" + File.separator + "header.tex"),
                logField, Logger.MEDIUM);
    }

    public void process() {
        log.println("============================================Merge start============================================");
        // categorize input files by their prefix
        HashMap<File, List<File>> rawTexMap = categorizeRawTexFiles(inputRawTexFiles);
        for (File folder : rawTexMap.keySet()) {
            for (File texFile : rawTexMap.get(folder)) {
                processTexFile(folder, texFile);
            }
        }
        // decorate trimmed files
        HashMap<File, List<File>> trimmedTexMap = new HashMap<>();
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

    private HashMap<File, List<File>> categorizeRawTexFiles(ArrayList<File> inputRawTexFiles) {
        HashMap<File, List<File>> map = new HashMap<>();
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
     * @param path
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
    private List<File> findByFileName(File folder, String filename) {
        List<File> foundList = new ArrayList<>();
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) foundList.addAll(findByFileName(file, filename));
            else {
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
     * @param size
     * @return
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
     * @param folder
     * @return
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

    private File processTexFile(File folder, File texFile) {
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
                    if (!titled && line.trim().startsWith("\\title")) {
                        titled = true;
                        writer.write(line.trim().replaceFirst("title", "chapter"));
                        writer.newLine();
                    }
                    if (flag && !line.trim().startsWith("\\end{document}")) {
                        writer.write(line);
                        writer.newLine();
                    }
                    if (line.trim().startsWith("\\maketitle")) flag = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return trimmedFile;
    }

    /**
     * Tweak the trimmed tex file content.
     * First tweak the chapter title, remove the illegal characters.
     * Second tweak the section and subsection title (if needed), adding separator between every pair of adjacent characters.
     * Third tweak the includegraphics line. Ensure the existence of the figure intended to include, calculate and correct
     * the width of the figure. Print warnings if the figure file does not exist or the figure file duplicates.
     *
     * @param trimmedFile
     */
    private void decorateTrimmedFile(File trimmedFile) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        StringBuilder content = new StringBuilder();
        StringBuilder chapterInfo = new StringBuilder();
        if (!headerFile.exists()) throw new RuntimeException("Need header file:" + headerFile.getPath());
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(trimmedFile), "UTF-8"));
            String line;
            try {
                Pattern filenamePattern = Pattern.compile("^(\\s*\\\\includegraphics\\[(width|height)\\s*=\\s*)(\\S+)(\\]\\{)(\\S+)(\\}\\S*\\s*)$");
                Pattern sizePattern = Pattern.compile("/size(\\d+)/");
                Pattern chapterPattern = Pattern.compile("^(\\s*\\\\chapter)\\{([\\s\\S]+)\\}(\\s*)$");
                Pattern sectionPattern = Pattern.compile("^(\\s*\\\\section)\\{(\\W+)\\}(\\s*)$");
                Pattern subsectionPattern = Pattern.compile("^(\\s*\\\\subsection)\\{(\\W+)\\}(\\s*)$");
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
                            if (logLevel >= Logger.MEDIUM)
                                log.println("INFO--title error (ignore this if title exists) at line " + lineNumber
                                        + " of file " + trimmedFile.getPath());
                        }
                        chapterInfo.append(line).append("\n").append("\\input{")
                                .append(headerFile.getPath().replace("\\", "/")).append("}\n");
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
                                String newFilePath = newPicFile.getPath().replace('\\', '/');
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
                        if (!line.trim().startsWith("\\input")) {
                            content.append(line).append("\n");
                        }
                    }
                    lineNumber++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trimmedFile), "UTF-8"));
            content = chapterInfo.append(content);
            writer.write(content.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Generate the main tex file using the map of part name and the corresponding list of tex files.
     * Inject the file names into the main file at certain position.
     * @param trimmedTexMap
     */
    private void generateMainFile(HashMap<File, List<File>> trimmedTexMap) {
        StringBuilder injectContent = new StringBuilder();
        for (File folder : partFolders) {
            for (File trimmedTexFile : trimmedTexMap.get(folder)) {
                injectContent.append("\\input{").append(trimmedTexFile.getPath().replace('\\', '/'))
                        .append("}\n");
                if (logLevel >= Logger.HIGH) log.println("File: " + trimmedTexFile.getPath() + " injected into main file.");
            }
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
                    if (injectionFlag) content.append(line).append("\n");
                    if (line.trim().startsWith("%!!!ContentStart")) {
                        content.append(injectContent);
                        injectionFlag = false;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mainFile), "UTF-8"));
            writer.write(content.toString());
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
