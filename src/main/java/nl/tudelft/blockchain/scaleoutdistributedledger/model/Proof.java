package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.List;
import java.util.OptionalInt;

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

    /**
     * Verifies this proof.
     * @return - boolean indicating if this proof is valid.
     */
    public boolean verify() {
        int absmark = 0;
        final int[] count = {0};

        for (Block block : transaction.getSender().getChain().getBlocks()) {if(block.getTransactions().contains(transaction)) count[0]++;
            // TODO: check block hash (hash function needed) if not genesis block
            BlockAbstract blockAbstract = block.getBlockAbstract();
            if (blockAbstract.isOnMainChain()) {
                absmark = blockAbstract.getBlockNumber();
                if (!blockAbstract.checkBlockHash(block) || !blockAbstract.checkSignature()) return false;
            }
        }
        OptionalInt blockNumber = transaction.getBlockNumber();
        if (!blockNumber.isPresent() || absmark < blockNumber.getAsInt()) return false;
        if(count[0] != 1) return false;

        for (Transaction sourceTransaction : transaction.getSource()) {
            // TODO: check if sources are valid
        }
        return true;
    }
}
