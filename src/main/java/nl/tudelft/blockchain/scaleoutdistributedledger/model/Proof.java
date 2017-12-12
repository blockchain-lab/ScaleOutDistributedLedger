package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
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
		boolean seen = false;

		ChainView chainView = new ChainView(transaction.getSender().getChain(), chainUpdates.get(transaction.getSender()));
		if (!chainView.isValid()) return false;

		for (Block block : chainView) {
			if (block.getTransactions().contains(transaction)) {
				if (seen) return false;
				seen = true;
			}

			if(block.getNumber() > 0 && !block.checkHash()) return false;
			BlockAbstract blockAbstract = block.getBlockAbstract();
			if (blockAbstract != null && blockAbstract.isOnMainChain()) {
				absmark = blockAbstract.getBlockNumber();
				if (!blockAbstract.checkBlockHash(block) || !blockAbstract.checkSignature()) return false;
			}
		}

		OptionalInt blockNumber = transaction.getBlockNumber();
		if (!blockNumber.isPresent() || absmark < blockNumber.getAsInt()) return false;

		// Verify source transaction
		for (Transaction sourceTransaction : transaction.getSource()) {
			if (!verify(sourceTransaction)) return false;
		}
		return true;
	}
}
