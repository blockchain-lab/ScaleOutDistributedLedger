package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;

public interface MainChain {
	public void commitAbstract(BlockAbstract abs);

	public boolean isPresent(BlockAbstract abs);
}
