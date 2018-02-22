package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.nodeselector;

import java.io.Serializable;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.PoissonRandom;

import lombok.Getter;
import lombok.Setter;

/**
 * Interface to represent a node selector.
 */
public abstract class NodeSelector implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Getter @Setter
	protected transient PoissonRandom random;
	
	/**
	 * @param localStore - the local store
	 * @return - the node that was selected
	 */
	public abstract Node selectNode(LocalStore localStore);
	
	/**
	 * @return - the limit
	 */
	public abstract int getLimit();
	
	/**
	 * @param str - the string
	 * @return - the NodeSelector represented by the given string
	 */
	public static NodeSelector fromString(String str) {
		String[] parts = str.split(":");
		switch (parts[0].toLowerCase()) {
			case "uniform":
				return new UniformNodeSelector(Integer.parseInt(parts[1]));
			case "poisson":
				return new PoissonNodeSelector(Double.parseDouble(parts[1]), Integer.parseInt(parts[2]));
			default:
				throw new IllegalArgumentException("Invalid node selector string!");
		}
	}
}
