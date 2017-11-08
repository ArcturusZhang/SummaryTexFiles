import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AsyFileArrange {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final Logger log;
    private File figureFolder;

    AsyFileArrange(File figureFolder) {
        this.figureFolder = figureFolder;
        this.log = Logger.getLog();
    }

    /**
     * Start the arrangement of asy files and the corresponding pdf files.
     */
    public void arrangeAsyFiles() {
        log.println("============================================Arrange start============================================");
        ArrayList<File> asyFileList = new ArrayList<>();
        ArrayList<File> folderList = new ArrayList<>();
        try {
            for (File file : figureFolder.listFiles()) {
                if (file.getName().endsWith(".asy")) asyFileList.add(file);
                if (file.isDirectory() && file.getName().matches("^size([\\d]+)$")) {
                    folderList.add(file);
                }
            }
            correctionFiles(folderList);
            moveAsyFiles(categorizeAsyFiles(asyFileList));
        } catch (NullPointerException e) {
            log.println("Figure folder: " + figureFolder.getName() + "does not exist.");
        }
    }

    void removeDuplicatedFilesByLastModified() {
        Map<String, List<File>> duplicated = getDuplicateFiles();
        for (String filename : duplicated.keySet()) {
            TreeMap<Long, List<File>> sorted = new TreeMap<>();
            for (File asyFile : duplicated.get(filename)) {
                if (sorted.containsKey(asyFile.lastModified())) {
                    sorted.get(asyFile.lastModified()).add(asyFile);
                } else {
                    List<File> list = new ArrayList<>();
                    list.add(asyFile);
                    sorted.put(asyFile.lastModified(), list);
                }
            }
            System.out.println(sorted);
            File preserved = sorted.get(sorted.lastKey()).get(0);
            for (List<File> list : sorted.values()) {
                for (File asyFile : list) {
                    if (asyFile == preserved) continue;
                    File pdfFile = new File(asyFile.getAbsolutePath().replace(".asy", ".pdf"));
                    if (asyFile.delete()) {
                        log.println("Duplicated file: " + asyFile.getPath() + " (last modified: "
                                + FORMAT.format(new Date(asyFile.lastModified())) + ") has been deleted.");
                    } else {
                        log.println("Sorry, an error occurred which causes the duplicated file: " + asyFile.getPath()
                                + "(last modified: " + FORMAT.format(new Date(asyFile.lastModified()))
                                + ") is not successfully deleted.");
                    }
                    if (pdfFile.exists() && pdfFile.delete()) {
                        log.println("PDF file associated: " + pdfFile.getPath() + " has been deleted.");
                    } else {
                        log.println("Sorry, an error occurred which causes the duplicated file: " + pdfFile.getPath()
                                + "is not successfully deleted or the pdf file does not exist.");
                    }
                }
            }
        }
    }

    /**
     * List duplicated asy files in log, return true if there are duplicated files, otherwise return false.
     *
     * @return true if there are duplicates, false if not.
     */
    boolean listDuplicateFiles() {
        Map<String, List<File>> duplicated = getDuplicateFiles();
        if (duplicated.isEmpty()) {
            log.println("No duplicated files.");
        } else {
            log.println("Duplicated files detected: ");
            for (String filename : duplicated.keySet()) {
                log.println("Filename: " + filename);
                int count = 1;
                for (File file : duplicated.get(filename)) {
                    log.println("\t" + count + ". " + file.getPath() + ", last modified date: "
                            + FORMAT.format(new Date(file.lastModified())));
                    count++;
                }
            }
        }
        return !duplicated.isEmpty();
    }

    private Map<String, List<File>> getDuplicateFiles() {
        Map<String, List<File>> duplicated = new HashMap<>();
        getDuplicateFilesCore(figureFolder, duplicated);
        Iterator<String> iterator = duplicated.keySet().iterator();
        while (iterator.hasNext()) {
            List<File> list = duplicated.get(iterator.next());
            if (list.size() <= 1) iterator.remove();
        }
        return duplicated;
    }

    private void getDuplicateFilesCore(File folder, Map<String, List<File>> duplicated) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory() && file.getName().matches("^size([\\d]+)$")) {
                getDuplicateFilesCore(file, duplicated);
            }
            // only process asy files.
            else if (file.getName().endsWith(".asy")) {
                if (duplicated.containsKey(file.getName())) {
                    duplicated.get(file.getName()).add(file);
                } else {
                    List<File> fileList = new ArrayList<>();
                    fileList.add(file);
                    duplicated.put(file.getName(), fileList);
                }
            }
        }
    }

    /**
     * Traversal all the folder with correct folder name, move every asy file in each of them to the right folder.
     *
     * @param folderList a list of asy file folders
     */
    private void correctionFiles(List<File> folderList) {
        for (File folder : folderList) {
            List<File> asyFileList = new ArrayList<>();
            for (File file : folder.listFiles()) {
                if (file.getName().endsWith(".asy")) asyFileList.add(file);
            }
            Map<String, List<File>> map = categorizeAsyFiles(asyFileList);
            if (map.size() >= 1) {
                moveAsyFiles(map);
            }
        }
    }

    /**
     * Move the asy files (and the pdf files with same file name) into the right folder, according to the map generated by
     * method {@code categorizeAsyFiles}.
     *
     * @param map a map of size and a list of asy file with certain size.
     */
    private void moveAsyFiles(Map<String, List<File>> map) {
        boolean flag;
        for (String size : map.keySet()) {
            File folder = new File(figureFolder.getPath() + File.separator + "size" + size);
            // if the folder does not exist, create it.
            if (!folder.exists()) {
                folder.mkdir();
            }
            // folder exists now
            for (File file : map.get(size)) {
                // move file to the corresponding folder
                String newPath = folder + File.separator;
                flag = file.renameTo(new File(newPath + file.getName()));
                if (flag) {
                    log.println("Moved file: " + file.getName() + " to: " + newPath, Logger.MEDIUM);
                } else {
                    log.println("Move file: " + file.getName() + " failed.", Logger.MEDIUM);
                }
                // move the pdf file (if exists) to the corresponding folder
                String filename = file.getPath();
                File pdfFile = new File(filename.substring(0, filename.length() - 3) + "pdf");
                if (pdfFile.exists()) {
                    flag = pdfFile.renameTo(new File(newPath + pdfFile.getName()));
                    if (flag) {
                        log.println("Moved file: " + pdfFile.getName() + " to: " + newPath, Logger.MEDIUM);
                    }
                }
            }
        }
    }

    /**
     * Create a map from size to a list of files of that size.
     *
     * @param asyFileList all the files need to categorize.
     * @return a categorized map
     */
    private Map<String, List<File>> categorizeAsyFiles(List<File> asyFileList) {
        Map<String, List<File>> map = new HashMap<>();
        BufferedReader reader = null;
        String lineContent;
        String key;
        for (File file : asyFileList) {
            try {
                reader = new BufferedReader(new FileReader(file));
                boolean sizeFound = false;
                while ((lineContent = reader.readLine()) != null) {
                    Pattern pattern = Pattern.compile("^size\\(([\\d]+)\\);$");
                    Matcher matcher = pattern.matcher(lineContent);
                    if (matcher.find()) {
                        sizeFound = true;
                        key = matcher.group(1);
                        if (!map.containsKey(key)) {
                            ArrayList<File> files = new ArrayList<>();
                            files.add(file);
                            map.put(key, files);
                        } else {
                            map.get(key).add(file);
                        }
                        break;
                    }
                }
                if (!sizeFound) {
                    log.println("Asy file: " + file.getPath() + " does not contains size information. This file has been ignored.");
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
            }
        }
        return map;
    }
}
