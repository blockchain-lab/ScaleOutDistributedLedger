package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.List;

/**
 * Proof class.
 */
public class Proof {

    @Getter
    private final Transaction transaction;

    @Getter
    private final List<Block> blocks;

    /**
     * Constructor.
     * @param transaction - the transaction to be proven.
     * @param blocks - the list of blocks needed to proof the transaction.
     */
    public Proof(Transaction transaction, List<Block> blocks) {
        this.transaction = transaction;
        this.blocks = blocks;
    }
}
