package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lombok.Getter;

/**
 * A class for representing meta knowledge (what we know that they know).
 */
public class MetaKnowledge extends HashMap<Integer, Integer> {
	private static final long serialVersionUID = 1L;
	
	/**
	 * @return - the node that this meta knowledge belongs to
	 */
	@Getter
	private final Node owner;
	
	/**
	 * @param owner - the owner of this meta knowledge
	 */
	public MetaKnowledge(Node owner) {
		this.owner = owner;
	}
	
	/**
	 * Determines the blocks that we have to send to owner in order to update them to the given
	 * end block number.
	 * 
	 * This method returns {@link Collections#emptyList()} if the given node is the owner or if
	 * owner already knows about all the blocks up till the given block number.
	 * @param node       - the node to send blocks of
	 * @param endBlockNr - the number of the last block we want to send
	 * @return           - the list of blocks to send
	 * @throws IndexOutOfBoundsException - If the given node does not have a block with endBlockNr.
	 */
	public List<Block> getBlocksToSend(Node node, int endBlockNr) {
		if (node == owner) return Collections.emptyList();
		
		//Determine the first block they don't know
		int firstUnknown = getFirstUnknownBlockNumber(node);
		
		//They already know everything we want to send
		if (firstUnknown > endBlockNr) return Collections.emptyList();
		
		//Get a sublist of the blocks on the chain. We need + 1 on endBlock because the end is exclusive.
		return node.getChain().getBlocks().subList(firstUnknown, endBlockNr + 1);
	}
	
	/**
	 * The number returned by this method is the number of the last block that {@link #getOwner()}
	 * knows from node {@code node}.
	 * 
	 * This method returns -1 if the owner of this meta knowledge does not know about this block.
	 * @param node - the node
	 * @return - the number of the last block from the given node that is known by owner
	 */
	public int getLastKnownBlockNumber(Node node) {
		return getLastKnownBlockNumber(node.getId());
	}
	
	/**
	 * The number returned by this method is the number of the last block that {@link #getOwner()}
	 * knows from the node with id {@code nodeId}.
	 * 
	 * This method returns -1 if the owner of this meta knowledge does not know about this block.
	 * @param nodeId - the id of the node
	 * @return - the number of the last block from the given node that is known by owner
	 */
	public int getLastKnownBlockNumber(int nodeId) {
		return getOrDefault(nodeId, -1);
	}
	
	/**
	 * @param node - the node
	 * @return - the number of the first block from the given node that is unknown by owner
	 */
	public int getFirstUnknownBlockNumber(Node node) {
		return getFirstUnknownBlockNumber(node.getId());
	}
	
	/**
	 * @param nodeId - the id of the node
	 * @return - the number of the first block from the given node that is unknown by owner
	 */
	public int getFirstUnknownBlockNumber(int nodeId) {
		return getLastKnownBlockNumber(nodeId) + 1;
	}
	
	/**
	 * Updates the last known block number of the given node to the given blockNumber.
	 * If the given block number is lower than the current last known block number, then this
	 * method does nothing.
	 * @param node        - the node
	 * @param blockNumber - the block number
	 */
	public void updateLastKnownBlockNumber(Node node, int blockNumber) {
		updateLastKnownBlockNumber(node.getId(), blockNumber);
	}
	
	/**
	 * Updates the last known block number of the given node to the given blockNumber.
	 * If the given block number is lower than the current last known block number, then this
	 * method does nothing.
	 * @param nodeId      - the id of the node
	 * @param blockNumber - the block number
	 */
	public synchronized void updateLastKnownBlockNumber(int nodeId, int blockNumber) {
		merge(nodeId, blockNumber, (oldNr, newNr) -> Math.max(oldNr, newNr));
	}
}
