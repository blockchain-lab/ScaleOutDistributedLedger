package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Chain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Class for creating transactions.
 * 
 * This class implements a modified version of algorithm 3 of the paper to select the best set of
 * sources for a transaction.
 */
public class TransactionCreator {
	private final int nodes;
	private final Node from;
	private final Node to;
	private final long amount;
	private final BitSet known;
	
	private int currentBest = Integer.MAX_VALUE;
	private TransactionTuple currentBestTuple;
	
	/**
	 * @param application - the application
	 * @param from        - the sender of the transaction
	 * @param to          - the receiver of the transaction
	 * @param amount      - the amount to send
	 */
	public TransactionCreator(Application application, Node from, Node to, long amount) {
		this.nodes = application.getNodes().size();
		this.from = from;
		this.to = to;
		this.amount = amount;
		this.known = calculateKnowledge();
	}
	
	/**
	 * Calculates what the receiver already knows about.
	 * 
	 * @return a bitset with the chains the receiver already knows about
	 */
	private BitSet calculateKnowledge() {
		BitSet collected = to.getMetaKnowledge()
				.keySet()
				.stream()
				.map(Node::getId)
				.collect(() -> new BitSet(nodes),
						(bs, i) -> bs.set(i),
						(bs1, bs2) -> bs1.or(bs2)
				);
		
		return collected;
	}
	
	/**
	 * @param number - the number to assign to the transaction
	 * @return         a new transaction
	 */
	public Transaction createTransaction(int number) {
		TransactionTuple sources = bestSources();
		
		//Mark sources as spent.
		for (Transaction transaction : sources.getTransactions()) {
			transaction.setUnspent(false);
		}
		
		return new Transaction(number, from, to, amount, sources.getRemainder(), sources.getTransactions());
	}
	
	/**
	 * @return the best TransactionTuple or null if the sender doesn't have enough money
	 */
	public TransactionTuple bestSources() {
		//TODO Cache unspent transactions?
		
		//Step 1: Collect all unspent transactions
		//Chain -> Blocks -> Transactions -> Unspent Transactions -> TransactionTuples
		Set<TransactionTuple> unspentTransactions = from.getChain()
				.getBlocks()
				.stream()
				.map(Block::getTransactions)
				.flatMap(List::stream)
				.filter(Transaction::isUnspent)
				.map(t -> new TransactionTuple(this, t))
				.collect(Collectors.toCollection(HashSet::new));
		
		//Step 2: Check if we can cover the transaction amount with a single transaction.
		firstRound(unspentTransactions);
		cleanup(unspentTransactions, Integer.MAX_VALUE);
		
		//If the single transaction we found is the best, then we return it.
		if (unspentTransactions.isEmpty()) return currentBestTuple;
		
		//Step 3: keep trying to improve for multiple rounds to get the best set of transactions
		int previousBest = currentBest;
		Set<TransactionTuple> temp;
		Set<TransactionTuple> singleElements = unspentTransactions;
		Set<TransactionTuple> currentRound = new HashSet<>(unspentTransactions);
		Set<TransactionTuple> nextRound = new HashSet<>();
		while (true) {
			doOneRound(singleElements, currentRound, nextRound);
			
			//If no better tuples were found, then we can return the best-so-far.
			if (nextRound.isEmpty()) return currentBestTuple;
			
			//Delete all sets that are worse than the current best.
			cleanup(nextRound, previousBest);
			
			//Swap the lists so we don't have to create a new one
			temp = currentRound;
			currentRound = nextRound;
			nextRound = temp;
			nextRound.clear();
			previousBest = currentBest;
		}
	}

	/**
	 * @param unspentTransactions - a set with all unspent transactions
	 */
	private void firstRound(Set<TransactionTuple> unspentTransactions) {
		Iterator<TransactionTuple> it = unspentTransactions.iterator();
		while (it.hasNext()) {
			TransactionTuple tuple = it.next();
			int amountRequired = tuple.getChainsRequired().cardinality();
			if (amountRequired >= currentBest) {
				it.remove();
				continue;
			}
			
			if (tuple.getRemainder() >= amount) {
				//Single transaction able to cover the whole transaction
				currentBest = amountRequired;
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
				
				int amountRequired = r3.cardinality();
				//If this combination is worse than the current best, we don't consider it.
				if (amountRequired >= currentBest) continue;
				
				TransactionTuple t3 = new TransactionTuple(t1, t2);
				if (t3.getRemainder() >= amount) {
					//This combination is a good candidate
					currentBest = amountRequired;
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
				int amountRequired = tuple.getChainsRequired().cardinality();
				if (amountRequired >= currentBest) it.remove();
			}
		}
	}
	
	/**
	 * @param transaction - the transaction
	 * @return the chains that are required for the given transaction
	 */
	public BitSet chainsRequired(Transaction transaction) {
		Set<Chain> chains = new HashSet<>();
		Proof.appendChains(transaction, to, chains);
		BitSet bitset = chains.stream()
				.map(Chain::getOwner)
				.map(Node::getId)
				.collect(() -> new BitSet(nodes),
						(bs, i) -> bs.set(i),
						(bs1, bs2) -> bs1.or(bs2)
				);
		
		//TODO determine chains required for transaction
		Log.log(Level.SEVERE, "TODO: Determine which chains are required for transaction!");
		
		
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
		if (b != null) c.or(b);
		return c;
	}
}
