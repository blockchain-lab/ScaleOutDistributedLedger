package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern;

/**
 * Random transaction pattern.
 */
public abstract class RandomTransactionPattern implements ITransactionPattern {
	private static final long serialVersionUID = 6328897872068347550L;
	
	protected final int commitEvery;
	protected Long seed;

	/**
	 * @param commitEvery - commit to the main chain after this amount of blocks
	 */
	public RandomTransactionPattern(int commitEvery) {
		this.commitEvery = commitEvery;
	}
	
	@Override
	public int getCommitEvery() {
		return commitEvery;
	}

	/**
	 * Sets the seed that is used for random number generation.
	 * @param seed - the seed
	 */
	public abstract void setSeed(long seed);
}
