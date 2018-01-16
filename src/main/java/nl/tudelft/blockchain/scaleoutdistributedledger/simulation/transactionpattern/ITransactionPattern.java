package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern;

import java.io.Serializable;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.CancellableInfiniteRunnable;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.SimulationMode;

/**
 * Interface for a transaction pattern.
 */
public interface ITransactionPattern extends Serializable {
	/**
	 * @return the name of this transaction pattern
	 */
	public String getName();
	
	/**
	 * @return the simulation mode of this transaction pattern
	 */
	public SimulationMode getSimulationMode();

	/**
	 * Perform one or more actions.
	 * @param localStore - the local store
	 * @throws InterruptedException - if the action is interrupted
	 */
	public void doAction(LocalStore localStore) throws InterruptedException;

	/**
	 * @param localStore - the local store
	 * @return             the time until the next action
	 */
	public long timeUntilNextAction(LocalStore localStore);

	/**
	 * Creates a CancellableInfiniteRunnable for executing this transaction pattern.
	 * @param localStore - the local store
	 * @return           - the runnable
	 */
	public default CancellableInfiniteRunnable getRunnable(LocalStore localStore) {
		return new CancellableInfiniteRunnable(() -> {
			doAction(localStore);
			Thread.sleep(timeUntilNextAction(localStore));
		});
	}
}
