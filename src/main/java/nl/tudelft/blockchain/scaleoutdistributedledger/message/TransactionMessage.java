package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.io.IOException;
import java.util.Arrays;
import java.util.TreeSet;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.GenesisNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import lombok.Getter;

/**
 * Transaction message for netty.
 */
public class TransactionMessage extends Message {
	public static final int MESSAGE_ID = 2;

	@Getter
	private final int number;
	
	@Getter
	private final int blockNumber;

	@Getter
	private final int senderId, receiverId;

	@Getter
	private final long amount, remainder;

	@Getter
	private final Sha256Hash hash;
	
	@Getter
	private final TransactionSource[] source;

	/**
	 * Constructor.
	 * @param transaction - the original transaction object
	 */
	public TransactionMessage(Transaction transaction) {
		if (transaction.getBlockNumber() == -1) {
			throw new RuntimeException("Block number not present");
		}
		this.number = transaction.getNumber();
		this.blockNumber = transaction.getBlockNumber();
		// It's a genesis transaction
		if (transaction.getSender() == null) {
			this.senderId = GenesisNode.GENESIS_NODE_ID;
		} else {
			this.senderId = transaction.getSender().getId();
		}
		this.receiverId = transaction.getReceiver().getId();
		this.amount = transaction.getAmount();
		this.remainder = transaction.getRemainder();
		this.hash = transaction.getHash();
		this.source = new TransactionSource[transaction.getSource().size()];
		int i = 0;
		// Optimization: categorize each transaction already known (or not) by the receiver
		for (Transaction sourceTransaction : transaction.getSource()) {
			Node sourceSender = sourceTransaction.getSender();
			if (sourceTransaction.getBlockNumber() != -1) {
				if (sourceSender == null) {
					// Genesis transaction
					this.source[i++] = new TransactionSource(
							sourceTransaction.getReceiver().getId(),
							sourceTransaction.getBlockNumber(),
							sourceTransaction.getNumber());
				} else {
					this.source[i++] = new TransactionSource(
							sourceSender.getId(),
							sourceTransaction.getBlockNumber(),
							sourceTransaction.getNumber());
				}
			} else {
				throw new IllegalStateException("Transaction without blocknumber found");
			}
		}
	}
	
	/**
	 * @param number      - the number
	 * @param blockNumber - the block number
	 * @param senderId    - the node id of the sender
	 * @param receiverId  - the node id of the receiver
	 * @param amount      - the amount
	 * @param remainder   - the remainder
	 * @param hash        - the hash
	 * @param source      - the sources
	 */
	private TransactionMessage(int number, int blockNumber, int senderId, int receiverId, long amount, long remainder,
			Sha256Hash hash, TransactionSource[] source) {
		this.number = number;
		this.blockNumber = blockNumber;
		this.senderId = senderId;
		this.receiverId = receiverId;
		this.amount = amount;
		this.remainder = remainder;
		this.hash = hash;
		this.source = source;
	}

	/**
	 * Converts this message into a transaction without any sources.
	 * @param localStore - the local store
	 * @param requirements - the cached requirements of the block
	 * @return - the transaction represented by this message, without sources
	 */
	public Transaction toTransactionWithoutSources(LocalStore localStore, int[] requirements) {
		Transaction tx = new Transaction(this.number, localStore.getNode(this.senderId),
				localStore.getNode(this.receiverId), this.amount, this.remainder, new TreeSet<>());
		tx.setMessage(this);
		tx.setCachedRequirements(requirements);
		return tx;
	}

	@Override
	public void handle(LocalStore localStore) {
		// Do nothing
	}
	
	@Override
	public int getMessageId() {
		return MESSAGE_ID;
	}
	
	@Override
	public void writeToStream(ByteBufOutputStream stream) throws IOException {
		//Write contents
		stream.writeInt(number);
		stream.writeInt(blockNumber);
		Utils.writeNodeId(stream, senderId);
		Utils.writeNodeId(stream, receiverId);
		stream.writeLong(amount);
		stream.writeLong(remainder);
		stream.write(hash.getBytes());
		stream.writeShort(source.length);
		for (TransactionSource ts : source) {
			ts.writeToStream(stream);
		}
	}
	
	/**
	 * @param stream       - the stream to read from
	 * @return             - the TransactionMessage that was read
	 * @throws IOException - If reading from the stream causes an IOException.
	 */
	public static TransactionMessage readFromStream(ByteBufInputStream stream) throws IOException {
		int number = stream.readInt();
		int blockNumber = stream.readInt();
		int senderId = Utils.readNodeId(stream);
		int receiverId = Utils.readNodeId(stream);
		long amount = stream.readLong();
		long remainder = stream.readLong();
		
		byte[] hashBytes = new byte[Sha256Hash.LENGTH];
		stream.read(hashBytes);
		Sha256Hash hash = Sha256Hash.withHash(hashBytes);
		
		int sourceCount = stream.readUnsignedShort();
		TransactionSource[] source = new TransactionSource[sourceCount];
		for (int i = 0; i < sourceCount; i++) {
			source[i] = TransactionSource.readFromStream(stream);
		}
		
		return new TransactionMessage(number, blockNumber, senderId, receiverId, amount, remainder, hash, source);
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
		if (Arrays.equals(source, other.source)) return false;
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
		result = prime * result + Arrays.hashCode(this.source);
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
		
		if (source.length == 0) {
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
	public static class TransactionSource {
		private int owner;
		private int id;
		private int blockNumber;
		
		/**
		 * @param owner - the id of the owner of the transaction
		 * @param blockNumber - the number of the block in which this transaction resides
		 * @param id - the id of the transaction
		 */
		public TransactionSource(int owner, int blockNumber, int id) {
			this.owner = owner;
			this.id = id;
			this.blockNumber = blockNumber;
		}

		/**
		 * @param stream       - the stream to write to
		 * @throws IOException - If writing to the stream causes an IOException.
		 */
		public void writeToStream(ByteBufOutputStream stream) throws IOException {
			Utils.writeNodeId(stream, owner);
			stream.writeInt(id);
			stream.writeInt(blockNumber);
		}
		
		/**
		 * @param stream       - the stream to read from
		 * @return             - the read TransactionSource
		 * @throws IOException - If reading from the stream causes an IOException.
		 */
		public static TransactionSource readFromStream(ByteBufInputStream stream) throws IOException {
			int owner = Utils.readNodeId(stream);
			int id = stream.readInt();
			int blockNumber = stream.readInt();
			return new TransactionSource(owner, blockNumber, id);
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
