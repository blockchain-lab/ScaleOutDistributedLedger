package nl.tudelft.blockchain.scaleoutdistributedledger.simulation;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.logging.Level;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.Message;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.StartTransactingMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.StopTransactingMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionPatternMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Test class for {@link Simulation}.
 */
public class SimulationTest {
	private Simulation simulationSpy;
	private SocketClient socketClientMock;
	
	/**
	 * Disables all logging before the tests.
	 */
	@BeforeClass
	public static void setUpClass() {
		Log.setLogLevel(Level.OFF);
	}
	
	/**
	 * Restores logging after the tests.
	 */
	@AfterClass
	public static void tearDownClass() {
		Log.setLogLevel(Log.LEVEL);
	}
	
	/**
	 * Creates a new simulation for every test.
	 * @throws Exception - Will not occur, needs to be in throws because of mockito.
	 */
	@Before
	public void setUp() throws Exception {
		socketClientMock = mock(SocketClient.class);
		
		simulationSpy = spy(new Simulation(socketClientMock));
		simulationSpy.getNodes().put(0, new Node(0));
		simulationSpy.getNodes().put(1, new Node(1));
	}

//	/**
//	 * Test method for {@link Simulation#runNodesLocally(List, Map, Map, Block, Map)}.
//	 */
//	@Test
//	public void testRunNodesLocally() {
//		//Tendermint makes this really difficult to test
//	}

	/**
	 * Test method for {@link Simulation#stopLocalNodes()}.
	 */
	@Test
	public void testStopLocalNodes() {
		//Set up the mocks
		Application appMock = mock(Application.class);
		LocalStore localStore = new LocalStore(new OwnNode(0), appMock, null, false);
		when(appMock.getLocalStore()).thenReturn(localStore);
		simulationSpy.setLocalApplications(appMock);
		
		simulationSpy.setState(SimulationState.STOPPED);
		simulationSpy.stopLocalNodes();
		
		verify(appMock, times(1)).kill();
	}
	
	/**
	 * Test method for {@link Simulation#stopLocalNodes()}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testStopLocalNodes_WrongState() {
		simulationSpy.setState(SimulationState.RUNNING);
		simulationSpy.stopLocalNodes();
	}

	/**
	 * Test method for {@link Simulation#initialize()}.
	 */
	@Test
	public void testInitialize() {
		ITransactionPattern pattern = mock(ITransactionPattern.class);
		simulationSpy.setTransactionPattern(pattern);
		
		simulationSpy.initialize();
		
		verify(simulationSpy, times(1)).broadcastMessage(any(TransactionPatternMessage.class));
		assertEquals(SimulationState.INITIALIZED, simulationSpy.getState());
	}
	
	/**
	 * Test method for {@link Simulation#initialize()}.
	 */
	@Test(expected = NullPointerException.class)
	public void testInitialize_NoPattern() {
		simulationSpy.initialize();
	}

	/**
	 * Test method for {@link Simulation#start()}.
	 */
	@Test
	public void testStart() {
		simulationSpy.setState(SimulationState.INITIALIZED);
		simulationSpy.start();
		
		verify(simulationSpy, times(1)).broadcastMessage(any(StartTransactingMessage.class));
		assertEquals(SimulationState.RUNNING, simulationSpy.getState());
	}
	
	/**
	 * Test method for {@link Simulation#start()}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testStart_WrongState() {
		simulationSpy.setState(SimulationState.STOPPED);
		simulationSpy.start();
	}

	/**
	 * Test method for {@link Simulation#stop()}.
	 */
	@Test
	public void testStop() {
		simulationSpy.setState(SimulationState.RUNNING);
		simulationSpy.stop();
		
		assertEquals(SimulationState.STOPPED, simulationSpy.getState());
	}
	
	/**
	 * Test method for {@link Simulation#stop()}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testStop_WrongState() {
		simulationSpy.setState(SimulationState.INITIALIZED);
		simulationSpy.stop();
	}

	/**
	 * Test method for {@link Simulation#cleanup()}.
	 */
	@Test
	public void testCleanup() {
		simulationSpy.setState(SimulationState.INITIALIZED);
		simulationSpy.cleanup();
		assertEquals(SimulationState.STOPPED, simulationSpy.getState());
	}
	
	/**
	 * Test method for {@link Simulation#cleanup()}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testCleanup_WrongState() {
		simulationSpy.setState(SimulationState.RUNNING);
		simulationSpy.cleanup();
	}

	/**
	 * Test method for {@link Simulation#checkState(SimulationState, String)}.
	 */
	@Test
	public void testCheckState() {
		simulationSpy.setState(SimulationState.STOPPED);
		simulationSpy.checkState(SimulationState.STOPPED, "");
	}
	
	/**
	 * Test method for {@link Simulation#checkState(SimulationState, String)}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testCheckState_WrongState1() {
		simulationSpy.setState(SimulationState.RUNNING);
		simulationSpy.checkState(SimulationState.STOPPED, "");
	}
	
	/**
	 * Test method for {@link Simulation#checkState(SimulationState, String)}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testCheckState_WrongState2() {
		simulationSpy.setState(SimulationState.INITIALIZED);
		simulationSpy.checkState(SimulationState.STOPPED, "");
	}

	/**
	 * Test method for {@link Simulation#broadcastMessage(Message)}.
	 * @throws Exception - will not occur.
	 */
	@Test
	public void testBroadcastMessage() throws Exception {
		Message message = mock(Message.class);
		simulationSpy.broadcastMessage(message);
		
		//A message should be sent to all nodes
		verify(socketClientMock, times(2)).sendMessage(any(Node.class), same(message));
	}
}
