import Node from './Node';

/**
 * Class to store a list of nodes.
 */
class NodeList {

	/**
	 * Constructor.
	 */
	constructor() {
		this.nodes = [];
	}

    /**
     *
     * Adds or updates a node to/in the nodelist.
     * @param id - id of the node to update
     * @param address - new address
     * @param port - new port
     * @param publicKey - new public key
     * @returns {boolean} - whether the id was valid.
     */
	updateNode(id, address, port, publicKey) {
		if(id < this.nodes.length) {
			this.nodes[id].address = address;
			this.nodes[id].port = port;
			this.nodes[id].publicKey = publicKey;
			return true;
		}
		return false;
	}

    /**
     * Register a new node.
     * @param address - the address of the new node
     * @param port - the port of the new node
     * @param publicKey - the public key of the new node
     * @returns {number} - the id of the new node
     */
	registerNode(address, port, publicKey) {
        return this.nodes.push(new Node(address, port, publicKey)) - 1;
    }

	/**
	 * Gets the node at the specified id, if present.
	 * @param id - the id of the node to get.
	 * @returns {Node} - the node at the specified id.
	 */
	getNode(id) {
		if(id < this.nodes.length) {
			return this.nodes[id];
		}
		return null;
	}

	/**
	 * Get the full list of nodes.
	 * @returns {Array} - the list of nodes.
	 */
	getNodes() {
		return this.nodes;
	}
}

module.exports = NodeList;