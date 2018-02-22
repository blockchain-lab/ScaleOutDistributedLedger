package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.amountselector;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Amount selector that selects a fixed amount of money.
 */
public class FixedAmountSelector extends AmountSelector {
	private static final long serialVersionUID = 1L;
	
	protected long amount;
	protected boolean allowLess;
	
	/**
	 * @param amount - the amount
	 * @param allowLess - if true then a smaller amount is allowed when there are insufficient funds
	 */
	public FixedAmountSelector(long amount, boolean allowLess) {
		this.amount = amount;
		this.allowLess = allowLess;
	}
	
	@Override
	public long selectAmount(LocalStore localStore) {
		long available = localStore.getAvailableMoney();
		if (available == 0) return -1;
		
		if (amount > available) return allowLess ? available : -1;
		return amount;
	}
}
