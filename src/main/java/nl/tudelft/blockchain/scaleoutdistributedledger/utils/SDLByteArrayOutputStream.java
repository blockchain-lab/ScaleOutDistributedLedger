package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.io.ByteArrayOutputStream;

import nl.tudelft.blockchain.scaleoutdistributedledger.SimulationMain;

/**
 * Extension of a ByteArrayOutputStream with some convenience methods.
 */
public class SDLByteArrayOutputStream extends ByteArrayOutputStream {
	/**
	 * Creates a new SDLByteArrayOutputStream with an initial size of 32 bytes.
	 */
	public SDLByteArrayOutputStream() {
		super();
	}
	
	/**
	 * @param size - the initial buffer size
	 */
	public SDLByteArrayOutputStream(int size) {
		super(size);
	}
	
	/**
	 * More efficient version of {@link #toByteArray()}, returning the underlying byte array
	 * instead of copying it, if the size is correct.
	 * @return - a byte array representing the contents of this output stream
	 */
	public synchronized byte[] getByteArray() {
		if (count == buf.length) return buf;
		
		return toByteArray();
	}
    
    /**
	 * Writes the given number as a short to this stream.
	 * @param number - the number to write
	 */
	public synchronized void writeShort(int number) {
		write(number >> 8);
		write(number);
	}
	
	/**
	 * Writes the given number as an int to this stream.
	 * @param number - the number to write
	 */
    public synchronized void writeInt(int number) {
    	write(number >> 24);
		write(number >> 16);
		write(number >> 8);
		write(number);
    }
	
	/**
	 * Writes the given number as a long to this stream.
	 * @param number - the number to write
	 */
	public synchronized void writeLong(long number) {
		write((byte) (number >> 56));
		write((byte) (number >> 48));
		write((byte) (number >> 40));
		write((byte) (number >> 32));
		write((byte) (number >> 24));
		write((byte) (number >> 16));
		write((byte) (number >> 8));
		write((byte) number);
	}
	
	/**
	 * Writes the given nodeId to this stream, either as a byte or as a short depending on
	 * {@link SimulationMain#TOTAL_NODES_NUMBER}.
	 * @param nodeId - the nodeId to write
	 */
	@SuppressWarnings("unused")
	public void writeNodeId(int nodeId) {
		if (SimulationMain.TOTAL_NODES_NUMBER < 255) {
			write(nodeId);
		} else {
			writeShort(nodeId);
		}
	}
}
