package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

/**
 * Transaction message for netty.
 */
public class TransactionMessage {
	
	@Getter
	private final int number;

	@Getter
	private final int senderId, receiverId;

	@Getter
	private final long amount, remainder;

	/**
	 * Transactions known by the receiver.
	 * Entry: node id, last block number
	 */
	@Getter
	private final Set<Entry<Integer, Integer>> knownSource;
	
	/**
	 * Transactions new to the receiver.
	 */
	@Getter
	private final Set<TransactionMessage> newSource;

	@Getter
	private final Sha256Hash hash;
	
	@Getter
	private final int blockNumber;

	/**
	 * Constructor.
	 * @param transaction - the original transaction object
	 */
	public TransactionMessage(Transaction transaction) {
		if (!transaction.getBlockNumber().isPresent()) {
			throw new RuntimeException("Block number not present");
		}
		this.number = transaction.getNumber();
		this.senderId = transaction.getSender().getId();
		this.receiverId = transaction.getReceiver().getId();
		this.amount = transaction.getAmount();
		this.remainder = transaction.getRemainder();
		this.knownSource = new HashSet<>();
		this.newSource = new HashSet<>();
		// Optimization: categorize each transaction already known (or not) by the receiver
		for (Transaction transactionAux : transaction.getSource()) {
			Node transactionAuxOwner = transactionAux.getSender();
			Node receiver = transaction.getReceiver();
			Integer lastBlockNumber = receiver.getMetaKnowledge().get(transactionAuxOwner);
			if (transactionAux.getBlockNumber().getAsInt() <= lastBlockNumber) {
				this.knownSource.add(new SimpleEntry<>(transactionAuxOwner.getId(), transactionAux.getNumber()));
			} else {
				this.newSource.add(new TransactionMessage(transactionAux));
			}
		}
		this.hash = transaction.getHash();
		this.blockNumber = transaction.getBlockNumber().getAsInt();
	}

}
