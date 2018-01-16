package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.Simulation;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.RandomTransactionPattern;

import javax.sound.midi.Track;

/**
 * Main class for running a simulation.
 */
public final class SimulationMain {
	private SimulationMain() {}
	
	/**
	 * @param args - the program arguments
	 * @throws Exception - If an exception occurs.
	 */
	public static void main(String[] args) throws Exception {
		//NOTE: The tracker should be started first, manually

		// Clean Tendermint folder
		TendermintHelper.cleanTendermintFiles();
		// reset the tracker server
		TrackerHelper.resetTrackerServer();

		Simulation simulation = new Simulation();
		ITransactionPattern itp = new RandomTransactionPattern(10, 20, 1000, 2000, 1);
		simulation.setTransactionPattern(itp);
		
		simulation.runNodesLocally(5);
		
		Thread.sleep(5000);
		simulation.initialize();
		
		Thread.sleep(5000);
		simulation.start();
		
		Thread.sleep(60 * 1000);
		simulation.stop();

		Thread.sleep(10 * 1000);
		
		simulation.stopLocalNodes();
		simulation.cleanup();
	}
}
