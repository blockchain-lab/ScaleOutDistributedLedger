/**
 * Class to store Node information.
 */
class Node {

	/**
	 * Constructor.
	 * @param address - the address of this node.
	 * @param port - the port of this node.
	 */
	constructor(address, port) {
		this.address = address;
		this.port = port;
		this.lastSeen = new Date();
	}
}

module.exports = Node;