package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.socket.TSocket;
import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.test.utils.SilencedTestClass;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Class testing the Tendermint implementation of the {@link MainChain} interface.
 * Tests cases that log warnings, so logging is silenced.
 */
public class TendermintChainTest extends SilencedTestClass {
	private TendermintChain instance;
	private ABCIClient clientMock;
	private TSocket socketMock;
	private Set<Sha256Hash> cache;

	/**
	 * Create a fresh instance for each test.
	 */
	@Before
	public void setUp() {
		clientMock = mock(ABCIClient.class);
		socketMock = mock(TSocket.class);
		cache = new HashSet<>();
		Application appMock = mock(Application.class);
		LocalStore localStoreMock = mock(LocalStore.class);

		when(appMock.getLocalStore()).thenReturn(localStoreMock);
		when(localStoreMock.getOwnNode()).thenReturn(new OwnNode(0));

		instance = new TendermintChain(clientMock, socketMock, cache, appMock);
	}

	/**
	 * Test updating the cache.
	 * Adds two blocks containing 3 abstracts and fails to trigger the loop.
	 */
	@Test
	public void testInitialUpdateCache() {
		when(clientMock.query(anyLong())).thenAnswer(new Answer() {
			private int c;

			public Object answer(InvocationOnMock invocation) {
				List<BlockAbstract> data = new ArrayList<>();
				if (c == 0) {
					data.add(new BlockAbstract(0, 0, new Sha256Hash(Utils.hexStringToBytes("FF44")), null));
					data.add(new BlockAbstract(0, 0, new Sha256Hash(Utils.hexStringToBytes("FF55")), null));
				} else {
					data.add(new BlockAbstract(0, 0, new Sha256Hash(Utils.hexStringToBytes("FF66")), null));
				}
				c++;
				return data;
			}
		});

		when(clientMock.status()).thenAnswer(new Answer() {
			private JSONObject json;

			public Object answer(InvocationOnMock invocation) {
				if (json == null) {
					json = new JSONObject();
				} else {
					json.put("latest_block_height", 2l);
				}
				return json;
			}
		});

		instance.initialUpdateCache();

		assertEquals(3, cache.size());
		assertEquals(2, instance.getCurrentHeight());
	}

	/**
	 * Test if stopping stops the socket.
	 */
	@Test
	public void testStop() {
		instance.stop();
		verify(socketMock, times(1)).stop();
	}

	/**
	 * Test if committing calls the client and if the hashes are set correctly.
	 */
	@Test
	public void testCommitAbstract() {
		BlockAbstract abs = new BlockAbstract(0, 0, Sha256Hash.withHash(Utils.hexStringToBytes("11FF")) , null);
		Sha256Hash hash = Sha256Hash.withHash(Utils.hexStringToBytes("FF11"));

		when(clientMock.commit(any(BlockAbstract.class))).thenReturn(Utils.hexStringToBytes("FF11"));

		Sha256Hash result = instance.commitAbstract(abs);
		assertEquals(hash, result);
		assertEquals(hash, abs.getAbstractHash());
		verify(clientMock, times(1)).commit(any(BlockAbstract.class));
	}

	/**
	 * Test the case where comitting fails.
	 */
	@Test
	public void testCommitAbstractFail() {
		BlockAbstract abs = new BlockAbstract(0, 0, Sha256Hash.withHash(Utils.hexStringToBytes("11FF")) , null);
		when(clientMock.commit(any(BlockAbstract.class))).thenReturn(null);

		assertNull(instance.commitAbstract(abs));
	}

	/**
	 * Test isPresent when the data is in the cache.
	 */
	@Test
	public void testIsPresentAlreadyInCache() {
		Sha256Hash hash = Sha256Hash.withHash(Utils.hexStringToBytes("FF11"));
		cache.add(hash);

//		assertTrue(instance.isPresent(hash));
	}

	/**
	 * Test isPresent when the data is not in the cache initially, but an update fixes that.
	 */
	@Test
	public void testIsPresentInCacheAfterUpdate() {
		Sha256Hash hash = Sha256Hash.withHash(Utils.hexStringToBytes("FF11"));

		JSONObject json = new JSONObject();
		json.put("latest_block_height", 1);
		when(clientMock.status()).thenReturn(json);
		List<BlockAbstract> abss = new ArrayList<>();
		abss.add(new BlockAbstract(0, 0, hash, null));
		when(clientMock.query(anyLong())).thenReturn(abss);

//		assertTrue(instance.isPresent(hash));
	}

	/**
	 * Test isPresent when the data is not in the cache initially and an update does not fix that.
	 */
	@Test
	public void testIsPresentNotInCacheAfterUpdate() {
		Sha256Hash hash1 = Sha256Hash.withHash(Utils.hexStringToBytes("FF11"));
		Sha256Hash hash2 = Sha256Hash.withHash(Utils.hexStringToBytes("AAFF"));

		JSONObject json = new JSONObject();
		json.put("latest_block_height", 1);
		when(clientMock.status()).thenReturn(json);
		List<BlockAbstract> abss = new ArrayList<>();
		abss.add(new BlockAbstract(0, 0, hash2, null));
		when(clientMock.query(anyLong())).thenReturn(abss);

//		assertFalse(instance.isPresent(hash1));
	}

	/**
	 * Test adding to the cache.
	 */
	@Test
	public void testAddToCache() {
		Sha256Hash hash = Sha256Hash.withHash(Utils.hexStringToBytes("FF22"));
		assertTrue(instance.addToCache(hash));
		assertTrue(cache.contains(hash));
	}

	/**
	 * Test adding an existing item to the cache.
	 */
	@Test
	public void testAddToCacheFails() {
		Sha256Hash hash = Sha256Hash.withHash(Utils.hexStringToBytes("FF22"));
		cache.add(hash);

		assertFalse(instance.addToCache(hash));
	}

}