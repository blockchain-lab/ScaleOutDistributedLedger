/**
 * Class to store Node information.
 */
class Node {

	/**
	 * Constructor.
	 * @param address - the address of this node.
	 * @param port - the port of this node.
	 */
	constructor(address, port, publicKey) {
		this.address = address;
		this.port = port;
		this.publicKey = publicKey;
		this.lastSeen = new Date();
		this.running = false;
	}
}

module.exports = Node;
