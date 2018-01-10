package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for logging functions.
 */
public final class Log {
	public static final Level LEVEL = Level.FINEST;
	public static final String FORMAT = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %3$s %5$s%6$s%n";
	public static final Logger PARENT_LOGGER = Logger.getLogger(Log.class.getName()).getParent();

	private Log() {
		throw new UnsupportedOperationException();
	}
	
	static {
		//Set the level and the format string.
		PARENT_LOGGER.setLevel(LEVEL);
		
		LogFormatter formatter = new LogFormatter(FORMAT);
		for (Handler handler : PARENT_LOGGER.getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				handler.setFormatter(formatter);
			}
		}
	}

	/**
	 * Change the log level.
	 * @param level - the level to change to.
	 */
	public static void setLogLevel(Level level) {
		PARENT_LOGGER.setLevel(level);
	}
	
	/**
	 * Handle logging of an exception.
	 * @param level - level of the exception
	 * @param str - message for the logs
	 * @param throwable - the object of the exception
	 */
	public static void log(Level level, String str, Throwable throwable) {
		// By default
		Logger.getLogger(getCallerClassName()).log(level, str, throwable);
	}
	
	/**
	 * Logs the given message.
	 * @param level - level to log at
	 * @param str   - message to log
	 */
	public static void log(Level level, String str) {
		Logger.getLogger(getCallerClassName()).log(level, str);
	}

	/**
	 * Get the name of the last class that added to the stack.
	 * @return class name
	 */
	private static String getCallerClassName() {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		for (int i = 3; i < stackTraceElements.length; i++) {
			StackTraceElement ste = stackTraceElements[i];
			if (ste.getClassName().indexOf("java.lang.Thread") != 0) {
				String name = ste.getClassName();
				return name.substring(name.lastIndexOf('.') + 1);
			}
		}
		return null;
	}
}
