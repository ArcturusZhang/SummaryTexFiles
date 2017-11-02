import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TexProcess implements Runnable {
    private final static String ORIGINAL_FOLDER_NAME = "originalTexes";
    private static int warningCount = 0;
    private static String headerPath = "." + File.separator + "parts" + File.separator + "header.tex";
    private String mainFileName;
    private MainWindow mainWindow;

    TexProcess(String mainFileName) {
        this.mainFileName = mainFileName;
        this.mainWindow = MainWindow.mainWindow;
    }

    /**
     * Decorate a String of certain length.
     *
     * @param string string need decorate
     * @return decorated string.
     */
    private static String decorateSection(String string) {
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
    private static int countEndingBraces(String path) {
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
    private static List<File> findByFileName(File folder, String filename) {
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
    private static String getWidth(int size) {
        double width = 13.0 / 500 * size;
        return String.format("%.2f", width);
    }

    /**
     * Returns a list of raw tex files in the given folder.
     * A raw tex files is a file with the extension name of "tex", and whose filename does not ended with "-trim".
     *
     * @param folder
     * @return
     */
    private static List<File> getTexFilesInFolder(File folder) {
        List<File> files = new ArrayList<>();
        for (File texFile : folder.listFiles()) {
            if (texFile.getName().endsWith(".tex") && !texFile.getName().endsWith("-trim.tex")) {
                files.add(texFile);
            }
        }
        return files;
    }

    /**
     * Returns a sorted list of trimmed tex files in the given folder.
     * A trimmed tex file is a file whose full name ended with "-trim.tex".
     * The list is sorted by the ordinal number which is contained in the filename.
     *
     * @param folder
     * @return
     */
    private static List<File> getTrimmedTexFileInFolder(File folder) {
        List<File> files = new ArrayList<>();
        for (File trimmedFile : folder.listFiles()) {
            if (trimmedFile.getName().endsWith("-trim.tex")) {
                files.add(trimmedFile);
            }
        }
        files.sort((File file1, File file2) -> {
            Pattern pattern = Pattern.compile("^\\S*-(\\d{2})-\\S*");
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
     * Copy the content within "\begin{document}" and "\end{document}" in a raw tex file to the trimmed tex file.
     * The title in raw tex files is renamed to chapter in trimmed tex files.
     *
     * @param texFile
     * @return
     */
    private static File processTexFile(File texFile) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        String path = texFile.getPath();
        File trimmedFile = new File(path.substring(0, path.length() - 4).replace(' ', '_') + "-trim.tex");
        File originalFolder = new File(texFile.getParent() + File.separator + ORIGINAL_FOLDER_NAME);
        if (!originalFolder.exists()) originalFolder.mkdir();
        File originalFile = new File(originalFolder.getPath() + File.separator + texFile.getName());
        boolean processComplete = false;
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
            processComplete = true;
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
        if (processComplete) {
            if (originalFile.exists()) originalFile.delete();
            texFile.renameTo(originalFile);
        }
        return trimmedFile;
    }

    @Override
    public void run() {
        mainWindow.getConfirmButton().setEnabled(false);
        if (mainWindow.getAsySortCheckBox().isSelected()) {
            mainWindow.getLogField().append("======================Arrange start======================\n");
            // TODO -- Arrange asy files here
        }
        mainWindow.getLogField().append("======================Merge start======================\n");
        File mainFile = new File(mainFileName);
        File currentFolder = new File(mainFile.getAbsolutePath().replace(mainFileName, "parts"));
        for (File file : currentFolder.listFiles()) {
            if (file.isDirectory()) {
                Pattern pattern = Pattern.compile("^\\S*(\\d{2})\\S*$");
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.find()) {
                    for (File texFile : getTexFilesInFolder(file)) {
                        processTexFile(texFile);
                    }
                }
            }
        }
        // sort the trimmed tex file
        TreeMap<String, List<File>> map = new TreeMap<>();
        for (File file : currentFolder.listFiles()) {
            if (file.isDirectory()) {
                Pattern pattern = Pattern.compile("^\\S*(\\d{2})\\S*$");
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.find()) {
                    String key = matcher.group(1);
                    map.put(key, getTrimmedTexFileInFolder(file));
                }
            }
        }
        // decorating the trimmed file
        for (String key : map.keySet()) {
            for (File trimmedFile : map.get(key)) {
                decorateTrimmedFile(trimmedFile);
            }
        }
        generateMainFile(mainFile, map);
        mainWindow.getLogField().append("All done");
        if (warningCount != 0) {
            mainWindow.getLogField().append(" with " + warningCount + " warning(s)");
        } else {
            mainWindow.getLogField().append(" without warnings");
        }
        mainWindow.getLogField().append(". \n");
        mainWindow.getConfirmButton().setEnabled(true);
        JOptionPane.showMessageDialog(mainWindow.getFrame(), "合并完成。", "合并完成", JOptionPane.INFORMATION_MESSAGE);
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
        File headerFile = new File(headerPath);
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
                File figureFolder = new File("." + File.separator + "fig");
                boolean flag = false;
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
                            mainWindow.getLogField().append("INFO--title error (ignore this if title exists) at line " + lineNumber
                                    + " of file " + trimmedFile.getPath() + "\n");
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
                                    mainWindow.getLogField().append("WARNING--picture file not found: " + picFile.getName() + " at line " + lineNumber + " of file " + trimmedFile.getPath() + "\n");
                                else {
                                    mainWindow.getLogField().append("WARNING--duplicated picture file: " + picFile.getName() + " at line " + lineNumber + " of file " + trimmedFile.getPath() + "\n");
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
                                    mainWindow.getLogField().append("WARNING--picture file: " + picFile.getName() + " does not have size info at line " + lineNumber + " of file " + trimmedFile.getPath() + "\n");
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
     *
     * @param mainFile
     * @param map
     */
    private void generateMainFile(File mainFile, TreeMap<String, List<File>> map) {
        StringBuilder injectContent = new StringBuilder();
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            for (File texFile : map.get(iterator.next())) {
                injectContent.append("\\input{").append(texFile.getPath().replace('\\', '/')).append("}\n");
                mainWindow.getLogField().append("File: " + texFile.getPath() + " injected into main file.\n");
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
}
