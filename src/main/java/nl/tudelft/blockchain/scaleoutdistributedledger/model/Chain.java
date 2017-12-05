package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bart on 05/12/2017.
 */
public class Chain {

    @Getter
    final Node owner;

    @Getter
    final List<Block> blocks;

    public Chain(Node owner) {
        this.owner = owner;
        this.blocks = new ArrayList<>();
    }

    public Chain(Node owner, List<Block> blocks) {
        this.owner = owner;
        this.blocks = blocks;
    }
}
