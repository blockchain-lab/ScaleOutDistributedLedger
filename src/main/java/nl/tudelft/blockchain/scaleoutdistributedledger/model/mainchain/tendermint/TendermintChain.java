package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.socket.TSocket;
import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.*;
import java.util.logging.Level;

/**
 * Class implementing {@link MainChain} for a Tendermint chain.
 * @see <a href="https://tendermint.com/">Tendermint.com</a>
 */
public final class TendermintChain implements MainChain {
	public static final String DEFAULT_ADDRESS = "localhost";
	public static int TENDERMINT_PORT;

	private ABCIServer handler;
	private ABCIClient client;
	private TSocket socket;

	private Set<Sha256Hash> cache;
	private long currentHeight = 0;

	/**
	 * Create and start the connection with Tendermint on the given address.
	 * This is a singleton, should be one per {@link Application}.
	 * @param port - the port on which we run the server
	 */
	public TendermintChain(final int port) {
		TENDERMINT_PORT = port;
		Log.log(Level.INFO, "Starting Tendermint chain on " + DEFAULT_ADDRESS + ":" + TENDERMINT_PORT);
		this.cache = new HashSet<Sha256Hash>();

		socket = new TSocket();
		handler = new ABCIServer(this);

		socket.registerListener(handler);

		Thread t = new Thread(() -> socket.start(TENDERMINT_PORT));
		t.setName("Main Chain Socket");
		t.start();

		//TODO: There should be a better way to do this than just waiting and hoping Tendermint initializes before we continue
		new Thread(() -> {
			try {
				Thread.sleep(10000);
				start();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * Called on start of the instance.
	 */
	public void start() {
		client = new ABCIClient(DEFAULT_ADDRESS + ":" + (TENDERMINT_PORT - 1));
		Log.log(Level.INFO, "Started Tendermint chain on " + DEFAULT_ADDRESS + ":" + TENDERMINT_PORT);
	}

	/**
	 * Update the cache of the chain.
	 * This method starts a separate thread, so the cache is not yet updated on returning from this call.
	 *
	 * @param height - The height to update to, if -1 check the needed height with Tendermint
	 */
	protected void updateCache(long height) {
		if (client == null) return; // If in startup
		new Thread(() -> {
			updateCacheBlocking(height);
		}).start();
	}

	/**
	 * Update the cache of the chain.
	 * Note that this method is blocking and execution may therefore take a while,
	 * It is recommended to use {@link TendermintChain#updateCache(long)} instead.
	 *
	 * @param height - The height to update to, if -1 check the needed height with Tendermint
	 */
	private void updateCacheBlocking(long height) {
		if (height == -1) {
			height = this.client.status().getLong("latest_block_height");
		}

		for(long i = currentHeight; i <= height; i++) {
			for(BlockAbstract abs : this.client.query(i)) {
				cache.add(abs.getBlockHash());
			}
		}
		Log.log(Level.INFO,"Updated the Tendermint cache from " + currentHeight + " -> " + height + ", new size is " + cache.size());
		currentHeight = Math.max(currentHeight, height);	// For concurrency reasons use the maximum
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
	public boolean isPresent(Sha256Hash blockHash) {
		if (cache.contains(blockHash)){
			return true;
		} else {
			// We could miss some blocks in our cache, so update and wait for the results
			updateCacheBlocking(-1);

			return cache.contains(blockHash);
			//TODO: We might want to check the actual main chain in the false case
			//      For when an abstract is in a block that is not yet closed by an ENDBLOCK
			//		This now works because the block size is 1
		}
	}

}
