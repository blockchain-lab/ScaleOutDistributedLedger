package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.Set;

/**
 * Transaction class.
 */
public class Transaction {

    @Getter
    private final int number;

    @Getter
    private final Node sender, receiver;

    @Getter
    private final long amount, remainder;

    @Getter
    private final Set<Transaction> source;

    /**
     * Constructor.
     * @param number - the number of this transaction.
     * @param sender - the sender of this transaction.
     * @param receiver - the receiver of this transaction.
     * @param amount - the amount to be transferred.
     * @param remainder - the remaining amount.
     * @param source - set of transactions that are used as sourc for this transaction.
     */
    public Transaction(int number, Node sender, Node receiver, long amount, long remainder, Set<Transaction> source) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.remainder = remainder;
        this.source = source;
        this.number = number;
    }
}
