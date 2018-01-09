package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for logging functions.
 */
public final class Log {

	private Log() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Change the log level
	 * @param level - the level to change to.
	 */
	public void setLogLevel(Level level) {
		Logger.getLogger("").setLevel(level);
		Logger.getLogger("").getHandlers()[0].setLevel(level);
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
		for (int i = 1; i < stackTraceElements.length; i++) {
			StackTraceElement ste = stackTraceElements[i];
			if (!ste.getClassName().equals(Log.class.getName()) && ste.getClassName().indexOf("java.lang.Thread") != 0) {
				return ste.getClassName();
			}
		}
		return null;
	}
}
