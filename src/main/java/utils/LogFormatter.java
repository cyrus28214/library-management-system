package utils;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogFormatter extends Formatter {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    @Override
    public String format(LogRecord record) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(new Date(record.getMillis()));
        String level = record.getLevel().getName();
        String color = ANSI_RESET;
        switch (level) {
            case "SEVERE":
                color = ANSI_RED;
                break;
            case "WARNING":
                color = ANSI_YELLOW;
                break;
            case "INFO":
                color = ANSI_GREEN;
                break;
        }
        String message = record.getMessage();

        StringBuilder builder = new StringBuilder();

        builder.append(ANSI_BLUE);
        builder.append(time);
        builder.append(ANSI_RESET);

        builder.append(" [");
        builder.append(color);
        builder.append(level);
        builder.append(ANSI_RESET);
        builder.append("] ");

        builder.append(message);
        builder.append(ANSI_RESET);
        builder.append("\n");
        return builder.toString();
    }
}