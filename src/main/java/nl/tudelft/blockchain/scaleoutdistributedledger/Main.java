package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Class to handle multiple applications.
 */
public final class Main {

	private Main() {
		// prevents instantiation
		throw new UnsupportedOperationException();
	}

	/**
	 * Main method, starting point of the application.
	 * @param args - command line arguments.
	 * @throws IOException - error while registering nodes.
	 * @throws NoSuchAlgorithmException - no such algorithm.
	 */
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		// Start a new node
		Application app = new Application();
		
		// TODO: Make an example transaction?
		
	}
}
