package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.socket.TSocket;
import lombok.SneakyThrows;
import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.Optional;
import java.util.logging.Level;

/**
 * Class implementing {@link MainChain} for a Tendermint chain.
 * @see <a href="https://tendermint.com/">Tendermint.com</a>
 */
public final class TendermintChain implements MainChain {
	private ABCIServer handler;
	private ABCIClient client;
	private TSocket socket;
	public static final String DEFAULT_ADDRESS = "localhost";
	public static final int DEFAULT_PORT = 46658;

	/**
	 * Create and start the connection with Tendermint on the given address.
	 * This is a singleton, should be one per {@link Application}.
	 * @param port - the port on which we run the server
	 */
	public TendermintChain(final int port) {
		System.out.println("Starting Tendermint chain on " + DEFAULT_ADDRESS +":"+port);
		socket = new TSocket();
		handler = new ABCIServer();
		client = new ABCIClient(DEFAULT_ADDRESS + ":" + (port-1));

		socket.registerListener(handler);

		Thread t = new Thread(() -> socket.start(port));
		t.setName("Main Chain Socket");
		t.start();
	}

	/**
	 * Stop the connection to Tendermint.
	 * Used for testing.
	 */
	protected void stop() {
		socket.stop();
	}

	@Override
	public Sha256Hash commitAbstract(BlockAbstract abs) {
		byte[] hash = client.commit(abs);
		if (hash == null) {
			Log.log(Level.WARNING, "Tendermint [COMMIT] failed");
			return null;
		} else {
			abs.setAbstractHash(Sha256Hash.withHash(hash));

			abs.setOnMainChain(Optional.of(true));
			return Sha256Hash.withHash(hash);
		}
	}

	@Override
	public boolean isPresent(BlockAbstract abs) {
		if(abs.getAbstractHash() == null) {
			Log.log(Level.WARNING, "Cannot query for an abstract with unknown hash");
			return false;
		}
		return client.query(abs.getAbstractHash());
	}

	@Override
	public void connectTo(Node node) {
		if (!client.connect(node.getAddress())) {
			Log.log(Level.WARNING, "Tendermint failed to connect to peer " + node.getAddress());
		}
	}
}
