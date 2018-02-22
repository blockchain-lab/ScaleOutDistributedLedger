package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.amountselector;

import java.io.Serializable;
import java.util.Arrays;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.PoissonRandom;

import lombok.Getter;
import lombok.Setter;

/**
 * Abstract class to represent an amount selector.
 */
public abstract class AmountSelector implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Getter @Setter
	protected transient PoissonRandom random;
	
	/**
	 * @param localStore - the local store
	 * @return - the amount of money that was selected, or -1 if insufficient
	 */
	public abstract long selectAmount(LocalStore localStore);
	
	/**
	 * @param str - the string
	 * @return - the AmountSelector represented by the given string
	 */
	public static AmountSelector fromString(String str) {
		String[] parts = str.split(":");
		switch (parts[0].toLowerCase()) {
			case "fixed":
				return new FixedAmountSelector(Long.parseLong(parts[1]), false);
			case "fixedpn":
				long[] amount = Arrays.stream(parts, 1, parts.length).mapToLong(Long::parseLong).toArray();
				return new FixedAmountSelectorPerNode(amount, false);
			case "uniform":
				return new UniformAmountSelector(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), false);
			case "uniformpn":
				int[] min = Arrays.stream(parts, 1, (parts.length + 1) / 2).mapToInt(Integer::parseInt).toArray();
				int[] max = Arrays.stream(parts, (parts.length + 1) / 2, parts.length).mapToInt(Integer::parseInt).toArray();
				return new UniformAmountSelectorPerNode(min, max, false);
			case "poisson":
				return new PoissonAmountSelector(Long.parseLong(parts[1]), Double.parseDouble(parts[2]), false);
			case "poissonpn":
				long[] base = Arrays.stream(parts, 1, (parts.length + 1) / 2).mapToLong(Long::parseLong).toArray();
				double[] lambda = Arrays.stream(parts, (parts.length + 1) / 2, parts.length).mapToDouble(Double::parseDouble).toArray();
				return new PoissonAmountSelectorPerNode(base, lambda, false);
			default:
				throw new IllegalArgumentException("Invalid amount selector string!");
		}
	}
}
