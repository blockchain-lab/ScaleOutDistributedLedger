package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.nodeselector;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;

import lombok.Getter;

/**
 * A node selector which uses a uniform random distribution for node selection (every option is as
 * likely).
 */
@Getter
public class UniformNodeSelector extends NodeSelector {
	private static final long serialVersionUID = 1L;
	protected int limit;
	
	/**
	 * @see #UniformNodeSelector(int)
	 */
	public UniformNodeSelector() {
		this(0);
	}
	
	/**
	 * If limit is 0, then every node has equal probability of being selected.
	 * If limit is positive, then every node can only select any of the limit nodes after it
	 * (wrapping around).
	 * @param limit - the limit for 
	 */
	public UniformNodeSelector(int limit) {
		this.limit = limit;
	}
	
	@Override
	public Node selectNode(LocalStore localStore) {
		final int amount = localStore.getNodes().size();
		if (limit == 0) {
			if (amount <= 1) throw new IllegalStateException("There is only 1 node!");
			
			int selected;
			Node node;
			do {
				selected = getRandom().nextInt(amount);
				node = localStore.getNode(selected);
			} while (node == localStore.getOwnNode());
			return node;
		} else {
			int ownId = localStore.getOwnNode().getId();
			int selected = (ownId + getRandom().nextInt(limit) + 1) % amount;
			return localStore.getNode(selected);
		}
	}
}
