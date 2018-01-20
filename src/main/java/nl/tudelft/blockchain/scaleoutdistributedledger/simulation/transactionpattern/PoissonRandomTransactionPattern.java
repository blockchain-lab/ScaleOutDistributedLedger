package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.PoissonRandom;

/**
 * Random transaction pattern that uses a poisson distribution for the wait time and the amount
 * of money to send.
 */
public class PoissonRandomTransactionPattern extends RandomTransactionPattern {
	private static final long serialVersionUID = 1L;
	
	private double lambdaWaitTime;
	private double lambdaAmount;
	private transient PoissonRandom random;

	/**
	 * @param lambdaAmount   - the lambda transaction amount
	 * @param lambdaWaitTime - the lambda time to wait
	 * @param commitEvery    - commit to the main chain after this amount of blocks
	 */
	public PoissonRandomTransactionPattern(double lambdaAmount, double lambdaWaitTime, int commitEvery) {
		super(commitEvery);
		this.lambdaAmount = lambdaAmount;
		this.lambdaWaitTime = lambdaWaitTime;
		this.random = new PoissonRandom();
	}

	@Override
	public void setSeed(long seed) {
		this.seed = seed;
		this.random.setSeed(seed);
	}

	@Override
	public String getName() {
		return "Poisson Random";
	}
	
	@Override
	public Node selectNode(LocalStore localStore) {
		int amount = localStore.getNodes().size();
		if (amount <= 1) throw new IllegalStateException("There is only 1 node!");
		
		//TODO do we want to select nodes to send to uniformly or also poisson distributed?
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
		if (available == 0) return -1;
		
		long amount = random.nextPoisson(lambdaAmount);
		return Math.min(amount, available);
	}

	@Override
	public long timeUntilNextAction(LocalStore localStore) {
		return random.nextPoisson(lambdaWaitTime);
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		this.random = new PoissonRandom();
		if (this.seed != null) {
			this.random.setSeed(this.seed);
		}
	}
}
