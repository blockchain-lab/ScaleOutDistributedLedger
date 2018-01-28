package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.mocks.TendermintChainMock;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.CancellableInfiniteRunnable;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Class to test {@link Application}.
 */
public class ApplicationTest {
	
	private Application instance;
	private Thread serverMock;
	private TransactionSender transactionSenderMock;
	private LocalStore localStoreMock;

	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		this.localStoreMock = mock(LocalStore.class);
		this.serverMock = mock(Thread.class);
		this.transactionSenderMock = mock(TransactionSender.class);

		this.instance = new Application(this.localStoreMock, this.serverMock, this.transactionSenderMock);
		when(this.localStoreMock.getMainChain()).thenReturn(spy(new TendermintChainMock()));
		when(this.localStoreMock.getOwnNode()).thenReturn(new OwnNode(0));
	}

	/**
	 * Test for {@link Application#kill()}.
	 */
	@Test
	public void testKill() {
		this.instance.kill();

		verify(this.transactionSenderMock, times(1)).shutdownNow();
		verify(this.instance.getMainChain(), times(1)).stop();
	}

	/**
	 * Test for {@link Application#startTransacting()}.
	 * @throws java.lang.InterruptedException - interrupted while sleeping
	 */
	@Test
	public void testStartTransacting() throws InterruptedException {
		CancellableInfiniteRunnable<LocalStore> runnableMock = this.setTransactionPattern();

		this.instance.startTransacting();
		// Wait for thread to start
		verify(runnableMock, timeout(2000).times(1)).run();
	}

	/**
	 * Test for {@link Application#getMainChain()}.
	 */
	@Test
	public void testGetMainChain() {
		MainChain chain = mock(MainChain.class);
		when(this.localStoreMock.getMainChain()).thenReturn(chain);
		assertEquals(chain, this.instance.getMainChain());
	}

	/**
	 * Test for {@link Application#finishTransactionSending()}.
	 * @throws InterruptedException - interrupted while sleeping
	 */
	@Test
	public void testFinishTransactionSending() throws InterruptedException {
		this.instance.finishTransactionSending();

		verify(this.transactionSenderMock, times(1)).stop();
		verify(this.transactionSenderMock, times(1)).waitUntilDone();
	}

	/**
	 * Set a transaction pattern.
	 * @return cancellable infinite runnable
	 */
	private CancellableInfiniteRunnable<LocalStore> setTransactionPattern() {
		ITransactionPattern transactionPatternMock = mock(ITransactionPattern.class);
		CancellableInfiniteRunnable<LocalStore> runnableMock = mock(CancellableInfiniteRunnable.class);
		when(transactionPatternMock.getRunnable(any(LocalStore.class))).thenReturn(runnableMock);
		this.instance.setTransactionPattern(transactionPatternMock);
		return runnableMock;
	}
	
}
