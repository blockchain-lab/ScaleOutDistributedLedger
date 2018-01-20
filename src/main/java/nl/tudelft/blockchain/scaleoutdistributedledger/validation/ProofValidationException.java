package nl.tudelft.blockchain.scaleoutdistributedledger.validation;

/**
 * Validation exception for proofs.
 */
public class ProofValidationException extends ValidationException {
	private static final long serialVersionUID = 1L;

	/**
	 * @param reason - the reason
	 */
	public ProofValidationException(String reason) {
		super(reason);
	}
	
	/**
	 * @param reason - the reason
	 * @param cause  - the validation exception that caused this one
	 */
	public ProofValidationException(String reason, ValidationException cause) {
		super(reason, cause);
	}
}
