package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.SDLByteArrayOutputStream;

import lombok.Getter;
import lombok.Setter;

/**
 * Transaction class.
 */
public class Transaction implements Comparable<Transaction> {

	// Represent the sender of a genesis transaction
	public static final int GENESIS_SENDER = -1;
	
	@Getter
	private final int number;

	@Getter
	private final Node sender;

	@Getter @Setter
	private Node receiver;

	@Getter
	private final long amount, remainder;

	@Getter
	private final TreeSet<Transaction> source;

	// Custem getter
	private Sha256Hash hash;
	
	private int blockNumber;

	// Only temporarily used while decoding
	@Getter @Setter
	private TransactionMessage message;

	@Getter @Setter
	private boolean locallyVerified;

	/**
	 * Constructor.
	 * @param number    - the number of this transaction.
	 * @param sender    - the sender of this transaction.
	 * @param receiver  - the receiver of this transaction.
	 * @param amount    - the amount to be transferred.
	 * @param remainder - the remaining amount.
	 * @param source    - set of transactions that are used as source for this transaction.
	 */
	public Transaction(int number, Node sender, Node receiver, long amount, long remainder, TreeSet<Transaction> source) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
		this.remainder = remainder;
		this.source = source;
		this.number = number;
		this.blockNumber = sender == null ? Block.GENESIS_BLOCK_NUMBER : -1;
	}
	
	/**
	 * Convenience constructor. The given sources are converted to a TreeSet with
	 * <pre>new TreeSet<>(Arrays.asList(source))</pre>.
	 * @param number    - the number of this transaction.
	 * @param sender    - the sender of this transaction.
	 * @param receiver  - the receiver of this transaction.
	 * @param amount    - the amount to be transferred.
	 * @param remainder - the remaining amount.
	 * @param source    - the transaction that are used as sources for this transaction.
	 */
	public Transaction(int number, Node sender, Node receiver, long amount, long remainder, Transaction... source) {
		this(number, sender, receiver, amount, remainder, new TreeSet<>(Arrays.asList(source)));
	}

	/**
	 * Returns the number of the block (if it is in a block).
	 * @return - the number of the block that this transaction is in or -1 if unknown
	 */
	public int getBlockNumber() {
		if (this.blockNumber == -1) {
			// It's a genesis transaction
			if (this.sender == null) {
				this.blockNumber = Block.GENESIS_BLOCK_NUMBER;
			} else {
				for (Block block : sender.getChain().getBlocks()) {
					if (block.getTransactions().contains(this)) {
						this.blockNumber = block.getNumber();
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
		this.blockNumber = number;
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
		try (SDLByteArrayOutputStream stream = new SDLByteArrayOutputStream(4 + 2 + 2 + 8 + 8 + Sha256Hash.LENGTH * this.source.size())) {
			// Important to keep the order of writings
			stream.writeInt(this.number);
			if (this.sender != null) {
				stream.writeShort(this.sender.getId());
			} else {
				stream.writeShort(-1);
			}
			stream.writeShort(this.receiver.getId());
			stream.writeLong(this.amount);
			stream.writeLong(this.remainder);
			
			for (Transaction tx : this.source) {
				stream.write(tx.getHash().getBytes());
			}
			
			return new Sha256Hash(stream.getByteArray());
		} catch (IOException ex) {
			Log.log(Level.SEVERE, "Unable to calculate hash", ex);
			return null;
		}
	}
	
	/**
	 * @return - a copy of this transaction
	 * @throws UnsupportedOperationException - If this transaction has sources.
	 */
	public Transaction genesisCopy() {
		if (!source.isEmpty()) throw new UnsupportedOperationException("Only genesis transactions can be copied");
		return new Transaction(number, sender, receiver, amount, remainder, new TreeSet<>());
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
		if (blockNumber != other.blockNumber) return false;
		if (!source.equals(other.source)) return false;
		
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

	@Override
	public int compareTo(Transaction o) {
		if (this.sender == null && o.sender != null) return -1;
		if (this.sender != null && o.sender == null) return 1;
		if (this.sender == null && o.sender == null) return 0;
		int senderCompare = Integer.compare(this.sender.getId(), o.sender.getId());
		if (senderCompare != 0) return senderCompare;
		return Integer.compare(this.getNumber(), o.getNumber());
	}
}
