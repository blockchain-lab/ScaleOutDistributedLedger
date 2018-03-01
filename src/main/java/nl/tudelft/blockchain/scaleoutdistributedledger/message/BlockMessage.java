package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.GenesisNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;
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
	
	@Getter
	private final int[] requirements;
	
	/**
	 * Constructor.
	 * @param block - original block
	 */
	public BlockMessage(Block block) {
		this.number = block.getNumber();
		// It's a genesis block
		if (block.getOwner() == null) {
			this.ownerId = GenesisNode.GENESIS_NODE_ID;
		} else {
			this.ownerId = block.getOwner().getId();
		}
		this.transactions = new TransactionMessage[block.getTransactions().size()];
		int i = 0;
		for (Transaction transaction : block.getTransactions()) {
			this.transactions[i++] = new TransactionMessage(transaction);
		}
		
		this.requirements = block.getCachedRequirements();
	}
	
	private BlockMessage(int number, int ownerId, TransactionMessage[] transactions, int[] requirements) {
		this.number = number;
		this.ownerId = ownerId;
		this.transactions = transactions;
		this.requirements = requirements;
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
		for (int i = 0; i < requirements.length; i++) {
			stream.writeInt(requirements[i]);
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
		final int nodeCount = Settings.INSTANCE.totalNodesNumber;
		int[] requirements = new int[nodeCount];
		for (int i = 0; i < nodeCount; i++) {
			requirements[i] = stream.readInt();
		}
		
		return new BlockMessage(number, ownerId, transactions, requirements);
	}

	/**
	 * @param localStore - the local store
	 * @return - the block that this message represents, without any sources in the transactions
	 */
	public Block toBlockWithoutSources(LocalStore localStore) {
		List<Transaction> transactions = new ArrayList<>();
		for (TransactionMessage tm : this.transactions) {
			transactions.add(tm.toTransactionWithoutSources(localStore, this.requirements));
		}
		Block block = new Block(this.number, localStore.getNode(this.ownerId), transactions);
		block.setCachedRequirements(this.requirements);
		return block;
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
