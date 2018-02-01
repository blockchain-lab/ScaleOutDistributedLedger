package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Transaction pattern where only node 0 makes transactions.
 * Only used for debugging.
 */
public class OnlyNodeZeroTransactionPattern extends UniformRandomTransactionPattern {
	private static final long serialVersionUID = 1L;

	/**
     * @param minAmount   - the minimum amount of money to send
     * @param maxAmount   - the maximum amount of money to send
     * @param minWaitTime - the minimum wait time between making transactions
     * @param maxWaitTime - the maximum wait time between making transactions
     * @param commitEvery - commit every x blocks
     */
    public OnlyNodeZeroTransactionPattern(int minAmount, int maxAmount, int minWaitTime, int maxWaitTime, int commitEvery) {
        super(minAmount, maxAmount, minWaitTime, maxWaitTime, commitEvery);
    }

    @Override
    public long selectAmount(LocalStore localStore) {
        //Only let node 0 make transactions
        if (localStore.getOwnNode().getId() != 0) return -1;

        return super.selectAmount(localStore);
    }
}
