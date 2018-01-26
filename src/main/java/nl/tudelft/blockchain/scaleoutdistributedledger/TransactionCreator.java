package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.*;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import lombok.Getter;

/**
 * Class for creating transactions.
 * 
 * This class implements a modified version of algorithm 3 of the paper to select the best set of
 * sources for a transaction.
 */
public class TransactionCreator {
	private final LocalStore localStore;
	private final int nodesCount;
	@Getter
	private final Node sender;
	private final Node receiver;
	private final long amount;
	private final BitSet known;

	private int currentBest = Integer.MAX_VALUE;
	private TransactionTuple currentBestTuple;

	/**
	 * @param localStore  - the local store
	 * @param receiver    - the receiver of the transaction
	 * @param amount      - the amount to send
	 */
	public TransactionCreator(LocalStore localStore, Node receiver, long amount) {
		this.localStore = localStore;
		this.nodesCount = localStore.getNodes().size();
		this.sender = localStore.getOwnNode();
		this.receiver = receiver;
		this.amount = amount;
		this.known = calculateKnowledge();
	}

	/**
	 * Calculates what the receiver already knows about.
	 * 
	 * @return a bitset with the chains the receiver already knows about
	 */
	private BitSet calculateKnowledge() {
		synchronized (receiver.getMetaKnowledge()) {
			BitSet collected = receiver.getMetaKnowledge()
					.keySet()
					.stream()
					.collect(() -> new BitSet(nodesCount),
							(bs, i) -> bs.set(i),
							(bs1, bs2) -> bs1.or(bs2)
					);
	
			return collected;
		}
	}

	/**
	 * Creates a transaction.
	 * 
	 * The sources used for the transaction are marked as spent.
	 * If the transaction has a remainder, it is marked as unspent.
	 * @return         a new transaction
	 * @throws NotEnoughMoneyException If the sender doesn't have enough money.
	 */
	public Transaction createTransaction() {
		if (localStore.getAvailableMoney() < amount) throw new NotEnoughMoneyException();
		
		TransactionTuple sources = bestSources();
		if (sources == null) throw new NotEnoughMoneyException();
		
		TreeSet<Transaction> sourceSet = sources.getTransactions();
		long remainder = sources.getAmount() - amount;

		//Mark sources as spent.
		localStore.removeUnspentTransactions(sourceSet);
		
		int number = localStore.getNewTransactionId();
		Transaction transaction = new Transaction(number, sender, receiver, amount, remainder, sourceSet);
		//If there is a remainder, or if we send money to ourselves, then add that the transaction is unspent.
		if (remainder > 0 || receiver == sender) {
			localStore.addUnspentTransaction(transaction);
		}
		
		return transaction;
	}

	/**
	 * @return the best TransactionTuple or null if the sender doesn't have enough money
	 */
	protected TransactionTuple bestSources() {
		//Step 1: Group all unspent transactions that have the same chain requirements.
		Map<BitSet, TransactionTuple> candidateMap = new HashMap<>();
		for (Transaction transaction : localStore.getUnspent()) {
			TransactionTuple tuple = new TransactionTuple(this, transaction);
			candidateMap.merge(tuple.getChainsRequired(), tuple, TransactionTuple::mergeNonOverlappingSameChainsTuple);
		}
		
		Collection<TransactionTuple> candidates = candidateMap.values();

		//Step 2: Check if we can cover the transaction amount with a single transaction (group).
		firstRound(candidates);
		cleanup(candidates, Integer.MAX_VALUE);

		//If the single transaction (group) we found is the best, then we return it.
		if (candidates.size() <= 1) return currentBestTuple;

		//Step 3: keep trying to improve for multiple rounds to get the best set of transactions
		int roundCount = candidates.size() - 1;
		int previousBest = currentBest;
		Set<TransactionTuple> temp;
		Set<TransactionTuple> currentRound = new HashSet<>(candidates);
		Set<TransactionTuple> nextRound = new HashSet<>();
		
		//Repeat for at most the number of total unspent transactions.
		//At that point we will have created the only possible set: the set of all unspent transactions.
		for (int i = 0; i < roundCount; i++) {
			doOneRound(candidates, currentRound, nextRound);

			//If no better tuples were found, then we can return the best-so-far.
			//If one tuple remains, it will never become better as all other combinations have been ruled out.
			if (nextRound.size() <= 1) return currentBestTuple;

			//Delete all sets that are worse than the current best.
			cleanup(nextRound, previousBest);

			//Swap the lists so we don't have to create a new one
			temp = currentRound;
			currentRound = nextRound;
			nextRound = temp;
			nextRound.clear();
			previousBest = currentBest;
		}
		
		//We didn't find an absolute best.
		return currentBestTuple;
	}

