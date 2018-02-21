package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import lombok.Getter;

/**
 * Block message for netty.
 */
public class BlockMessage extends Message {
	public static final int MESSAGE_ID = 1;

	@Getter
	private final int number;
	
	@Getter
	private final int ownerId;

	@Getter
	private final TransactionMessage[] transactions;
	
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
			this.ownerId = block.getOwner().getId();
		}
		this.transactions = new TransactionMessage[block.getTransactions().size()];
		int i = 0;
		for (Transaction transaction : block.getTransactions()) {
			this.transactions[i++] = new TransactionMessage(transaction);
		}
	}
	
	private BlockMessage(int number, int ownerId, TransactionMessage[] transactions) {
		this.number = number;
		this.ownerId = ownerId;
		this.transactions = transactions;
	}

	@Override
	public void handle(LocalStore localStore) {
		// Do nothing.
	}
	
	@Override
	public int getMessageId() {
		return MESSAGE_ID;
	}
	
	@Override
	public void writeToStream(ByteBufOutputStream stream) throws IOException {
		//Write contents
		stream.writeInt(number);
		Utils.writeNodeId(stream, ownerId);
		stream.writeShort(transactions.length);
		for (TransactionMessage tm : transactions) {
			tm.writeToStream(stream);
		}
	}
	
	/**
	 * @param stream       - the stream to read from
	 * @return             - the BlockMessage that was read
	 * @throws IOException - If reading from the stream causes an IOException.
	 */
	public static BlockMessage readFromStream(ByteBufInputStream stream) throws IOException {
		int number = stream.readInt();
		int ownerId = Utils.readNodeId(stream);
		int transactionCount = stream.readUnsignedShort();
		TransactionMessage[] transactions = new TransactionMessage[transactionCount];
		for (int i = 0; i < transactionCount; i++) {
			transactions[i] = TransactionMessage.readFromStream(stream);
		}
		
		return new BlockMessage(number, ownerId, transactions);
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
		if (transactions.length == 0) return sb.append("]").toString();
		
		for (TransactionMessage tm : transactions) {
			sb.append("\n      ").append(tm);
		}
		sb.append("\n    ]");
		return sb.toString();
	}
}
