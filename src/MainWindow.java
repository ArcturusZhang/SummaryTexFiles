import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import javax.swing.*;
import java.awt.*;

public class MainWindow {
    static MainWindow mainWindow;
    private JFrame frame;
    private JTextField textField;
    private JButton confirmButton;
    private JTextArea logField;
    private JScrollPane scrollPane;
    private JCheckBox asySortCheckBox;

    public JFrame getFrame() {
        return frame;
    }

    public JTextField getTextField() {
        return textField;
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

    public static void main(String[] args) {
        mainWindow = new MainWindow();
    }

    MainWindow() {
        frame = new JFrame("Simple Tex Process Program");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1024, 768);
        frame.setResizable(false);
        frame.setLocation(200, 200);
        frame.setLayout(null);
        JLabel label = new JLabel("主文件路径：");
        label.setBounds(Constants.MARGIN_GAP, Constants.MARGIN_GAP, 80, Constants.COMPONENT_HEIGHT);
        textField = new JTextField("Calculus_lecture_HighDimension.tex");
        textField.setBounds(label.getX() + label.getWidth() + Constants.MARGIN_GAP, label.getY(), 300, Constants.COMPONENT_HEIGHT);
        confirmButton = new JButton("开始合并");
        confirmButton.setBounds(textField.getX() + textField.getWidth() + Constants.MARGIN_GAP, label.getY(),
                100, Constants.COMPONENT_HEIGHT);
        asySortCheckBox = new JCheckBox("整理fig文件夹");
        asySortCheckBox.setBounds(confirmButton.getX() + confirmButton.getWidth() + Constants.MARGIN_GAP, Constants.MARGIN_GAP,
                120, Constants.COMPONENT_HEIGHT);
        frame.add(label);
        frame.add(textField);
        frame.add(confirmButton);
        frame.add(asySortCheckBox);
        logField = new JTextArea();
        logField.setLineWrap(true);
        logField.setEditable(false);
        logField.setForeground(Color.black);
        scrollPane = new JScrollPane(logField, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBounds(Constants.MARGIN_GAP, label.getY() + label.getHeight() + Constants.MARGIN_GAP,
                1000, 680);
        frame.add(scrollPane);
        frame.setVisible(true);
        confirmButton.addActionListener(e -> {
            if (e.getSource() == confirmButton) {
                new Thread(new TexProcess(textField.getText())).start();
            }
        });
    }

    public JCheckBox getAsySortCheckBox() {
        return asySortCheckBox;
    }
}
