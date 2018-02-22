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
     * @param knowledge - object containing what the receiver of the transaction knows about other nodes
     * @param setC - the set of chains that the node knows extra
     */
    constructor(from, to, amount, remainder, numberOfChains, numberOfBlocks, knowledge, setC) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.remainder = remainder;
        this.numberOfChains = numberOfChains;
        this.numberOfBlocks = numberOfBlocks;
        this.knowledge = knowledge;
        this.setC = setC;
    }
}

module.exports = Transaction;