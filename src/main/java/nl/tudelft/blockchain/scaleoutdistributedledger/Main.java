package nl.tudelft.blockchain.scaleoutdistributedledger;


import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;

/**
 * Main starting point of the application.
 */
public final class Main {

	private Main() {
		// prevents instantiation
		throw new UnsupportedOperationException();
	}

	/**
	 * Main method, starting point of the application.
	 * @param args - command line arguments.
	 */
	public static void main(String[] args) {
		//TODO
		testTendermintCommitQuery();
	}

	private static void testTendermintCommitQuery() {
		Application app = new Application(46658);
		Node node = new Node(0);
		BlockAbstract abs1 = new BlockAbstract(node.getId(), 0, null, null);
		try {
			System.out.println("Giving Tendermint some time to figure stuff out");
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Sha256Hash h = Application.getMainChain().commitAbstract(abs1);
		if (h != null) {
			System.out.println("Committed an abstract with hash " + h.toString());
		}

		BlockAbstract abs2 = new BlockAbstract(node.getId(), 0, null, null);
		abs2.setAbstractHash(h);

		try {
			System.out.println("Giving tendermint some time to process committed block");
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Query for the same abstract gives a " + Application.getMainChain().isPresent(abs2));
	}
}

