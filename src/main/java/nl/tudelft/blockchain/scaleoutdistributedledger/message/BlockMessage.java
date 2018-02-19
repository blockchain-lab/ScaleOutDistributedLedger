package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import lombok.Getter;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Block message for netty.
 */
public class BlockMessage extends Message {
	private static final long serialVersionUID = 1L;

	@Getter
	private final int number;
	
	@Getter
	private final byte ownerId;

	@Getter
	private final List<TransactionMessage> transactions;
	
	/**
	 * Constructor.
	 * @param block - original block
	 */
	public BlockMessage(Block block) {
		this.number = block.getNumber();
		// It's a genesis block
		if (block.getOwner() == null) {
			this.ownerId = Transaction.GENESIS_SENDER;
		} else {
			this.ownerId = (byte) block.getOwner().getId();
		}
		this.transactions = new ArrayList<>();
		for (Transaction transaction : block.getTransactions()) {
			this.transactions.add(new TransactionMessage(transaction));
		}
	}

	@Override
	public void handle(LocalStore localStore) {
		// Do nothing.
	}

	/**
	 * @param localStore - the local store
	 * @return - the block that this message represents, without any sources in the transactions
	 */
	public Block toBlockWithoutSources(LocalStore localStore) {
		List<Transaction> transactions = new ArrayList<>();
		for (TransactionMessage tm : this.transactions) {
			transactions.add(tm.toTransactionWithoutSources(localStore));
		}
		return new Block(this.number, localStore.getNode(this.ownerId), transactions);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64);
		sb.append("BlockMessage<nr=").append(number).append(", owner=").append(ownerId).append(", transactions=[");
		if (transactions.isEmpty()) return sb.append("]").toString();
		
		for (TransactionMessage tm : transactions) {
			sb.append("\n      ").append(tm);
		}
		sb.append("\n    ]");
		return sb.toString();
	}
}
