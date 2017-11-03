import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

public class ProcessFiles implements Runnable {
    private final static String ORIGINAL_FOLDER_NAME = "originalTexes";
    private static int warningCount = 0;
    private String headerPath = "." + File.separator + "parts" + File.separator + "header.tex";
    private String mainFilePath;
    private String figFolderPath;
    private SimpleTexProcessProgram mainWindow;

    ProcessFiles(String mainFilePath, String figFolderPath) {
        this.mainFilePath = mainFilePath;
        this.figFolderPath = figFolderPath;
        this.mainWindow = SimpleTexProcessProgram.mainWindow;
    }

    @Override
    public void run() {
        mainWindow.lockComponents();
        File mainFile = new File(mainFilePath);
        File figureFolder = new File(figFolderPath);
        File headerFile = new File(headerPath);
        if (mainWindow.getAsySortCheckBox().isSelected()) {
            AsyFileArrange asyFileArrange = new AsyFileArrange(figureFolder, mainWindow.getLogField());
            asyFileArrange.arrangeAsyFiles();
        }
        TexProcess texProcess = new TexProcess(mainFile, figureFolder, headerFile, mainWindow.getLogField(), Logger.LOW);
        texProcess.processTexFiles();
        mainWindow.unlockComponents();
        JOptionPane.showMessageDialog(mainWindow.getMainFrame(), "合并完成。", "合并完成", JOptionPane.INFORMATION_MESSAGE);
    }

    private ArrayList<File> getInputFiles() {
        ArrayList<File> inputRawTexFiles = new ArrayList<>();
        // TODO --
        return inputRawTexFiles;
    }
}
