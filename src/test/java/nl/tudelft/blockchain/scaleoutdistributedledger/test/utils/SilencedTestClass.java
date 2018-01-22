package nl.tudelft.blockchain.scaleoutdistributedledger.test.utils;

import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.logging.Level;

public class SilencedTestClass {
	private static Level level;

	/**
	 * Store the current log level (for restoring) and disable logging.
	 */
	@BeforeClass
	public static void setUpClass() {
		level = Log.getLogLevel();
		Log.setLogLevel(Level.OFF);
	}

	/**
	 * Re-enable the logging with the old level.
	 */
	@AfterClass
	public static void tearDownClass() throws Exception {
		Log.setLogLevel(level);
	}
}
