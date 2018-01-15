package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

/**
 * Proof class.
 */
public class Proof {

	@Getter
	private final Transaction transaction;

	@Getter
	private final Map<Node, List<Block>> chainUpdates;

	/**
	 * Constructor.
	 * @param transaction - the transaction to be proven.
	 */
	public Proof(Transaction transaction) {
		this.transaction = transaction;
		this.chainUpdates = new HashMap<>();
	}
	
	/**
	 * Constructor.
	 * @param transaction  - the transaction to be proven.
	 * @param chainUpdates - a map of chain updates
	 */
	public Proof(Transaction transaction, Map<Node, List<Block>> chainUpdates) {
		this.transaction = transaction;
		this.chainUpdates = chainUpdates;
	}
	
	/**
	 * Constructor to decode a proof message.
	 * @param proofMessage - proof received from the network
	 * @param localStore - local store
	 * @throws IOException - error while getting node info from tracker
	 */
	public Proof(ProofMessage proofMessage, LocalStore localStore) throws IOException {
		this.transaction = new Transaction(proofMessage.getTransactionMessage(), localStore);
		this.chainUpdates = new HashMap<>();
		for (Map.Entry<Integer, List<BlockMessage>> entry : proofMessage.getChainUpdates().entrySet()) {
			Node node = localStore.getNode(entry.getKey());
			List<BlockMessage> blockMessageList = entry.getValue();
			// Convert BlockMessage to Block
			List<Block> blockList = new ArrayList<>();
			for (BlockMessage blockMessage : blockMessageList) {
				blockList.add(new Block(blockMessage, localStore));
			}
			this.chainUpdates.put(node, blockList);
		}
	}
	
	/**
	 * Add a block to the proof.
	 * @param block - the block to be added
	 */
	public void addBlock(Block block) {
		List<Block> blocks = chainUpdates.get(block.getOwner());
		if (blocks == null) {
			blocks = new ArrayList<>();
			chainUpdates.put(block.getOwner(), blocks);
		}
		blocks.add(block);
	}
	
	/**
	 * Adds the blocks with numbers start to end of the given chain to the proof.
	 * @param chain - the chain
	 * @param start - the block to start at (inclusive)
	 * @param end   - the block to end at (exclusive)
	 */
	public void addBlocksOfChain(Chain chain, int start, int end) {
		if (start >= end || end > chain.getBlocks().size()) return;
		
		List<Block> blocks = chainUpdates.get(chain.getOwner());
		if (blocks == null) {
			blocks = new ArrayList<>();
			chainUpdates.put(chain.getOwner(), blocks);
		}
		blocks.addAll(chain.getBlocks().subList(start, end));
	}

	/**
	 * Verifies this proof.
	 * @return - boolean indicating if this proof is valid.
	 */
	public boolean verify(LocalStore localStore) {
		return verify(this.transaction, localStore);
	}

	/**
	 * Verifies the given transaction using this proof.
	 * @param transaction - the transaction to verify
	 * @return - boolean indicating if this transaction is valid.
	 */
	private boolean verify(Transaction transaction, LocalStore localStore) {
		// Check genesis transactions
		if (transaction.getSender() == null && transaction.getBlockNumber().isPresent() && transaction.getBlockNumber().getAsInt() == 0) {
			Log.log(Level.FINE, "Verfied genesis block");
			return true;
		}

		int absmark = 0;
		boolean seen = false;

		ChainView chainView = new ChainView(transaction.getSender().getChain(), chainUpdates.get(transaction.getSender()));
		if (!chainView.isValid()) {
			Log.log(Level.WARNING, "Invalid ChainView found, proof not verified");
			return false;
		}

		for (Block block : chainView) {
			if (block.getTransactions().contains(transaction)) {
				if (seen) {
					Log.log(Level.WARNING, "Duplicate transaction found, proof not verified");
					return false;
				}
				seen = true;
			}
			if (localStore.getMainChain().isPresent(block)) absmark = block.getNumber();
		}

		OptionalInt blockNumber = transaction.getBlockNumber();
		if (!blockNumber.isPresent() || absmark < blockNumber.getAsInt()) {
			Log.log(Level.WARNING, "No suitable committed block found, proof not verified");
			return false;
		}

		// Verify source transaction
		for (Transaction sourceTransaction : transaction.getSource()) {
			if (!verify(sourceTransaction, localStore)) return false;
		}
		return true;
	}
	
	/**
	 * Applies the updates in this proof.
	 */
	public void applyUpdates() {
		for (Entry<Node, List<Block>> entry : chainUpdates.entrySet()) {
			Node node = entry.getKey();
			List<Block> updates = entry.getValue();
			node.getChain().update(updates);
		}
	}
	
	/**
	 * @param transaction - the transaction
	 * @return the proof for the given transaction
	 */
	public static Proof createProof(Transaction transaction) {
		Node receiver = transaction.getReceiver();
		Proof proof = new Proof(transaction);
		
		//Step 1: determine the chains that need to be sent
		//TODO We might want to do some kind of caching?
		Set<Chain> chains = new HashSet<>();
		appendChains(transaction, receiver, chains);
		
		//Step 2: add only those blocks that are not yet known
		Map<Node, Integer> metaKnowledge = receiver.getMetaKnowledge();
		for (Chain chain : chains) {
			Node owner = chain.getOwner();
			int alreadyKnown = metaKnowledge.getOrDefault(owner, -1);
			int requiredKnown = chain.getLastCommittedBlock().getNumber();
			
			proof.addBlocksOfChain(chain, alreadyKnown + 1, requiredKnown + 1);
		}
		
		return proof;
	}
	
	/**
	 * Recursively calls itself with all the sources of the given transaction. Transactions which
	 * are in the chain of {@code receiver} are ignored.
	 * @param transaction - the transaction to check the sources of
	 * @param receiver    - the node receiving the transaction
	 * @param chains      - the list of chains to append to
	 */
	public static void appendChains(Transaction transaction, Node receiver, Set<Chain> chains) {
		Node owner = transaction.getSender();
		if (owner == null || owner == receiver) return;
		
		chains.add(owner.getChain());
		for (Transaction source : transaction.getSource()) {
			appendChains(source, receiver, chains);
		}
	}
}
