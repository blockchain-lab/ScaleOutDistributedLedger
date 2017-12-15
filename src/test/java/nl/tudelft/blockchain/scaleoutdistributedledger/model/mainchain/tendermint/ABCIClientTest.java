package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test class for {@link ABCIClient}.
 */
public class ABCIClientTest {
	private ABCIClient instance;

	/**
	 * Create a fresh client for each test.
	 */
	@Before
	public void setUp() {
		this.instance = new ABCIClient("");
	}

	/**
	 * Test the byte array conversion when using the 0x prefix.
	 */
	@Test
	public void testHexStringToByteArrayWithPrefix() {
		String test = "0x0011FF";
		assertTrue(Arrays.equals(new byte[] {0x00, 0x11, (byte) 0xFF}, instance.hexStringToByteArray(test)));
	}

	/**
	 * Test the byte array conversion without using the 0x prefix.
	 */
	@Test
	public void testHexStringToByteArrayWithoutPrefix() {
		String test = "0011FF";
		assertTrue(Arrays.equals(new byte[] {0x00, 0x11, (byte) 0xFF}, instance.hexStringToByteArray(test)));
	}

	/**
	 * Test the byte array conversion to a String.
	 */
	@Test
	public void testByteArrayToHexString() {
		byte[] array = new byte[]{0x00, 0x12, (byte) 0xFF};
		assertEquals("0x0012FF", instance.byteArrayToHexString(array));
	}

}