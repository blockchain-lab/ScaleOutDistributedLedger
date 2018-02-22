package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.amountselector;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Amount selector that selects a uniformly distributed amount within a range.
 */
public class UniformAmountSelector extends AmountSelector {
	private static final long serialVersionUID = 1L;
	
	protected int minAmount;
	protected int maxAmount;
	protected boolean allowLess;
	
	/**
	 * @param minAmount - the minimal amount (inclusive)
	 * @param maxAmount - the maximum amount (inclusive)
	 * @param allowLess - if true then a smaller amount is allowed when there are insufficient funds
	 */
	public UniformAmountSelector(int minAmount, int maxAmount, boolean allowLess) {
		this.minAmount = minAmount;
		this.maxAmount = maxAmount;
		this.allowLess = allowLess;
	}
	
	@Override
	public long selectAmount(LocalStore localStore) {
		long available = localStore.getAvailableMoney();
		if (available == 0) return -1;
		
		long amount = (long) minAmount + getRandom().nextInt(maxAmount - minAmount + 1);
		if (amount > available) return allowLess ? available : -1;
		return amount;
	}

}
