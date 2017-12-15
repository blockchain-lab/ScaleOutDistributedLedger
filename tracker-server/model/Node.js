/**
 * Class to store Node information.
 */
class Node {

	/**
	 * Constructor.
	 * @param address - the address of this node.
	 * @param port - the port of this node.
	 */
	constructor(id, address, port) {
		this.id = id;
		this.address = address;
		this.port = port;
		this.lastSeen = new Date();
	}
}

module.exports = Node;