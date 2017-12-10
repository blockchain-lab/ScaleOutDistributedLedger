package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.nio.ByteBuffer;

/**
 * Class for helper functions
 */
public class Utils {
	
	private static final ByteBuffer bufferInteger = ByteBuffer.allocate(Integer.BYTES);   
	
	private static final ByteBuffer bufferLong = ByteBuffer.allocate(Long.BYTES);   
	
	/**
	 * Convert a integer into a byte array
	 * @param number
	 * @return byte array
	 */
	public static byte[] intToByteArray(int number) {
		bufferInteger.putInt(0, number);
        return bufferInteger.array();
	}
	
	/**
	 * Convert a long into a byte array
	 * @param number
	 * @return byte array
	 */
	public static byte[] longToByteArray(long number) {
		bufferLong.putLong(0, number);
        return bufferLong.array();
	}
	
	/**
	 * Converts a byte array into a hexadecimal string
	 * @param bytes - array of bytes to be converted
	 * @return hexadecimal string
	 */
	public static String bytesToHexString(byte[] bytes) {
		StringBuilder buffer = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			String s = Integer.toHexString(0xFF & b);
			if (s.length() == 1) {
				buffer.append('0');
			}
			buffer.append(s);
		}
		return buffer.toString();
    }
	
}
