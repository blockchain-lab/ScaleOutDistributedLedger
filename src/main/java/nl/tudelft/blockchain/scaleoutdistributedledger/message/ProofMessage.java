package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import lombok.Getter;

import nl.tudelft.blockchain.scaleoutdistributedledger.CommunicationHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

/**
 * Proof message for netty.
 */
public class ProofMessage extends Message {
	private static final long serialVersionUID = 1L;

	@Getter
	private final TransactionMessage transactionMessage;

	/**
	 * Same map as in the original proof.
	 * Map: node id, list of blocks
	 */
	@Getter
	private final Map<Integer, List<BlockMessage>> chainUpdates;
	
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

	@Override
	public void handle(LocalStore localStore) {
		try {
			CommunicationHelper.receiveTransaction(new Proof(this, localStore), localStore);
		} catch (IOException e) {
			Log.log(Level.SEVERE, "Exception while handling proof message", e);
		}
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
