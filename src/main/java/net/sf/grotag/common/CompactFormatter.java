package net.sf.grotag.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatter to log a much more compact format than <code>SimpleFormatter</code>.
 * In particular the output for each message fits in one line.
 * 
 * @see java.util.logging.SimpleFormatter
 * @author Thomas Aglassinger
 */
public class CompactFormatter extends Formatter {
    private static final String BORING_LOGGER_PREFIX = "net.sf.grotag.";
    private static final int INITIAL_BUFFER_SIZE = 200;
    private DateFormat timeFormat;

    public CompactFormatter() {
        timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    }

    @Override
    public String format(LogRecord record) {
        StringBuffer result = new StringBuffer(INITIAL_BUFFER_SIZE);
        Date recordTime = new Date(record.getMillis());
        String loggerName = record.getLoggerName();

        if ((loggerName != null) && (loggerName.startsWith(BORING_LOGGER_PREFIX))) {
            loggerName = loggerName.substring(BORING_LOGGER_PREFIX.length());
        }

        result.append(timeFormat.format(recordTime));
        result.append(' ');
        result.append(record.getLevel());
        result.append(' ');
        result.append(loggerName);
        result.append(": ");
        result.append(formatMessage(record));
        result.append('\n');
        return result.toString();
    }

}
