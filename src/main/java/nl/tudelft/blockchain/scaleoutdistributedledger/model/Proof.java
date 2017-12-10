package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.OptionalInt;

/**
 * Proof class.
 */
public class Proof {

    @Getter
    private final Transaction transaction;

    @Getter
    private final HashMap<Node, List<Block>> chainUpdates;

    /**
     * Constructor.
     * @param transaction - the transaction to be proven.
     */
    public Proof(Transaction transaction) {
        this.transaction = transaction;
        this.chainUpdates = new HashMap<>();
    }

    /**
     * Add a block to the proof.
     * @param block - the block to be added
     */
    public void addBlock(Block block) {
        chainUpdates.get(block.getOwner().getId()).add(block);
    }

    /**
     * Verifies this proof.
     * @return - boolean indicating if this proof is valid.
     */
    public boolean verify() {
        return verify(this.transaction);
    }

    /**
     * Verifies the given transaction using this proof.
     * @param transaction - the transaction to verify
     * @return - boolean indicating if this transaction is valid.
     */
    private boolean verify(Transaction transaction) {
        int absmark = 0;
        final int[] count = {0};

        ChainView chainView = new ChainView(transaction.getSender().getChain(), chainUpdates.get(transaction.getSender()));
        if (!chainView.isValid()) return false;
        ListIterator<Block> iterator = chainView.listIterator();
        while(iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getTransactions().contains(transaction)) count[0]++;
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

        // Verify source transaction
        for (Transaction sourceTransaction : transaction.getSource()) {
            if (!verify(sourceTransaction)) return false;
        }
        return true;
    }
}
