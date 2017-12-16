package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Chain;
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
	private final Application application;
	private final int nodesCount;
	@Getter
	private final Node sender;
	private final Node receiver;
	private final long amount;
	private final BitSet known;

	private int currentBest = Integer.MAX_VALUE;
	private TransactionTuple currentBestTuple;

	/**
	 * @param application - the application
	 * @param receiver    - the receiver of the transaction
	 * @param amount      - the amount to send
	 */
	public TransactionCreator(Application application, Node receiver, long amount) {
		this.application = application;
		this.nodesCount = application.getNodes().size();
		this.sender = application.getOwnNode();
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
		BitSet collected = receiver.getMetaKnowledge()
				.keySet()
				.stream()
				.map(Node::getId)
				.collect(() -> new BitSet(nodesCount),
						(bs, i) -> bs.set(i),
						(bs1, bs2) -> bs1.or(bs2)
				);

		return collected;
	}

	/**
	 * Creates a transaction.
	 * 
	 * The sources used for the transaction are marked as spent.
	 * If the transaction has a remainder, it is marked as unspent.
	 * @param number - the number to assign to the transaction
	 * @return         a new transaction
	 * @throws NotEnoughMoneyException If the sender doesn't have enough money.
	 */
	public Transaction createTransaction(int number) {
		TransactionTuple sources = bestSources();
		if (sources == null) throw new NotEnoughMoneyException();
		
		Set<Transaction> sourceSet = sources.getTransactions();
		long remainder = sources.getAmount() - amount;

		//Mark sources as spent.
		application.getUnspent().removeAll(sourceSet);
		
		Transaction transaction = new Transaction(number, sender, receiver, amount, remainder, sourceSet);
		if (remainder > 0) {
			application.getUnspent().add(transaction);
		}
		
		return transaction;
	}

	/**
	 * @return the best TransactionTuple or null if the sender doesn't have enough money
	 */
	public TransactionTuple bestSources() {
		//TODO Cache unspent transactions?

		//Step 1: Collect all unspent transactions
		Set<TransactionTuple> candidates = application
				.getUnspent()
				.stream()
				.map(t -> new TransactionTuple(this, t))
				.collect(Collectors.toCollection(HashSet::new));

		//Step 2: Check if we can cover the transaction amount with a single transaction.
		firstRound(candidates);
		cleanup(candidates, Integer.MAX_VALUE);

		//If the single transaction we found is the best, then we return it.
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
	 * @param unspentTransactions - a set with all unspent transactions
	 */
	private void firstRound(Set<TransactionTuple> unspentTransactions) {
		Iterator<TransactionTuple> it = unspentTransactions.iterator();
		while (it.hasNext()) {
			TransactionTuple tuple = it.next();
			int chainsRequired = tuple.getChainsRequired().cardinality();
			if (chainsRequired >= currentBest) {
				it.remove();
				continue;
			}

			if (tuple.getAmount() >= amount) {
				//Single transaction able to cover the whole transaction
				currentBest = chainsRequired;
				currentBestTuple = tuple;
				it.remove();
			}
		}
	}

	/**
	 * @param singleElements - a set with individual transactions
	 * @param currentRound   - a set with tuples of the current round
	 * @param nextRound      - a set to which tuples for the next round are added
	 */
	private void doOneRound(Set<TransactionTuple> singleElements, Set<TransactionTuple> currentRound, Set<TransactionTuple> nextRound) {
		for (TransactionTuple t1 : singleElements) {
			for (TransactionTuple t2 : currentRound) {
				if (t2.contains(t1)) continue;

				BitSet r3 = combineBitSets(t1.getChainsRequired(), t2.getChainsRequired());

				//If this combination is worse than the current best, we don't consider it.
				int chainsRequired = r3.cardinality();
				if (chainsRequired >= currentBest) continue;

				TransactionTuple t3 = new TransactionTuple(t1, t2);
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
	private void cleanup(Set<TransactionTuple> tuples, int previousBest) {
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
		Set<Chain> chains = new HashSet<>();
		Proof.appendChains(transaction, receiver, chains);
		BitSet bitset = chains.stream()
				.map(Chain::getOwner)
				.map(Node::getId)
				.collect(() -> new BitSet(nodesCount),
						(bs, i) -> bs.set(i),
						(bs1, bs2) -> bs1.or(bs2)
				);

		//Remove all chains that are already known
		bitset.andNot(known);

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
