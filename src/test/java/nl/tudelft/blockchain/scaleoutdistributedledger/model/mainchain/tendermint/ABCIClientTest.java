package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.test.utils.SilencedTestClass;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link ABCIClient}.
 * Tests cases that log warnings, so logging is silenced.
 */
public class ABCIClientTest extends SilencedTestClass {
	private ABCIClient instance;

	/**
	 * Create a fresh client for each test.
	 */
	@Before
	public void setUp() {
		this.instance = spy(new ABCIClient("localhost:9998"));
	}

	/**
	 * Test a successful commit.
	 */
	@Test
	public void testCommitSuccess() {
		String hash = "AAFF";
		BlockAbstract abs = new BlockAbstract(0, 0, null, null);
		JSONObject json = new JSONObject();
		json.put("result", json);
		json.put("deliver_tx", json);
		json.put("code", 0);
		json.put("hash", hash);

		doReturn(json).when(instance).sendRequest(anyString(), any());
		assertArrayEquals(Utils.hexStringToBytes(hash), instance.commit(abs));
	}

	/**
	 * Test a commit that returns an error.
	 */
	@Test
	public void testCommitError() {
		BlockAbstract abs = new BlockAbstract(0, 0, null, null);
		JSONObject json = new JSONObject();
		JSONObject jsonError = new JSONObject();
		json.put("error", jsonError);
		jsonError.put("data", "An error message");

		doReturn(json).when(instance).sendRequest(anyString(), any());
		assertNull(instance.commit(abs));
	}

	/**
	 * Test a commit where the connection fails.
	 */
	@Test
	public void testCommitFail() {
		BlockAbstract abs = new BlockAbstract(0, 0, null, null);

		doReturn(null).when(instance).sendRequest(anyString(), any());
		assertNull(instance.commit(abs));
	}

	/**
	 * Test a successful query.
	 */
	@Test
	public void testQuerySuccess() {
		Sha256Hash hash = Sha256Hash.withHash(Utils.hexStringToBytes("FF00"));
		JSONObject json = new JSONObject();
		json.put("result", 42);

		doReturn(json).when(instance).sendRequest(anyString(), any());
		assertTrue(instance.query(hash));
	}

	/**
	 * Test a query that returns an error.
	 */
	@Test
	public void testQueryError() {
		Sha256Hash hash = Sha256Hash.withHash(Utils.hexStringToBytes("FF00"));
		JSONObject json = new JSONObject();
		json.put("error", 42);

		doReturn(json).when(instance).sendRequest(anyString(), any());
		assertFalse(instance.query(hash));
	}

	/**
	 * Test a query on a failing connection.
	 */
	@Test
	public void testQueryFail() {
		Sha256Hash hash = Sha256Hash.withHash(Utils.hexStringToBytes("FF00"));

		doReturn(null).when(instance).sendRequest(anyString(), any());
		assertFalse(instance.query(hash));
	}

	/**
	 * Test a successful height lookup.
	 */
	@Test
	public void testQueryHeightSuccess() {
		BlockAbstract abs1 = new BlockAbstract(0, 0, Sha256Hash.withHash(Utils.hexStringToBytes("11FF")), null);
		BlockAbstract abs2 = new BlockAbstract(0, 0, Sha256Hash.withHash(Utils.hexStringToBytes("AABB")), null);

		JSONObject json = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		json.put("result", json);
		json.put("block", json);
		json.put("data", json);
		json.put("txs", jsonArray);

		jsonArray.put(Utils.bytesToBas64String(abs1.toBytes()));
		jsonArray.put(Utils.bytesToBas64String(abs2.toBytes()));

		doReturn(json).when(instance).sendRequest(anyString(), any());

		List<BlockAbstract> result = instance.query(10);
		assertEquals(2, result.size());
		assertEquals(abs1.getBlockHash(), result.get(0).getBlockHash());
	}

	/**
	 * Test a height lookup that gives a malformed result.
	 */
	@Test
	public void testQueryHeightMalformed() {
		JSONObject json = new JSONObject();
		json.put("malformed", 42);

		doReturn(json).when(instance).sendRequest(anyString(), any());

		assertNull(instance.query(10));
	}

	/**
	 * Test a height lookup that results in an error.
	 */
	@Test
	public void testQueryHeightError() {
		JSONObject json = new JSONObject();
		JSONObject jsonError = new JSONObject();
		json.put("error", jsonError);
		jsonError.put("data", "An error message");

		doReturn(json).when(instance).sendRequest(anyString(), any());

		assertNull(instance.query(10));
	}

	/**
	 * Test a height lookup when the connection fails.
	 */
	@Test
	public void testQueryHeightFail() {
		doReturn(null).when(instance).sendRequest(anyString(), any());
		assertNull(instance.query(10));
	}

	/**
	 * Test a successful status call.
	 */
	@Test
	public void testStatusSuccess() {
		JSONObject json = new JSONObject();
		JSONObject jsonResult = new JSONObject();
		json.put("result", jsonResult);

		doReturn(json).when(instance).sendRequest(anyString(), any());

		assertEquals(jsonResult, instance.status());
	}

	/**
	 * Test a status call that results in a fail.
	 */
	@Test
	public void testStatusFail() {
		doReturn(null).when(instance).sendRequest(anyString(), any());

		assertEquals(0, instance.status().length());
	}

}