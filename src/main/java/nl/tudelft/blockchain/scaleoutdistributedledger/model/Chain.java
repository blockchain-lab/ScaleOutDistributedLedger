package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.ReversedIterator;

/**
 * Chain class.
 */
public class Chain {

	@Getter
	private final Node owner;

	@Getter
	private final List<Block> blocks;
	
	@Getter
	private Transaction genesisTransaction;
	
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
		
		ArrayList<Block> copy = new ArrayList<>(updates);
		copy.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
		if (!copy.equals(updates)) {
			throw new IllegalArgumentException("The blocks are not ordered correctly :(");
		}
		
		int nextNr;
		Block previousBlock;
		if (blocks.isEmpty()) {
			//Should start at 0, there is no previous block
			nextNr = 0;
			previousBlock = null;
		} else {
			//Should start with the first block after our last block.
			Block lastBlock = blocks.get(blocks.size() - 1);
			nextNr = lastBlock.getNumber() + 1;
			previousBlock = lastBlock;
		}
		
		for (Block block : updates) {
			//Skip any overlap
			if (block.getNumber() != nextNr) continue;
			block.setPreviousBlock(previousBlock);
			blocks.add(block);
			nextNr++;
			previousBlock = block;
		}
		
		for (Block block : ReversedIterator.reversed(this.blocks)) {
			if (block.isOnMainChain(localStore)) {
				setLastCommittedBlock(block);
				return;
			}
		}
	}
	
	/**
	 * @return - the genesis block
	 */
	public synchronized Block getGenesisBlock() {
		if (blocks.isEmpty()) return null;

		return blocks.get(0);
	}
	
	/**
	 * Sets the genesis block.
	 * @param genesisBlock - the genesis block
	 * @throws IllegalStateException - If this chain already has a genesis block.
	 */
	public synchronized void setGenesisBlock(Block genesisBlock) {
		if (!blocks.isEmpty()) throw new IllegalStateException("Adding genesis block to non-empty chain");
		
		blocks.add(genesisBlock);
		setLastCommittedBlock(genesisBlock);
		genesisTransaction = findGenesisTransaction(owner, genesisBlock);
		if (genesisTransaction != null) {
			genesisTransaction.setReceiver(owner);
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
	 * @return - the number of the last block
	 */
	public int getLastBlockNumber() {
		//TODO Remove
		Block last;
		assert blocks.size() - 1 == ((last = getLastBlock()) == null ? -1 : last.getNumber());
		return blocks.size() - 1;
	}
	
	/**
	 * @return - the last block that was committed to the main chain
	 */
	public synchronized Block getLastCommittedBlock() {
		return lastCommittedBlock;
	}
	
	/**
	 * Sets the last committed block to the given block.
	 * If the given block is before the current last committed block then this method has no effect.
	 * @param block - the block
	 */
	public synchronized void setLastCommittedBlock(Block block) {
		if (lastCommittedBlock == null) {
			lastCommittedBlock = block;
		} else if (block.getNumber() > lastCommittedBlock.getNumber()) {
			lastCommittedBlock = block;
		}
	}
	
	/**
	 * Creates a new block and appends it to this chain.
	 * @return - the newly appended block
	 * @throws UnsupportedOperationException - If this chain is not owned by us.
	 * @throws IllegalStateException         - If there is no genesis block in this chain.
	 */
	public synchronized Block appendNewBlock() {
		if (!(owner instanceof OwnNode)) throw new UnsupportedOperationException("You cannot append blocks to a chain that is not yours!");
		
		Block last = getLastBlock();
		if (last == null) throw new IllegalStateException("There is no genesis block!");
		
		Block newBlock = new Block(last, this.owner);
		blocks.add(newBlock);
		return newBlock;
	}
	
	/**
	 * Finds the genesis transaction of the given node.
	 * @param node         - the node 
	 * @param genesisBlock - the genesis block
	 * @return             - the genesis transaction of the given node, or null if there is none
	 */
	private static Transaction findGenesisTransaction(Node node, Block genesisBlock) {
		return genesisBlock.getTransactions()
				.stream()
				.filter(t -> t.getReceiver().getId() == node.getId())
				.findFirst()
				.orElse(null);
	}
}
