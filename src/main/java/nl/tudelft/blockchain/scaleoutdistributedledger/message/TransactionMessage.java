package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import java.util.AbstractMap.SimpleEntry;
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
	 * Entry: node id, transaction number
	 */
	@Getter
	private final Set<Entry<Integer, Integer>> knownSource;
	
	/**
	 * Transactions new to the receiver.
	 * Entry: node id, transaction number
	 */
	@Getter
	private final Set<Entry<Integer, Integer>> newSource;

	@Getter
	private final Sha256Hash hash;
	
	@Getter
	private final int blockNumber;

	/**
	 * Constructor.
	 * @param transaction - the original transaction object
	 */
	public TransactionMessage(Transaction transaction, Node finalReceiver) {
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
		this.knownSource = new HashSet<>();
		this.newSource = new HashSet<>();
		// Optimization: categorize each transaction already known (or not) by the receiver
		for (Transaction sourceTransaction : transaction.getSource()) {
			Node sourceSender = sourceTransaction.getSender();
			if (sourceSender == null) {
				// Receiver already knows about a genesis transaction
				this.knownSource.add(new SimpleEntry<>(Transaction.GENESIS_SENDER, sourceTransaction.getNumber()));
			} else {
				if (finalReceiver.equals(sourceSender)) {
					// Receiver is the sender of the source
					// So receiver knows about himself
					this.knownSource.add(new SimpleEntry<>(sourceSender.getId(), sourceTransaction.getNumber()));
				} else {
					// Receiver is not the sender of the source
					Integer lastBlockNumber = finalReceiver.getMetaKnowledge().get(sourceSender);
					if (lastBlockNumber != null && sourceTransaction.getBlockNumber().getAsInt() <= lastBlockNumber) {
						// Receiver knows about other node
						this.knownSource.add(new SimpleEntry<>(sourceSender.getId(), sourceTransaction.getNumber()));
					} else {
						// Receiver does NOT know about other node
						this.newSource.add(new SimpleEntry<>(sourceSender.getId(), sourceTransaction.getNumber()));
					}
				}
			}
		}
		this.hash = transaction.getHash();
		this.blockNumber = transaction.getBlockNumber().getAsInt();
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
		if (knownSource.equals(other.knownSource)) return false;
		if (newSource.equals(other.newSource)) return false;
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
		result = prime * result + Objects.hashCode(this.knownSource);
		result = prime * result + Objects.hashCode(this.newSource);
		result = prime * result + Objects.hashCode(this.hash);
		result = prime * result + this.blockNumber;
		return result;
	}
	
}
