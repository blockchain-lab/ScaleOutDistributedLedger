package nl.tudelft.blockchain.scaleoutdistributedledger;

import static nl.tudelft.blockchain.scaleoutdistributedledger.enums.TransactionValidation.*;

import java.util.HashMap;
import java.util.Set;

import nl.tudelft.blockchain.scaleoutdistributedledger.enums.TransactionValidation;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Chain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

/**
 * Verification and validation algorithms.
 */
public final class Verification {
	private static HashMap<Transaction, TransactionValidation> validationCache = new HashMap<>();
	
	private Verification() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Implementation of algorithm 1 in the paper.
	 * 
	 * @param transaction
	 * 		the transaction to validate
	 * @param proof
	 * 		the proof to validate with
	 * 
	 * @return
	 * 		true if the transaction is valid, false otherwise
	 */
	public static boolean isValid(Transaction transaction, Proof proof) {
		TransactionValidation valid = validate(transaction, proof);
		
		//Store in the cache
		TransactionValidation old = validationCache.put(transaction, valid);
		if (old != valid) {
			throw new IllegalStateException(
					"We validated transaction " + transaction + "to be " + old + " before, but " + valid + " now!");
		}
		
		return valid == VALID;
	}
	
	/**
	 * @param transaction
	 * 		the transaction to validate
	 * @param proof
	 * 		the proof to validate with
	 * 
	 * @return
	 * 		VALID if the transaction is valid, UNKNOWN otherwise
	 */
	private static TransactionValidation validate(Transaction transaction, Proof proof) {
		//Verify the proof
		if (!verify(proof)) return UNKNOWN;
		
		//Equality check: Check if the counts match up
		long expectedSum = transaction.getAmount() + transaction.getRemainder();
		long sum = 0L;
		for (Transaction txj : transaction.getSource()) {
			sum += txj.getRemainder();
			if (sum > expectedSum) return UNKNOWN;
		}
		
		if (sum != expectedSum) return UNKNOWN;

		//Double spending check
		Chain chain = transaction.getSender().getChain();
		for (Block block : chain.getBlocks()) {
			boolean found = false;
			for (Transaction txj : block.getTransactions()) {
				//TODO Possible optimization point, use faster equality check
				if (transaction.equals(txj)) {
					found = true;
					continue;
				}
				
				if (!intersectEmpty(transaction.getSource(), txj.getSource())) return UNKNOWN;
			}
			
			//TODO IMPORTANT We might have to remove the found check. I'm not sure if we should stop at k.
			if (found) break;
		}
		
		//Validate sources
		for (Transaction txj : transaction.getSource()) {
			TransactionValidation cached = validationCache.get(txj);
			if (cached == null) {
				//We didn't see this transaction before, so we need to validate it.
				if (!isValid(txj, getProof(txj, proof))) return UNKNOWN;
			} else if (cached == UNKNOWN) {
				//We already invalidated this transaction
				return UNKNOWN;
			} else {
				//The transaction was validated before, so we don't have to do anything.
				continue;
			}
		}
		
		return VALID;
	}
	
	/**
	 * Algorithm 2.
	 * 
	 * @param proof
	 * 		the proof to verify
	 * 
	 * @return
	 * 		true if the proof is valid, false otherwise
	 */
	public static boolean verify(Proof proof) {
		//TODO Algorithm 2
		return true;
	}
	
	/**
	 * @param transaction
	 * 		the transaction to get the proof for
	 * @param proof
	 * 		the proof of a parent transaction
	 * 
	 * @return
	 * 		the proof of the given transaction
	 */
	public static Proof getProof(Transaction transaction, Proof proof) {
		//TODO IMPORTANT
		throw new UnsupportedOperationException("This feature is not yet implemented");
	}
	
	/**
	 * Checks if the intersection of the given sets is empty or not.
	 * 
	 * This method has O(m*c) performance, where {@code m = max(|A|, |B|)} and
	 * {@code c} is the performance of the contains operation ({@code O(1)} for a HashSet).
	 * 
	 * @param setA
	 * 		the first set
	 * @param setB
	 * 		the second set
	 * 
	 * @return
	 * 		true if the intersection is empty, false otherwise
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
