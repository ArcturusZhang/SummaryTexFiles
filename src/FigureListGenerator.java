import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FigureListGenerator implements Runnable {
    private static final Pattern CONTAINS_CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final String FILE_HEAD = "\\documentclass{ctexart}\n" + "\\begin{document}\n";
    private static final String FILE_TAIL = "\\end{document}";
//    private static final String FIGURE_HEAD = "\\begin{figure}[htbp!]\n" + "\\centering\n" + "\\includegraphics{";
    private static final String FIGURE_HEAD = "\\begin{figure}\n" + "\\centering\n" + "\\includegraphics{";
    private static final String FIGURE_BODY = "}\n" + "\\caption{";
    private static final String FIGURE_TAIL = "}\n" + "\\end{figure}\n";
    private static final String CLEAR_PAGE = "\\clearpage\n";
    private File figureListFile;
    private File figureFolder;
    private Logger log;
    private boolean isChineseFilenameExcluded = true;
    private final int clearPageCount = 20;
    private SimpleTexProcessProgram mainWindow;
    private boolean needArrange;
    private boolean deleteDuplicated;

    FigureListGenerator(File figureListFile, File figureFolder, boolean needArrange, boolean deleteDuplicated) {
        this.figureListFile = figureListFile;
        this.figureFolder = figureFolder;
        this.needArrange = needArrange;
        this.deleteDuplicated = deleteDuplicated;
        this.log = Logger.getLog();
        this.mainWindow = SimpleTexProcessProgram.mainWindow;
    }

    FigureListGenerator(String figureListFilePath, String figFolderPath, boolean needArrange, boolean deleteDuplicated) {
        this(new File(figureListFilePath), new File(figFolderPath), needArrange, deleteDuplicated);
    }

    FigureListGenerator(String figureListFilePath, String figFolderPath) {
        this(new File(figureListFilePath), new File(figFolderPath), true, true);
    }

    public void generate() {
        if (needArrange) {
            AsyFileArrange arrange = new AsyFileArrange(figureFolder);
            if (deleteDuplicated) arrange.removeDuplicatedFilesByLastModified();
            arrange.arrangeAsyFiles();
        }
        log.println("============================================Generate start============================================");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(figureListFile), "UTF-8"));
            writer.write(generateFileContent());
        } catch (UnsupportedEncodingException e) {
            log.printStackTrace(e);
        } catch (FileNotFoundException e) {
            log.printStackTrace(e);
        } catch (IOException e) {
            log.printStackTrace(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.printStackTrace(e);
                }
            }
        }
    }

    private String generateFileContent() {
        StringBuilder content = new StringBuilder();
        content.append(FILE_HEAD);
        // get list of all figures
        List<File> figureFileList = new ArrayList<>();
        getListOfAllFigures(figureFolder, figureFileList);
        int count = 0;
        for (File figure : figureFileList) {
            String figurePath = modifyPath(figure.getAbsolutePath()).replace(".asy", ".pdf");
            String pathAsCaption = figurePath.replace("_", "\\_");
            content.append(FIGURE_HEAD).append(figurePath).append(FIGURE_BODY)
                    .append(pathAsCaption).append(FIGURE_TAIL);
            if (count % clearPageCount == 0) content.append(CLEAR_PAGE);
            count++;
        }
        // generate file content
        content.append(FILE_TAIL);
        return content.toString();
    }

    private void getListOfAllFigures(File figureFolder, List<File> figureFileList) {
        for (File file : figureFolder.listFiles()) {
            if (isLegalFileName(file)) {
                if (file.isDirectory()) {
                    getListOfAllFigures(file, figureFileList);
                } else {
                    // if file is a file
                    figureFileList.add(file);
                }
            }
        }
    }

    private boolean isLegalFileName(File file) {
        if (file.isDirectory()) {
            if (isChineseFilenameExcluded) {
                Matcher matcher = CONTAINS_CHINESE_PATTERN.matcher(file.getName());
                return !matcher.find();
            }
            return true;
        } else {
            if (file.getName().endsWith(".asy")) {
                if (isChineseFilenameExcluded) {
                    Matcher matcher = CONTAINS_CHINESE_PATTERN.matcher(file.getName());
                    return !matcher.find();
                } else return true;
            }
        }
        return false;
    }

    private String modifyPath(String path) {
        String pathPrefix = figureListFile.getAbsolutePath().replace(figureListFile.getName(), "");
        return path.replace(pathPrefix, "./").replace(" ", "_").replace("\\", "/");
    }

    @Override
    public void run() {
        mainWindow.lockComponents();
        generate();
//        int result = JOptionPane.showConfirmDialog(mainWindow.getMainFrame(),
//                "合并已完成，是否编译文件" + mainFile.getName() + "?", "合并完成", JOptionPane.YES_NO_OPTION);
        JOptionPane.showMessageDialog(mainWindow.getMainFrame(), "已生成图片列表文件。", "已生成",
                JOptionPane.INFORMATION_MESSAGE);
        mainWindow.unlockComponents();
    }
}
