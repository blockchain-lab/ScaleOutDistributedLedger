package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.*;

import java.util.*;

/**
 * Class for constructing proofs.
 */
public class ProofConstructor {
	
	private final Transaction mainTransaction;
	private final Node receiver;
	private final Node sender;
	private final Map<Node, List<Block>> toSend;
	private final Proof proof;
	
	/*
	 * The Problem:
	 * We want to send block A.
	 * We have committed block B.
	 * With metaknowledge, we can translate this to a list of blocks that need to be sent.
	 * 
	 * From all these blocks, we have to determine "what is the last block of each node that needs to be sent."
	 * This is "loop over the blocks, loop over the sources of transactions (recursively):
	 * 
	 * Block A
	 * Block B
	 * Node sender
	 * Node receiver
	 * List<Block> ownBlocks = MetaKnowledge.determineBlocks(sender, B)
	 * processBlocks(sender, ownBlocks)
	 * Map<Node, List<Block>> toSend
	 * Map<Node, Set<Integer>> alreadyChecked
	 * 
	 * processBlocks(Node owner, List<Block> blocks):
	 *     //newlyAdded is the list of all the blocks that were added (not already present) (HAS TO BE ORDERED)
	 *     List<Block> newlyAdded = toSend.addAll(owner, blocks)
	 *     for (Block b : newlyAdded) {
	 *         alreadyChecked.add(owner, b.getNumber())
	 *         for (Transaction t : b.getTransactions()) {
	 *             processSources(t)
	 *         }
	 *     }
	 * 
	 * processSources(Transaction t):
	 *     for (Transaction s : t.getSource())
	 *         Node owner = s.getOwner();
	 *         //Skip all sources in genesis blocks, our own blocks or in receiver blocks
	 *         if (owner == null || owner == sender || owner == receiver) continue;
	 *         
	 *         Block b = s.getBlock()
	 *         //Skip all blocks that were already checked
	 *         if (b.getNumber() in alreadyChecked.get(owner)) continue;
	 *         
	 *         Block bCom = the first committed block at or after b
	 *         if (bCom.getNumber() in alreadyChecked.get(owner)) continue;
	 *         
	 *         List<Block> blocksOfSource = MetaKnowledge.determineBlocks(owner, bCom)
	 *         if (blocksOfSource is empty) {
	 *             alreadyChecked.addAll(owner, 0 up to (inclusive) bCom.getNumber())
	 *             continue;
	 *         }
	 *         
	 *         processBlocks(owner, blocksOfSource)
	 */
	
	/**
	 * @param mainTransaction - the transaction to construct the proof for
	 */
	public ProofConstructor(Transaction mainTransaction) {
		this.mainTransaction = mainTransaction;
		this.receiver = mainTransaction.getReceiver();
		this.sender = mainTransaction.getSender();
		this.proof = new Proof(mainTransaction);
		this.toSend = proof.getChainUpdates();
	}
	
	/**
	 * @return - the constructed proof
	 */
	public synchronized Proof constructProof() {
		//If the proof was already constructed, return it.
		if (!toSend.isEmpty()) return proof;
		
		MetaKnowledge metaKnowledge = receiver.getMetaKnowledge();
		int mainBlockNr = mainTransaction.getBlockNumber().getAsInt();
		Block nextCommitted = sender.getChain().getBlocks().get(mainBlockNr).getNextCommittedBlock();
		List<Block> ownBlocks = metaKnowledge.getBlocksToSend(sender, nextCommitted.getNumber());
		
		//Base case: no blocks to send
		if (ownBlocks.isEmpty()) {
			return proof;
		}
		
		//Recursively process all the blocks
		processBlocks(sender, ownBlocks);
		return proof;
	}
	
	/**
	 * Processes the given list of blocks belonging to the given owner.
	 * The given list is expected to be non-empty.
	 * @param owner  - the owner of the blocks
	 * @param blocks - the blocks
	 */
	protected void processBlocks(Node owner, List<Block> blocks) {
		List<Block> newlyAdded = addBlocksToSend(owner, blocks);
		for (Block block : newlyAdded) {
			for (Transaction transaction : block.getTransactions()) {
				processSources(transaction);
			}
		}
	}
	
	/**
	 * Processes the sources of the given transaction.
	 * @param transaction - the transaction to process
	 */
	protected void processSources(Transaction transaction) {
		for (Transaction source : transaction.getSource()) {
			Node owner = source.getSender();
			//Skip all sources in genesis blocks, our own blocks and in receiver blocks
			if (owner == null || owner == this.sender || owner == this.receiver) continue;
			
			int blockNumber = source.getBlockNumber().getAsInt();
			
			Block block = owner.getChain().getBlocks().get(blockNumber);
			int nextCommittedBlockNr = block.getNextCommittedBlock().getNumber();
			
			//Determine the blocks that we would need to send.
			MetaKnowledge metaKnowledge = this.receiver.getMetaKnowledge();
			List<Block> blocksOfSource = metaKnowledge.getBlocksToSend(owner, nextCommittedBlockNr);
			
			processBlocks(owner, blocksOfSource);
		}
	}
	
	/**
	 * Adds the given blocks belonging to the given owner to the toSend map.
	 * @param owner - the owner of the blocks
	 * @param toAdd - the blocks to add
	 * @return      - all the blocks that were added (not already in the toSend map)
	 */
	protected List<Block> addBlocksToSend(Node owner, List<Block> toAdd) {
		List<Block> current = toSend.computeIfAbsent(owner, n -> new ArrayList<>());
		if (current.isEmpty()) {
			current.addAll(toAdd);
			return toAdd;
		}
		
		if (current.size() >= toAdd.size()) {
			//Nothing new
			return Collections.EMPTY_LIST;
		}
		
		//Since the blocks we are adding have been selected through the meta knowledge, they will go from firstUnknown up to a certain block.
		//E.g. [0, 1, 2] or [2, 3]
		//Also, we skip all blocks that we have already checked, we know that the entire history is effectively linear and that we check from
		//low to high block numbers. This means that the given list will contain all elements that we already have + some extra.
		//So we can start at the index equal to what we already have.
		
		assert current.containsAll(toAdd);
		assert current.size() < toAdd.size();
		
		int startBlockNr = current.size();
		List<Block> added = toAdd.subList(startBlockNr, toAdd.size());
		current.addAll(added);
		return added;
	}
}
