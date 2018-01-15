package nl.tudelft.blockchain.scaleoutdistributedledger;

import lombok.SneakyThrows;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Ed25519Key;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint.TendermintChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import java.io.IOException;
import java.util.*;

/**
 * Class for running manual tests
 */
public class ManualTest {
	//TODO: adjust this to your own liking; perhaps make get it from properties file?
	static String tendermintBinary = "/path/to/tendermint/executable";
	//uses directory of this path+[nodeNumber] for each node
	static String nodeFilesBaseLocation = "/some/path/to/store/node/tendermint/data/node";

	public static void main(String[] args){
		testRunningTendermint();
//		testTendermintCommitQuery();
	}


	/**
	 * Test the entire tendermint part - start with generating node keys and files, then genesis and configs,
	 * then run nodes, commit some stuff and see whether it works. Needs the node server to be running.
	 */
	@SneakyThrows
	public static void testRunningTendermint() {

		int numberOfNodes = 3;
		List<String> publicKeys = new LinkedList<>();
		for (int i = 1; i <= numberOfNodes; i++) {
			String nodeLoc = nodeFilesBaseLocation + i;
			Ed25519Key nodeKey = TendermintHelper.generatePrivValidatorFile(tendermintBinary, nodeLoc);
			publicKeys.add(Utils.bytesToHexString(nodeKey.getPublicKey()).toUpperCase());
		}

		Date now = new Date();
		List<String> addresses = new ArrayList<>();
		final Block genesisBlock = TendermintHelper.generateGenesisBlock(numberOfNodes, 1000);
		byte[] appHash = genesisBlock.getHash().getBytes();
		for (int i = 1; i <= numberOfNodes; i++) {
			TendermintHelper.generateGenesisFile(nodeFilesBaseLocation + i, now, publicKeys, appHash);
			addresses.add("192.168.1.107:" + (Application.NODE_PORT + 1 + 4 * (i - 1)));
		}
		Application[] apps = new Application[numberOfNodes];
		for (int i = 1; i<= numberOfNodes; i++) {
			String nodeFilesLocation = nodeFilesBaseLocation + i;
			int nodeBasePort = Application.NODE_PORT + 4 * (i - 1);
			List<String> addressesForThisNode = new ArrayList<>(addresses);
			final int currentNumber = i;
			addressesForThisNode.remove(i-1);
			Thread nodeThread = new Thread(() -> {
				try {
					TendermintHelper.runTendermintNode(tendermintBinary, nodeFilesLocation, nodeBasePort, addressesForThisNode);
					Application node = new Application(false);
					node.init(nodeBasePort, genesisBlock);
					apps[currentNumber-1] = node;
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			nodeThread.start();
		}
		Thread.sleep(10000);
		Random random = new Random();
		int randomBlockNumber = random.nextInt();
		byte[] randomBlockHashBytes = new byte[20];
		random.nextBytes(randomBlockHashBytes);
		Sha256Hash randomBlockHash = Sha256Hash.withHash(randomBlockHashBytes);
		BlockAbstract abs1 = new BlockAbstract(1, randomBlockNumber, randomBlockHash, null);
		System.out.println("About to commit an abstract");
		Sha256Hash h = apps[0].getMainChain().commitAbstract(abs1);
		if (h != null) {
			System.out.println("Committed an abstract with hash " + h.toString());
		} else {
			System.out.println("Hash is null");
		}

		BlockAbstract abs2 = new BlockAbstract(1, randomBlockNumber, randomBlockHash, null);
		abs2.setAbstractHash(h);
		for (int i = 0; i < numberOfNodes; i++) {
			System.out.println("Query for the same abstract gives a " + apps[i].getMainChain().isPresent(abs2.getBlockHash()));
		}
	}

	/**
	 * Manual test for {@link TendermintChain} commit and query.
	 */
	@SneakyThrows
	private static void testTendermintCommitQuery() {
		Application app = new Application(false);

		TendermintHelper.runTendermintNode(tendermintBinary,
				nodeFilesBaseLocation, Application.NODE_PORT, new LinkedList<>());
		final Block genesisBlock = TendermintHelper.generateGenesisBlock(1, 1000);
		app.init(Application.NODE_PORT, genesisBlock);
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
