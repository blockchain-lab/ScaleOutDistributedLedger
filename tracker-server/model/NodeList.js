/**
 * Class to store a list of nodes.
 */
class NodeList {

    /**
     * Constructor.
     * @param numberOfNodes - maximum allowed number of nodes.
     */
    constructor(numberOfNodes) {
        this.nodes = [];
        this.numberOfNodes = numberOfNodes;
    }

    /**
     * Adds or updates a node to/in the nodelist.
     * @param node - the node to add.
     * @returns {boolean} - whether the node was valid.
     */
    updateNode(node) {
        if(node.id < this.numberOfNodes) {
            this.nodes[node.id] = node;
            return true;
        }
        return false;
    }

    /**
     * Gets the node at the specified id, if present.
     * @param id - the id of the node to get.
     * @returns {Node} - the node at the specified id.
     */
    getNode(id) {
        if(id < this.numberOfNodes) {
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