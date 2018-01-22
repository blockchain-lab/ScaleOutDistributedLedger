package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import static org.mockito.Mockito.*;

import org.junit.Test;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Test class for {@link StopTransactingMessage}.
 */
public class StopTransactingMessageTest extends MessageTest {

	/**
	 * Test method for {@link StopTransactingMessage#handle(LocalStore)}.
	 */
	@Test
	public void testHandle() {
		StopTransactingMessage msg = new StopTransactingMessage();
		msg.handle(storeSpy);
		
		//We should have stopped transacting.
		verify(appMock, times(1)).stopTransacting();
	}

}
