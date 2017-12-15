package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.IOException;

/**
 * Main starting point of the application.
 */
public final class Main {

	private Main() {
		// prevents instantiation
		throw new UnsupportedOperationException();
	}

	/**
	 * Main method, starting point of the application.
	 * @param args - command line arguments.
	 */
	public static void main(String[] args) throws IOException {
		//TODO
//		TrackerHelper.updateNodes(null);
		TrackerHelper.registerNode(null);
	}
}
