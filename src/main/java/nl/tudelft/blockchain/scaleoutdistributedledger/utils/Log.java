package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for logging functions.
 */
public final class Log {
	public static final Level LEVEL         = Level.INFO;
	public static final String FORMAT       = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %3$s %5$s%6$s%n";
	public static final String DEBUG_FORMAT = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n";
	
	public static final Logger PARENT_LOGGER = Logger.getLogger(Log.class.getName()).getParent();
	public static final Logger DEBUG_LOGGER = Logger.getLogger("DEBUG");

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
				handler.setLevel(LEVEL);
			}
		}
		
		//Create debug console handler
		ConsoleHandler debugHandler = new ConsoleHandler();
		debugHandler.setLevel(Level.ALL);
		debugHandler.setFormatter(new LogFormatter(DEBUG_FORMAT));
		
		//Setup debug logger
		DEBUG_LOGGER.setUseParentHandlers(false);
		DEBUG_LOGGER.addHandler(debugHandler);
		DEBUG_LOGGER.setLevel(LEVEL);
	}

	/**
	 * Change the log level.
	 * @param level - the level to change to.
	 */
	public static void setLogLevel(Level level) {
		PARENT_LOGGER.setLevel(level);
	}

	/**
	 * Get the current log level.
	 * @return - the current log level
	 */
	public static Level getLogLevel() {
		return PARENT_LOGGER.getLevel();
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
	 * Logs the given message for the given node id.
	 * @param level  - level to log at
	 * @param str    - message to log
	 * @param nodeId - the id of the node
	 */
	public static void log(Level level, String str, int nodeId) {
		Logger.getLogger(getCallerClassName()).log(level, "[" + nodeId + "] " + str);
	}
	
	/**
	 * Logs the given debug message.
	 * @param msg    - the message
	 * @param params - the parameters
	 */
	public static void debug(String msg, Object... params) {
		StackTraceElement ste = getCallerStackTrace(3);
		if (ste == null) {
			DEBUG_LOGGER.logp(Level.INFO, null, null, msg, params);
		} else {
			String className = ste.getClassName();
			className = className.substring(className.lastIndexOf('.') + 1);
			String sourceMethod = ste.getMethodName() + ":" + ste.getLineNumber();
			DEBUG_LOGGER.logp(Level.INFO, className, sourceMethod, msg, params);
		}
	}

	/**
	 * Get the name of the last class that added to the stack.
	 * @return class name
	 */
	private static String getCallerClassName() {
		StackTraceElement ste = getCallerStackTrace(4);
		if (ste == null) return null;
		String className = ste.getClassName();
		return className.substring(className.lastIndexOf('.') + 1);
	}
	
	/**
	 * Determines the stack trace element of the caller.
	 * @return - the stack trace element of the caller
	 */
	private static StackTraceElement getCallerStackTrace(int start) {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		for (int i = start; i < stackTraceElements.length; i++) {
			StackTraceElement ste = stackTraceElements[i];
			if (ste.getClassName().indexOf("java.lang.Thread") != 0) {
				return ste;
			}
		}
		return null;
	}
}
