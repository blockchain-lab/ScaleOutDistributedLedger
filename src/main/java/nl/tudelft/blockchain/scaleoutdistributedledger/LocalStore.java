package nl.tudelft.blockchain.scaleoutdistributedledger;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.mocks.TendermintChainMock;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint.TendermintChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.validation.Verification;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to store information related to our own node.
 */
public class LocalStore {
	@Getter
	private final Application application;
	
	@Getter
	private final OwnNode ownNode;
	
	@Getter
	private final Map<Integer, Node> nodes;
	
	@Getter
	private final Verification verification = new Verification();
	
	@Getter
	private final Set<Transaction> unspent = new HashSet<>();

	@Getter
	private final MainChain mainChain;
	
	@Getter
	private long availableMoney;
	
	private int transactionId;
	
	/**
	 * Constructor.
	 * @param ownNode      - our own node.
	 * @param application  - the application
	 * @param genesisBlock - the genesis (initial) block for the entire system
	 * @param isProduction - if this is production or testing
	 */
	public LocalStore(OwnNode ownNode, Application application, Block genesisBlock, boolean isProduction) {
		this.nodes = new HashMap<>();
		this.ownNode = ownNode;
		this.application = application;
		this.nodes.put(ownNode.getId(), ownNode);
		if (isProduction) {
			this.mainChain = new TendermintChain(ownNode.getPort() + 3, genesisBlock, application);
		} else {
			this.mainChain = new TendermintChainMock();
		}
		
		if (genesisBlock != null) {
			this.transactionId = genesisBlock.getTransactions().size();
			
			Transaction genesisTransaction = ownNode.getChain().getGenesisTransaction();
			if (genesisTransaction != null) {
				this.addUnspentTransaction(genesisTransaction);
			}
		}
	}
	
	/**
	 * Get a node.
	 * Retrieves it from the tracker if it's not in the local store.
	 * @param id - the id
	 * @return the node with the given id, or null
	 * @throws IllegalStateException - exception while updating nodes
	 */
	public Node getNode(int id) {
		return nodes.get(id);
	}
	
	/**
	 * Gets the current list of nodes from the tracker.
	 */
	public void updateNodes() {
		try {
			TrackerHelper.updateNodes(nodes, ownNode);
			normalizeGenesis();
		} catch (IOException ex) {
			throw new IllegalStateException("Tracker update failed!", ex);
		}
	}
	
	/**
	 * Get transaction from a specific node with a transaction id.
	 * @param nodeId        - identifier of the node
	 * @param transactionId - identifier of the transaction
	 * @return              - the transaction
	 */
	public Transaction getTransactionFromNode(int nodeId, int transactionId) {
		if (nodeId == Transaction.GENESIS_SENDER) {
			// It's a genesis transaction
			Block genesisBlock = this.ownNode.getChain().getGenesisBlock();
			return genesisBlock.getTransactions().get(transactionId);
		}
		
		Node node = getNode(nodeId);
		for (Block block : node.getChain().getBlocks()) {
			for (Transaction transaction : block.getTransactions()) {
				if (transaction.getNumber() == transactionId) return transaction;
			}
		}
		
		throw new IllegalStateException("Transaction with id " + transactionId + " from node " + nodeId + " not found.");
	}

	/**
	 * Get transaction from a specific node with a transaction id.
	 * @param nodeId        - identifier of the node
	 * @param blockId       - identifier of the block
	 * @param transactionId - identifier of the transaction
	 * @return              - the transaction
	 */
	public Transaction getTransactionFromNode(int nodeId, int blockId, int transactionId) {
		if (nodeId == Transaction.GENESIS_SENDER) {
			// It's a genesis transaction
			Block genesisBlock = this.ownNode.getChain().getGenesisBlock();
			return genesisBlock.getTransactions().get(transactionId);
		}
		
		Node node = getNode(nodeId);
		try {
			Block block = node.getChain().getBlocks().get(blockId);
			for (Transaction transaction : block.getTransactions()) {
				if (transaction.getNumber() == transactionId) return transaction;
			}
		} catch (IndexOutOfBoundsException ex) {
			throw new IllegalStateException("Block with id " + blockId + " from node " + nodeId + " not found.");
		}
		
		throw new IllegalStateException("Transaction with id " + transactionId + " in block " + blockId + " from node " + nodeId + " not found.");
	}
	
	/**
	 * Adds the given transaction as unspent.
	 * @param transaction - the transaction to add
	 */
	public void addUnspentTransaction(Transaction transaction) {
		if (!unspent.add(transaction)) return;

		if (ownNode.equals(transaction.getReceiver())) {
			availableMoney += transaction.getAmount();
		}
		
		if (ownNode.equals(transaction.getSender())) {
			availableMoney += transaction.getRemainder();
		}
	}
	
	/**
	 * @param toRemove - the unspent transactions to remove
	 */
	public void removeUnspentTransactions(Collection<Transaction> toRemove) {
		for (Transaction transaction : toRemove) {
			if (!unspent.remove(transaction)) continue;
			
			if (ownNode.equals(transaction.getReceiver())) {
				availableMoney -= transaction.getAmount();
			}
			if (ownNode.equals(transaction.getSender())) {
				availableMoney -= transaction.getRemainder();
			}
		}
	}

	/**
	 * @return a new transaction id
	 */
	public synchronized int getNewTransactionId() {
		return transactionId++;
	}

	/**
	 * Initializes the main chain.
	 */
	public void initMainChain() {
		this.mainChain.init();
	}
	
	/**
	 * Fixes all the transactions in the genesis block to use the correct receiver nodes.
	 */
	private void normalizeGenesis() {
		for (Transaction transaction : ownNode.getChain().getGenesisBlock().getTransactions()) {
			Node receiver = getNode(transaction.getReceiver().getId());
			if (receiver != transaction.getReceiver()) {
				transaction.setReceiver(receiver);
			}
		}
	}
}
