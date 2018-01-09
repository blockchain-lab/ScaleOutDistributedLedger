package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Message abstract super class.
 */
public abstract class Message {

    public abstract void handle(LocalStore localStore);
}
