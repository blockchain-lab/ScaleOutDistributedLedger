package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for {@link Utils}.
 */
public class UtilsTest {
	/**
	 * Test the byte array conversion without using the 0x prefix.
	 */
	@Test
	public void testHexStringToByteArray_WithoutPrefix_Uppercase() {
		String test = "091AFE";
		assertArrayEquals(new byte[] {0x09, 0x1A, (byte) 0xFE}, Utils.hexStringToBytes(test));
	}

	/**
	 * Test the byte array conversion without using the 0x prefix.
	 */
	@Test
	public void testHexStringToByteArray_WithoutPrefix_Lowercase() {
		String test = "091afe";
		assertArrayEquals(new byte[] {0x09, 0x1A, (byte) 0xFE}, Utils.hexStringToBytes(test));
	}
	
	/**
	 * Test the byte array conversion with an invalid input.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testHexStringToByteArray_Invalid() {
		String test = "00G0FF";
		Utils.hexStringToBytes(test);
	}

	/**
	 * Test the byte array conversion to a String.
	 */
	@Test
	public void testByteArrayToHexString() {
		byte[] array = new byte[]{0x00, 0x12, (byte) 0xFF};
		assertEquals("0012ff", Utils.bytesToHexString(array));
	}
}
