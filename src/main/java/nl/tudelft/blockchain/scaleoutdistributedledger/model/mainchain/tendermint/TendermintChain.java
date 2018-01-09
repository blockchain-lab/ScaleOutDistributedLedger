package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.socket.TSocket;
import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
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
	public static final String DEFAULT_ADDRESS = "localhost";
	public static final int DEFAULT_PORT = 46658;
	private ABCIServer handler;
	private ABCIClient client;
	private TSocket socket;

	/**
	 * Create and start the connection with Tendermint on the given address.
	 * This is a singleton, should be one per {@link Application}.
	 * @param port - the port on which we run the server
	 */
	public TendermintChain(final int port) {
		System.out.println("Starting Tendermint chain on " + DEFAULT_ADDRESS + ":" + port);
		socket = new TSocket();
		handler = new ABCIServer();
		client = new ABCIClient(DEFAULT_ADDRESS + ":" + (port - 1));

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
			Log.log(Level.INFO, "Tendermint [COMMIT] failed");
			return null;
		} else {
			abs.setAbstractHash(Sha256Hash.withHash(hash));

			abs.setOnMainChain(Optional.of(true));
			return Sha256Hash.withHash(hash);
		}
	}

	@Override
	public boolean isPresent(BlockAbstract abs) {
		if (abs.getAbstractHash() == null) {
			//TODO: find out what hashing algorithm is used and hash it the same way instead of failing.
			//There's no mention of it in the documentation, after reading the sources I think it might be just
			// a signature with the private ed25519 key, but not 100% sure
			Log.log(Level.WARNING, "Cannot query for an abstract with unknown hash");
			return false;
		}
		return client.query(abs.getAbstractHash());
	}

}
