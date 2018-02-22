package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.nodeselector;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;

import lombok.Getter;

/**
 * A node selector which uses a poisson random distribution for node selection, centered around
 * lambda, within an optional limit.
 */
@Getter
public class PoissonNodeSelector extends NodeSelector {
	private static final long serialVersionUID = 1L;
	protected int limit;
	protected double lambda;
	
	/**
	 * @param lambda - the lambda for the distribution
	 * @see #PoissonNodeSelector(double, int)
	 */
	public PoissonNodeSelector(double lambda) {
		this(lambda, 0);
	}
	
	/**
	 * If limit is positive, then every node can only select any of the limit nodes after it
	 * (wrapping around).
	 * @param lambda - the lambda for the distribution
	 * @param limit - the limit for node selection. Disabled if 0.
	 */
	public PoissonNodeSelector(double lambda, int limit) {
		this.lambda = lambda;
		this.limit = limit;
	}
	
	@Override
	public Node selectNode(LocalStore localStore) {
		final int amount = localStore.getNodes().size();
		Node node;
		do {
			int delta;
			do {
				delta = getRandom().nextPoisson(lambda);
			} while (delta == 0);
			
			if (limit != 0 && delta > limit) delta = limit;
			
			int target = (localStore.getOwnNode().getId() + delta) % amount;
			node = localStore.getNode(target);
		} while (node == localStore.getOwnNode());
		return node;
	}
}
