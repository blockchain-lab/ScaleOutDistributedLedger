package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.MetaKnowledge;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;

/**
 * Class for constructing proofs.
 */
public class ProofConstructor {
	
	private final Transaction mainTransaction;
	private final Node receiver;
	private final Node sender;
	private final Map<Node, List<Block>> toSend;
	private final Proof proof;
	private final LocalStore localStore;
	
	/**
	 * @param mainTransaction - the transaction to construct the proof for
	 * @param localStore      - the local store
	 */
	public ProofConstructor(Transaction mainTransaction, LocalStore localStore) {
		this.mainTransaction = mainTransaction;
		this.receiver = mainTransaction.getReceiver();
		this.sender = mainTransaction.getSender();
		this.proof = new Proof(mainTransaction);
		this.toSend = proof.getChainUpdates();
		this.localStore = localStore;
	}
	
	/**
	 * @return - the constructed proof
	 */
	public synchronized Proof constructProof() {
		//If the proof was already constructed, return it.
		if (!toSend.isEmpty()) return proof;
		
		MetaKnowledge metaKnowledge = receiver.getMetaKnowledge();
		Block nextCommitted = mainTransaction.getBlock().getNextCommittedBlock();
		int[] requirements = nextCommitted.getCachedRequirements();
		for (int i = 0; i < requirements.length; i++) {
			if (requirements[i] == 0) continue;
			
			//Check if we have anything to send for this node
			int firstUnknown = metaKnowledge.getFirstUnknownBlockNumber(i);
			if (requirements[i] < firstUnknown) continue;
			
			//Add the blocks
			Node node = localStore.getNode(i);
			toSend.put(node, node.getChain().getBlocks().subList(firstUnknown, requirements[i] + 1));
		}
		
		int sum = 0;
		int[] actualBlocksRequired = new int[requirements.length];
		for (int i = 0; i < actualBlocksRequired.length; i++) {
			final int nodeId = i;
			Node node = toSend.keySet().stream().filter(n -> n.getId() == nodeId).findFirst().orElse(null);
			if (node == null) continue;
			
			sum += (actualBlocksRequired[i] = toSend.get(node).size());
		}
		
		if (sum > 400) {
			System.out.println("OVER 400 blocks are going to be sent! For " + sender.getId() + " -> " + receiver.getId()
			+ " Block " + mainTransaction.getBlockNumber() + " transaction sources: " + mainTransaction.getSource());
			
			StringBuilder sb = new StringBuilder(4096);
			printTree(mainTransaction, sb, 0);
			
			synchronized (System.out) {
				System.out.println(sender.getId() + " -> " + receiver.getId() + ": "
						+ Arrays.toString(requirements) + " / " + Arrays.toString(actualBlocksRequired));
				System.out.println(sb.toString());
			}
		}
		
		return proof;
	}
	
//	/**
//	 * @return - the constructed proof
//	 */
//	public synchronized Proof constructProof() {
//		//If the proof was already constructed, return it.
//		if (!toSend.isEmpty()) return proof;
//		
//		MetaKnowledge metaKnowledge = receiver.getMetaKnowledge();
//		int mainBlockNr = mainTransaction.getBlockNumber();
//		Block nextCommitted = sender.getChain().getBlocks().get(mainBlockNr).getNextCommittedBlock();
//		if (nextCommitted.getNumber() - mainBlockNr > Settings.INSTANCE.commitEvery) {
//			System.out.println("Block " + mainBlockNr + " of " + sender.getId() + " claims that his commit is more than commitEvery blocks away!");
//		}
//		List<Block> ownBlocks = metaKnowledge.getBlocksToSend(sender, nextCommitted.getNumber());
//		
//		
//		
//		//Base case: no blocks to send
//		if (ownBlocks.isEmpty()) {
////			boolean found = false;
////			for (int i = 0; i < nodesCount; i++) {
////				if (blocksRequired[i] != 0) {
////					found = true;
////					break;
////				}
////			}
////			
////			if (found) System.out.println(sender.getId() + "-> " + receiver.getId() + ": " + Arrays.toString(blocksRequired) + "\n        Nothing");
//			return proof;
//		}
//		
//		//Recursively process all the blocks
//		processBlocks(sender, ownBlocks);
//		
//		final int nodesCount = Settings.INSTANCE.totalNodesNumber;
//		int[] blocksRequired = nextCommitted.getCachedRequirements();
//		if (blocksRequired == null) {
//			throw new IllegalStateException("Block requirements are not cached!");
//		}
//		int[] realBlocksRequired = Arrays.copyOf(blocksRequired, nodesCount);
//		for (int i = 0; i < nodesCount; i++) {
//			blocksRequired[i] = Math.max(blocksRequired[i] - metaKnowledge.getLastKnownBlockNumber(i), 0);
//		}
//		if (ownBlocks.isEmpty()) {
//			blocksRequired[sender.getId()] = 0;
//		} else {
//			blocksRequired[sender.getId()] = Math.max(ownBlocks.get(ownBlocks.size() - 1).getNumber() - metaKnowledge.getLastKnownBlockNumber(sender.getId()), 0);
//		}
//		
//		int sum = 0;
//		int[] actualBlocksRequired = new int[nodesCount];
//		for (int i = 0; i < nodesCount; i++) {
//			final int nodeId = i;
//			Node node = toSend.keySet().stream().filter(n -> n.getId() == nodeId).findFirst().orElse(null);
//			if (node == null) continue;
//			
//			sum += (actualBlocksRequired[i] = toSend.get(node).size());
//		}
//		
//		if (sum > 400) {
//			System.out.println("OVER 400 blocks are going to be sent! For " + sender.getId() + " -> " + receiver.getId()
//			+ " Block " + mainTransaction.getBlockNumber() + " transaction sources: " + mainTransaction.getSource());
//			
//			if (!Arrays.equals(blocksRequired, actualBlocksRequired)) {
//				StringBuilder sb = new StringBuilder(4096);
//				printTree(mainTransaction, sb, 0);
//				
//				synchronized (System.out) {
//					System.out.println(sender.getId() + " -> " + receiver.getId() + ": "
//							+ Arrays.toString(blocksRequired) + " / " + Arrays.toString(realBlocksRequired)
//							+ "\r\n        " + Arrays.toString(actualBlocksRequired));
//					System.out.println(sb.toString());
//				}
//			}
//		}
//		
//		return proof;
//	}
	
