package nl.tudelft.blockchain.scaleoutdistributedledger.test.utils;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;

/**
 * Helper class for tests.
 */
public final class TestHelper {
	private TestHelper() {}
	
	/**
	 * @param ownNode   - the own node
	 * @param nrOfNodes - the number of nodes
	 * @param amount    - the amount each node gets
	 * @return          - the genesis block
	 */
	public static Block generateGenesis(OwnNode ownNode, int nrOfNodes, long amount) {
		Map<Integer, Node> nodeList = IntStream.rangeClosed(0, nrOfNodes)
				.boxed()
				.collect(Collectors.toMap(i -> i, Node::new));
		nodeList.put(ownNode.getId(), ownNode);
		Block genesisBlock = TendermintHelper.generateGenesisBlock(amount, nodeList);
		
		for (Node node : nodeList.values()) {
			node.getChain().setGenesisBlock(genesisBlock);
		}
		return genesisBlock;
	}
	
	/**
	 * @param genesisBlock - the genesis block
	 * @return             - the map of nodes
	 */
	public static Map<Integer, Node> getNodeList(Block genesisBlock) {
		return genesisBlock.getTransactions()
				.stream()
				.map(Transaction::getReceiver)
				.collect(Collectors.toMap(Node::getId, n -> n));
	}
}
