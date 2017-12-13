package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.RSAKey;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import lombok.Getter;

/**
 * Class to represent an application.
 */
public class Application {
	private final Verification verification = new Verification();
	
	@Getter
	private Node ownNode;
	private Map<Integer, Node> nodes = new HashMap<>();
	
	/**
	 * Creates a new application.
	 */
	public Application() {
		init();
	}
	
	/**
	 * @param id - the id
	 * @return the node with the given id, or null
	 */
	public Node getNode(int id) {
		Node node = nodes.get(id);
		if (node == null) {
			updateNodeList();
			node = nodes.get(id);
		}
		return node;
	}
	
	/**
	 * @return a collection of all nodes
	 */
	public Collection<Node> getNodes() {
		return nodes.values();
	}
	
	/**
	 * Called when we receive a new transaction.
	 * @param transaction - the transaction
	 * @param proof       - the proof
	 */
	public synchronized void receiveTransaction(Transaction transaction, Proof proof) {
		//If we have seen this transaction before, reject it
		if (verification.isCached(transaction)) {
			Log.log(Level.WARNING, "Received a transaction we already received!");
			return;
		}
		
		if (!verification.isValid(transaction, proof)) return;
		
		proof.applyUpdates();
	}
	
	/**
	 * Send a transaction to the receiver of the transaction.
	 * An abstract of the block containing the transaction (or a block after it) must already be
	 * committed to the main chain.
	 * @param transaction - the transaction to send
	 */
	public void sendTransaction(Transaction transaction) {
		Node to = transaction.getReceiver();
		String address = to.getAddress();
		//TODO Open socket connection to other
		//TODO Create message and send it
	}
	
	/**
	 * Initializes this application.
	 */
	private void init() {
		RSAKey key = new RSAKey();
		int id = register(key.getPublicKey());
		this.ownNode = new Node(id, key.getPublicKey(), "localhost");
		this.ownNode.setPrivateKey(key.getPrivateKey());
		
		nodes.put(id, this.ownNode);
	}
	
	/**
	 * Registers this node with the given public key.
	 * @return the assigned id
	 */
	private int register(byte[] publicKey) {
		//TODO Register with server, return node id
		return 0;
	}
	
	/**
	 * Updates the list of nodes.
	 */
	private void updateNodeList() {
		//TODO Get csv list with node id, public key (hex), address from server.
		List<String> lines = new ArrayList<>();
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
}
