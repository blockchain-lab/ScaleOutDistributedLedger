package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

/**
 * Class for helper functions.
 */
public final class Utils {
	private static final char[] HEX = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};
	
	private Utils() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Convert a integer into a byte array.
	 * @param number - the number to convert
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
	 * Convert a long into a byte array.
	 * @param number - the number to convert
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
	 * Converts a byte array into a hexadecimal string.
	 * @param bytes - array of bytes to be converted
	 * @return hexadecimal string
	 */
	public static String bytesToHexString(byte[] bytes) {
		StringBuilder buffer = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			buffer.append(HEX[(b >> 4) & 0xF]);
			buffer.append(HEX[b & 0xF]);
		}
		return buffer.toString();
	}
	
	/**
	 * Converts a hexadecimal String to a byte array.
	 * @param hex - the hexadecimal string
	 * @return      the byte array
	 */
	public static byte[] hexStringToBytes(String hex) {
		return javax.xml.bind.DatatypeConverter.parseHexBinary(hex);
	}
}
