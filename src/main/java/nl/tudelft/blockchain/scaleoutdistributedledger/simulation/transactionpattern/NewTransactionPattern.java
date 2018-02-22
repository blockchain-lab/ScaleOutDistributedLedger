package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.CancellableInfiniteRunnable;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.PoissonRandom;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.amountselector.AmountSelector;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.nodeselector.NodeSelector;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NewTransactionPattern implements ITransactionPattern {
	private static final long serialVersionUID = -2847452820485718387L;
	
	protected AmountSelector amountSelector;
	protected NodeSelector nodeSelector;
	protected transient PoissonRandom random = new PoissonRandom();
	protected Long seed;

	@Override
	public String getName() {
		return nodeSelector.getClass().getSimpleName() + " / " + amountSelector.getClass().getSimpleName();
	}

	@Override
	public Node selectNode(LocalStore localStore) {
		return nodeSelector.selectNode(localStore);
	}

	@Override
	public long selectAmount(LocalStore localStore) {
		return amountSelector.selectAmount(localStore);
	}

	/**
	 * @param amountSelector the amountSelector to set
	 */
	public void setAmountSelector(AmountSelector amountSelector) {
		this.amountSelector = amountSelector;
		amountSelector.setRandom(random);
	}

	/**
	 * @param nodeSelector the nodeSelector to set
	 */
	public void setNodeSelector(NodeSelector nodeSelector) {
		this.nodeSelector = nodeSelector;
		nodeSelector.setRandom(random);
	}

	/**
	 * @param random the random to set
	 */
	public void setRandom(PoissonRandom random) {
		this.random = random;
		if (amountSelector != null) amountSelector.setRandom(random);
		if (nodeSelector != null) nodeSelector.setRandom(random);
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
		
		if (amountSelector != null) amountSelector.setRandom(random);
		if (nodeSelector != null) nodeSelector.setRandom(random);
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
}
