package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import lombok.Getter;
import lombok.Setter;

/**
 * Class which represents a multiple of transactions.
 */
public class TransactionTuple {
	private final TransactionCreator creator;
	
	@Getter
	private int amount;
	
	@Getter
	private Set<Transaction> transactions = new HashSet<>();
	
	@Getter @Setter
	private BitSet chainsRequired;
	
	/**
	 * @param creator     - the TransactionCreator
	 * @param transaction - the initial transaction
	 */
	public TransactionTuple(TransactionCreator creator, Transaction transaction) {
		this.creator = creator;
		addTransaction(transaction);
	}
	
	/**
	 * Creates a new tuple containing the given transactions.
	 * @param creator      - the TransactionCreator
	 * @param transaction1 - the first transaction
	 * @param transaction2 - the second transaction
	 */
	public TransactionTuple(TransactionCreator creator, Transaction transaction1, Transaction transaction2) {
		this.creator = creator;
		addTransaction(transaction1);
		addTransaction(transaction2);
	}
	
	/**
	 * Creates a new tuple containing the given tuple and the given transaction.
	 * @param transaction - the transaction
	 * @param tuple       - the tuple
	 */
	public TransactionTuple(Transaction transaction, TransactionTuple tuple) {
		this(tuple, transaction);
	}
	
	/**
	 * Creates a new tuple containing the given tuple and the given transaction.
	 * @param tuple       - the tuple
	 * @param transaction - the transaction
	 */
	public TransactionTuple(TransactionTuple tuple, Transaction transaction) {
		this.creator = tuple.creator;
		addTuple(tuple);
		addTransaction(transaction);
	}
	
	/**
	 * @param tuple1 - the first tuple
	 * @param tuple2 - the second tuple
	 */
	public TransactionTuple(TransactionTuple tuple1, TransactionTuple tuple2) {
		this.creator = tuple1.creator;
		addTuple(tuple1);
		addTuple(tuple2);
	}
	
	/**
	 * Adds the given transaction to this tuple.
	 * @param transaction - the transaction
	 */
	public void addTransaction(Transaction transaction) {
		this.transactions.add(transaction);
		if (creator.getSender() == transaction.getSender()) {
			//A transaction we sent, so use the remainder
			this.amount += transaction.getRemainder();
		} else if (creator.getSender() == transaction.getReceiver()) {
			//A transaction we received, so use the amount
			this.amount += transaction.getAmount();
		} else {
			throw new IllegalArgumentException("The given transaction does not involve us, so we cannot use it as a source!");
		}
		
		BitSet newChainsRequired = creator.chainsRequired(transaction);
		if (this.chainsRequired == null) {
			this.chainsRequired = newChainsRequired;
		} else {
			this.chainsRequired.or(newChainsRequired);
		}
	}
	
	/**
	 * Adds the given transaction tuple to this tuple.
	 * @param tuple - the tuple
	 */
	public void addTuple(TransactionTuple tuple) {
		this.transactions.addAll(tuple.transactions);
		this.amount += tuple.amount;
		if (this.chainsRequired == null) {
			this.chainsRequired = (BitSet) tuple.chainsRequired.clone();
		} else {
			this.chainsRequired.or(tuple.chainsRequired);
		}
	}
	
	/**
	 * @param tuple - the tuple
	 * @return        if this tuple contains all the transactions in the given tuple
	 */
	public boolean contains(TransactionTuple tuple) {
		return transactions.containsAll(tuple.transactions);
	}
	
	/**
	 * @return the amount of transactions in this tuple
	 */
	public int size() {
		return transactions.size();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + chainsRequired.hashCode();
		result = prime * result + amount;
		result = prime * result + transactions.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof TransactionTuple)) return false;
		
		TransactionTuple other = (TransactionTuple) obj;
		if (amount != other.amount) return false;
		if (!chainsRequired.equals(other.chainsRequired)) return false;
		if (!transactions.equals(other.transactions)) return false;
		return true;
	}
}
