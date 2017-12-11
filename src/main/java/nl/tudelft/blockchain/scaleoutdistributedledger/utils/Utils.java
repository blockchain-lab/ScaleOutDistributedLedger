package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.nio.ByteBuffer;

/**
 * Class for helper functions
 */
public class Utils {
	
	/**
	 * Convert a integer into a byte array
	 * @param number
	 * @return byte array
	 */
	public static byte[] intToByteArray(int number) {
		return new byte[] {
			(byte) (number >>> 24),
			(byte) (number >>> 16),
			(byte) (number >>> 8),
			(byte) number
		};
	}
	
	/**
	 * Convert a long into a byte array
	 * @param number
	 * @return byte array
	 */
	public static byte[] longToByteArray(long number) {
		return new byte[] {
			(byte) (number >> 56),
			(byte) (number >> 48),
			(byte) (number >> 40),
			(byte) (number >> 32),
			(byte) (number >> 24),
			(byte) (number >> 16),
			(byte) (number >> 8),
			(byte) number
		};
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
