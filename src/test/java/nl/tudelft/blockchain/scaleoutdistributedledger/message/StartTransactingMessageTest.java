package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import static org.mockito.Mockito.*;

import org.junit.Test;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Test class for {@link StartTransactingMessage}.
 */
public class StartTransactingMessageTest extends MessageTest {

	/**
	 * Test method for {@link StartTransactingMessage#handle(LocalStore)}.
	 */
	@Test
	public void testHandle() {
		StartTransactingMessage msg = new StartTransactingMessage();
		msg.handle(storeSpy);
		
		//We should have started transacting.
		verify(appMock, times(1)).startTransacting();
	}

}
