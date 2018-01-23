package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.validation.ProofValidationException;
import nl.tudelft.blockchain.scaleoutdistributedledger.validation.ValidationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
		this.chainUpdates = new HashMap<>();

		// Decode the transactions while skipping sources
		for (Map.Entry<Integer, List<BlockMessage>> entry : proofMessage.getChainUpdates().entrySet()) {
			List<Block> blocks = new ArrayList<>();
			entry.getValue().forEach(blockMessage -> blocks.add(blockMessage.toBlockWithoutSources(localStore)));
			chainUpdates.put(localStore.getNode(entry.getKey()), blocks);
		}

		// Fix backlinks
		this.fixPreviousBlockPointersAndOrder();

		// Fix the sources
		this.fixTransactionSources(localStore);

		Node senderNode = localStore.getNode(proofMessage.getTransactionMessage().getSenderId());
		ChainView senderChainView = getChainView(senderNode);
		this.transaction = senderChainView.getBlock(proofMessage.getTransactionMessage().getBlockNumber())
				.getTransaction(proofMessage.getTransactionMessage().getNumber());
	}
	
	private void fixPreviousBlockPointersAndOrder() {
		for (Entry<Node, List<Block>> entry : this.chainUpdates.entrySet()) {
			Node node = entry.getKey();
			List<Block> updates = entry.getValue();

			updates.sort(Comparator.comparingInt(Block::getNumber));
			
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
		
		verify(this.transaction, localStore);
	}

	/**
	 * Verifies the given transaction using this proof.
	 * @param transaction - the transaction to verify
	 * @throws ProofValidationException - If the proof is invalid.
	 */
	private void verify(Transaction transaction, LocalStore localStore) throws ProofValidationException {
		int blockNumber = transaction.getBlockNumber().orElse(-1);
		if (blockNumber == -1) {
			throw new ProofValidationException("The transaction has no block number, so we cannot validate it.");
		}
		
		if (transaction.getSender() == null) {
			verifyGenesisTransaction(transaction, localStore);
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
	 * @param localStore  - the local store
	 * @param transaction - the transaction
	 * @return the proof for the given transaction
	 */
	public static Proof createProof(LocalStore localStore, Transaction transaction) {
		Node receiver = transaction.getReceiver();
		
		synchronized (receiver.getMetaKnowledge()) {
			Log.debug("Creating proof based on " + receiver.getMetaKnowledge());
			Proof proof = new Proof(transaction);

			//Step 1: determine what blocks need to be sent
			int blockRequired = transaction.getBlockNumber().getAsInt();
			Chain senderChain = transaction.getSender().getChain();
			
			Map<Node, Integer> metaKnowledge = receiver.getMetaKnowledge();
			
			//The from block is the first unknown block
			int firstUnknownBlock = metaKnowledge.getOrDefault(transaction.getSender(), -1) + 1;
			blockRequired = Math.min(blockRequired, firstUnknownBlock);
			
			Block fromBlock = senderChain.getBlocks().get(blockRequired);
			Block toBlock = getNextCommittedBlock(localStore, blockRequired, senderChain);

			List<Block> blocksToSend = senderChain.getBlocks().subList(fromBlock.getNumber(), toBlock.getNumber());

			//Step 2: determine the chains that need to be sent
//			Map<Chain, Integer> chains = determineChains(transaction, fromBlock, toBlock);
			List<Transaction> sources = blocksToSend.stream().flatMap(block -> block.getTransactions().get(0).getSource().stream()).collect(Collectors.toList());
			Log.debug("Sources of from block: " + fromBlock.getTransactions().get(0).getSource());
			Set<Integer> expected = sources.stream().map(Transaction::getSender).map(n -> n == null ? -1 : n.getId()).collect(Collectors.toSet());

			Map<Chain, Integer> chains = determineChains(transaction, fromBlock, toBlock);
			Set<Integer> actual = chains.keySet().stream().map(Chain::getOwner).map(n -> n == null ? -1 : n.getId()).collect(Collectors.toSet());
			expected.removeAll(actual);
			expected.remove(-1);
			Log.debug("Removing receiver id from expected: " + receiver.getId());
			Log.debug("Removed? " + expected.remove(receiver.getId()));
			if (!expected.isEmpty()) {
				System.out.println("badddd");
			} else {
				Log.debug("actual set of node numbers required: " + actual);
			}

			//Step 3: add only those blocks that are not yet known
			for (Entry<Chain, Integer> entry : chains.entrySet()) {
				Chain chain = entry.getKey();
				Node owner = chain.getOwner();
				if (owner == receiver) continue;
				
				int alreadyKnown = metaKnowledge.getOrDefault(owner, -1);
				int requiredKnown = entry.getValue();
				if (alreadyKnown < requiredKnown) {
					proof.addBlocksOfChain(chain, alreadyKnown + 1, requiredKnown + 1);
				} else {
					Log.debug("Already known " + alreadyKnown + ", required known " + requiredKnown);
				}
			}
			
			return proof;
		}
	}

	/**
	 * @param mainTransaction - the transaction that we want to send
	 * @param fromBlock       - the first block that we want to send
	 * @param toBlock         - the last block that we want to send
	 * @return                - the chains required and the corresponding block number
	 */
	private static Map<Chain, Integer> determineChains(Transaction mainTransaction, Block fromBlock, Block toBlock) {
		//TODO We might want to do some kind of caching?
		
		Node receiver = mainTransaction.getReceiver();
		Map<Node, Integer> metaKnowledge = receiver.getMetaKnowledge();
		
		//Determine what the sender already knows about us
		int alreadyKnown = metaKnowledge.getOrDefault(mainTransaction.getSender(), -1);
		
		Map<Chain, Integer> chains = new HashMap<>();
		
		//Only consider the transactions that the receiver doesn't have yet.
		Block current = toBlock;
		while (current.getNumber() > alreadyKnown) {
			for (Transaction transaction : current.getTransactions()) {
				appendChains2(transaction, receiver, chains);
			}
			
			if (current == fromBlock) break;
			current = current.getPreviousBlock();
		}
		
		//Only add the chains of the transaction itself if it isn't in a known block
		if (mainTransaction.getBlockNumber().getAsInt() > alreadyKnown) {
			appendChains2(mainTransaction, receiver, chains);
		}
		return chains;
	}

	/**
	 * @param localStore
	 * @param blockRequired
	 * @param senderChain
	 * @return
	 */
	private static Block getNextCommittedBlock(LocalStore localStore, int blockRequired, Chain senderChain) {
		ListIterator<Block> it = senderChain.getBlocks().listIterator(blockRequired);
		while (it.hasNext()) {
			Block block = it.next();
			if (block.isOnMainChain(localStore)) {
				return block;
			}
		}
		
		throw new IllegalStateException("There is no next committed block!");
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
	
	/**
	 * Recursively calls itself with all the sources of the given transaction. Transactions which
	 * are in the chain of {@code receiver} are ignored.
	 * @param transaction - the transaction to check the sources of
	 * @param receiver    - the node receiving the transaction
	 * @param chains      - the map of chains to append to
	 */
	public static void appendChains2(Transaction transaction, Node receiver, Map<Chain, Integer> chains) {
		Node owner = transaction.getSender();
		if (owner == null || owner == receiver) return;
		
		//Skip transactions that are already known
		Map<Node, Integer> metaKnowledge = receiver.getMetaKnowledge();
		int alreadyKnown = metaKnowledge.getOrDefault(owner, -1);
		int blockNumber = transaction.getBlockNumber().getAsInt();
		if (alreadyKnown >= blockNumber) return;
		
		//Store the highest block number.
		chains.compute(owner.getChain(), (k, v) -> v == null ? blockNumber : Math.max(v, blockNumber));
		for (Transaction source : transaction.getSource()) {
			appendChains2(source, receiver, chains);
		}
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
