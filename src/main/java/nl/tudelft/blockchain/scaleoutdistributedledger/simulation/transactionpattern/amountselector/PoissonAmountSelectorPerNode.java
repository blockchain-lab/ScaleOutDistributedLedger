package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.amountselector;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Amount selector that selects an amount from a Poisson distribution, where each node can have
 * different parameters.
 */
public class PoissonAmountSelectorPerNode extends AmountSelector {
	private static final long serialVersionUID = 1L;
	
	protected long[] baseAmount;
	protected double[] lambda;
	protected boolean allowLess;
	
	/**
	 * @param baseAmount - the base amount (baseAmount + poisson(lambda))
	 * @param lambda     - the lambda parameter
	 * @param allowLess  - if true then a smaller amount is allowed when there are insufficient funds
	 */
	public PoissonAmountSelectorPerNode(long[] baseAmount, double[] lambda, boolean allowLess) {
		this.baseAmount = baseAmount;
		this.lambda = lambda;
		this.allowLess = allowLess;
	}
	
	@Override
	public long selectAmount(LocalStore localStore) {
		long available = localStore.getAvailableMoney();
		if (available == 0) return -1;
		
		int ownNode = localStore.getOwnNode().getId();
		
		long amount;
		do {
			amount = baseAmount[ownNode] + getRandom().nextPoisson(lambda[ownNode]);
		} while (amount == 0);
		
		if (amount > available) return allowLess ? available : -1;
		return amount;
	}

}
