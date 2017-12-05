package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.List;

/**
 * Created by Bart on 05/12/2017.
 */
public class Block {

    @Getter
    final int number;

    @Getter
    Block previousBlock;

    @Getter
    final Node owner;

    @Getter
    final List<Transaction> transactions;

    public Block(int number, Node owner, List<Transaction> transactions) {
        this.number = number;
        this.owner = owner;
        this.transactions = transactions;
        this.previousBlock = null;
    }

    public Block(int number, Block previousBlock, Node owner, List<Transaction> transactions) {
        this.number = number;
        this.previousBlock = previousBlock;
        this.owner = owner;
        this.transactions = transactions;
    }
}
