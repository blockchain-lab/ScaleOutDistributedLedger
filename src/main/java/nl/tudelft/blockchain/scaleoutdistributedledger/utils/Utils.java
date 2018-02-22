package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.SimulationMain;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;

/**
 * Class for helper functions.
 */
public final class Utils {
	public static final int NODEID_LENGTH = Settings.INSTANCE.totalNodesNumber < 255 ? 1 : 2;
	
	private static final char[] HEX = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};
	
	private Utils() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Writes the given nodeId to the given stream, either as a byte or as a short depending on
	 * {@link SimulationMain#totalNodesNumber}.
	 * @param stream - the stream to write to
	 * @param nodeId - the nodeId to write
	 * @throws IOException - If writing to the stream triggers an IOException.
	 */
	public static void writeNodeId(DataOutput stream, int nodeId) throws IOException {
		if (Settings.INSTANCE.totalNodesNumber < 255) {
			stream.writeByte(nodeId);
		} else {
			stream.writeShort(nodeId);
		}
	}

	/**
	 * Reads a node id from the given stream.
	 * @param stream - the stream to read from
	 * @return - the read node id
	 * @throws IOException - If reading from the stream causes an IOException.
	 */
	public static int readNodeId(DataInput stream) throws IOException {
		if (Settings.INSTANCE.totalNodesNumber < 255) {
			byte b = stream.readByte();
			return b == -1 ? -1 : b & 0xFF;
		} else {
			short s = stream.readShort();
			return s == -1 ? -1 : s & 0xFFFF;
		}
	}
	
	/**
	 * Reads a node id from the given array of bytes, starting at the given index.
	 * @param bytes - the byte array to read from
	 * @param index - the index to read at
	 * @return - the read node id
	 */
	public static int readNodeId(byte[] bytes, int index) {
		if (Settings.INSTANCE.totalNodesNumber < 255) {
			byte b = bytes[index];
			return b == -1 ? -1 : b & 0xFF;
		} else {
			int s = bytes[index] << 8 | (bytes[index + 1] & 0xFF);
			return s == -1 ? -1 : s;
		}
	}
	
	/**
	 * Reads a short from the given array of bytes, starting at the given index.
	 * @param bytes - the byte array to read from
	 * @param index - the index to read at
	 * @return - the read short
	 */
	public static int readShort(byte[] bytes, int index) {
		return bytes[index] << 8 | (bytes[index + 1] & 0xFF);
	}
	
	/**
	 * Reads an unsigned short from the given array of bytes, starting at the given index.
	 * @param bytes - the byte array to read from
	 * @param index - the index to read at
	 * @return - the read unsigned short
	 */
	public static int readUnsignedShort(byte[] bytes, int index) {
		return readShort(bytes, index) & 0xFFFF;
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
	 * Reads a long from the given array of bytes, starting at the given index.
	 * @param bytes - the byte array to read from
	 * @param index - the index to read at
	 * @return - the read long
	 */
	public static long readLong(byte[] bytes, int index) {
		long value = 0;
		
		for (int i = index; i < index + 8; i++) {
			value = (value << 8) | (bytes[i] & 0xff);
		}
		return value;
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
