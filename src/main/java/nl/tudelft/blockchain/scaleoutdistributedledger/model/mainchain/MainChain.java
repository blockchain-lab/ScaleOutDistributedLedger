package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;

import java.util.List;

/**
 * Interface for main chain inplementations.
 */
public interface MainChain {
	/**
	 * Commit an abstract to the main chain.
	 *
	 * @param abs - the abstract to commit
	 * @return - the hash given to the transaction on commit
	 */
	public Sha256Hash commitAbstract(BlockAbstract abs);

	/**
	 * Query the main chain for the presence of an abstract.
	 *
	 * @param abs - the abstract to query for
	 * @return - true when present, false otherwise
	 */
	public boolean isPresent(BlockAbstract abs);

	/**
	 * Connect to another node.
	 *
	 * @param node - the node to connect to
	 */
	public void connectTo(Node node);

	/**
	 * Connect to multiple nodes
	 *
	 * @param nodes - a list of node to connect to
	 */
	public default void connectTo(List<Node> nodes) {
		for (Node node : nodes) {
			connectTo(node);
		}
	}
}
