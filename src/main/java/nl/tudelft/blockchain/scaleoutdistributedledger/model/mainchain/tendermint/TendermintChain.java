package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.socket.TSocket;
import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

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
	private ExecutorService threadPool;
	private Set<Sha256Hash> cache;
	private final Object cacheLock = new Object();
	private Map<Integer, Set<Integer>> extraCache = new HashMap<>();
	private Map<String, Sha256Hash> superExtraCache = new HashMap<>();
	@Getter
	private long currentHeight;
	@Getter
	private Application app;

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
		this.threadPool = Executors.newSingleThreadExecutor();
		Thread t = new Thread(() -> socket.start(abciServerPort));
		t.setName("Main Chain Socket");
		t.start();
		this.initClient();
		this.initialUpdateCache();

		Log.log(Level.INFO, "Successfully started Tendermint chain (ABCI server + client); server on " + DEFAULT_ADDRESS + ":" + abciServerPort);
	}
	/**
	 * Called on start of the instance.
	 */
	private void initClient() {
		this.client = new ABCIClient(DEFAULT_ADDRESS + ":" + (abciServerPort - 1));
		Log.log(Level.INFO, "Started ABCI Client on " + DEFAULT_ADDRESS + ":" + (abciServerPort - 1));
	}

	protected void initialUpdateCache() {
		boolean updated = false;
		do {
			try {
				updateCacheBlocking(-1, false);
				updated = true;
			} catch (Exception e) {
				int retryTime = 3;
				Log.log(Level.INFO, "Could not update cache on startup, trying again in " + retryTime + "s.");
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
	 * This method starts a separate thread, so the cache is not yet updated on returning from this call.
	 *
	 * @param height - The height to update to, if -1 check the needed height with Tendermint
	 */
	protected void updateCache(long height) {
		if (client == null) return; // If in startup
		this.threadPool.submit(() -> updateCacheBlocking(height, false));
	}

	//TODO IMPORTANT Remove the extra
	/**
	 * Update the cache of the chain.
	 * Note that this method is blocking and execution may therefore take a while,
	 * It is recommended to use {@link TendermintChain#updateCache(long)} instead.
	 *
	 * @param height - The height to update to, if -1 check the needed height with Tendermint
	 */
	private void updateCacheBlocking(long height, boolean extra) {
		if (height == -1) {
			height = this.client.status().getLong("latest_block_height");
		}

		for (long i = currentHeight + 1; i <= height; i++) {
			List<BlockAbstract> abstractsAtCurrentHeight = this.client.query(i);
			if (abstractsAtCurrentHeight == null) {
				Log.log(Level.WARNING, "Could not get block at height " + i + ", perhaps the tendermint rpc is not (yet) running (or broken)");
				return;
			}
			synchronized (cacheLock) {
				for (BlockAbstract abs : abstractsAtCurrentHeight) {
					if (cache.add(abs.getBlockHash()) && extra) {
						Log.debug("{0}: updateCacheBlocking caused new block to be added", getApp().getLocalStore().getOwnNode().getId());
					}
					int blockNum = abs.getBlockNumber();
					int owner = abs.getOwnerNodeId();
					Set<Integer> setOfBlockNums = extraCache.getOrDefault(owner, new HashSet<>());
					setOfBlockNums.add(blockNum);
					extraCache.put(owner, setOfBlockNums);
					superExtraCache.putIfAbsent(owner + "-" + blockNum, abs.getBlockHash());
				}
			}
		}
		if (currentHeight < height) {
			Log.log(Level.FINE, "Successfully updated the Tendermint cache for node " + this.app.getLocalStore().getOwnNode().getId()
					+ " from height " + currentHeight + " -> " + height	+ ", number of cached hashes of abstracts on main chain is now " + cache.size());
			Log.log(Level.FINE, "Node " + this.getApp().getLocalStore().getOwnNode().getId() + " current view of cache is " + this.getCurrentCache());
			Log.log(Level.FINE, "Node " + this.getApp().getLocalStore().getOwnNode().getId() + " current view of super cache is " + this.getSuperExtraCache());
		}
		currentHeight = Math.max(currentHeight, height);	// For concurrency reasons use the maximum
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
		byte[] hash = client.commit(abs);
		if (hash == null) {
			Log.log(Level.INFO, "Commiting abstract to tendermint failed");
			return null;
		} else {
			abs.setAbstractHash(Sha256Hash.withHash(hash));
			return Sha256Hash.withHash(hash);
		}
	}

	@Override
	public boolean isPresent(Block block) {
		Sha256Hash blockHash = block.getHash();
		return cache.contains(blockHash);
		
//		//TODO IMPORTANT Decide what to do here
//		if (cache.contains(blockHash)) {
//			return true;
//		} else {
////			Log.log(Level.INFO, "Node" + this.getApp().getLocalStore().getOwnNode() + " checking cache for " + block.getOwner().getId() + "-" + block.getNumber() + ": " + blockHash + ", the set is " + this.cache);
//			// We could miss some blocks in our cache, so update and wait for the results
//			//TODO We might not want to update here. The cache should be enough
//			updateCacheBlocking(-1, true);
////			Log.log(Level.INFO, "Node" + this.getApp().getLocalStore().getOwnNode() + " did not find " + blockHash + ", so updated the cache and now the set is " + this.cache);
//			return cache.contains(blockHash);
//			//TODO: We might want to check the actual main chain in the false case
//			//      For when an abstract is in a block that is not yet closed by an ENDBLOCK
//			//		This now works because the block size is 1
//		}
	}

	/**
	 * Only to be used for initial block.
	 * @param genesisBlockHash the hash of the first block (genesis block)
	 * @return true if succeeded, false otherwise
	 */
	boolean addToCache(Sha256Hash genesisBlockHash) {
		return cache.add(genesisBlockHash);
	}

	public Map<Integer, Set<Integer>> getCurrentCache() {
		synchronized (cacheLock) {
			return new HashMap<>(this.extraCache);
		}
	}

	public Map<String, Sha256Hash> getSuperExtraCache() {
		synchronized (cacheLock) {
			return new HashMap<>(this.superExtraCache);
		}
	}

}
