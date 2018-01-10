package nl.tudelft.blockchain.scaleoutdistributedledger.simulation;

import java.util.Random;

/**
 * Extension of Random which adds a method to get a poisson number.
 */
public class PoissonRandom extends Random {
	private static final long serialVersionUID = -8301097576667444995L;
	
	private static final double STEP = 500;
	
	/**
	 * Creates a new PoissonRandom.
	 */
	public PoissonRandom() {}
	
	/**
	 * Creates a new PoissonRandom with the given seed.
	 * @param seed - the seed
	 */
	public PoissonRandom(long seed) {
		super(seed);
	}
	
	/**
	 * @param lambda - the poisson parameter lambda
	 * @return a number generated according to a poisson distribution
	 */
	public int nextPoisson(double lambda) {
		//Implementation of the second algorithm on https://en.wikipedia.org/wiki/Poisson_distribution#Generating_Poisson-distributed_random_variables
		double lambdaLeft = lambda;
		int k = 0;
		double p = 1;
		
		do {
			k++;
			p *= nextDouble();
			if (p < Math.E && lambdaLeft > 0) {
				if (lambdaLeft > STEP) {
					p *= Math.exp(STEP);
					lambdaLeft -= STEP;
				} else {
					p *= Math.exp(lambdaLeft);
					lambdaLeft = -1;
				}
			}
		} while (p > 1);
		return k - 1;
	}
}
