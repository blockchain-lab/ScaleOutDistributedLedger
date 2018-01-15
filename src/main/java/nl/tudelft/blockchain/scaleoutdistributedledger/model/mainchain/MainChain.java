package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain;

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
	 * Query the main chain for the presence of an abstract.
	 *
	 * @param abs - the abstract to query for
	 * @return - true when present, false otherwise
	 */
	public boolean isPresent(BlockAbstract abs);
	
	/**
	 * Stops the main chain.
	 */
	public void stop();
}
