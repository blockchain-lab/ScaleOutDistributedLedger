package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.ReversedIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Chain class.
 */
public class Chain {

	@Getter
	private final Node owner;

	@Getter
	private final List<Block> blocks;
	
	/**
	 * TODO The last committed Block has to be set somewhere.
	 * @return the last block that was committed to the main chain
	 */
	@Getter @Setter
	private Block lastCommittedBlock;

	/**
	 * Constructor.
	 * @param owner - the owner of this chain.
	 */
	public Chain(Node owner) {
		this.owner = owner;
		this.blocks = new ArrayList<>();
	}

	/**
	 * Constructor.
	 * @param owner - the owner of this chain.
	 * @param blocks - list of blocks in this chain.
	 */
	public Chain(Node owner, List<Block> blocks) {
		this.owner = owner;
		this.blocks = blocks;
	}
	
	/**
	 * Updates this chain with the given updates.
	 * This method is used for updating a chain belonging to a different node.
	 * @param updates - the new blocks to append
	 * @param localStore - the localStore
	 * @throws UnsupportedOperationException - If this chain is owned by us.
	 */
	public synchronized void update(List<Block> updates, LocalStore localStore) {
		if (owner instanceof OwnNode) throw new UnsupportedOperationException("You cannot use update to update your own chain");
		
		if (updates.isEmpty()) return;
		
		if (blocks.isEmpty()) {
			blocks.addAll(updates);
		} else {
			Block lastBlock = blocks.get(blocks.size() - 1);
			int nextNr = lastBlock.getNumber() + 1;
			for (Block block : updates) {
				//Skip any overlap
				if (block.getNumber() != nextNr) continue;
				blocks.add(block);
				nextNr++;
			}
		}
		for (Block block : ReversedIterator.reversed(this.blocks)) {
			if (block.isOnMainChain(localStore)) {
				this.lastCommittedBlock = block;
				return;
			}
		}
	}
	
	/**
	 * @return the last block in this chain
	 */
	public synchronized Block getLastBlock() {
		if (blocks.isEmpty()) return null;

		return blocks.get(blocks.size() - 1);
	}
	
	/**
	 * @return - the genesis block
	 */
	public synchronized Block getGenesisBlock() {
		if (blocks.isEmpty()) return null;

		return blocks.get(0);
	}
	
	/**
	 * Creates a new block with the given transactions and appends it to this chain.
	 * @param transactions - the transactions to put in the block
	 * @return             - the newly appended block
	 * @throws UnsupportedOperationException - If this chain is not owned by us.
	 * @throws IllegalStateException         - If there is no genesis block in this chain.
	 */
	public synchronized Block appendNewBlock(List<Transaction> transactions) {
		if (!(owner instanceof OwnNode)) throw new UnsupportedOperationException("You cannot append blocks to a chain that is not yours!");
		
		Block last = getLastBlock();
		if (last == null) throw new IllegalStateException("There is no genesis block!");
		
		Block newBlock = new Block(last, transactions);
		newBlock.setOwner(this.owner);
		blocks.add(newBlock);
		return newBlock;
	}
}
