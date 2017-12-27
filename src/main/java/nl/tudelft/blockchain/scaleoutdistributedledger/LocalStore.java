package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;

/**
 * Class to store information related to our own node.
 */
public class LocalStore {
	
	@Getter
	private final Node ownNode;
	
	@Getter
	private final Map<Integer, Node> nodes = new HashMap<>();
	
	@Getter
	private final Verification verification = new Verification();
	
	@Getter
	private final Set<Transaction> unspent = new HashSet<>();
	
	/**
	 * Constructor.
	 * @param ownNode - our own node.
	 * @throws IOException - exception while updating nodes
	 */
	public LocalStore(Node ownNode) throws IOException {
		this.ownNode = ownNode;
		this.nodes.put(this.ownNode.getId(), this.ownNode);
		
		// Get current list of nodes from tracker
		TrackerHelper.updateNodes(this.nodes);
	}
	
	/**
	 * Get a node.
	 * Retrieves it from the tracker if it's not in the local store.
	 * @param id - the id
	 * @return the node with the given id, or null
	 * @throws IOException - exception while updating nodes
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
	 * Get transaction from a specific node with a transaction id.
	 * @param nodeId - identifier of the node
	 * @param transactionId - identifier of the transaction
	 * @return transaction
	 * @throws IOException - error while getting the node
	 */
	public Transaction getTransactionFromNode(int nodeId, int transactionId) throws IOException {
		Node node = getNode(nodeId);
		for (Block block : node.getChain().getBlocks()) {
			for (Transaction transaction : block.getTransactions()) {
				if (transaction.getNumber() == transactionId) return transaction;
			}
		}
		
		throw new IllegalStateException("Transaction with id " + transactionId + " from node " + nodeId + " not found.");
	}
	
}
