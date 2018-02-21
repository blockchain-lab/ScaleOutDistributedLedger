package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import lombok.Getter;
import lombok.Setter;

/**
 * Proof message for netty.
 */
public class ProofMessage extends Message {
	public static final int MESSAGE_ID = 0;

	@Getter
	private final TransactionMessage transactionMessage;

	/**
	 * Same map as in the original proof.
	 * Map: node id, list of blocks
	 */
	@Getter
	private final Map<Integer, List<BlockMessage>> chainUpdates;
	
	@Getter @Setter
	private long requiredHeight;
	
	/**
	 * Constructor.
	 * @param proof - original proof 
	 */
	public ProofMessage(Proof proof) {
		this.transactionMessage = new TransactionMessage(proof.getTransaction());
		this.chainUpdates = new HashMap<>();
		for (Entry<Node, List<Block>> entry : proof.getChainUpdates().entrySet()) {
			Node node = entry.getKey();
			List<Block> blockList = entry.getValue();
			if (!blockList.isEmpty()) {
				// Convert Block to BlockMessage
				List<BlockMessage> blockMessageList = new ArrayList<>();
				for (int i = 0; i < blockList.size(); i++) {
					Block block = blockList.get(i);
					blockMessageList.add(new BlockMessage(block));
				}
				this.chainUpdates.put(node.getId(), blockMessageList);
			}
		}
	}
	
	private ProofMessage(TransactionMessage transactionMessage, Map<Integer, List<BlockMessage>> chainUpdates, long requiredHeight) {
		this.transactionMessage = transactionMessage;
		this.chainUpdates = chainUpdates;
		this.requiredHeight = requiredHeight;
	}

	@Override
	public void handle(LocalStore localStore) {
		localStore.getApplication().getTransactionReceiver().receiveTransaction(this);
	}
	
	@Override
	public int getMessageId() {
		return MESSAGE_ID;
	}
	
	@Override
	public void writeToStream(ByteBufOutputStream stream) throws IOException {
		transactionMessage.writeToStream(stream);
		
		stream.writeLong(requiredHeight);
		
		Utils.writeNodeId(stream, chainUpdates.size());
		for (Entry<Integer, List<BlockMessage>> entry : chainUpdates.entrySet()) {
			Utils.writeNodeId(stream, entry.getKey());
			stream.writeInt(entry.getValue().size());
			for (BlockMessage bm : entry.getValue()) {
				bm.writeToStream(stream);
			}
		}
	}
	
	/**
	 * @param stream       - the stream to read from
	 * @return             - the ProofMessage that was read
	 * @throws IOException - If reading from the stream causes an IOException.
	 */
	public static ProofMessage readFromStream(ByteBufInputStream stream) throws IOException {
		TransactionMessage tm = TransactionMessage.readFromStream(stream);
		long requiredHeight = stream.readLong();
		
		int chainUpdatesSize = Utils.readNodeId(stream);
		Map<Integer, List<BlockMessage>> chainUpdates = new HashMap<>(chainUpdatesSize);
		for (int i = 0; i < chainUpdatesSize; i++) {
			int nodeId = Utils.readNodeId(stream);
			int blockCount = stream.readInt();
			
			List<BlockMessage> blockMessages = new ArrayList<>(blockCount);
			for (int j = 0; j < blockCount; j++) {
				blockMessages.add(BlockMessage.readFromStream(stream));
			}
			
			chainUpdates.put(nodeId, blockMessages);
		}

		return new ProofMessage(tm, chainUpdates, requiredHeight);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64);
		sb.append("ProofMessage\n  Transaction = ").append(transactionMessage).append("\n{");
		if (chainUpdates.isEmpty()) {
			return sb.append("}").toString();
		}
		
		for (Entry<Integer, List<BlockMessage>> entry : chainUpdates.entrySet()) {
			sb.append("\n  ").append(entry.getKey()).append(": [");
			for (BlockMessage bm : entry.getValue()) {
				sb.append("\n    ").append(bm);
			}
			sb.append("\n  ]");
		}
		sb.append("\n}");
		return sb.toString();
	}
}
