package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;

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
		throw new IllegalArgumentException("'" + ch + "' is not a hexadecimal character!");
	}

	/**
	 * Convert a base 64 string to a byte array.
	 * @param data	- The base 64 string to convert
	 * @return 		- The resulting byte array, or an empty one if the conversion fails
	 */
	public static byte[] base64StringToBytes(String data) {
		try {
			return Base64.getDecoder().decode(data.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Log.log(Level.SEVERE, "This encoding should always be present", e);
			return new byte[0];
		}
	}
}
