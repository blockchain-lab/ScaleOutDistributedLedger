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
		int strLen = hex.length();
		if (strLen % 2 != 0) throw new IllegalArgumentException("Length must be even!");

		byte[] bytes = new byte[strLen / 2];
		for (int i = 0; i < strLen; i += 2) {
			int high = hexToBin(hex.charAt(i));
			int low  = hexToBin(hex.charAt(i + 1));
			bytes[i / 2] = (byte) ((high << 4) + low);
		}

		return bytes;
	}

	/**
	 * @param ch - the hex character to convert
	 * @return     the binary value of the given hex character
	 */
	private static int hexToBin(char ch) {
		if ('0' <= ch && ch <= '9') {
			return ch - '0';
		}
		if ('A' <= ch && ch <= 'F') {
			return ch - 'A' + 10;
		}
		if ('a' <= ch && ch <= 'f') {
			return ch - 'a' + 10;
		}
		return -1;
	}
}
