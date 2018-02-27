package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import lombok.Getter;
import lombok.Setter;

/**
 * Class which represents a multiple of transactions.
 */
public class BlockTransactionTuple {
	private final BlockTransactionCreator creator;
	
	@Getter
	private long amount;
	
	@Getter
	private HashSet<Transaction> transactions = new HashSet<>();
	
	@Getter @Setter
	private int[] blocksRequired;
	
	/**
	 * @param creator     - the TransactionCreator
	 * @param transaction - the initial transaction
	 */
	public BlockTransactionTuple(BlockTransactionCreator creator, Transaction transaction) {
		this.creator = creator;
		addTransaction(transaction);
	}
	
	/**
	 * Creates a new tuple consisting of the given tuples.
	 * @param tuple1         - the first tuple
	 * @param tuple2         - the second tuple
	 * @param blocksRequired - the list of required blocks
	 */
	public BlockTransactionTuple(BlockTransactionTuple tuple1, BlockTransactionTuple tuple2, int[] blocksRequired) {
		this.creator = tuple1.creator;
		this.blocksRequired = blocksRequired;
		
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
		
		int[] newBlocksRequired = creator.blocksRequired(transaction);
		if (this.blocksRequired == null) {
			this.blocksRequired = newBlocksRequired;
		} else {
			merge(newBlocksRequired);
		}
	}
	
	private void merge(int[] newBlocksRequired) {
		int sum = 0;
		for (int i = 0; i < blocksRequired.length - 1; i++) { 
			sum += (blocksRequired[i] = Math.max(blocksRequired[i], newBlocksRequired[i]));
		}
		blocksRequired[blocksRequired.length - 1] = sum;
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
	 * @param tuple - the tuple
	 * @return        if this tuple contains all the transactions in the given tuple
	 */
	public boolean containsAll(BlockTransactionTuple tuple) {
		return transactions.containsAll(tuple.transactions);
	}
	
	/**
	 * @return the amount of transactions in this tuple
	 */
	public int size() {
		return transactions.size();
	}
	
	/**
	 * @return - the number of blocks required
	 */
	public int cardinality() {
		return blocksRequired[blocksRequired.length - 1];
	}
	
	/**
	 * @param blocksRequired - the array of blocks required
	 * @return - the cardinality (last entry) of the given blocksRequired.
	 */
	public static int cardinality(int[] blocksRequired) {
		return blocksRequired[blocksRequired.length - 1];
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(blocksRequired);
		result = prime * result + (int) amount;
		result = prime * result + transactions.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof BlockTransactionTuple)) return false;
		
		BlockTransactionTuple other = (BlockTransactionTuple) obj;
		if (amount != other.amount) return false;
		if (!Arrays.equals(blocksRequired, other.blocksRequired)) return false;
		if (!transactions.equals(other.transactions)) return false;
		return true;
	}
	
	@Override
	public String toString() {
		String sources = transactions.stream()
				.map(Transaction::getNumber)
				.map(String::valueOf)
				.collect(Collectors.joining(", ", "<", ">"));
		return "Tuple(" + sources + ", $" + amount + ", required=" + Arrays.toString(blocksRequired) + ")"; 
	}
}
