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
     * @returns {boolean} - whether the id was valid.
     */
	updateNode(id, address, port) {
		if(id < this.nodes.length) {
			this.nodes[id].address = address;
			this.nodes[id].port = port;
			return true;
		}
		return false;
	}

    /**
     * Register a new node.
     * @param address - the address of the new node
     * @param port - the port of the new node
     * @returns {number} - the id of the new node
     */
	registerNode(address, port) {
        return this.nodes.push(new Node(address, port)) - 1;
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