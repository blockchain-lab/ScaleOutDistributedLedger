package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

public class OnlyNodeZeroTransactionPattern extends UniformRandomTransactionPattern {

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
