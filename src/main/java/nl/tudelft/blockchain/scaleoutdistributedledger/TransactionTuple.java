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
public class TransactionTuple implements Cloneable {
	private final TransactionCreator creator;
	
	@Getter
	private int remainder;
	
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
		this.remainder += transaction.getRemainder();
		
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
		this.remainder += tuple.remainder;
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
	
	@Override
	public Object clone() {
		try {
			TransactionTuple tt = (TransactionTuple) super.clone();
            tt.transactions = new HashSet<>(transactions);
            return tt;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
	}
}
