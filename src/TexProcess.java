import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TexProcess {
    private final static String ORIGINAL_FOLDER_NAME = "originalTexes";
    private static int warningCount = 0;

    public static void main(String[] args) {
        String mainFileName;
        if (args.length == 0) {
            mainFileName = "Calculus_lecture_HighDimension.tex";
        } else {
            mainFileName = args[0];
        }
        File currentFolder = new File("./parts");
        File mainFile = new File(mainFileName);
        // Trim raw tex files
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
        // sort the trimed tex file
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
        // decorating the trimed file
        for (String key : map.keySet()) {
            for (File trimedFile : map.get(key)) {
                decorateTrimmedFile(trimedFile);
            }
        }
        generateMainFile(mainFile, map);
        System.out.print("All done");
        if (warningCount != 0) {
            System.out.print(" with " + warningCount + " warning(s)");
        } else {
            System.out.print(" without warnings");
        }
        System.out.println(". ");
    }

    private static void decorateTrimmedFile(File trimmedFile) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        StringBuilder content = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(trimmedFile), "UTF-8"));
            String line;
            try {
                Pattern filenamePattern = Pattern.compile("^(\\s*\\\\includegraphics\\[(width|height)\\s*=\\s*)(\\S+)(\\]\\{)(\\S+)(\\}\\S*\\s*)$");
                Pattern sizePattern = Pattern.compile("/size(\\d+)/");
                Pattern chapterPattern = Pattern.compile("^(\\s*\\\\chapter)\\{([\\s\\S]+)\\}(\\s*)$");
                int lineNumber = 1;
                File figureFolder = new File("." + File.separator + "fig");
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
                            System.out.println("INFO--title error (ignore this if title existed) at line " + lineNumber + " of file " + trimmedFile.getPath());
                        }
                    } else {
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
                                    System.out.println("WARNING--picture file not found: " + picFile.getName() + " at line " + lineNumber + " of file " + trimmedFile.getPath());
                                else {
                                    System.out.println("WARNING--duplicated picture file: " + picFile.getName() + " at line " + lineNumber + " of file " + trimmedFile.getPath());
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
                                    System.out.println("WARNING--picture file: " + picFile.getName() + " does not have size info at line " + lineNumber + " of file " + trimmedFile.getPath());
                                    warningCount++;
                                    newSize = filenameMatcher.group(3);
                                }
                                newline.append(filenameMatcher.group(1)).append(newSize).append(filenameMatcher.group(4))
                                        .append(newFilePath).append(postfix);
                                line = newline.toString();
                            }
                        }
                    }
                    content.append(line).append("\n");
                    lineNumber++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trimmedFile), "UTF-8"));
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

    private static int countEndingBraces(String path) {
        int pos = path.length() - 1;
        int braceCount = 0;
        while (path.charAt(pos) == '}') {
            braceCount++;
            pos--;
        }
        return braceCount;
    }

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

    private static String getWidth(int size) {
        double width = 13.0 / 500 * size;
        return String.format("%.2f", width);
    }

    private static void generateMainFile(File mainFile, TreeMap<String, List<File>> map) {
        StringBuilder injectContent = new StringBuilder();
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            for (File texFile : map.get(iterator.next())) {
                injectContent.append("\\input{").append(texFile.getPath().replace('\\', '/')).append("}\n");
                System.out.println("File: " + texFile.getPath() + " injected into main file.");
            }
        }
        BufferedReader reader = null;
        BufferedWriter writer = null;
        StringBuilder content = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(mainFile), "UTF-8"));
            String line;
            try {
                boolean flag = true;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("%!!!ContentEnd")) {
                        flag = true;
                    }
                    if (flag) content.append(line).append("\n");
                    if (line.trim().startsWith("%!!!ContentStart")) {
                        content.append(injectContent);
                        flag = false;
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

    private static List<File> getTexFilesInFolder(File folder) {
        List<File> files = new ArrayList<>();
        for (File texFile : folder.listFiles()) {
            if (texFile.getName().endsWith(".tex") && !texFile.getName().endsWith("-trim.tex")) {
                files.add(texFile);
            }
        }
        return files;
    }

    private static List<File> getTrimmedTexFileInFolder(File folder) {
        List<File> files = new ArrayList<>();
        for (File trimedFile : folder.listFiles()) {
            if (trimedFile.getName().endsWith("-trim.tex")) {
                files.add(trimedFile);
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

    private static File processTexFile(File texFile) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        String path = texFile.getPath();
        File trimedFile = new File(path.substring(0, path.length() - 4).replace(' ', '_') + "-trim.tex");
        File originalFolder = new File(texFile.getParent() + File.separator + ORIGINAL_FOLDER_NAME);
        if (!originalFolder.exists()) originalFolder.mkdir();
        File originalFile = new File(originalFolder.getPath() + File.separator + texFile.getName());
        boolean processComplete = false;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(texFile), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trimedFile), "UTF-8"));
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
        return trimedFile;
    }
}
