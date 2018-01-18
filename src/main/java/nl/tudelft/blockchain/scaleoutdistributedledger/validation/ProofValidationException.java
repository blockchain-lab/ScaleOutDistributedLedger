package nl.tudelft.blockchain.scaleoutdistributedledger.validation;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;

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
	 * @param cause  - the exception that caused this exception
	 */
	public ProofValidationException(String reason, ValidationException cause) {
		super(reason, cause);
	}
	
	/**
	 * @param proof  - the proof
	 * @param reason - the reason
	 */
	public ProofValidationException(Proof proof, String reason) {
		super(proof, reason);
	}
}
