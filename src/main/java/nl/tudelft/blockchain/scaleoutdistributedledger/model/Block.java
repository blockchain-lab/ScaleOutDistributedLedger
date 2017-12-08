package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

/**
 * Block class.
 */
public class Block {

    @Getter
    private final int number;

    @Getter
    private Block previousBlock;

    @Getter
    private final Node owner;

    @Getter
    private final List<Transaction> transactions;

    private Optional<BlockAbstract> blockAbstract;

    /**
     * Constructor.
     * @param number - the number of this block.
     * @param owner - the owner of this block.
     * @param transactions - a list of transactions of this block.
     */
    public Block(int number, Node owner, List<Transaction> transactions) {
        this.number = number;
        this.owner = owner;
        this.transactions = transactions;
        this.previousBlock = null;
    }

    /**
     * Constructor.
     * @param number - the number of this block.
     * @param previousBlock - reference to the previous block in the chain of this block.
     * @param owner - the owner of this block.
     * @param transactions - a list of transactions of this block.
     */
    public Block(int number, Block previousBlock, Node owner, List<Transaction> transactions) {
        this.number = number;
        this.previousBlock = previousBlock;
        this.owner = owner;
        this.transactions = transactions;
    }

    /**
     * Returns the abstract of this block, and generates it if it is not present.
     * @return - the abstract of this block.
     */
    public BlockAbstract getBlockAbstract() {
        if (!this.blockAbstract.isPresent()) {
            this.blockAbstract = Optional.of(null);
            //TODO: actually generate block abstract
        }
        return this.blockAbstract.get();
    }
}
