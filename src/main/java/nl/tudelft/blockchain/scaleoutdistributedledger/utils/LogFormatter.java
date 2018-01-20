package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatter for the log. This class is mostly a clone of SimpleFormatter but with a few small adjustments.
 */
public class LogFormatter extends Formatter {
	private final String format;
	private final Date dat = new Date();
	
	/**
	 * Creates a new log formatter with the given format.
	 * <pre>
	 * %1 = date
	 * %2 = source class
	 * %3 = logger name
	 * %4 = level
	 * %5 = message
	 * %6 = exception
	 * </pre>
	 * @param format - the format string
	 */
	public LogFormatter(String format) {
		this.format = format;
	}

	@Override
	public synchronized String format(LogRecord record) {
		dat.setTime(record.getMillis());
		String source;
		if (record.getSourceClassName() != null) {
			source = record.getSourceClassName();
			if (record.getSourceMethodName() != null) {
				source += " " + record.getSourceMethodName();
			}
		} else {
			source = record.getLoggerName();
		}
		String message = formatMessage(record);
		String throwable;
		if (record.getThrown() != null) {
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				pw.println();
				record.getThrown().printStackTrace(pw);
			}
			throwable = sw.toString();
		} else {
			throwable = "";
		}

		return String.format(format, dat, source, record.getLoggerName(), record.getLevel().getLocalizedName(), message, throwable);
	}

}
