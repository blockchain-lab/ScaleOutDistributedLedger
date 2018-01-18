package nl.tudelft.blockchain.scaleoutdistributedledger.validation;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

/**
 * Exception for representing validation going wrong.
 */
public class ValidationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ValidationException(String reason, ValidationException cause) {
		super(reason, cause);
	}
	
	public ValidationException(String reason) {
		super(reason);
	}
	
	public ValidationException(Transaction transaction, String reason) {
		super("Transaction " + transaction + " is invalid: " + reason);
	}
	
	public ValidationException(Transaction transaction, String reason, ValidationException cause) {
		super("Transaction " + transaction + " is invalid: " + reason, cause);
	}
	
	
	public ValidationException(Proof proof, String reason) {
		super(reason + " [PROOF] " + proof);
	}
	
//	@Override
//	public String getMessage() {
//		if (this.getCause() != null) {
//			return super.getMessage() + " caused by " + this.getCause();
//		} else {
//			return super.getMessage();
//		}
//	}
}
