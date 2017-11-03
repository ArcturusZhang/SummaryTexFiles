import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class SimpleTexProcessProgram {
    static SimpleTexProcessProgram mainWindow;
    private JLabel mainFileLabel;
    private JLabel figLabel;
    private JFrame mainFrame;
    private JTextField mainFileTextField;
    private JButton mainFileBrowseButton;
    private JButton confirmButton;
    private JTextArea logField;
    private JScrollPane scrollPane;
    private JCheckBox asySortCheckBox;
    private JTextField figTextField;
    private JButton figBrowseButton;
    private JFrame logFrame;
    private JList<String> texFilesList;
    private DefaultListModel<String> listModel;
    private JCheckBox ignoreWrongFilenameCheckBox;

    private JCheckBox autoSortCheckButton;
    private JButton insertButton;
    private JButton deleteButton;
    private JButton moveUpButton;
    private JButton moveDownButton;

    private SimpleTexProcessProgram() {
        mainFrame = new JFrame("Simple Tex Process Program");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setSize(650, 768);
        mainFrame.setResizable(false);
        mainFrame.setLocation(100, 200);
        mainFrame.setLayout(null);
        mainFileLabel = new JLabel("主文件路径：");
        mainFileLabel.setBounds(Constants.MARGIN_GAP, Constants.MARGIN_GAP, 105, Constants.COMPONENT_HEIGHT);
        mainFileTextField = new JTextField("Calculus_lecture_HighDimension.tex");
        mainFileTextField.setBounds(mainFileLabel.getX() + mainFileLabel.getWidth() + Constants.MARGIN_GAP,
                mainFileLabel.getY(), 300, Constants.COMPONENT_HEIGHT);
        mainFileBrowseButton = new JButton("浏览");
        mainFileBrowseButton.setBounds(mainFileTextField.getX() + mainFileTextField.getWidth() + Constants.MARGIN_GAP,
                mainFileLabel.getY(), 80, Constants.COMPONENT_HEIGHT);
        confirmButton = new JButton("开始合并");
        confirmButton.setBounds(mainFileBrowseButton.getX() + mainFileBrowseButton.getWidth() + Constants.MARGIN_GAP,
                mainFileLabel.getY(), 100, Constants.COMPONENT_HEIGHT);
        figLabel = new JLabel("图片文件夹路径：");
        figLabel.setBounds(Constants.MARGIN_GAP, mainFileTextField.getY() + mainFileTextField.getHeight() + Constants.MARGIN_GAP,
                110, Constants.COMPONENT_HEIGHT);
        figTextField = new JTextField("fig");
        figTextField.setBounds(mainFileTextField.getX(), figLabel.getY(),
                mainFileTextField.getWidth(), Constants.COMPONENT_HEIGHT);
        figBrowseButton = new JButton("浏览");
        figBrowseButton.setBounds(mainFileBrowseButton.getX(), figTextField.getY(),
                mainFileBrowseButton.getWidth(), Constants.COMPONENT_HEIGHT);
        asySortCheckBox = new JCheckBox("整理图片文件夹");
        asySortCheckBox.setBounds(confirmButton.getX(), figBrowseButton.getY(),
                120, Constants.COMPONENT_HEIGHT);
        listModel = new DefaultListModel<>();
        texFilesList = new JList<>(listModel);
        texFilesList.setBorder(BorderFactory.createTitledBorder("需要插入的Tex文件"));
        texFilesList.setBounds(Constants.MARGIN_GAP, figLabel.getY() + figLabel.getHeight() + Constants.MARGIN_GAP,
                525, 625);
        autoSortCheckButton = new JCheckBox("自动排序");
        autoSortCheckButton.setBounds(texFilesList.getX() + texFilesList.getWidth() + Constants.MARGIN_GAP, texFilesList.getY(),
                90, Constants.COMPONENT_HEIGHT);
        ignoreWrongFilenameCheckBox = new JCheckBox("忽略错误");
        ignoreWrongFilenameCheckBox.setBounds(autoSortCheckButton.getX(),
                autoSortCheckButton.getY() + autoSortCheckButton.getHeight() + Constants.MARGIN_GAP,
                autoSortCheckButton.getWidth(), Constants.COMPONENT_HEIGHT);
        ignoreWrongFilenameCheckBox.setToolTipText("忽略不符合规范的文件名");
        insertButton = new JButton("添加文件");
        insertButton.setBounds(ignoreWrongFilenameCheckBox.getX(),
                ignoreWrongFilenameCheckBox.getY() + ignoreWrongFilenameCheckBox.getHeight() + Constants.MARGIN_GAP,
                90, Constants.COMPONENT_HEIGHT);
        deleteButton = new JButton("移除文件");
        deleteButton.setBounds(insertButton.getX(), insertButton.getY() + insertButton.getHeight() + Constants.MARGIN_GAP,
                insertButton.getWidth(), Constants.COMPONENT_HEIGHT);
        moveUpButton = new JButton("上移");
        moveUpButton.setBounds(insertButton.getX(), deleteButton.getY() + deleteButton.getHeight() + Constants.MARGIN_GAP,
                insertButton.getWidth(), Constants.COMPONENT_HEIGHT);
        moveDownButton = new JButton("下移");
        moveDownButton.setBounds(insertButton.getX(), moveUpButton.getY() + moveUpButton.getHeight() + Constants.MARGIN_GAP,
                insertButton.getWidth(), Constants.COMPONENT_HEIGHT);
        initMainFrame();
        initLogFrame();
        setToDefault();
    }

    public static void main(String[] args) {
        mainWindow = new SimpleTexProcessProgram();
    }

    private void initLogFrame() {
        logFrame = new JFrame("日志记录");
        logFrame.setLocation((int) mainFrame.getLocation().getX() + mainFrame.getWidth(), (int) mainFrame.getLocation().getY());
        logFrame.setSize(800, mainFrame.getHeight());
        logFrame.setResizable(false);
        logFrame.setVisible(true);
        logField = new JTextArea();
        logField.setLineWrap(true);
        logField.setEditable(false);
        logField.setForeground(Color.black);
        DefaultCaret caret = (DefaultCaret) logField.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        scrollPane = new JScrollPane(logField, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBounds(Constants.MARGIN_GAP, Constants.MARGIN_GAP,
                logFrame.getWidth() - 2 * Constants.MARGIN_GAP, 720);
        logFrame.add(scrollPane);
    }

    private void initMainFrame() {
        addComponents();
        addActionListeners();
    }

    private void setToDefault() {
        asySortCheckBox.setSelected(true);
        autoSortCheckButton.setSelected(true);
        ignoreWrongFilenameCheckBox.setSelected(true);
        deleteButton.setEnabled(false);
        moveUpButton.setEnabled(false);
        moveDownButton.setEnabled(false);
        mainFrame.setVisible(true);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                confirmButton.grabFocus();
            }
        });
    }

    private void addComponents() {
        mainFrame.add(mainFileLabel);
        mainFrame.add(mainFileTextField);
        mainFrame.add(mainFileBrowseButton);
        mainFrame.add(confirmButton);
        mainFrame.add(asySortCheckBox);
        mainFrame.add(figLabel);
        mainFrame.add(figTextField);
        mainFrame.add(figBrowseButton);
        mainFrame.add(texFilesList);
        mainFrame.add(autoSortCheckButton);
        mainFrame.add(ignoreWrongFilenameCheckBox);
        mainFrame.add(insertButton);
        mainFrame.add(deleteButton);
        mainFrame.add(moveUpButton);
        mainFrame.add(moveDownButton);
    }

    private void addActionListeners() {
        mainFileBrowseButton.addActionListener(new BrowseButtonActionListener());
        figBrowseButton.addActionListener(new BrowseButtonActionListener());
        insertButton.addActionListener(new BrowseButtonActionListener());
        confirmButton.addActionListener(e -> {
            if (e.getSource() == confirmButton) {
                new Thread(new ProcessFiles(mainFileTextField.getText(), figTextField.getText())).start();
            }
        });
        autoSortCheckButton.addChangeListener(e -> {
            if (e.getSource() == autoSortCheckButton) {
                moveUpButton.setEnabled(!autoSortCheckButton.isSelected());
                moveDownButton.setEnabled(!autoSortCheckButton.isSelected());
            }
        });
        deleteButton.addActionListener(new ListButtonActionListener());
        texFilesList.addListSelectionListener(e -> {
            if (e.getSource() == texFilesList) {
                deleteButton.setEnabled(true);
            }
        });
    }

    public void lockComponents() {
        setEnabled(false);
    }

    public void unlockComponents() {
        setEnabled(true);
    }

    private void setEnabled(boolean enabled) {
        mainFileTextField.setEnabled(enabled);
        confirmButton.setEnabled(enabled);
        asySortCheckBox.setEnabled(enabled);
        mainFileBrowseButton.setEnabled(enabled);
        figBrowseButton.setEnabled(enabled);
        figTextField.setEnabled(enabled);
        texFilesList.setEnabled(enabled);
        insertButton.setEnabled(enabled);
        autoSortCheckButton.setEnabled(enabled);
        ignoreWrongFilenameCheckBox.setSelected(enabled);
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public JTextField getMainFileTextField() {
        return mainFileTextField;
    }

    public JButton getConfirmButton() {
        return confirmButton;
    }

    public JTextArea getLogField() {
        return logField;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JCheckBox getAsySortCheckBox() {
        return asySortCheckBox;
    }

    public JButton getMainFileBrowseButton() {
        return mainFileBrowseButton;
    }

    public JTextField getFigTextField() {
        return figTextField;
    }

    public JButton getFigBrowseButton() {
        return figBrowseButton;
    }

    public JFrame getLogFrame() {
        return logFrame;
    }

    public DefaultListModel<String> getListModel() {
        return listModel;
    }

    class BrowseButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser;
            if (e.getSource() == mainFileBrowseButton) {
                fileChooser = new JFileChooser(new File("."));
                fileChooser.setDialogTitle("选择主文件");
                fileChooser.setFileFilter(new FileNameExtensionFilter("*.tex", "tex"));
                int val = fileChooser.showOpenDialog(mainFrame);
                if (val == JFileChooser.APPROVE_OPTION) {
                    mainFileTextField.setText(fileChooser.getSelectedFile().getPath());
                }
            } else if (e.getSource() == figBrowseButton) {
                fileChooser = new JFileChooser(new File("."));
                fileChooser.setDialogTitle("选择图片文件夹");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int val = fileChooser.showOpenDialog(mainFrame);
                if (val == JFileChooser.APPROVE_OPTION) {
                    figTextField.setText(fileChooser.getSelectedFile().getPath());
                }
            } else if (e.getSource() == insertButton) {
                fileChooser = new JFileChooser(new File("."));
                fileChooser.setDialogTitle("选择要插入的tex文件");
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setFileFilter(new FileNameExtensionFilter(".tex", "tex"));
                int val = fileChooser.showOpenDialog(mainFrame);
                if (val == JFileChooser.APPROVE_OPTION) {
                    for (File file : fileChooser.getSelectedFiles()) listModel.addElement(file.getPath());
                }
            }
        }
    }

    class ListButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == deleteButton) {
                int[] indices = texFilesList.getSelectedIndices();
                for (int i = indices.length - 1; i >= 0; i--) {
                    listModel.remove(indices[i]);
                }
                if (texFilesList.isSelectionEmpty() || listModel.getSize() == 0) deleteButton.setEnabled(false);
            }
        }
    }
}
