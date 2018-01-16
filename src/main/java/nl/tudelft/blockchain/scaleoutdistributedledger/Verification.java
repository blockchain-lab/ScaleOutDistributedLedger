package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Chain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import java.util.HashMap;
import java.util.Set;

/**
 * Verification and validation algorithms.
 */
public class Verification {
	private HashMap<Transaction, Boolean> validationCache = new HashMap<>();

	/**
	 * Implementation of algorithm 1 in the paper.
	 *
	 * @param transaction - the transaction to validate
	 * @param proof       - the proof to validate with
	 * @return              true if the transaction is valid, false otherwise
	 */
	public boolean isValid(Transaction transaction, Proof proof, LocalStore localStore) {
		boolean valid = validate(transaction, proof, localStore);

		//Store in the cache
		Boolean old = validationCache.put(transaction, valid);
		if (old != null && old.booleanValue() != valid) {
			throw new IllegalStateException(
					"We validated transaction " + transaction + "to be " + old + " before, but " + valid + " now!");
		}
		
		return valid;
	}
	
	/**
	 * @param transaction - the transaction
	 * @return              if the transaction is cached
	 */
	public boolean isCached(Transaction transaction) {
		return validationCache.containsKey(transaction);
	}
	
	/**
	 * @param transaction - the transaction to validate
	 * @param proof       - the proof to validate with
	 * @return              true if the transaction is valid, false otherwise
	 */
	private boolean validate(Transaction transaction, Proof proof, LocalStore localStore) {
		// Genesis transaction is always valid, TODO: something?
		if (transaction.getSender() == null) return true;

		//Verify the proof
		if (!proof.verify(localStore)) return false;
		
		//Equality check: Check if the counts match up
		long expectedSum = transaction.getAmount() + transaction.getRemainder();
		long sum = 0L;
		for (Transaction txj : transaction.getSource()) {
			if(txj.getSender() != null && txj.getSender() == transaction.getSender()) sum += txj.getRemainder();
			else sum += txj.getAmount();
			if (sum > expectedSum) return false;
		}

		if (sum != expectedSum) return false;

		//Double spending check
		Chain chain = transaction.getSender().getChain();
		for (Block block : chain.getBlocks()) {
			boolean found = false;
			for (Transaction txj : block.getTransactions()) {
				if (transaction.equals(txj)) {
					found = true;
					continue;
				}

				if (!intersectEmpty(transaction.getSource(), txj.getSource())) return false;
			}

			if (found) break;
		}

		//Validate sources
		for (Transaction txj : transaction.getSource()) {
			Boolean cached = validationCache.get(txj);
			if (cached == null) {
				//We didn't see this transaction before, so we need to validate it.
				if (!isValid(txj, proof, localStore)) return false;
			} else if (!cached) {
				//We already invalidated this transaction
				return false;
			} else {
				//The transaction was validated before, so we don't have to do anything.
				continue;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if the intersection of the given sets is empty or not.
	 * 
	 * This method has O(m*c) performance, where {@code m = max(|A|, |B|)} and
	 * {@code c} is the performance of the contains operation ({@code O(1)} for a HashSet).
	 * 
	 * @param setA - the first set
	 * @param setB - the second set
	 * @return       true if the intersection is empty, false otherwise
	 */
	private static <T> boolean intersectEmpty(Set<T> setA, Set<T> setB) {
		//Loop over the smallest set. Contains in hash sets is O(1).
		if (setA.size() >= setB.size()) {
			for (T element : setA) {
				if (setB.contains(element)) return false;
			}
			return true;
		} else {
			for (T element : setB) {
				if (setA.contains(element)) return false;
			}
			return true;
		}
	}
}
