import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class AsySortingOld {
    public static void main(String[] args) {
        String filePath;
        if (args.length == 0) {
            filePath = ".";
        } else {
            filePath = args[0];
        }
        File current = new File(filePath);
        if (!current.isDirectory()) {
            System.out.println("Invalid input.");
            System.exit(0);
        }
        ArrayList<File> asyFileList = new ArrayList<>();
        ArrayList<File> folderList = new ArrayList<>();
        for (File file : current.listFiles()) {
            if (file.getName().endsWith(".asy")) asyFileList.add(file);
            if (file.isDirectory() && file.getName().matches("^size([\\d]+)$")) {
                folderList.add(file);
            }
        }
        correctionFiles(folderList);
        sortAsyFiles(sortFiles(asyFileList));
    }

    private static void correctionFiles(ArrayList<File> folderList) {
        for (File folder : folderList) {
            HashMap<String, ArrayList<File>> map = sortFiles(new ArrayList<>(Arrays.asList(folder.listFiles())));
            if (map.size() >= 1) {
                sortAsyFiles(map);
            }
        }
    }

    private static void sortAsyFiles(HashMap<String, ArrayList<File>> map) {
        boolean flag;
        for (String size : map.keySet()) {
            File folder = new File("." + File.separator + "size" + size);
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
                    System.out.println("Moved file: " + file.getName() + " to: " + newPath);
                } else {
                    System.out.println("Move file: " + file.getName() + " failed.");
                }
                // move the pdf file (if exists) to the corresponding folder
                String filename = file.getPath();
                File pdfFile = new File(filename.substring(0, filename.length() - 3) + "pdf");
                if (pdfFile.exists()) {
                    flag = pdfFile.renameTo(new File(newPath + pdfFile.getName()));
                    if (flag) {
                        System.out.println("Moved file: " + pdfFile.getName() + " to: " + newPath);
                    }
                }
            }
        }
    }

    private static HashMap<String, ArrayList<File>> sortFiles(ArrayList<File> asyFileList) {
        HashMap<String, ArrayList<File>> map = new HashMap<>();
        BufferedReader reader = null;
        String lineContent;
        String key;
        for (File file : asyFileList) {
            try {
                reader = new BufferedReader(new FileReader(file));
                while ((lineContent = reader.readLine()) != null) {
                    Pattern pattern = Pattern.compile("^size\\(([\\d]+)\\);$");
                    Matcher matcher = pattern.matcher(lineContent);
                    if (matcher.find()) {
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
            }
        }
        return map;
    }
}
