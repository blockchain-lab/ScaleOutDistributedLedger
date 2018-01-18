//package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;
//
//import com.github.jtendermint.jabci.socket.TSocket;
//import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.util.concurrent.ExecutorService;
//
//import static org.junit.Assert.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//public class TendermintChainTest {
//
//	private TSocket tsockMock;
//	private Application appMock;
//	private TendermintChain instance;
//	private ExecutorService threadpoolMock;
//	private ABCIClient clientMock;
//
//	@Before
//	public void setUp() {
//		tsockMock = mock(TSocket.class);
//		appMock = mock(Application.class);
//		threadpoolMock = mock(ExecutorService.class);
//		clientMock = mock(ABCIClient.class);
//		instance = new TendermintChain(1234, tsockMock, appMock, threadpoolMock, clientMock);
//	}
//
//	@Test
//	public void testUpdateCache() {
//		instance.updateCache(12);
//
//		verify(threadpoolMock, times(1)).submit(any(Runnable.class));
//	}
//
//	@Test
//	public void testUpdateCacheNoClient() {
//		instance.setClient(null);
//		instance.updateCache(12);
//
//		verify(threadpoolMock, times(0)).submit(any(Runnable.class));
//	}
//
//}