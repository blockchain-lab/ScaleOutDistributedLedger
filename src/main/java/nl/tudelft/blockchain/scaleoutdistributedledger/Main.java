package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.RSAKey;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

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
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		//TODO
		KeyPair keyPair = RSAKey.generateKeys();
		System.out.println(TrackerHelper.registerNode(keyPair.getPublic().getEncoded()));
		Map<Integer, Node> nodes = new HashMap<>();
		TrackerHelper.updateNodes(nodes);
	}
}
