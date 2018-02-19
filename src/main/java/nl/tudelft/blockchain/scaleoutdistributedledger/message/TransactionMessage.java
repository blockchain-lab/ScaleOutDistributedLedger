package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import lombok.Getter;

/**
 * Transaction message for netty.
 */
public class TransactionMessage extends Message {
	private static final long serialVersionUID = 1L;

	@Getter
	private final int number;

	@Getter
	private final byte senderId, receiverId;

	@Getter
	private final long amount, remainder;

	@Getter
	private final Set<TransactionSource> source;

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
		// It's a genesis transaction
		if (transaction.getSender() == null) {
			this.senderId = Transaction.GENESIS_SENDER;
		} else {
			this.senderId = (byte) transaction.getSender().getId();
		}
		this.receiverId = (byte) transaction.getReceiver().getId();
		this.amount = transaction.getAmount();
		this.remainder = transaction.getRemainder();
		this.source = new HashSet<>();
		// Optimization: categorize each transaction already known (or not) by the receiver
		for (Transaction sourceTransaction : transaction.getSource()) {
			Node sourceSender = sourceTransaction.getSender();
			if (sourceTransaction.getBlockNumber().isPresent()) {
				if (sourceSender == null) {
					// Genesis transaction
					this.source.add(new TransactionSource(
							sourceTransaction.getReceiver().getId(),
							sourceTransaction.getBlockNumber().getAsInt(),
							sourceTransaction.getNumber()));
				} else {
					this.source.add(new TransactionSource(
							sourceSender.getId(),
							sourceTransaction.getBlockNumber().getAsInt(),
							sourceTransaction.getNumber()));
				}
			} else {
				throw new IllegalStateException("Transaction without blocknumber found");
			}
		}
		this.hash = transaction.getHash();
		this.blockNumber = transaction.getBlockNumber().getAsInt();
	}

	/**
	 * Converts this message into a transaction without any sources.
	 * @param localStore - the local store
	 * @return - the transaction represented by this message, without sources
	 */
	public Transaction toTransactionWithoutSources(LocalStore localStore) {
		Transaction tx = new Transaction(this.number, localStore.getNode(this.senderId),
				localStore.getNode(this.receiverId), this.amount, this.remainder, new TreeSet<>());
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
		if (blockNumber != other.blockNumber) return false;
		if (!source.equals(other.source)) return false;
		return true;
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
		result = prime * result + this.blockNumber;
		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(128);
		sb.append("TransactionMessage<nr=").append(number)
		.append(", sender=").append(senderId)
		.append(", receiver=").append(receiverId)
		.append(", amount=").append(amount)
		.append(", remainder=").append(remainder)
		.append(", source=[");
		
		if (source.isEmpty()) {
			return sb.append("]").toString();
		}
		
		for (TransactionSource ts : source) {
			sb.append("\n        ").append(ts.getOwner())
			.append(": block=").append(ts.getBlockNumber())
			.append(", id=").append(ts.getId());
		}
		sb.append("\n      ]");
		return sb.toString();
	}
	
	/**
	 * Class to represent a transaction source.
	 */
	@Getter
	public static class TransactionSource implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private final byte owner;
		private final int id;
		private final int blockNumber;
		
		/**
		 * @param owner - the id of the owner of the transaction
		 * @param blockNumber - the number of the block in which this transaction resides
		 * @param id - the id of the transaction
		 */
		public TransactionSource(int owner, int blockNumber, int id) {
			this.owner = (byte) owner;
			this.id = id;
			this.blockNumber = blockNumber;
		}
		
		/**
		 * @return - the owner of this transaction source
		 */
		public int getOwner() {
			return owner;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			result = prime * result + blockNumber;
			result = prime * result + owner;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof TransactionSource)) return false;
			
			TransactionSource other = (TransactionSource) obj;
			if (id != other.id) return false;
			if (blockNumber != other.blockNumber) return false;
			if (owner != other.owner) return false;
			return true;
		}
	}
}
