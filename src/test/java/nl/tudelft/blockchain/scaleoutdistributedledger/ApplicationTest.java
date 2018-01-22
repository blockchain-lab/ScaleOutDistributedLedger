package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.mocks.TendermintChainMock;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.*;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.CancellableInfiniteRunnable;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ApplicationTest {
	private Application instance;
	private Thread serverMock;
	private TransactionSender transactionSenderMock;
	private LocalStore localStoreMock;

	@Before
	public void setUp() {
		localStoreMock = mock(LocalStore.class);
		serverMock = mock(Thread.class);
		transactionSenderMock = mock(TransactionSender.class);

		this.instance = new Application(localStoreMock, serverMock, transactionSenderMock);
		when(localStoreMock.getMainChain()).thenReturn(spy(new TendermintChainMock()));
		when(localStoreMock.getOwnNode()).thenReturn(new OwnNode(0));
	}

	@Test
	public void testKill() {
		instance.kill();

		verify(transactionSenderMock, times(1)).shutdownNow();
		verify(instance.getMainChain(), times(1)).stop();
	}

	@Test
	public void testStartTransacting() {
		CancellableInfiniteRunnable<LocalStore> runnableMock = setTransactionPattern();

		instance.startTransacting();
		verify(runnableMock, times(1)).run();
	}

	@Test
	public void testGetMainChain() {
		MainChain chain = mock(MainChain.class);
		when(localStoreMock.getMainChain()).thenReturn(chain);
		assertEquals(chain, instance.getMainChain());
	}

	@Test
	public void testFinishTransactionSending() throws Exception {
		instance.finishTransactionSending();

		verify(transactionSenderMock, times(1)).stop();
		verify(transactionSenderMock, times(1)).waitUntilDone();
	}

	@SuppressWarnings("unchecked")
	public CancellableInfiniteRunnable<LocalStore> setTransactionPattern() {
		ITransactionPattern transactionPatternMock = mock(ITransactionPattern.class);
		CancellableInfiniteRunnable<LocalStore> runnableMock = mock(CancellableInfiniteRunnable.class);
		when(transactionPatternMock.getRunnable(any(LocalStore.class))).thenReturn(runnableMock);
		instance.setTransactionPattern(transactionPatternMock);
		return runnableMock;
	}
}