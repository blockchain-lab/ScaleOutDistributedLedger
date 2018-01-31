package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.validation.ProofValidationException;
import nl.tudelft.blockchain.scaleoutdistributedledger.validation.ValidationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Proof class.
 */
public class Proof {

	@Getter
	private final Transaction transaction;

	@Getter
	private final Map<Node, List<Block>> chainUpdates;
	
	private final Map<Node, ChainView> chainViews = new HashMap<>();

	/**
	 * Constructor.
	 * @param transaction - the transaction to be proven.
	 */
	public Proof(Transaction transaction) {
		this.transaction = transaction;
		this.chainUpdates = new HashMap<>();
	}
	
	/**
	 * Constructor to decode a proof message.
	 * @param proofMessage - proof received from the network
	 * @param localStore - local store
	 * @throws IOException - error while getting node info from tracker
	 */
	public Proof(ProofMessage proofMessage, LocalStore localStore) throws IOException {
		this.chainUpdates = new HashMap<>();

		// Decode the transactions while skipping sources
		for (Map.Entry<Integer, List<BlockMessage>> entry : proofMessage.getChainUpdates().entrySet()) {
			List<Block> blocks = new ArrayList<>();
			//TODO Turn into normal for loop
			entry.getValue().forEach(blockMessage -> blocks.add(blockMessage.toBlockWithoutSources(localStore)));
			chainUpdates.put(localStore.getNode(entry.getKey()), blocks);
		}

		// Fix backlinks
		this.fixPreviousBlockPointers();

		// Fix the sources
		this.fixTransactionSources(localStore);

		Node senderNode = localStore.getNode(proofMessage.getTransactionMessage().getSenderId());
		ChainView senderChainView = getChainView(senderNode);
		this.transaction = senderChainView.getBlock(proofMessage.getTransactionMessage().getBlockNumber())
				.getTransaction(proofMessage.getTransactionMessage().getNumber());
	}
	
	private void fixPreviousBlockPointers() {
		for (Entry<Node, List<Block>> entry : this.chainUpdates.entrySet()) {
			Node node = entry.getKey();
			List<Block> updates = entry.getValue();
			if (updates.isEmpty()) continue;
			
			Block previousBlock = null;
			for (int i = 0; i < updates.size(); i++) {
				Block block = updates.get(i);
				block.setPreviousBlock(previousBlock);
				previousBlock = block;
			}
			
			Block firstBlock = updates.get(0);
			if (firstBlock.getNumber() != 0) {
				previousBlock = node.getChain().getBlocks().get(firstBlock.getNumber() - 1);
				firstBlock.setPreviousBlock(previousBlock);
			}
		}
	}

	private void fixTransactionSources(LocalStore localStore) {
		HashMap<Integer, LightView> lightViews = new HashMap<>();
		// Initialize the chainviews only once
		for (Node node : this.chainUpdates.keySet()) {
			lightViews.put(node.getId(), new LightView(node.getChain(), chainUpdates.get(node)));
		}

		// For all transactions of all nodes do
		for (Node node : this.chainUpdates.keySet()) {
			for (Block block : this.chainUpdates.get(node)) {
				for (Transaction tx : block.getTransactions()) {
					for (Entry<Integer, int[]> entry : tx.getMessage().getSource()) {
						Block sourceBlock;
						if (!lightViews.containsKey(entry.getKey())) {
							sourceBlock = localStore.getNode(entry.getKey()).getChain().getBlocks().get(entry.getValue()[0]);
						} else {
							sourceBlock = lightViews.get(entry.getKey()).getBlock(entry.getValue()[0]);
						}
						tx.getSource().add(sourceBlock.getTransaction(entry.getValue()[1]));
					}
				}
			}
		}
	}

	/**
	 * Add a block to the proof.
	 * @param block - the block to be added
	 */
	public void addBlock(Block block) {
		List<Block> blocks = chainUpdates.computeIfAbsent(block.getOwner(), k -> new ArrayList<>());
		blocks.add(block);
	}
	
