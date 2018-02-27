package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.SimulationMain;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;

/**
 * Cheaty meta knowledge that leaks state to give exact answers.
 */
public class CheatyMetaKnowledge extends MetaKnowledge {
	private static final long serialVersionUID = 1L;
	
	private volatile LocalStore ownLocalStore;
	
	/**
	 * @param owner - the node this metaknowledge is about
	 */
	public CheatyMetaKnowledge(Node owner) {
		super(owner);
	}
	
	private LocalStore getOwnLocalStore() {
		if (this.ownLocalStore != null) return this.ownLocalStore;
		return this.ownLocalStore = SimulationMain.getApplicationOfNode(owner.getId()).getLocalStore();
	}
	
	@Override
	public int getLastKnownBlockNumber(int nodeId) {
		return getOwnLocalStore().getNode(nodeId).getChain().getLastBlockNumber();
	}
	
	@Override
	public int[] getBlocksKnown() {
		final int length = Settings.INSTANCE.totalNodesNumber;
		int[] blocksKnown = new int[length];
		for (int i = 0; i < length; i++) {
			blocksKnown[i] = getLastKnownBlockNumber(i);
		}
		return blocksKnown;
	}
}
