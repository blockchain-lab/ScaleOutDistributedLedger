package nl.tudelft.blockchain.scaleoutdistributedledger.simulation;

/**
 * Enum to represent the simulation mode.
 */
public enum SimulationMode {
	/**
	 * Represents a simulation where all nodes are directed from a central simulator.
	 */
	DIRECTED,
	
	/**
	 * Represents a simulation where all nodes make decisions by themselves.
	 */
	DISTRIBUTED
}
