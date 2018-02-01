package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Class to store all information needed to register a transaction.
 */
public class TransactionRegistration {

	@Getter @Setter
	private Transaction transaction;

	@Getter @Setter
	private int numberOfChains;

	@Getter @Setter
	private int numberOfBlocks;

	/**
	 * Constructor.
	 * @param transaction - the traansactions of this registration.
	 * @param numberOfChains - the number of chains used in the proof.
	 * @param numberOfBlocks - the number of blocks used in the proof.
	 */
	public TransactionRegistration(Transaction transaction, int numberOfChains, int numberOfBlocks) {
		this.transaction = transaction;
		this.numberOfChains = numberOfChains;
		this.numberOfBlocks = numberOfBlocks;
	}
}
