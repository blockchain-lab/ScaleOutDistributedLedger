package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

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
	
	private Map<Transaction, BitSet> chainSets = new HashMap<>();
	private Map<Set<Transaction>, BitSet> chainSets2 = new HashMap<>();
	
	private int currentBest = Integer.MAX_VALUE;
	private Set<Transaction> currentBestSources;
	
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
				.collect(() -> new BitSet(nodes), (bs, i) -> bs.set(i), (bs1, bs2) -> {
					bs1.or(bs2);
				});
		
		return collected;
	}
	
	/**
	 * @param number - the number to assign to the transaction
	 * @return         a new transaction
	 */
	public Transaction createTransaction(int number) {
		Set<Transaction> sources = bestSources();
		long remainder = -amount;
		for (Transaction source : sources) {
			remainder += source.getRemainder();
		}
		
		return new Transaction(number, from, to, amount, remainder, sources);
	}
	
	/**
	 * @return the best set of sources
	 */
	Set<Transaction> bestSources() {
		//TODO Cache unspent transactions
		//Chain -> Blocks -> Transactions -> Unspent Transactions
		List<Transaction> unspentTransactions = from.getChain()
				.getBlocks()
				.stream()
				.map(Block::getTransactions)
				.flatMap(List::stream)
				.filter(Transaction::isUnspent)
				.collect(Collectors.toCollection(ArrayList::new));
		
		int currentBestSingle = Integer.MAX_VALUE;
		Set<Transaction> currentBestSources = null;
		
		List<Transaction> combine = new ArrayList<>();
		for (Transaction transaction : unspentTransactions) {
			BitSet chainsRequired = chainsRequired(transaction);
			int amountRequired = chainsRequired.cardinality();
			
			//If this transaction is worse than our current best, then we don't even have to consider it.
			if (amountRequired >= currentBestSingle) continue;
			
			//Remember the required chains for this transaction
			chainSets.put(transaction, chainsRequired);
			
			if (transaction.getRemainder() >= amount) {
				//This transaction is enough on its own
				currentBestSingle = amountRequired;
				currentBestSources = Collections.singleton(transaction);
			} else {
				combine.add(transaction);
			}
		}
		
		//If we have a current best, we delete all transactions which are not worth considering
		if (currentBestSingle != Integer.MAX_VALUE) {
			Iterator<Transaction> it = combine.iterator();
			while (it.hasNext()) {
				Transaction t = it.next();
				int amountRequired = chainSets.get(t).cardinality();
				if (amountRequired >= currentBestSingle) it.remove();
			}
		}
		
		//Check if we have found anything
		if (combine.isEmpty()) return currentBestSources;
		
		List<Set<Transaction>> combine2 = new ArrayList<>();
		int currentBestPair = currentBestSingle;
		for (Transaction t1 : combine) {
			BitSet r1 = chainSets.get(t1);
			
			for (Transaction t2 : combine) {
				BitSet r2 = chainSets.get(t2);
				BitSet r3 = (BitSet) r1.clone();
				r3.or(r2);
				
				int amountRequired = r3.cardinality();
				
				//If this combination is worse than the current best, we don't consider it.
				if (amountRequired >= currentBestPair) continue;
				
				if (t1.getRemainder() + t2.getRemainder() >= amount) {
					Set<Transaction> t3 = new HashSet<>(2);
					t3.add(t1);
					t3.add(t2);
					chainSets2.put(t3, r3);
					
					currentBestPair = amountRequired;
					currentBestSources = t3;
				} else {
					//Consider this pair for the next round
					Set<Transaction> t3 = new HashSet<>(2);
					t3.add(t1);
					t3.add(t2);
					chainSets2.put(t3, r3);
					
					combine2.add(t3);
				}
			}
		}
		
		if (currentBestPair != currentBestSingle) {
			Iterator<Set<Transaction>> it = combine2.iterator();
			while (it.hasNext()) {
				Set<Transaction> ts = it.next();
				int amountRequired = chainSets2.get(ts).cardinality();
				if (amountRequired >= currentBestPair) it.remove();
			}
		}
		
		from.getChain().getBlocks().stream().map(Block::getTransactions).flatMap(List::stream).filter(Transaction::isUnspent).sorted((a, b) -> -Long.compare(a.getRemainder(), b.getRemainder()));
		return null;
	}

	private BitSet chainsRequired(Transaction transaction) {
		BitSet bitset = new BitSet(nodes);
		
		
		//TODO determine chains required for transaction
		
		//Remove all chains that are already known
		bitset.andNot(known);
		
		return bitset;
	}
}
