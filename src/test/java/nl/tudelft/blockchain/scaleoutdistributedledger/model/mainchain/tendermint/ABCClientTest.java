package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ABCClientTest {
	private ABCIClient instance;

	@Before
	public void setUp() {
		this.instance = new ABCIClient("");
	}

	@Test
	public void testHexStringToByteArrayWithPrefix() {
		String test = "0x0011FF";
		assertTrue(Arrays.equals(new byte[] {0x00, 0x11, (byte) 0xFF}, instance.hexStringToByteArray(test)));
	}

	@Test
	public void testHexStringToByteArrayWithoutPrefix() {
		String test = "0011FF";
		assertTrue(Arrays.equals(new byte[] {0x00, 0x11, (byte) 0xFF}, instance.hexStringToByteArray(test)));
	}

}