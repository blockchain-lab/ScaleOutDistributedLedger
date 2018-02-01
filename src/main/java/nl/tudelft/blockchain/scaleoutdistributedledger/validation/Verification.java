package nl.tudelft.blockchain.scaleoutdistributedledger.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.ChainView;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

/**
 * Verification and validation algorithms.
 */
public class Verification {
	private HashMap<Transaction, Boolean> validationCache = new HashMap<>();
	private HashSet<Transaction> receivedTransactions = new HashSet<>();
	
	/**
	 * @param proof - the proof
	 * @param localStore - the local store
	 * @throws ValidationException - If the proof or the transaction is invalid.
	 */
	public void validateNewMessage(Proof proof, LocalStore localStore) throws ValidationException {
		Transaction transaction = proof.getTransaction();
		if (!receivedTransactions.add(transaction)) {
			throw new ValidationException("Transaction " + transaction + " has been made to us already!");
		}
		
		proof.verify(localStore);
		
		HashMap<Transaction, Boolean> cache = new HashMap<>();
		validateTransaction(transaction, proof, localStore, cache);
		
		//Transaction is valid, so update the global cache.
		validationCache.putAll(cache);
	}
	
	/**
	 * Validates the given transaction with the given proof.
	 * @param transaction - the transaction to validate
	 * @param proof - the proof
	 * @param localStore - the local store
	 * @param cache - the cache to use
	 */
	public void validateTransaction(Transaction transaction, Proof proof, LocalStore localStore, HashMap<Transaction, Boolean> cache) {
		if (transaction.getSender() == null) {
			validateGenesisTransaction(transaction, localStore, cache);
			return;
		}
		
		checkMoney(transaction);
		checkDoubleSpending(transaction, proof);
		validateSources(transaction, proof, localStore, cache);
		
		cache.put(transaction, true);
	}

	/**
	 * Checks if the amount of money in the given transaction is correct.
	 * @param transaction - the transaction to check
	 * @throws ValidationException - If the amount of money is incorrect.
	 */
	private void checkMoney(Transaction transaction) throws ValidationException {
		Node sender = transaction.getSender();
		long expectedSum = transaction.getAmount() + transaction.getRemainder();
		long sum = 0L;
		for (Transaction txj : transaction.getSource()) {
			if (txj.getReceiver().getId() == sender.getId()) {
				//Sender of the transaction received this money from someone else and is spending it
				sum += txj.getAmount();
			} else if (txj.getSender() != null && txj.getSender().getId() == sender.getId()) {
				//This transaction was sent by them to someone else but has a remainder
				sum += txj.getRemainder();
			} else {
				//This transaction cannot be a source, since they weren't involved in it
				throw new ValidationException(transaction, "source " + txj + " does not involve the sender (" + sender.getId() + ").");
			}
			
			if (sum > expectedSum) {
				throw new ValidationException(transaction, "money does not add up. Expected: " + expectedSum + ". Current sum is higher: " + sum);
			}
		}

		if (sum != expectedSum) {
			throw new ValidationException(transaction, "money does not add up. Expected: " + expectedSum + ". Actual: " + sum);
		}
	}
	
	/**
	 * Checks if the given transaction tries to double spend.
	 * @param transaction - the transaction to check
	 * @param proof       - the proof for the transaction
	 * @throws ValidationException - If we detect double spending.
	 */
	private void checkDoubleSpending(Transaction transaction, Proof proof) throws ValidationException {
		ChainView chainView = proof.getChainView(transaction.getSender());
		for (Block block : chainView) {
			boolean found = false;
			for (Transaction txj : block.getTransactions()) {
				if (transaction.equals(txj)) {
					found = true;
					continue;
				}

				if (!intersectEmpty(transaction.getSource(), txj.getSource())) {
					throw new ValidationException(transaction, "double spending detected with source " + txj);
				}
			}

			if (found) break;
		}
	}
	
	/**
	 * Validates all the sources of the given transaction.
	 * @param transaction - the transaction to validate the sources of
	 * @param proof       - the proof
	 * @param localStore  - the local store
	 * @param cache       - the cache to use
	 * @throws ValidationException - If one of the sources is invalid.
	 */
	private void validateSources(Transaction transaction, Proof proof, LocalStore localStore, HashMap<Transaction, Boolean> cache) {
		for (Transaction txj : transaction.getSource()) {
			Boolean cached = validationCache.get(txj);
			if (cached == null) cached = cache.get(txj);
			if (cached == null) {
				//We didn't see this transaction before, so we need to validate it.
				try {
					validateTransaction(txj, proof, localStore, cache);
				} catch (ValidationException ex) {
					throw new ValidationException(transaction, "source " + txj + " is not valid.", ex);
				}
			} else if (!cached) {
				//We already invalidated this transaction
				throw new ValidationException(transaction, "source " + txj + " has been cached as invalid.");
			} else {
				//The transaction was validated before, so we don't have to do anything.
				continue;
			}
		}
	}
	
	/**
	 * Validates a genesis transaction.
	 * @param transaction - the genesis transaction
	 * @param localStore - the local store
	 * @param cache - the cache to use
	 */
	public void validateGenesisTransaction(Transaction transaction, LocalStore localStore, HashMap<Transaction, Boolean> cache) {
		if (transaction.getBlockNumber().orElse(0) != 0) {
			throw new ValidationException("Genesis Transaction " + transaction + " is invalid: its block number is not 0");
		}
		
		if (!transaction.getSource().isEmpty()) {
			throw new ValidationException("Genesis Transaction " + transaction + " is invalid: there are sources specified.");
		}
		
		if (transaction.getRemainder() != 0) {
			throw new ValidationException("Genesis Transaction " + transaction + " is invalid: there is a remainder.");
		}
		
		cache.put(transaction, true);
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
