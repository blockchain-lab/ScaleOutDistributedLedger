/**
 * Class to store transaction information.
 */
class Transaction {

    /**
     * Constructor.
     * @param from - id of sender node
     * @param to - id of receiver node
     * @param amount - amount of transaction
     * @param remainder - remainder of transaction
     * @param numberOfChains - number of chains sent in proof
     * @param numberOfBlocks - number of blocks sent in proof
     */
    constructor(from, to, amount, remainder, numberOfChains, numberOfBlocks) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.remainder = remainder;
        this.numberOfChains = numberOfChains;
        this.numberOfBlocks = numberOfBlocks;
    }
}

module.exports = Transaction;