	private void printTree(Transaction t, StringBuilder sb, int i) {
		for (Transaction s : t.getSource()) {
			if (s.getBlockNumber() == 0) {
				sb.append("Source: genesis of ").append(s.getReceiver().getId()).append("\r\n");
				continue;
			}
			
			sb.append("Source: T")
				.append(s.getNumber())
				.append(" in B")
				.append(s.getBlockNumber())
				.append(" ")
				.append(s.getSender().getId())
				.append(" -> ")
				.append(s.getReceiver().getId())
				.append(" ")
				.append(Arrays.toString(s.getBlock().getCachedRequirements()))
				.append("\r\n");
			
			printTree(s, sb, i + 1);
		}
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
			
			int blockNumber = source.getBlockNumber();
			
			Block block = owner.getChain().getBlocks().get(blockNumber);
			int nextCommittedBlockNr = block.getNextCommittedBlock().getNumber();
			
			//Determine the blocks that we would need to send.
			MetaKnowledge metaKnowledge = this.receiver.getMetaKnowledge();
			List<Block> blocksOfSource = metaKnowledge.getBlocksToSend(owner, nextCommittedBlockNr);
			if (blocksOfSource.isEmpty()) continue;
			
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
		
		int startBlockNr = current.size();
		List<Block> added = toAdd.subList(startBlockNr, toAdd.size());
		current.addAll(added);
		return added;
	}
}
