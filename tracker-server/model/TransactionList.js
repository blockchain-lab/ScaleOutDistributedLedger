/**
 * Class to store a list of all transactions.
 */
class TransactionList {

    /**
     * Constructor.
     */
    constructor() {
        // Store transactions in a dictionary
        this.transactions = {};
        this.numberOfTransactions = 0;
        this.numberOfChains = 0;
        this.numberOfBlocks = 0;
    }

    /**
     * Add a transaction to the transaction list.
     * @param transaction - the transaction to add
     */
    addTransaction(transaction) {
        this.numberOfTransactions += 1;
        this.numberOfChains += transaction.numberOfChains;
        this.numberOfBlocks += transaction.numberOfBlocks;

        // Make sure we use a consistent key for all transactions between the same two nodes
        let key = [transaction.from, transaction.to];
        if(transaction.from > transaction.to)
            key = [transaction.to, transaction.from];

        if(!this.transactions[key])
            this.transactions[key] = [transaction];
        else
            this.transactions[key].push(transaction);
    }

    /**
     * Gets the edge weight between two nodes.
     * @param node1 - the first node
     * @param node2 - the second node
     * @returns {number}
     */
    getEdgeWeight(node1, node2) {
        let key = [node1, node2];
        if(node1 > node2) {
            key = [node2, node1];
        }
        return this.getEdgeWeightWithKey(key);
    }

    /**
     * Gets the edge weight for a certain key.
     * @param key - the key
     * @returns {number}
     */
    getEdgeWeightWithKey(key) {
        if(!this.transactions[key]) return 0;

        // TODO: something other than number of transactions?
        return this.transactions[key].length;
    }

    /**
     * Returns parsed edges for the graph.
     * @returns {Array}
     */
    getGraphEdges() {
        const edges = [];
        for (const key of Object.keys(this.transactions)) {
            const keyArray = key.split(",");
            const weight = this.getEdgeWeightWithKey(key);
            edges.push({from: keyArray[0], to: keyArray[1], value: weight});
        }
        return edges;
    }

    /**
     * Returns a JSON object containing some interesting number.
     * @returns {{numberOfTransactions: number, averageNumberOfBlocks: number, averageNumberOfChains: number}}
     */
    getNumbers() {
        let averageNumberOfChains = 0,
            averageNumberOfBlocks = 0;
        if(this.numberOfTransactions !== 0) {
            averageNumberOfBlocks = this.numberOfBlocks / this.numberOfTransactions;
            averageNumberOfChains = this.numberOfChains / this.numberOfTransactions;
        }
        return {
            numberOfTransactions: this.numberOfTransactions,
            averageNumberOfBlocks: averageNumberOfBlocks,
            averageNumberOfChains: averageNumberOfChains
        };
    }
}

module.exports = TransactionList;