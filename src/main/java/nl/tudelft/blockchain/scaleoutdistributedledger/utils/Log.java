package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for logging functions.
 */
public class Log {
	
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
	 * Get the name of the last class that added to the stack.
	 * @return class name
	 */
	public static String getCallerClassName() {
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
