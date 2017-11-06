import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ALL")
class AsyFileArrange {
    private File figureFolder;
    private final Logger log;

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
            //noinspection ConstantConditions
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

    /**
     * Traversal all the folder with correct folder name, move every asy file in each of them to the right folder.
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
