package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.github.jtendermint.jabci.socket.TSocket;

import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import lombok.Getter;

/**
 * Class implementing {@link MainChain} for a Tendermint chain.
 * @see <a href="https://tendermint.com/">Tendermint.com</a>
 */
public final class TendermintChain implements MainChain {
	public static final String DEFAULT_ADDRESS = "localhost";
	public static final int DEFAULT_ABCI_SERVER_PORT = 46658;
	public int abciServerPort;

	private ABCIServer handler;
	private ABCIClient client;
	private TSocket socket;
	private Set<Sha256Hash> cache;
	private final Object cacheLock = new Object();
	private volatile long currentHeight;
	@Getter
	private Application app;
	private byte[] hash;

	/**
	 * Create and start the ABCI app (server) to connect with Tendermint on the default port (46658).
	 * Also uses (port - 1), which Tendermint should listen on for RPC (rpc.laddr)
	 * @param genesisBlock - the genesis (initial) block for the entire system
	 * @param app          - the application
	 */
	public TendermintChain(Block genesisBlock, Application app) {
		this(DEFAULT_ABCI_SERVER_PORT, genesisBlock, app);
	}
	
	/**
	 * Create and start the ABCI app (server) to connect with Tendermint on the given port.
	 * Also uses (port - 1), which Tendermint should listen on for RPC (rpc.laddr)
	 * @param port         - the port on which we run the server
	 * @param genesisBlock - the genesis (initial) block for the entire system
	 * @param app          - the application
	 */
	public TendermintChain(final int port, Block genesisBlock, Application app) {
		this.abciServerPort = port;
		this.cache = new HashSet<>();
		this.app = app;

		this.socket = new TSocket();
		this.handler = new ABCIServer(this, genesisBlock);

		this.socket.registerListener(handler);
	}

	/**
	 * Constructor used for testing.
	 * @param client - the client to use
	 * @param socket - the socket to use
	 * @param cache - the cache to use
	 * @param app - the application to use
	 */
	protected TendermintChain(ABCIClient client, TSocket socket, Set<Sha256Hash> cache, Application app) {
		this.client = client;
		this.socket = socket;
		this.cache = cache;
		this.app = app;
	}

	/**
	 * Initializes the tendermint chain.
	 */
	@Override
	public void init() {
		Thread t = new Thread(() -> socket.start(abciServerPort));
		t.setName("Main Chain Socket");
		t.start();
		this.initClient();
		this.initialUpdateCache();

		Log.log(Level.INFO, "Successfully started Tendermint chain (ABCI server + client); server on " + DEFAULT_ADDRESS + ":" + abciServerPort, getNodeId());
	}
	/**
	 * Called on start of the instance.
	 */
	private void initClient() {
		this.client = new ABCIClient(DEFAULT_ADDRESS + ":" + (abciServerPort - 1));
		Log.log(Level.INFO, "Started ABCI Client on " + DEFAULT_ADDRESS + ":" + (abciServerPort - 1), getNodeId());
	}

	/**
	 * Performs the initial update of the cache.
	 */
	protected void initialUpdateCache() {
		boolean updated = false;
		do {
			try {
				Thread.sleep(1000);
				updateCacheBlocking(-1);
				updated = true;
			} catch (Exception e) {
				int retryTime = 2;
				Log.log(Level.INFO, "Could not update cache on startup, trying again in " + retryTime + "s.", getNodeId());
				Log.log(Level.FINE, "", e);
				try {
					Thread.sleep(retryTime * 1000);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
			}
		} while (!updated);
		Log.log(Level.INFO, "Successfully updated cache on startup.");
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
		
		//Nothing to update, so block until actually at given height
		long oldHeight = currentHeight;
		if (height <= currentHeight) return;
		
		for (long i = currentHeight + 1; i <= height; i++) {
			List<BlockAbstract> abstractsAtCurrentHeight = this.client.query(i);
			if (abstractsAtCurrentHeight == null) {
				Log.log(Level.WARNING, "Could not get block at height " + i + ", perhaps the tendermint rpc is not (yet) running (or broken)", getNodeId());
				return;
			}
			synchronized (cacheLock) {
				for (BlockAbstract abs : abstractsAtCurrentHeight) {
					addToCache(abs.getBlockHash());
				}
			}
		}
		
		currentHeight = height;

		Log.log(Level.FINE, "Successfully updated the Tendermint cache from height "
				+ oldHeight + " -> " + height	+ ", number of cached hashes of abstracts on main chain is now " + cache.size(), getNodeId());
	}

	/**
	 * Stop the connection to Tendermint.
	 */
	@Override
	public void stop() {
		socket.stop();
		Thread.interrupted();
	}

	@Override
	public Sha256Hash commitAbstract(BlockAbstract abs) {
		byte[] hash = client.commitAsync(abs);
//		byte[] hash = client.commit(abs);
		if (hash == null) {
			Log.log(Level.INFO, "Commiting abstract to tendermint failed", getNodeId());
			return null;
		} else {
			abs.setAbstractHash(Sha256Hash.withHash(hash));
			return abs.getAbstractHash();
		}
	}
	
	@Override
	public boolean isPresent(Sha256Hash hash) {
		return cache.contains(hash);
	}

	@Override
	public boolean isPresent(Block block) {
		Sha256Hash blockHash = block.getHash();
		return cache.contains(blockHash);
	}

	/**
	 * Adds the given block hash to the cache.
	 * 
	 * @param blockHash - the hash of the block
	 * @return          - true if succeeded, false otherwise
	 */
	boolean addToCache(Sha256Hash blockHash) {
		synchronized (cacheLock) {
			if (cache.add(blockHash)) {
				byte[] bytes = blockHash.getBytes();
				if (this.hash == null) {
					this.hash = Arrays.copyOf(bytes, bytes.length);
				} else {
					for (int i = 0; i < bytes.length; i++) {
						this.hash[i] += bytes[i];
					}
				}
				
				return true;
			}
			
			return false;
		}
	}
	
	@Override
	public long getCurrentHeight() {
		return currentHeight;
	}
	
	/**
	 * Sets the current height.
	 * @param height - the height to set to
	 */
	void setCurrentHeight(long height) {
		currentHeight = height;
		
		Log.log(Level.INFO, "Updated to height " + height, getNodeId());
	}
	
	/**
	 * @return - the hash of the current state
	 */
	public byte[] getStateHash() {
		synchronized (cacheLock) {
			return Arrays.copyOf(hash, hash.length);
		}
	}
	
	/**
	 * @return - the id of the node
	 */
	protected int getNodeId() {
		return app.getLocalStore().getOwnNode().getId();
	}
}
