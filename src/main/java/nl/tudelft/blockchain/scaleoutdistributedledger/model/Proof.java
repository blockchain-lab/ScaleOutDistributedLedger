package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.List;

/**
 * Created by Bart on 05/12/2017.
 */
public class Proof {

    @Getter
    final Transaction transaction;

    @Getter
    final List<Block> blocks;

    public Proof(Transaction transaction, List<Block> blocks) {
        this.transaction = transaction;
        this.blocks = blocks;
    }
}
