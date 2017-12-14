package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

/**
 * Helper class for interacting with the tracker.
 */
public final class TrackerHelper {
	private TrackerHelper() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Registers this node with the given public key.
	 * @param publicKey - the public key to register with
	 * @return            the assigned id
	 */
	public static int registerNode(byte[] publicKey) {
		//TODO Register with server, return node id
		return 0;
	}
	
	/**
	 * Updates the given map of nodes with new information from the tracker.
	 * @param nodes - the map of nodes
	 */
	public static void updateNodes(Map<Integer, Node> nodes) {
		List<String> lines = downloadNodesList();
		for (String line : lines) {
			String[] parts = line.split(",");
			if (parts.length != 3) continue;
			
			int id = Integer.parseInt(parts[0]);
			
			//We already know about this node
			if (nodes.containsKey(id)) continue;
			
			//Create a new node
			byte[] publicKey = Utils.hexStringToBytes(parts[1]);
			String address = parts[2];
			Node node = new Node(id, publicKey, address);
			nodes.put(id, node);
		}
	}
	
	/**
	 * Downloads the list of nodes from the tracker.
	 * The format is {@code id,public key (hex string),address}.
	 * @return a list of strings where each string represents a single node
	 */
	public static List<String> downloadNodesList() {
		//TODO Actually get this info
		return new ArrayList<>();
	}
}
