package nl.tudelft.blockchain.scaleoutdistributedledger;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.RSAKey;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to represent an application.
 */
public class Application {

	public static final String NODE_ADDRESS = "localhost";
	public static final int NODE_PORT = 8007;

	private final Verification verification = new Verification();
	
	@Getter
	private Node ownNode;
	
	@Getter
	private Map<Integer, Node> nodes = new HashMap<>();
	
	@Getter
	private Set<Transaction> unspent = new HashSet<>();
	
	/**
	 * Creates a new application.
	 */
	public Application() throws IOException {
		init();
	}
	
	/**
	 * @param id - the id
	 * @return the node with the given id, or null
	 */
	public Node getNode(int id) throws IOException {
		Node node = nodes.get(id);
		if (node == null) {
			TrackerHelper.updateNodes(nodes);
			node = nodes.get(id);
		}
		return node;
	}
	
	/**
	 * Called when we receive a new transaction.
	 * @param transaction - the transaction
	 * @param proof       - the proof
	 */
	public synchronized void receiveTransaction(Transaction transaction, Proof proof) {
		if (CommunicationHelper.receiveTransaction(verification, transaction, proof)) {
			if (transaction.getAmount() > 0) {
				unspent.add(transaction);
			}
		}
	}
	
	/**
	 * Send a transaction to the receiver of the transaction.
	 * An abstract of the block containing the transaction (or a block after it) must already be
	 * committed to the main chain.
	 * @param transaction - the transaction to send
	 */
	public void sendTransaction(Transaction transaction) {
		CommunicationHelper.sendTransaction(transaction);
	}
	
	/**
	 * Initializes this application.
	 */
	private void init() throws IOException {
		RSAKey key = new RSAKey();
		this.ownNode = TrackerHelper.registerNode(key.getPublicKey(), "localhost", 80);
		// TODO: set actual IP address and port
		this.ownNode.setPrivateKey(key.getPrivateKey());
		
		nodes.put(this.ownNode.getId(), this.ownNode);
	}
}