	/**
	 * @param unspentTransactions - a collection with all unspent transactions
	 */
	private void firstRound(Collection<TransactionTuple> unspentTransactions) {
		Iterator<TransactionTuple> it = unspentTransactions.iterator();
		while (it.hasNext()) {
			TransactionTuple tuple = it.next();
			int chainsRequired = tuple.getChainsRequired().cardinality();
			if (chainsRequired >= currentBest) {
				it.remove();
				continue;
			}

			if (tuple.getAmount() >= amount) {
				//Single tuple able to cover the whole transaction
				currentBest = chainsRequired;
				currentBestTuple = tuple;
				it.remove();
			}
		}
	}

	/**
	 * @param baseElements - a collection with individual transactions / grouped transactions
	 * @param currentRound - a set with tuples of the current round
	 * @param nextRound    - a set to which tuples for the next round are added
	 */
	private void doOneRound(Collection<TransactionTuple> baseElements, Set<TransactionTuple> currentRound, Set<TransactionTuple> nextRound) {
		//Choose the best collection to iterate over.
		//If we choose to use currentRound, we can skip the containment check since it is a set and thus no two tuples will be equal.
		boolean skipContainsCheck = false;
		if (currentRound.size() < baseElements.size()) {
			baseElements = currentRound;
			skipContainsCheck = true;
		}
		
		for (TransactionTuple t1 : baseElements) {
			for (TransactionTuple t2 : currentRound) {
				if (!skipContainsCheck && t2.containsAll(t1)) continue;

				BitSet r3 = combineBitSets(t1.getChainsRequired(), t2.getChainsRequired());

				//If this combination is worse than the current best, we don't consider it.
				int chainsRequired = r3.cardinality();
				if (chainsRequired >= currentBest) continue;

				TransactionTuple t3 = new TransactionTuple(t1, t2, r3);
				if (t3.getAmount() >= amount) {
					//This combination is a good candidate
					currentBest = chainsRequired;
					currentBestTuple = t3;
				} else {
					//Consider this tuple for the next round
					nextRound.add(t3);
				}
			}
		}
	}

	/**
	 * Removes all tuples that are not better than the current best.
	 * @param tuples       - the tuples to clean up
	 * @param previousBest - the previous best
	 */
	private void cleanup(Collection<TransactionTuple> tuples, int previousBest) {
		//If there were no changes to the best, then tuples will only contain tuples that are better than the current best.
		if (currentBest != previousBest) {
			Iterator<TransactionTuple> it = tuples.iterator();
			while (it.hasNext()) {
				TransactionTuple tuple = it.next();
				int chainsRequired = tuple.getChainsRequired().cardinality();
				if (chainsRequired >= currentBest) it.remove();
			}
		}
	}

	/**
	 * @param transaction - the transaction
	 * @return the chains that are required for the given transaction
	 */
	public BitSet chainsRequired(Transaction transaction) {
		//TODO Verify that this collection of chains is correct.
		Map<Node, Integer> chains = new HashMap<>();
		
		int nrOfNodes = localStore.getNodes().size();
		Proof.appendChains2(nrOfNodes, transaction, receiver, chains);
		BitSet bitset = chains
				.keySet()
				.stream()
				.map(Node::getId)
				.collect(() -> new BitSet(nodesCount),
						(bs, i) -> bs.set(i),
						(bs1, bs2) -> bs1.or(bs2)
				);

		return bitset;
	}

	/**
	 * @param a - the first BitSet
	 * @param b - the second BitSet
	 * @return    a new BitSet {@code a || b}
	 */
	public static BitSet combineBitSets(BitSet a, BitSet b) {
		BitSet c = (BitSet) a.clone();
		c.or(b);
		return c;
	}
}
