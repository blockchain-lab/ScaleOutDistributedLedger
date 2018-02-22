package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.amountselector;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Amount selector that selects an amount from a Poisson distribution.
 */
public class PoissonAmountSelector extends AmountSelector {
	private static final long serialVersionUID = 1L;
	
	protected long baseAmount;
	protected double lambda;
	protected boolean allowLess;
	
	/**
	 * @param baseAmount - the base amount (baseAmount + poisson(lambda))
	 * @param lambda     - the lambda parameter
	 * @param allowLess  - if true then a smaller amount is allowed when there are insufficient funds
	 */
	public PoissonAmountSelector(long baseAmount, double lambda, boolean allowLess) {
		this.baseAmount = baseAmount;
		this.lambda = lambda;
		this.allowLess = allowLess;
	}
	
	@Override
	public long selectAmount(LocalStore localStore) {
		long available = localStore.getAvailableMoney();
		if (available == 0) return -1;
		
		long amount;
		do {
			amount = baseAmount + getRandom().nextPoisson(lambda);
		} while (amount == 0);
		
		if (amount > available) return allowLess ? available : -1;
		return amount;
	}

}
