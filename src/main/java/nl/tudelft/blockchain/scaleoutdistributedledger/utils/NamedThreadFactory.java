package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple {@link ThreadFactory} which creates threads with names.
 */
public class NamedThreadFactory implements ThreadFactory {
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;

	/**
	 * @param namePrefix - the name prefix
	 */
	public NamedThreadFactory(String namePrefix) {
		this.group = Thread.currentThread().getThreadGroup();
		this.namePrefix = namePrefix + "-";
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);

		if (t.isDaemon()) {
			t.setDaemon(false);
		}

		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}

		return t;
	}
}
