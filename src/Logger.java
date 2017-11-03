import javax.swing.*;

public class Logger {
    public final static int LOW = 0;
    public final static int MEDIUM = 1;
    public final static int HIGH = 2;
    private static Logger log = null;
    private JTextArea logField;
    private Logger(JTextArea logField) {
        this.logField = logField;
    }

    public static Logger getLog(JTextArea logField) {
        if (log == null) {
            log = new Logger(logField);
        }
        return log;
    }

    void println(String content) {
        if (logField != null) {
            logField.append(content + "\n");
        }
    }

    void print(String content) {
        if (logField != null) {
            logField.append(content);
        }
    }
}
