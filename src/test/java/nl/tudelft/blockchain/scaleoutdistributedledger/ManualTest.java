package nl.tudelft.blockchain.scaleoutdistributedledger;
import lombok.SneakyThrows;
import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint.TendermintChain;

import java.util.Random;

/**
 * Class for running manual tests
 */
public class ManualTest {

	public static void main(String[] args){
		testTendermintCommitQuery();
	}

	/**
	 * Manual test for {@link TendermintChain} commit and query.
	 */
	@SneakyThrows
	private static void testTendermintCommitQuery() {
		Application app = new Application();
		app.init(Application.NODE_PORT);
		Random random = new Random();
		int randomBlockNumber = random.nextInt();
		byte[] randomBlockHashBytes = new byte[20];
		random.nextBytes(randomBlockHashBytes);
		Sha256Hash randomBlockHash = Sha256Hash.withHash(randomBlockHashBytes);

		Node node = new Node(0);
		BlockAbstract abs1 = new BlockAbstract(node.getId(), randomBlockNumber, randomBlockHash, null);

		System.out.println("About to commit an abstract");
		Sha256Hash h = app.getMainChain().commitAbstract(abs1);
		if (h != null) {
			System.out.println("Committed an abstract with hash " + h.toString());
		}

		BlockAbstract abs2 = new BlockAbstract(node.getId(), randomBlockNumber, randomBlockHash, null);
		abs2.setAbstractHash(h);

		System.out.println("Query for the same abstract gives a " + app.getMainChain().isPresent(abs2.getBlockHash()));

	}
}
