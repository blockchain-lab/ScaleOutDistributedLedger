package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.junit.Before;

import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;

/**
 * Base class for testing messages.
 */
public abstract class MessageTest {
	protected LocalStore storeSpy;
	protected Application appMock;
	
	/**
	 * Creates the store and the application.
	 */
	@Before
	public void setUp() {
		OwnNode ownNode = new OwnNode(0);
		appMock = mock(Application.class);
		storeSpy = spy(new LocalStore(ownNode, appMock, null, false));
	}
}
