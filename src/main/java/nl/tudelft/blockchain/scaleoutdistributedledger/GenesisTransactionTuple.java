package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

/**
 * Special type of transaction tuple to distinguish the genesis money from other transactions.
 */
public class GenesisTransactionTuple extends TransactionTuple {
	public GenesisTransactionTuple(TransactionCreator creator, Transaction transaction) {
		super(creator, transaction);
	}
}
