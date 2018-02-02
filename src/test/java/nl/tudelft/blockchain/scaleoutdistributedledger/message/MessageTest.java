package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import static org.mockito.Mockito.*;

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
		this.appMock = mock(Application.class);
		this.storeSpy = spy(new LocalStore(ownNode, this.appMock, null, false));
		doNothing().when(this.storeSpy).updateNodes();
	}
	
}
