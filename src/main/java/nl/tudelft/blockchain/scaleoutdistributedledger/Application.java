package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.HashMap;
import java.util.Map;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.RSAKey;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint.TendermintChain;

/**
 * Class to represent an application.
 */
public class Application {
	private final Verification verification = new Verification();
	
	@Getter
	private Node ownNode;
	
	@Getter
	private Map<Integer, Node> nodes = new HashMap<>();
	private static MainChain mainChain;

	/**
	 * Creates a new application.
	 * @param tendermintPort - the port on which the tendermint server will run.
	 */
	public Application(int tendermintPort) {
		init(tendermintPort);
	}
	
	/**
	 * @param id - the id
	 * @return the node with the given id, or null
	 */
	public Node getNode(int id) {
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
		CommunicationHelper.receiveTransaction(verification, transaction, proof);
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
	 * @param tmPort - the port on which to run the Tendermint server
	 */
	private void init(int tmPort) {
		RSAKey key = new RSAKey();
		int id = TrackerHelper.registerNode(key.getPublicKey());
		this.ownNode = new Node(id, key.getPublicKey(), "localhost");
		this.ownNode.setPrivateKey(key.getPrivateKey());

		mainChain = new TendermintChain(tmPort);

		nodes.put(id, this.ownNode);
	}

	/**
	 * Returns the singleton main chain.
	 * @return -
	 */
	public static MainChain getMainChain() {
		return mainChain;
	}
}
