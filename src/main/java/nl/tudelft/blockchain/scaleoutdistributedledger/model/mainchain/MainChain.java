package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;

/**
 * Interface for main chain inplementations.
 */
public interface MainChain {
	/**
	 * Commit an abstract to the main chain.
	 *
	 * @param abs - the abstract to commit
	 * @return - the hash given to the transaction on commit
	 */
	public Sha256Hash commitAbstract(BlockAbstract abs);

	/**
	 * Query the main chain for the presence of a block.
	 *
	 * @param block - the block to query for
	 * @return - true when present, false otherwise
	 */
//	public default boolean isPresent(Block block) {
//		return isPresent(block.getHash());
//	}

	/**
	 * Check whether the block (from a local chain) is on the main chain (in a form of BlockAbstract).
	 * @param block the hash of the block
	 * @return true if there is a block abstract of the given block, false otherwise.
	 */
	public boolean isPresent(Block block);

	/**
	 * Initializes the tendermint chain.
	 */
	public void init();

	/**
	 * Stop the main chain.
	 */
	public void stop();
}
