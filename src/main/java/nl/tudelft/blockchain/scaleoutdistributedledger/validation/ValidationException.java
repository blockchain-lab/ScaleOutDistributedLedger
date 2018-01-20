package nl.tudelft.blockchain.scaleoutdistributedledger.validation;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

/**
 * Exception for representing validation going wrong.
 */
public class ValidationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * @param reason - the reason
	 */
	public ValidationException(String reason) {
		super(reason);
	}
	
	/**
	 * @param reason - the reason
	 * @param cause  - the validation exception that caused this one
	 */
	public ValidationException(String reason, ValidationException cause) {
		super(reason, cause);
	}
	
	/**
	 * Creates a validation exception with the following message.
	 * <pre>Transaction [transaction] is invalid: [reason]</pre>
	 * @param transaction - the transaction
	 * @param reason      - the reason
	 */
	public ValidationException(Transaction transaction, String reason) {
		super("Transaction " + transaction + " is invalid: " + reason);
	}
	
	/**
	 * Creates a validation exception with the following message.
	 * <pre>Transaction [transaction] is invalid: [reason]</pre>
	 * @param transaction - the transaction
	 * @param reason      - the reason
	 * @param cause       - the validation exception that caused this one
	 */
	public ValidationException(Transaction transaction, String reason, ValidationException cause) {
		super("Transaction " + transaction + " is invalid: " + reason, cause);
	}
	
	@Override
	public String getMessage() {
		if (this.getCause() != null) {
			return super.getMessage() + " caused by " + this.getCause().getMessage();
		} else {
			return super.getMessage();
		}
	}
}
