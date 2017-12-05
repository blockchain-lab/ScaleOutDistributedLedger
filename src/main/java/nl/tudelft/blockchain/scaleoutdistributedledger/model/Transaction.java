package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.Set;

/**
 * Transaction class.
 */
public class Transaction {

    @Getter
    final int number;

    @Getter
    final Node sender, receiver;

    @Getter
    final long amount, remainder;

    @Getter
    final Set<Transaction> source;

    public Transaction(int number, Node sender, Node receiver, long amount, long remainder, Set<Transaction> source) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.remainder = remainder;
        this.source = source;
        this.number = number;
    }
}
