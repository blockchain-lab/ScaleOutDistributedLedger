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
    public void testHexStringToByteArrayWithoutPrefix() {
        String test = "0011FF";
        assertArrayEquals(new byte[] {0x00, 0x11, (byte) 0xFF}, Utils.hexStringToBytes(test));
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