	/**
	 * Adds the blocks with numbers start to end of the given chain to the proof.
	 * @param chain - the chain
	 * @param start - the block to start at (inclusive)
	 * @param end   - the block to end at (exclusive)
	 */
	public void addBlocksOfChain(Chain chain, int start, int end) {
		//TODO IMPORTANT Is this check correct? Shouldn't it be start > end? And the end also seems strange.
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
	 * @param localStore - the local store
	 * @throws ProofValidationException - If this proof is invalid.
	 */
	public void verify(LocalStore localStore) throws ProofValidationException {
		if (this.transaction.getSender() == null) {
			throw new ProofValidationException("We directly received a transaction with a null sender.");
		}
		
		//TODO [BFT] all the blocks that were sent but not required for the proof are not validated at all.
		verify(this.transaction, localStore);
	}

	/**
	 * Verifies the given transaction using this proof.
	 * @param transaction - the transaction to verify
	 * @throws ProofValidationException - If the proof is invalid.
	 */
	private void verify(Transaction transaction, LocalStore localStore) throws ProofValidationException {
		if (transaction.isLocallyVerified()) return;

		int blockNumber = transaction.getBlockNumber().orElse(-1);
		if (blockNumber == -1) {
			throw new ProofValidationException("The transaction has no block number, so we cannot validate it.");
		}
		
		if (transaction.getSender() == null) {
			verifyGenesisTransaction(transaction, localStore);
			transaction.setLocallyVerified(true);
			return;
		}

		int absmark = 0;
		boolean seen = false;

		ChainView chainView = getChainView(transaction.getSender());
		if (!chainView.isValid()) {
			throw new ProofValidationException("ChainView of node " + transaction.getSender().getId() + " is invalid.");
		}

		for (Block block : chainView) {
			if (block.getTransactions().contains(transaction)) {
				if (seen) {
					throw new ProofValidationException("Duplicate transaction found.");
				}
				seen = true;
			}
			if (block.isOnMainChain(localStore)) absmark = block.getNumber();
		}

		if (absmark < blockNumber) {
			System.out.println(this.getTransaction());
			throw new ProofValidationException("No suitable committed block found");
		}

		// Verify source transaction
		for (Transaction sourceTransaction : transaction.getSource()) {
			try {
				verify(sourceTransaction, localStore);
			} catch (ValidationException ex) {
				ex.printStackTrace();
				System.out.println(this);
				System.exit(1);
				throw new ProofValidationException("Source " + sourceTransaction + " is not valid", ex);
			}
		}
		transaction.setLocallyVerified(true);
	}
	
	/**
	 * Verifies a genesis transaction.
	 * @param transaction - the genesis transaction
	 * @param localStore  - the local store
	 * @throws ProofValidationException - If the given transaction is not a valid genesis transaction.
	 */
	private void verifyGenesisTransaction(Transaction transaction, LocalStore localStore) throws ProofValidationException {
		int blockNumber = transaction.getBlockNumber().orElse(-1);
		if (blockNumber != 0) {
			throw new ProofValidationException("Genesis transaction " + transaction + " is invalid: block number is not 0");
		}
		
		Node receiver = transaction.getReceiver();
		ChainView chainView = getChainView(receiver);
		Block genesisBlock;
		try {
			genesisBlock = chainView.getBlock(0);
		} catch (IndexOutOfBoundsException ex) {
			throw new ProofValidationException("The genesis block for node " + receiver.getId() + " cannot be found!");
		} catch (IllegalStateException ex) {
			throw new ProofValidationException("ChainView of node " + receiver.getId() + " is invalid.");
		}
		
		if (!genesisBlock.isOnMainChain(localStore)) {
			throw new ProofValidationException("The genesis block of node " + receiver.getId() + " is not on the main chain.");
		}
	}
	
	/**
	 * @param node - the node
	 * @return - a chainview for the specified node
	 */
	public synchronized ChainView getChainView(Node node) {
		ChainView chainView = chainViews.get(node);
		if (chainView == null) {
			chainView = new ChainView(node.getChain(), chainUpdates.get(node), false);
			chainView.isValid();
			chainViews.put(node, chainView);
		}
		return chainView;
	}
	
	/**
	 * Applies the updates in this proof.
	 * This method also updates the meta knowledge of the sender of the transaction.
	 * @param localStore - the localStore
	 */
	public void applyUpdates(LocalStore localStore) {
		for (Entry<Node, List<Block>> entry : chainUpdates.entrySet()) {
			Node node = entry.getKey();
			
			List<Block> updates = entry.getValue();
			node.getChain().update(updates, localStore);
		}
		
		//Update the meta knowledge of the sender
		transaction.getSender().updateMetaKnowledge(this);
	}
	
	/**
	 * Recursively calls itself with all the sources of the given transaction. Transactions which
	 * are in the chain of {@code receiver} are ignored.
	 * @param nrOfNodes   - the total number of nodes
	 * @param transaction - the transaction to check the sources of
	 * @param receiver    - the node receiving the transaction
	 * @param chains      - the list of chains to append to
	 */
	public static void appendChains(int nrOfNodes, Transaction transaction, Node receiver, MetaKnowledge metaKnowledge, Set<Chain> chains) {
		Node owner = transaction.getSender();
		if (owner == null || owner == receiver) return;
		
		//TODO Do we want to cut off at known blocks?
		int alreadyKnown = metaKnowledge.getFirstUnknownBlockNumber(owner);
		int blockNumber = transaction.getBlockNumber().getAsInt();
		if (alreadyKnown >= blockNumber) return;
		
		chains.add(owner.getChain());
		if (chains.size() >= nrOfNodes - 1) return;
		
		for (Transaction source : transaction.getSource()) {
			appendChains(nrOfNodes, source, receiver, metaKnowledge, chains);
		}
	}
	
	/**
	 * Recursively calls itself with all the sources of the given transaction. Transactions which
	 * are in the chain of {@code receiver} are ignored.
	 * @param nrOfNodes   - the total number of nodes
	 * @param transaction - the transaction to check the sources of
	 * @param receiver    - the node receiving the transaction
	 * @param chains      - the map of chains to append to
	 */
	public static void appendChains2(int nrOfNodes, Transaction transaction, Node receiver, Map<Node, Integer> chains) {
		Node owner = transaction.getSender();
		if (owner == null || owner == receiver) return;
		
		//Skip transactions that are already known
		MetaKnowledge metaKnowledge = receiver.getMetaKnowledge();
		int lastKnown = metaKnowledge.getLastKnownBlockNumber(owner);
		int blockNumber = transaction.getBlockNumber().getAsInt();
		if (lastKnown >= blockNumber) return;
		
		//Store the highest block number.
		//Now consider this chain up to the last committed block
		chains.merge(owner, blockNumber, Math::max);
		if (chains.size() >= nrOfNodes - 1) return;
		
		//Check all the sources
		for (Transaction source : transaction.getSource()) {
			appendChains2(nrOfNodes, source, receiver, chains);
		}
	}

	/**
	 * Gets the number of blocks used in the proof.
	 * @return - the number of blocks
	 */
	public int getNumberOfBlocks() {
		return chainUpdates.values().stream().mapToInt(List::size).sum();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Proof: ").append(transaction);
		
		for (Entry<Node, List<Block>> entry : this.chainUpdates.entrySet()) {
			sb.append('\n').append(entry.getKey().getId()).append(": ").append(entry.getValue());
		}
		return sb.toString();
	}
}
