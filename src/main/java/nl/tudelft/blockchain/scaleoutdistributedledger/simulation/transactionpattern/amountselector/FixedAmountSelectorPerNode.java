package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.amountselector;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Amount selector that selects a fixed amount of money, that is different per node.
 */
public class FixedAmountSelectorPerNode extends AmountSelector {
	private static final long serialVersionUID = 1L;
	
	protected long[] amount;
	protected boolean allowLess;
	
	/**
	 * @param amount - the amounts
	 * @param allowLess - if true then a smaller amount is allowed when there are insufficient funds
	 */
	public FixedAmountSelectorPerNode(long[] amount, boolean allowLess) {
		this.amount = amount;
		this.allowLess = allowLess;
	}
	
	@Override
	public long selectAmount(LocalStore localStore) {
		long available = localStore.getAvailableMoney();
		if (available == 0) return -1;
		
		long myAmount = amount[localStore.getOwnNode().getId()];
		if (myAmount > available) return allowLess ? available : -1;
		return myAmount;
	}

}
