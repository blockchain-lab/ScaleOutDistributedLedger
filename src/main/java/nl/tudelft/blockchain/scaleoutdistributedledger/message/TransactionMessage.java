package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * Transaction message for netty.
 */
public class TransactionMessage extends Message {
	
	@Getter
	private final int number;

	@Getter
	private final int senderId, receiverId;

	@Getter
	private final long amount, remainder;

	/**
	 * Transactions known by the receiver.
	 * Entry: node id, [block number, transaction number]
	 */
	@Getter
	private final Set<Entry<Integer, int[]>> source;

	@Getter
	private final Sha256Hash hash;
	
	@Getter
	private final int blockNumber;

	/**
	 * Constructor.
	 * @param transaction - the original transaction object
	 * @param proofReceiver - receiver of the proof
	 */
	public TransactionMessage(Transaction transaction, Node proofReceiver) {
		if (!transaction.getBlockNumber().isPresent()) {
			throw new RuntimeException("Block number not present");
		}
		this.number = transaction.getNumber();
		// It's a genesis transaction
		if (transaction.getSender() == null) {
			this.senderId = Transaction.GENESIS_SENDER;
		} else {
			this.senderId = transaction.getSender().getId();
		}
		this.receiverId = transaction.getReceiver().getId();
		this.amount = transaction.getAmount();
		this.remainder = transaction.getRemainder();
		this.source = new HashSet<>();
		// Optimization: categorize each transaction already known (or not) by the receiver
		for (Transaction sourceTransaction : transaction.getSource()) {
			Node sourceSender = sourceTransaction.getSender();
			if (sourceTransaction.getBlockNumber().isPresent()) {
				if (sourceSender == null) {
					// Genesis transaction
					this.source.add(new SimpleEntry<>(sourceTransaction.getReceiver().getId(),
							new int[]{sourceTransaction.getBlockNumber().getAsInt(),
									sourceTransaction.getNumber()}));
				} else {
					this.source.add(new SimpleEntry<>(sourceSender.getId(),
							new int[]{sourceTransaction.getBlockNumber().getAsInt(),
									sourceTransaction.getNumber()}));
				}
			} else {
				throw new IllegalStateException("Transaction without blocknumber found");
			}
		}
		this.hash = transaction.getHash();
		this.blockNumber = transaction.getBlockNumber().getAsInt();
	}

	public Transaction toTransactionWithoutSources(LocalStore localStore) {
		Transaction tx = new Transaction(this.number, localStore.getNode(this.senderId),
				localStore.getNode(this.receiverId), this.amount, this.remainder, new HashSet<>());
		tx.setMessage(this);
		return tx;
	}

	@Override
	public void handle(LocalStore localStore) {
		// Do nothing
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof TransactionMessage)) return false;
		
		TransactionMessage other = (TransactionMessage) obj;
		if (number != other.number) return false;
		if (senderId != other.senderId) return false;
		if (receiverId != other.receiverId) return false;
		if (amount != other.amount) return false;
		if (remainder != other.remainder) return false;
		if (source.equals(other.source)) return false;
		if (hash.equals(other.hash)) return false;
		return blockNumber == other.blockNumber;
	}

	@Override
	public int hashCode() {
		final int prime = 89;
		int result = 3;
		result = prime * result + this.number;
		result = prime * result + this.senderId;
		result = prime * result + this.receiverId;
		result = prime * result + (int) (this.amount ^ (this.amount >>> 32));
		result = prime * result + (int) (this.remainder ^ (this.remainder >>> 32));
		result = prime * result + Objects.hashCode(this.source);
		result = prime * result + Objects.hashCode(this.hash);
		result = prime * result + this.blockNumber;
		return result;
	}
}
