package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import static org.mockito.Mockito.*;

import org.junit.Test;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;

/**
 * Test class for {@link TransactionPatternMessage}.
 */
public class TransactionPatternMessageTest extends MessageTest {

	/**
	 * Test method for {@link TransactionPatternMessage#handle(LocalStore)}.
	 */
	@Test
	public void testHandle() {
		ITransactionPattern pattern = mock(ITransactionPattern.class);
		TransactionPatternMessage msg = new TransactionPatternMessage(pattern);
		msg.handle(storeSpy);
		
		//The correct transaction pattern should be set.
		verify(appMock, times(1)).setTransactionPattern(same(pattern));
	}

}
