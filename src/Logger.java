import javax.swing.*;

public class Logger {
    public final static int LOW = 0;
    public final static int MEDIUM = 1;
    public final static int HIGH = 2;
    private static Logger log = null;
    private static int LOG_LEVEL = MEDIUM;
    private JTextArea logField = null;

    private Logger() {
    }

    /**
     * Query the current log level.
     *
     * @return current log level
     */
    public static int getLogLevel() {
        return LOG_LEVEL;
    }

    /**
     * Set log level to control how detailed the log would be.
     *
     * @param logLevel log level
     */
    public static void setLogLevel(int logLevel) {
        Logger.LOG_LEVEL = logLevel;
    }

    public synchronized static Logger getLog() {
        if (log == null) {
            log = new Logger();
        }
        return log;
    }

    /**
     * Set the logField, a JTextArea, to show the log to user. If logField is {@code null}, for instance, print a log
     * before set log field, no log will be output.
     *
     * @param logField where the log will be shown
     */
    public void setLogField(JTextArea logField) {
        this.logField = logField;
    }

    /**
     * Print log with certain level.
     *
     * @param content  log content
     * @param logLevel log level
     */
    void print(String content, int logLevel) {
        if (logField != null && logLevel <= LOG_LEVEL) {
            logField.append(content);
        }
    }

    /**
     * Print log with certain level then create a new line
     *
     * @param content  log content
     * @param logLevel log level
     */
    void println(String content, int logLevel) {
        print(content + "\n", logLevel);
    }

    /**
     * Print log with default level (LOW) which indicates the log will be shown under any level setting,
     * then create a new line
     *
     * @param content log content
     */
    void println(String content) {
        println(content, LOW);
    }

    /**
     * Print log with default level (LOW) which indicates the log will be shown under any level setting.
     *
     * @param content log content
     */
    void print(String content) {
        print(content, LOW);
    }

    /**
     * Print an array of objects as log. This was intended to print stack trace of an {@code Exception} by
     * calling the {@code getStackTrace} method of an Exception. This have been replaced by {@code printStackTrace}.
     *
     * @param objects array of object, will be print to log.
     * @see Logger#printStackTrace(Exception)
     */
    @Deprecated
    void println(Object[] objects) {
        for (Object object : objects) {
            println(object.toString());
        }
    }

    /**
     * Print stack trace of an {@code Exception} to log, syntax is similar to the method in class
     * {@code Exception.printStackTrace}.
     *
     * @param e exception
     */
    void printStackTrace(Exception e) {
        for (StackTraceElement element : e.getStackTrace()) {
            println(element.toString());
        }
    }
}
