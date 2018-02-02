package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

/**
 * Class to test {@link Sha256Hash}.
 */
public class Sha256HashTest {
	
	/**
	 * Test for {@link Sha256Hash#Sha256Hash(java.lang.String) }.
	 */
	@Test
	public void testConstuctorMessage() {
		Sha256Hash hash = new Sha256Hash("hello");
		String expectedStr = "2CF24DBA5FB0A30E26E83B2AC5B9E29E1B161E5C1FA7425E73043362938B9824";
		
		assertEquals(expectedStr, hash.toString().toUpperCase());
	}
	
	/**
	 * Test for {@link Sha256Hash#Sha256Hash(byte[]) }.
	 */
	@Test
	public void testConstructorBytes() {
		Sha256Hash hash = new Sha256Hash(Utils.hexStringToBytes("ABBA"));
		
		assertNotEquals("ABBA", hash.toString());
	}
	
	/**
	 * Test for {@link Sha256Hash#withHash(byte[])}.
	 */
	@Test
	public void testWith() {
		Sha256Hash hash = Sha256Hash.withHash(Utils.hexStringToBytes("ABBA"));
		
		assertNotEquals("ABBA", hash.toString());
	}
	
	/**
	 * Test for {@link Sha256Hash#equals(java.lang.Object)}.
	 */
	@Test
	public void testEquals() {
		Sha256Hash hash = new Sha256Hash("hello");
		Sha256Hash otherHash = new Sha256Hash("hello");
		
		assertEquals(hash, otherHash);
	}
	
	/**
	 * Test for {@link Sha256Hash#equals(java.lang.Object)}.
	 */
	@Test
	public void testHashCode() {
		Sha256Hash hash = new Sha256Hash("hello");
		Sha256Hash otherHash = new Sha256Hash("hello2");
		
		assertNotEquals(hash.hashCode(), otherHash.hashCode());
	}
	
}
