package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;

/**
 * Proof message for netty.
 */
public class ProofMessage {
	
	@Getter
	private final TransactionMessage transactionMessage;

	@Getter
	private final Map<Node, List<Block>> chainUpdates;
	
	/**
	 * Constructor.
	 * @param proof - original proof 
	 */
	public ProofMessage(Proof proof) {
		// TODO
		this.transactionMessage = new TransactionMessage(proof);
		this.chainUpdates = proof.getChainUpdates();
	}
	
}
