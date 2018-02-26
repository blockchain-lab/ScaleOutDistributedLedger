/**
 * Class to store a list of all transactions.
 */
class TransactionList {

    /**
     * Constructor.
     */
    constructor() {
        // Store transactions in a dictionary
        this.transactions = [];
        this.transactions2 = [];
        this.numberOfTransactions = 0;
        this.numberOfChains = 0;
        this.numberOfBlocks = 0;
        this.setCSize = {};
        this.numbersArray = [];
    }

    /**
     * Add a transaction to the transaction list.
     * @param transaction - the transaction to add
     */
    addTransaction(transaction) {
        this.numberOfTransactions += 1;
        this.numberOfChains += transaction.numberOfChains;
        this.numberOfBlocks += transaction.numberOfBlocks;
        this.setCSize[transaction.to] = transaction.setC.length;
        this.transactions2.push(transaction);
        this.transactions[transaction.to] = transaction;
    }

    /**
     * Returns parsed edges for the graph.
     * @returns {Array}
     */
    getGraphEdges() {
        const edges = [];
        //Draw edge if knowledge, weight of highest
        for (let i = 0; i < this.transactions.length; i++) {
            for (let knowKey in this.transactions[i].knowledge) {
                edges.push({from: i, to: knowKey, value: this.transactions[i][knowKey]});
            }
        }
        return edges;
    }

    /**
     * Returns a JSON object containing some interesting number.
     * @returns {{numberOfTransactions: number, averageNumberOfBlocks: number, averageNumberOfChains: number, averageSetCSize: number}}
     */
    getNumbers() {
        let averageNumberOfChains = 0,
            averageNumberOfBlocks = 0,
            averageSetCSize = 0,
            sumSetCSize = 0,
            count = 0;
        if(this.numberOfTransactions !== 0) {
            averageNumberOfBlocks = this.numberOfBlocks / this.numberOfTransactions;
            averageNumberOfChains = this.numberOfChains / this.numberOfTransactions;
            
            for (var key in this.setCSize) {
                sumSetCSize += this.setCSize[key];
                count++;
            }
            averageSetCSize = sumSetCSize / count;
        }
        return {
            numberOfTransactions: this.numberOfTransactions,
            averageNumberOfBlocks: averageNumberOfBlocks,
            averageNumberOfChains: averageNumberOfChains,
            averageSetCSize: averageSetCSize
        };
    }

    /**
     * Adds numbers data point to the array.
     */
    addNumbersToArray() {
        const numbers = this.getNumbers();
        if(numbers.numberOfTransactions !== 0 || numbers.averageNumberOfBlocks !== 0 || numbers.averageNumberOfChains !== 0 || numbers.averageSetCSize !== 0) {
            this.numbersArray.push(numbers);
        }
    }

    /**
     * Gets the numbers array.
     * @returns {Array}
     */
    getNumbersArray() {
        return this.numbersArray;
    }
}

module.exports = TransactionList;