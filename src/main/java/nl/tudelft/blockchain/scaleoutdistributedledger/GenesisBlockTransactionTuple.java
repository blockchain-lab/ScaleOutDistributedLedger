package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

/**
 * Special type of transaction tuple to distinguish the genesis money from other transactions.
 */
public class GenesisBlockTransactionTuple extends BlockTransactionTuple {
	public GenesisBlockTransactionTuple(BlockTransactionCreator creator, Transaction transaction) {
		super(creator, transaction);
	}
}
