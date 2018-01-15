package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

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
	
	// Custom getter
	private OptionalInt blockNumber;

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
	 * Constructor to decode a transaction message.
	 * @param transactionMessage - the message received from a transaction.
	 * @param localStore - local store, to get each Node object
	 */
	public Transaction(TransactionMessage transactionMessage, LocalStore localStore)  {
		this.number = transactionMessage.getNumber();
		// It's a genesis transaction
		if (transactionMessage.getSenderId() == GENESIS_SENDER) {
			this.sender = null;
		} else {
			this.sender = localStore.getNode(transactionMessage.getSenderId());
		}
		this.receiver = localStore.getNode(transactionMessage.getReceiverId());
		this.amount = transactionMessage.getAmount();
		this.remainder = transactionMessage.getRemainder();
		// Decode transaction messages to normal transactions
		this.source = new HashSet<>();
		for (Entry<Integer, Integer> knownSourceEntry : transactionMessage.getKnownSource()) {
			Integer nodeId = knownSourceEntry.getKey();
			Integer transactionId = knownSourceEntry.getValue();
			this.source.add(localStore.getTransactionFromNode(nodeId, transactionId));
		}
		for (TransactionMessage transactionMessageAux : transactionMessage.getNewSource()) {
			this.source.add(new Transaction(transactionMessageAux, localStore));
		}
		this.hash = transactionMessage.getHash();
		this.blockNumber = OptionalInt.of(transactionMessage.getBlockNumber());
	}
	
	/**
	 * Creates a proof for this transaction.
	 * @return - the proof
	 */
	public Proof getProof() {
		// TODO: do a smart include of chainUpdates

		Map<Node, List<Block>> chainUpdates = new HashMap<>();
		for (Transaction t : source) {
			if (!chainUpdates.containsKey(t.getSender()))
				chainUpdates.put(t.getSender(), t.getSender().getChain().getBlocks());
		}

		return new Proof(this, chainUpdates);
	}
	
	/**
	 * Returns the number of the block (if it is in a block).
	 * TODO: maybe do this more efficiently (when adding the transaction to the local chain or something)
	 * @return - optional that is empty if the transaction is not in a block, and filled with the number of the block otherwise.
	 */
	public OptionalInt getBlockNumber() {
		if (!this.blockNumber.isPresent()) {
			for (Block block : sender.getChain().getBlocks()) {
				if (block.getTransactions().contains(this)) {
					this.blockNumber = OptionalInt.of(block.getNumber());
					return this.blockNumber;
				}
			}
		}
		return this.blockNumber;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		result = prime * result + ((receiver == null) ? 0 : receiver.hashCode());
		result = prime * result + ((sender == null) ? 0 : sender.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Transaction)) return false;
		
		Transaction other = (Transaction) obj;
		if (number != other.number) return false;
		if (receiver != other.receiver) return false;
		if (sender != other.sender) return false;
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
