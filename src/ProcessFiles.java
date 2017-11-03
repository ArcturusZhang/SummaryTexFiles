import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessFiles implements Runnable {
    private final static String ORIGINAL_FOLDER_NAME = "originalTexes";
    private static int warningCount = 0;
    private String headerPath = "." + File.separator + "parts" + File.separator + "header.tex";
    private String mainFilePath;
    private String figFolderPath;
    private SimpleTexProcessProgram mainWindow;
    private Logger log;

    ProcessFiles(String mainFilePath, String figFolderPath) {
        this.mainFilePath = mainFilePath;
        this.figFolderPath = figFolderPath;
        this.mainWindow = SimpleTexProcessProgram.mainWindow;
        this.log = Logger.getLog(mainWindow.getLogField());
    }

    @Override
    public void run() {
        ArrayList<File> inputRawTexFiles = getInputFiles();
        mainWindow.lockComponents();
        File mainFile = new File(mainFilePath);
        File figureFolder = new File(figFolderPath);
        File headerFile = new File(headerPath);
        if (mainWindow.getAsySortCheckBox().isSelected()) {
            AsyFileArrange asyFileArrange = new AsyFileArrange(figureFolder, mainWindow.getLogField());
            asyFileArrange.arrangeAsyFiles();
        }
        TexProcess texProcess = new TexProcess(mainFile, figureFolder, headerFile, inputRawTexFiles, mainWindow.getLogField(), Logger.LOW);
//        texProcess.processTexFiles();
        texProcess.process();
        mainWindow.unlockComponents();
//        JOptionPane.showMessageDialog(mainWindow.getMainFrame(), "合并完成。", "合并完成", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Extract a list of files selected.
     *
     * @return a list of tex files.
     */
    private ArrayList<File> getInputFiles() {
        ArrayList<File> inputRawTexFiles = new ArrayList<>();
        Enumeration<String> enumeration = mainWindow.getListModel().elements();
        Pattern pattern = Pattern.compile("^(Differential|Integral|Series)\\S*(\\d{2})");
        while (enumeration.hasMoreElements()) {
            File file = new File(enumeration.nextElement());
            if (!file.exists()) {
                log.println("WARNING--tex file not found: " + file.getName());
            } else {
                if (!mainWindow.getIgnoreWrongFilenameCheckBox().isSelected()) {
                    Matcher matcher = pattern.matcher(file.getName());
                    if (matcher.find() && !file.getName().endsWith("-trim.tex")) {
                        inputRawTexFiles.add(file);
                    }
                } else {
                    if (!file.getName().endsWith("-trim.tex")) inputRawTexFiles.add(file);
                }
            }
        }
        if (!inputRawTexFiles.isEmpty() && mainWindow.getAsySortCheckBox().isSelected()) {
            Comparator<File> fileComparator = (File file1, File file2) -> {
                char c1 = file1.getName().charAt(0);
                char c2 = file2.getName().charAt(0);
                if (c1 != c2) return c1 - c2;
                else {
                    Matcher matcher1 = pattern.matcher(file1.getName());
                    Matcher matcher2 = pattern.matcher(file2.getName());
                    if (matcher1.find() && matcher2.find()) {
                        int result = matcher1.group(2).compareTo(matcher2.group(2));
                        return result;
                    }
                    return file1.getName().compareTo(file2.getName());
                }
            };
            inputRawTexFiles.sort(fileComparator);
            // remove duplicated files
            Iterator<File> iterator = inputRawTexFiles.iterator();
            File last = iterator.next();
            while (iterator.hasNext()) {
                File current = iterator.next();
                if (fileComparator.compare(last, current) == 0) {
                    iterator.remove();
                } else {
                    last = current;
                }
            }
        }
        return inputRawTexFiles;
    }
}