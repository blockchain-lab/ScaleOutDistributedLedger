package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

import lombok.Getter;
import lombok.Setter;

/**
 * Class to store all information needed to register a transaction.
 */
@Getter @Setter
public class TransactionRegistration {

	private Transaction transaction;
	private int numberOfChains;
	private int numberOfBlocks;
	private Map<Integer, Integer> knowledge;
	private List<Integer> setC;

	/**
	 * Constructor.
	 * @param transaction - the traansactions of this registration.
	 * @param numberOfChains - the number of chains used in the proof.
	 * @param numberOfBlocks - the number of blocks used in the proof.
	 * @param localStore     - the local store
	 */
	public TransactionRegistration(Transaction transaction, int numberOfChains, int numberOfBlocks, LocalStore localStore) {
		this.transaction = transaction;
		this.numberOfChains = numberOfChains;
		this.numberOfBlocks = numberOfBlocks;
		this.setC = new ArrayList<>();
		this.knowledge = new HashMap<>();
		
		OwnNode ownNode = localStore.getOwnNode();
		for (Node node : localStore.getNodes().values()) {
			if (node instanceof OwnNode) continue;
			
			int lastBlock = node.getChain().getLastBlockNumber();
			if (lastBlock < 1) continue;
			
			knowledge.put(node.getId(), lastBlock);
			if (ownNode.isDisallowedChain(node.getId())) {
				setC.add(node.getId());
			}
		}
	}
}
