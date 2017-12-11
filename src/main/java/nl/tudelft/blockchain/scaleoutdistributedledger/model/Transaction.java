package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.OptionalInt;
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

    private OptionalInt blockNumber;

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
        blockNumber = OptionalInt.empty();
    }

    /**
     * Returns the number of the block (if it is in a block).
     * TODO: maybe do this more efficiently (when adding the transaction to the local chain or something)
     * @return - optional that is empty if the transaction is not in a block, and filled with the number of the block otherwise.
     */
    public OptionalInt getBlockNumber() {
        if (!this.blockNumber.isPresent()) {
            for (Block block : sender.getChain().getBlocks()) {
                if(block.getTransactions().contains(this)) this.blockNumber = OptionalInt.of(block.getNumber());
            }
        }
        return this.blockNumber;
    }
}
