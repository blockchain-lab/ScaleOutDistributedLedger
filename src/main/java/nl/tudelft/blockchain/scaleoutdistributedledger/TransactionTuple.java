package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
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
	private TreeSet<Transaction> transactions = new TreeSet<>();
	
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
	 * Creates a new tuple consisting of the given tuples.
	 * @param tuple1         - the first tuple
	 * @param tuple2         - the second tuple
	 * @param chainsRequired - the bitset of required chains
	 */
	public TransactionTuple(TransactionTuple tuple1, TransactionTuple tuple2, BitSet chainsRequired) {
		this.creator = tuple1.creator;
		this.chainsRequired = chainsRequired;
		
		for (Transaction transaction : tuple1.transactions) {
			addTransactionAndAmount(transaction);
		}
		
		for (Transaction transaction : tuple2.transactions) {
			addTransactionAndAmount(transaction);
		}
	}
	
	/**
	 * Adds the given transaction to this tuple.
	 * @param transaction - the transaction
	 */
	public void addTransaction(Transaction transaction) {
		if (!addTransactionAndAmount(transaction)) return;
		
		BitSet newChainsRequired = creator.chainsRequired(transaction);
		if (this.chainsRequired == null) {
			this.chainsRequired = newChainsRequired;
		} else {
			this.chainsRequired.or(newChainsRequired);
		}
	}
	
	/**
	 * Adds the given transaction and the correct amount to this tuple.
	 * @param transaction - the transaction to add
	 * @return              true if the transaction was added, false if it was already in this tuple
	 */
	private boolean addTransactionAndAmount(Transaction transaction) {
		Node ownNode = creator.getSender();
		if (ownNode != transaction.getSender() && ownNode != transaction.getReceiver()) {
			throw new IllegalArgumentException("The given transaction does not involve us, so we cannot use it as a source!");
		}
		
		if (!this.transactions.add(transaction)) return false;
		
		if (ownNode == transaction.getSender()) {
			//A transaction we sent, so use the remainder
			this.amount += transaction.getRemainder();
		}
		
		if (ownNode == transaction.getReceiver()) {
			//A transaction we received, so use the amount
			this.amount += transaction.getAmount();
		}
		
		return true;
	}
	
	/**
	 * Merges the given tuple into this tuple.
	 * 
	 * The given tuple must not share any transactions with this tuple and must have the same
	 * chain requirements.
	 * @param tuple - the tuple to merge
	 * @return        this transaction tuple
	 */
	public TransactionTuple mergeNonOverlappingSameChainsTuple(TransactionTuple tuple) {
		this.transactions.addAll(tuple.transactions);
		this.amount += tuple.amount;
		return this;
	}
	
	/**
	 * @param tuple - the tuple
	 * @return        if this tuple contains all the transactions in the given tuple
	 */
	public boolean containsAll(TransactionTuple tuple) {
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
	
	@Override
	public String toString() {
		String sources = transactions.stream()
				.map(Transaction::getNumber)
				.map(String::valueOf)
				.collect(Collectors.joining(", ", "<", ">"));
		return "Tuple(" + sources + ", $" + amount + ", required=" + chainsRequired + ")"; 
	}
}
