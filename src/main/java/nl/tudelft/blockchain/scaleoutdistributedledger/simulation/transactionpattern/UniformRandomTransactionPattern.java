package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;

/**
 * Random transaction pattern that uses a uniform distribution.
 */
public class UniformRandomTransactionPattern extends RandomTransactionPattern {
	private static final long serialVersionUID = 1L;
	
	private int minWaitTime;
	private int maxWaitTime;
	private int minAmount;
	private int maxAmount;
	private transient Random random;

	/**
	 * @param minAmount   - the minimal transaction amount
	 * @param maxAmount   - the maximal transaction amount
	 * @param minWaitTime - the minimum time to wait
	 * @param maxWaitTime - the maximum time to wait
	 * @param commitEvery - commit to the main chain after this amount of blocks
	 */
	public UniformRandomTransactionPattern(int minAmount, int maxAmount, int minWaitTime, int maxWaitTime, int commitEvery) {
		super(commitEvery);
		this.minAmount = minAmount;
		this.maxAmount = maxAmount;
		this.minWaitTime = minWaitTime;
		this.maxWaitTime = maxWaitTime;
		this.random = new Random();
	}

	@Override
	public void setSeed(long seed) {
		this.seed = seed;
		this.random.setSeed(seed);
	}

	@Override
	public String getName() {
		return "Uniform Random";
	}

	@Override
	public long timeUntilNextAction(LocalStore localStore) {
		if (maxWaitTime == minWaitTime) {
			return minWaitTime;
		} else {
			return minWaitTime + random.nextInt(maxWaitTime - minWaitTime);
		}
	}

	@Override
	public Node selectNode(LocalStore localStore) {
		int amount = localStore.getNodes().size();
		if (amount <= 1) throw new IllegalStateException("There is only 1 node!");
		
		int selected;
		Node node;
		do {
			selected = random.nextInt(amount);
			node = localStore.getNode(selected);
		} while (node == localStore.getOwnNode());
		return node;
	}
	
	@Override
	public long selectAmount(LocalStore localStore) {
		long available = localStore.getAvailableMoney();
		if (available == 0 || minAmount > available) return -1;
		long amount;
		if (maxAmount == minAmount) {
			amount = minAmount;
		} else {
			amount = (long) minAmount + random.nextInt(maxAmount - minAmount);
		}
		return Math.min(amount, available);
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		this.random = new Random();
		if (this.seed != null) {
			this.random.setSeed(this.seed);
		}
	}
}
