package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.amountselector;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Amount selector that selects a uniformly distributed amount within a range, where each node
 * can have a different range.
 */
public class UniformAmountSelectorPerNode extends AmountSelector {
	private static final long serialVersionUID = 1L;
	
	protected int[] minAmount;
	protected int[] maxAmount;
	protected boolean allowLess;
	
	/**
	 * @param minAmount - the minimal amounts (inclusive)
	 * @param maxAmount - the maximum amounts (inclusive)
	 * @param allowLess - if true then a smaller amount is allowed when there are insufficient funds
	 */
	public UniformAmountSelectorPerNode(int[] minAmount, int[] maxAmount, boolean allowLess) {
		this.minAmount = minAmount;
		this.maxAmount = maxAmount;
		this.allowLess = allowLess;
	}
	
	@Override
	public long selectAmount(LocalStore localStore) {
		long available = localStore.getAvailableMoney();
		if (available == 0) return -1;
		
		int ownNodeId = localStore.getOwnNode().getId();
		long amount = (long) minAmount[ownNodeId] + getRandom().nextInt(maxAmount[ownNodeId] - minAmount[ownNodeId] + 1);
		if (amount > available) return allowLess ? available : -1;
		return amount;
	}

}
