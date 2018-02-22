package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.Set;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.SimulationMain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.CancellableInfiniteRunnable;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.PoissonRandom;

public class CustomTransactionPattern extends RandomTransactionPattern {
	private static final long serialVersionUID = 1L;
	
	private int waitTime;
	private int muAmount;
	private transient PoissonRandom random;
	private int g;

	/**
	 * @param minAmount   - the minimal transaction amount
	 * @param maxAmount   - the maximal transaction amount
	 * @param waitTime    - the time to wait between making transactions
	 * @param commitEvery - commit to the main chain after this amount of blocks
	 */
	public CustomTransactionPattern(int mu, int waitTime, int commitEvery, int g) {
		super(commitEvery);
		this.muAmount = mu;
		this.waitTime = waitTime;
		this.g = g;
		this.random = new PoissonRandom();
	}

	@Override
	public String getName() {
		return "Custom";
	}

	@Override
	public Node selectNode(LocalStore localStore) {
		int ownId = localStore.getOwnNode().getId();
		int selected = (ownId + random.nextInt(g)) % Settings.INSTANCE.totalNodesNumber;
		return localStore.getNode(selected);
	}

	@Override
	public long selectAmount(LocalStore localStore) {
		long available = localStore.getAvailableMoney();
		if (available == 0) return -1;
		
		int amount = random.nextInt(muAmount - 1) + 1;
		return Math.min(amount, available);
	}

	@Override
	public long timeUntilNextAction(LocalStore localStore) {
		return waitTime;
	}

	@Override
	public void setSeed(long seed) {
		this.seed = seed;
		this.random.setSeed(seed);
	}
	
	@Override
	public CancellableInfiniteRunnable<LocalStore> getRunnable(LocalStore localStore) {
		if (this.seed != null) {
			this.random.setSeed(this.seed + localStore.getOwnNode().getId());
		}
		
		return new CancellableInfiniteRunnable<LocalStore>(
				localStore,
				this::doAction,
				this::timeUntilNextAction,
				this::onStop
		);
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		this.random = new PoissonRandom();
	}
}
