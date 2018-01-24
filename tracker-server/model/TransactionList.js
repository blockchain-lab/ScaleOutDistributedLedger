/**
 * Class to store a list of all transactions.
 */
class TransactionList {

    constructor() {
        // Store transactions in a dictionary
        this.transactions = {};
    }

    addTransaction(transaction) {
        // Make sure we use a consistent key for all transactions between the same two nodes
        let key = [transaction.from, transaction.to];
        if(transaction.from > transaction.to)
            key = [transaction.to, transaction.from];

        if(!this.transactions[key])
            this.transactions[key] = [transaction];
        else
            this.transactions[key].push(transaction);
    }

    getEdgeWeight(node1, node2) {
        let key = [node1, node2];
        if(node1 > node2) {
            key = [node2, node1];
        }
        return this.getEdgeWeightWithKey(key);
    }

    getEdgeWeightWithKey(key) {
        if(!this.transactions[key]) return 0;

        // TODO: something other than number of transactions?
        return this.transactions[key].length;
    }

    getGraphEdges() {
        const edges = [];
        for (const key of Object.keys(this.transactions)) {
            console.log(key);
            const keyArray = key.split(",");
            const weight = this.getEdgeWeightWithKey(key);
            edges.push({from: keyArray[0], to: keyArray[1], value: weight});
        }
        return edges;
    }
}

module.exports = TransactionList;