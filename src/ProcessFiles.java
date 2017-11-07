import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ProcessFiles implements Runnable {
    private final String headerFileName = "header.tex";
    private final String indexContentFilename = "indexcontent.tex";
    private File mainFile;
    private File figureFolder;
    private File partFolder;
    private File headerFile;
    private File indexContentFile;
    private List<File> partFolders;
    private SimpleTexProcessProgram mainWindow;
    private final Logger log;
    private Process process;

    ProcessFiles(String mainFilePath, String figFolderPath) {
        this.mainFile = new File(mainFilePath);
        this.figureFolder = new File(figFolderPath);
        this.mainWindow = SimpleTexProcessProgram.mainWindow;
        this.log = Logger.getLog();
        log.setLogField(mainWindow.getLogField());
        initialize();
    }

    /**
     * This method initialize some of the constant settings, such as folder name.
     * These info should be changed into program settings, other than writing here directly in the code.
     */
    private void initialize() {
        partFolder = new File(mainFile.getPath().replace(mainFile.getName(), "parts"));
        headerFile = new File(partFolder.getPath() + File.separator + headerFileName);
        partFolders = new ArrayList<>();
        partFolders.add(new File(partFolder.getPath() + File.separator + "Differential-01"));
        partFolders.add(new File(partFolder.getPath() + File.separator + "Integral-02"));
        partFolders.add(new File(partFolder.getPath() + File.separator + "Series-03"));
        partFolders.add(new File(partFolder.getPath() + File.separator + "UnCategorized"));
        indexContentFile = new File(partFolder.getPath() + File.separator + indexContentFilename);
    }

    /**
     * Ensure the existence of important files. If one of them do not exist, terminate the whole process.
     *
     * @return {@code true} if all exist, otherwise {@code false}
     */
    private boolean ensureExistence() {
        boolean flag = true;
        if (!headerFile.exists()) {
            log.println("Header file: " + headerFile.getPath() + " does not exist.");
            flag = false;
        }
        if (!mainFile.exists()) {
            log.println("Main tex file: " + mainFile.getPath() + " does not exist.");
            flag = false;
        }
        if (!partFolder.exists()) {
            log.println("Part folder: " + partFolder.getPath() + " does not exist.");
            flag = false;
        }
        if (!figureFolder.exists()) {
            log.println("Figure folder: " + figureFolder.getPath() + " does not exist.");
            flag = false;
        }
        return flag;
    }

    /**
     * Stop the process of compilation of the main tex file.
     */
    public void destroyTexCompileProcess() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    @Override
    public void run() {
        if (ensureExistence()) {
            mainWindow.lockComponents();
            Logger.setLogLevel(Logger.LOW);
            if (mainWindow.getAsySortCheckBox().isSelected()) {
                AsyFileArrange asyFileArrange = new AsyFileArrange(figureFolder);
                asyFileArrange.arrangeAsyFiles();
            }
            ArrayList<File> inputRawTexFiles = getInputFiles();
            TexProcess texProcess = new TexProcess(inputRawTexFiles, mainFile, figureFolder, headerFile, partFolders);
            texProcess.process();
            int result = JOptionPane.showConfirmDialog(mainWindow.getMainFrame(),
                    "合并已完成，是否编译文件" + mainFile.getName() + "?", "合并完成", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                deleteTempFiles();
                compileMainFile();
                makeIndex();
                generateIndexContent();
                compileMainFile();
                JOptionPane.showMessageDialog(mainWindow.getMainFrame(), "已全部完成。", "已完成",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            mainWindow.unlockComponents();
        }
    }

    private void deleteTempFiles() {
        File currentFolder = new File(mainFile.getAbsolutePath().replace(mainFile.getName(), ""));
        String mainFileNameWithoutExtension = mainFile.getName().replace(".tex", "");
        for (File file : currentFolder.listFiles()) {
            if (file.isFile() && file.getName().startsWith(mainFileNameWithoutExtension) && !file.getName().endsWith(".tex")) {
                if (file.delete()) {
                    log.println("Delete file: " + file.getName());
                }
            }
        }
    }

    /**
     * Compile the main tex file. Execute the command by {@code Runtime.getRuntime().exec(String)}, and use
     * {@code Process.getInputStream} to get the information the executed command print to the command line.
     */
    private void compileMainFile() {
        log.println("============================================Compile start============================================");
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec("xelatex " + mainFile.getPath());
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.println(line);
            }
            process.waitFor();
        } catch (IOException e) {
            log.printStackTrace(e);
        } catch (InterruptedException e) {
            log.println("Compile has been terminated.");
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

    /**
     * Execute {@code makeindex} program to generate the index information of the main tex file.
     */
    private void makeIndex() {
        log.println("============================================Makeindex start============================================");
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime()
                    .exec("makeindex " + mainFile.getPath().replace(".tex", ".idx"));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.println(line);
            }
            process.waitFor();
        } catch (IOException e) {
            log.printStackTrace(e);
        } catch (InterruptedException e) {
            log.println("Makeindex has been terminated.");
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

    /**
     * Generate index file content using the output of {@code makeindex} program.
     */
    private void generateIndexContent() {
        log.println("============================================Generating index file============================================");
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(mainFile.getPath().replace(".tex", ".ind")), "UTF-8"
            ));
            StringBuilder indexContent = new StringBuilder();
            String line;
            boolean indexBeginFlag = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("\\end{theindex}")) indexBeginFlag = false;
                if (indexBeginFlag) {
                    indexContent.append(line).append("\n");
                }
                if (line.trim().startsWith("\\begin{theindex}")) indexBeginFlag = true;
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexContentFile), "UTF-8"));
            writer.write(indexContent.toString());
        } catch (FileNotFoundException e) {
            log.println("Index file " + mainFile.getPath().replace(".tex", ".ind") + "not found.");
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
                log.println("WARNING--tex file not found: " + file.getName(), Logger.LOW);
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
                        return matcher1.group(2).compareTo(matcher2.group(2));
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
        mainWindow.getListModel().removeAllElements();
        return inputRawTexFiles;
    }
}