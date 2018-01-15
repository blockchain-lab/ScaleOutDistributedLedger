package nl.tudelft.blockchain.scaleoutdistributedledger.mocks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

/**
 * Mock for TendermintChain.
 */
public class TendermintChainMock implements MainChain {
	
	@Override
	public Sha256Hash commitAbstract(BlockAbstract abs) {
		// Generate a deterministic hash, just in case
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(Utils.intToByteArray(abs.getBlockNumber()));
			outputStream.write(Utils.intToByteArray(abs.getOwnerNodeId()));
		} catch (IOException ex) {
			Logger.getLogger(TendermintChainMock.class.getName()).log(Level.SEVERE, null, ex);
			Log.log(Level.SEVERE, "Unexpected error while making hash of abstract");
		}
		byte[] hash = outputStream.toByteArray();
		
		// Update abstract
		abs.setAbstractHash(Sha256Hash.withHash(hash));
		abs.setOnMainChain(Optional.of(true));
		return Sha256Hash.withHash(hash);
	}

	@Override
	public boolean isPresent(BlockAbstract abs) {
		return true;
	}
	
	@Override
	public void stop() {}
	
}
