package nl.tudelft.blockchain.scaleoutdistributedledger.simulation;

/**
 * Runnable implementation which will run repeatedly until it is cancelled.
 */
public class CancellableInfiniteRunnable implements Runnable {
	private Thread runner;
	private boolean cancelled;
	private final InterruptibleRunnable runnable;
	
	/**
	 * @param runnable - the runnable
	 */
	public CancellableInfiniteRunnable(InterruptibleRunnable runnable) {
		this.runnable = runnable;
	}
	
	/**
	 * Cancels this runnable.
	 * This also interrupts the task if it is running.
	 */
	public void cancel() {
		if (cancelled) return;
		cancelled = true;
		synchronized (this) {
			if (runner != null) {
//				runner.interrupt();
			}
		}
	}
	
	/**
	 * @return if this runnable has been cancelled
	 */
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void run() {
		synchronized (this) {
			runner = Thread.currentThread();
		}
		
		try {
			while (!isCancelled()) {
				try {
					runnable.run();
				} catch (InterruptedException ex) {
					continue;
				}
			}
		} finally {
			synchronized (this) {
				runner = null;
			}
		}
	}

	/**
	 * Interruptible runnable interface.
	 */
	@FunctionalInterface
	public interface InterruptibleRunnable {
		/**
		 * Runs this interruptible runnable.
		 * @throws InterruptedException - when the method is interrupted
		 */
		public void run() throws InterruptedException;
	}
}

