package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;

/**
 * Transaction class.
 */
public class Transaction {

	// Represent the sender of a genesis transaction
	public static final int GENESIS_SENDER = -1;
	
	@Getter
	private final int number;

	@Getter
	private final Node sender;

	// TODO: change back to final somehow
	@Getter @Setter
	private Node receiver;

	@Getter
	private final long amount, remainder;

	@Getter
	private final Set<Transaction> source;

	// Custem getter
	private Sha256Hash hash;
	
	private OptionalInt blockNumber;

	// Only temporarily used while decoding
	@Getter @Setter
	private TransactionMessage message;

	/**
	 * Constructor.
	 * @param number - the number of this transaction.
	 * @param sender - the sender of this transaction.
	 * @param receiver - the receiver of this transaction.
	 * @param amount - the amount to be transferred.
	 * @param remainder - the remaining amount.
	 * @param source - set of transactions that are used as source for this transaction.
	 */
	public Transaction(int number, Node sender, Node receiver, long amount, long remainder, Set<Transaction> source) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
		this.remainder = remainder;
		this.source = source;
		this.number = number;
		this.blockNumber = OptionalInt.empty();
	}

	/**
	 * Returns the number of the block (if it is in a block).
	 * TODO: maybe do this more efficiently (when adding the transaction to the local chain or something)
	 * @return - optional that is empty if the transaction is not in a block, and filled with the number of the block otherwise.
	 */
	public OptionalInt getBlockNumber() {
		if (!this.blockNumber.isPresent()) {
			// It's a genesis transaction
			if (this.sender == null) {
				this.blockNumber = OptionalInt.of(Block.GENESIS_BLOCK_NUMBER);
			} else {
				for (Block block : sender.getChain().getBlocks()) {
					if (block.getTransactions().contains(this)) {
						this.blockNumber = OptionalInt.of(block.getNumber());
						break;
					}
				}
			}
		}
		return this.blockNumber;
	}
	
	/**
	 * Sets the block number of this transaction.
	 * @param number - the block number
	 */
	public void setBlockNumber(int number) {
		this.blockNumber = OptionalInt.of(number);
	}

	/**
	 * Get hash of the transaction.
	 * @return Hash SHA256
	 */
	public Sha256Hash getHash() {
		if (this.hash == null) {
			this.hash = this.calculateHash();
		}
		return this.hash;
	}
	
	/**
	 * Calculate the transaction hash.
	 * @return Hash SHA256
	 */
	private Sha256Hash calculateHash() {
		// Convert attributes of transaction into an array of bytes
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			// Important to keep the order of writings
			outputStream.write(Utils.intToByteArray(this.number));
			if (this.sender != null) {
				outputStream.write(Utils.intToByteArray(this.sender.getId()));
			}
			outputStream.write(Utils.intToByteArray(this.receiver.getId()));
			outputStream.write(Utils.longToByteArray(this.amount));
			outputStream.write(Utils.longToByteArray(this.remainder));
			
			// TODO: check if we really need to do this
			for (Transaction tx : this.source) {
				outputStream.write(tx.getHash().getBytes());
			}
		} catch (IOException ex) {
			Log.log(Level.SEVERE, null, ex);
		}
		byte[] transactionInBytes = outputStream.toByteArray();
		
		return new Sha256Hash(transactionInBytes);
	}
	
	/**
	 * @return - a copy of this transaction
	 * @throws UnsupportedOperationException - If this transaction has sources.
	 */
	public Transaction genesisCopy() {
		if (!source.isEmpty()) throw new UnsupportedOperationException("Only genesis transactions can be copied");
		return new Transaction(number, sender, receiver, amount, remainder, new HashSet<>(0));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		result = prime * result + receiver.getId();
		result = prime * result + ((sender == null) ? -1 : sender.getId());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Transaction)) return false;
		
		Transaction other = (Transaction) obj;
		if (number != other.number) return false;
		if (!receiver.equals(other.receiver)) return false;
		if (sender == null) {
			if (other.sender != null) return false;
		} else if (!sender.equals(other.sender)) return false;
		if (amount != other.amount) return false;
		if (remainder != other.remainder) return false;
		if (!source.equals(other.source)) return false;
		if (!blockNumber.equals(other.blockNumber)) return false;
		return true;
	}

	@Override
	public String toString() {
		if (sender == null) {
			return "Transaction(" + number + ": GENESIS -> " + receiver.getId() + " $" + amount + ")";
		} else {
			return "Transaction(" + number + ": " + sender.getId() + " -> " + receiver.getId() + " $" + amount + " / $" + remainder + ")";
		}
	}

}
