package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketServer;

import java.util.ArrayList;

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
	public static void main(String[] args) {
		//TODO

		Thread t = new Thread(new SocketServer(8007));
		t.start();

		SocketClient client = new SocketClient();
		client.initSocketClient();
		client.sendObject("localhost", 8007, new ArrayList<Integer>());
	}
}
