package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import nl.tudelft.blockchain.scaleoutdistributedledger.exceptions.NotEnoughMoneyException;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;

import lombok.Getter;

/**
 * Class for creating transactions.
 * 
 * This class implements a modified version of algorithm 3 of the paper to select the best set of
 * sources for a transaction.
 */
public class BlockTransactionCreator {
	private final LocalStore localStore;
	private final int nodesCount;
	@Getter
	private final Node sender;
	private final Node receiver;
	private final long amount;

	private int currentBest = Integer.MAX_VALUE;
	private BlockTransactionTuple currentBestTuple;
	//private int[] metaBlocks;

	/**
	 * @param localStore  - the local store
	 * @param receiver    - the receiver of the transaction
	 * @param amount      - the amount to send
	 */
	public BlockTransactionCreator(LocalStore localStore, Node receiver, long amount) {
		this.localStore = localStore;
		this.nodesCount = localStore.getNodes().size();
		this.sender = localStore.getOwnNode();
		this.receiver = receiver;
		this.amount = amount;
		//this.metaBlocks = receiver.getMetaKnowledge().getBlocksKnown();
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
		
		BlockTransactionTuple sources = bestSources();
		if (sources == null) throw new NotEnoughMoneyException();
		
		TreeSet<Transaction> sourceSet = new TreeSet<>(sources.getTransactions());
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
	 * @return the best BlockTransactionTuple or null if the sender doesn't have enough money
	 */
	protected BlockTransactionTuple bestSources() {
		//Step 1: Collect candidate transactions
		Collection<BlockTransactionTuple> candidates = collectCandidates();

		//Step 2: Check if we can cover the transaction amount with a single transaction (group).
		firstRound(candidates);
		cleanup(candidates, Integer.MAX_VALUE);

		//If the single transaction (group) we found is the best, then we return it.
		if (candidates.size() <= 1) return currentBestTuple;

		//Step 3: keep trying to improve for multiple rounds to get the best set of transactions
		int roundCount = candidates.size() - 1;
		int previousBest = currentBest;
		Set<BlockTransactionTuple> temp;
		Set<BlockTransactionTuple> currentRound = new HashSet<>(candidates);
		Set<BlockTransactionTuple> nextRound = new HashSet<>();
		
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
	 * Collects candidate transactions.
	 * If {@link Settings#grouping} is enabled, then transactions with the same requirements are
	 * grouped together.
	 * @return - a collection with transaction candidates.
	 */
	protected Collection<BlockTransactionTuple> collectCandidates() {
		final Set<Transaction> unspent = localStore.getUnspent();
		
		//We treat our genesis money in a special way
		final long genesisAmount = Settings.INSTANCE.initialMoney / 2;
		final boolean genesisGrouping = Settings.INSTANCE.genesisGrouping;
		
		//No grouping, just convert them into tuples
		Set<BlockTransactionTuple> candidates = new HashSet<>(unspent.size());
		
		boolean genesisFound = genesisGrouping;
		for (Transaction transaction : unspent) {
			if (!genesisFound && transaction.getSender() == sender && transaction.getRemainder() > genesisAmount) {
				genesisFound = true;
				candidates.add(new GenesisBlockTransactionTuple(this, transaction));
				continue;
			}
			
			candidates.add(new BlockTransactionTuple(this, transaction));
		}
		return candidates;
	}

	/**
	 * @param unspentTransactions - a collection with all unspent transactions
	 */
	private void firstRound(Collection<BlockTransactionTuple> unspentTransactions) {
		final boolean genesisGrouping = Settings.INSTANCE.genesisGrouping;
		Iterator<BlockTransactionTuple> it = unspentTransactions.iterator();
		while (it.hasNext()) {
			BlockTransactionTuple tuple = it.next();
			int blocksRequired = tuple.cardinality();
			if (blocksRequired > currentBest) {
				//Worse than current --> remove
				it.remove();
			} else if (blocksRequired == currentBest) {
				//Equally good to current. We prefer other tuples over the genesis money.
				if (!genesisGrouping && currentBestTuple instanceof GenesisBlockTransactionTuple && tuple.getAmount() >= amount) {
					//Single tuple able to cover the whole transaction
					currentBest = blocksRequired;
					currentBestTuple = tuple;
				}
				
				it.remove();
			} else if (tuple.getAmount() >= amount) {
				//Single tuple, better than current and able to cover the entire transaction
				currentBest = blocksRequired;
				currentBestTuple = tuple;
				it.remove();
			}
			
			//Any tuples not removed need to be checked again in the next round, to see if they group to something nice.
		}
	}

	/**
	 * @param baseElements - a collection with individual transactions / grouped transactions
	 * @param currentRound - a set with tuples of the current round
	 * @param nextRound    - a set to which tuples for the next round are added
	 */
	private void doOneRound(Collection<BlockTransactionTuple> baseElements, Set<BlockTransactionTuple> currentRound, Set<BlockTransactionTuple> nextRound) {
		//Choose the best collection to iterate over.
		//If we choose to use currentRound, we can skip the containment check since it is a set and thus no two tuples will be equal.
		boolean skipContainsCheck = false;
		if (currentRound.size() < baseElements.size()) {
			baseElements = currentRound;
			skipContainsCheck = true;
		}
		
		for (BlockTransactionTuple t1 : baseElements) {
			for (BlockTransactionTuple t2 : currentRound) {
				if (!skipContainsCheck && t2.containsAll(t1)) continue;

				int[] r3 = combineBlocksRequired(t1.getBlocksRequired(), t2.getBlocksRequired());

				//If this combination is worse than the current best, we don't consider it.
				int blocksRequired = BlockTransactionTuple.cardinality(r3);
				if (blocksRequired >= currentBest) continue;

				BlockTransactionTuple t3 = new BlockTransactionTuple(t1, t2, r3);
				if (t3.getAmount() >= amount) {
					//This combination is a good candidate
					currentBest = blocksRequired;
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
	private void cleanup(Collection<BlockTransactionTuple> tuples, int previousBest) {
		//If there were no changes to the best, then tuples will only contain tuples that are better than the current best.
		if (currentBest != previousBest) {
			Iterator<BlockTransactionTuple> it = tuples.iterator();
			while (it.hasNext()) {
				BlockTransactionTuple tuple = it.next();
				int blocksRequired = tuple.cardinality();
				if (blocksRequired >= currentBest) it.remove();
			}
		}
	}

	/**
	 * @param transaction - the transaction
	 * @return the chains that are required for the given transaction
	 */
	public int[] blocksRequired(Transaction transaction) {
		//TODO Verify that this collection of blocks is correct.
		int[] blocksRequired = new int[nodesCount + 1];
		Proof.appendChains3(transaction, receiver, blocksRequired);
		int sum = 0;
		for (int i = 0; i < nodesCount; i++) {
			sum += blocksRequired[i];
		}
		blocksRequired[nodesCount] = sum;

		return blocksRequired;
	}

	/**
	 * @param a - the first blocks required
	 * @param b - the second blocks required
	 * @return    a new blocks required (pairwise {@code max(a, b)} and sum fixed)
	 */
	public static int[] combineBlocksRequired(int[] a, int[] b) {
		int[] c = new int[a.length];
		int sum = 0;
		for (int i = 0; i < a.length - 1; i++) {
			sum += (c[i] = Math.max(a[i], b[i]));
		}
		c[a.length - 1] = sum;
		return c;
	}
}
