package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.SimulationMain;

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
	 * Writes the given number as a short to the given stream.
	 * @param stream - the stream to write to
	 * @param number - the number to write
	 * @throws IOException - If writing to the stream triggers an IOException.
	 */
	public static void writeShort(OutputStream stream, int number) throws IOException {
		stream.write(number >> 8);
		stream.write(number);
	}
	
	/**
	 * Writes the given number as an int to the given stream.
	 * @param stream - the stream to write to
	 * @param number - the number to write
	 * @throws IOException - If writing to the stream triggers an IOException.
	 */
	public static void writeInt(OutputStream stream, int number) throws IOException {
		stream.write(number >> 24);
		stream.write(number >> 16);
		stream.write(number >> 8);
		stream.write(number);
	}
	
	/**
	 * Writes the given number as a long to the given stream.
	 * @param stream - the stream to write to
	 * @param number - the number to write
	 * @throws IOException - If writing to the stream triggers an IOException.
	 */
	public static void writeLong(OutputStream stream, long number) throws IOException {
		stream.write((byte) (number >> 56));
		stream.write((byte) (number >> 48));
		stream.write((byte) (number >> 40));
		stream.write((byte) (number >> 32));
		stream.write((byte) (number >> 24));
		stream.write((byte) (number >> 16));
		stream.write((byte) (number >> 8));
		stream.write((byte) number);
	}
	
	/**
	 * Writes the given nodeId to the given stream, either as a byte or as a short depending on
	 * {@link SimulationMain#TOTAL_NODES_NUMBER}.
	 * @param stream - the stream to write to
	 * @param nodeId - the nodeId to write
	 * @throws IOException - If writing to the stream triggers an IOException.
	 */
	@SuppressWarnings("unused")
	public static void writeNodeId(OutputStream stream, int nodeId) throws IOException {
		if (SimulationMain.TOTAL_NODES_NUMBER < 128) {
			stream.write(nodeId);
		} else {
			stream.write(nodeId >> 8);
			stream.write(nodeId);
		}
	}
	
	/**
	 * Reads a node id from the given array of bytes, starting at the given index.
	 * @param bytes - the byte array to read from
	 * @param index - the index to read at
	 * @return - the read node id
	 */
	@SuppressWarnings("unused")
	public static int readNodeId(byte[] bytes, int index) {
		if (SimulationMain.TOTAL_NODES_NUMBER < 128) {
			return bytes[index];
		} else {
			return bytes[index] << 8 |
				  (bytes[index + 1] & 0xFF);
		}
	}
	
	/**
	 * Reads a short from the given array of bytes, starting at the given index.
	 * @param bytes - the byte array to read from
	 * @param index - the index to read at
	 * @return - the read short
	 */
	public static int readShort(byte[] bytes, int index) {
		return bytes[index] << 8 |
			  (bytes[index + 1] & 0xFF);
	}
	
	/**
	 * Reads an int from the given array of bytes, starting at the given index.
	 * @param bytes - the byte array to read from
	 * @param index - the index to read at
	 * @return - the read int
	 */
	public static int readInt(byte[] bytes, int index) {
		return bytes[index] << 24 |
			  (bytes[index + 1] & 0xFF) << 16 |
			  (bytes[index + 2] & 0xFF) << 8 |
			  (bytes[index + 3] & 0xFF);
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
	 * Converts a byte array into a hexadecimal string starting with 0x.
	 * @param bytes - array of bytes to be converted
	 * @return hexadecimal string
	 */
	public static String bytesTo0xHexString(byte[] bytes) {
		StringBuilder buffer = new StringBuilder(bytes.length * 2 + 2);
		buffer.append("0x");
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

	/**
	 * Convert a byte array to a base 64 string.
	 * @param bytes	- The bytes to convert
	 * @return 		- The resulting base 64 string, or an empty one if the conversion fails
	 */
	public static String bytesToBas64String(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}
}
