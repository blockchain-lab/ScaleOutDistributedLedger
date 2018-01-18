package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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
	 */
	public static void main(String[] args) {
		if (args[0].equalsIgnoreCase("s")) {
			try {
				SimulationMain.main(args);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return;
		}
		// Start a new node
		// TODO: Make an example transaction?
		Application app = new Application(true);
	}

	/**
	 * Manual testing method for sockets.
	 */
	private static void testSockets() {
		try {
			Thread t = new Thread(new SocketServer(8007, new LocalStore(new OwnNode(0), null, null, false)));
			t.start();

			Node node = new Node(1, null, "localhost", 8007);

			SocketClient client = new SocketClient();
			client.sendMessage(node, new ArrayList<Integer>());
			Thread.sleep(2500);
			client.sendMessage(node, new ArrayList<>());
			Thread.sleep(7500);
			client.sendMessage(node, new ArrayList<>());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
