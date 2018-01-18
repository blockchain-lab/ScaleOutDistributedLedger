package nl.tudelft.blockchain.scaleoutdistributedledger.simulation;

import java.util.function.Consumer;
import java.util.function.ToLongFunction;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Runnable implementation which will run repeatedly until it is cancelled.
 * @param <T> - the type of elements to accept
 */
public class CancellableInfiniteRunnable<T> implements Runnable {
	private Thread runner;
	private boolean cancelled;
	private final T t;
	private final InterruptibleConsumer<T> action;
	private final ToLongFunction<T> sleepFunction;
	private final Consumer<T> onStop;
	
	/**
	 * @param t             - the parameter to pass
	 * @param action        - the action that is repeated
	 * @param sleepFunction - a function to determine the time to sleep
	 * @param onStop        - the consumer to execute when stopping
	 */
	public CancellableInfiniteRunnable(T t, InterruptibleConsumer<T> action, ToLongFunction<T> sleepFunction, Consumer<T> onStop) {
		this.t = t;
		this.action = action;
		this.sleepFunction = sleepFunction;
		this.onStop = onStop;
	}
	
	/**
	 * Cancels this runnable.
	 * This also interrupts the task if it is running.
	 */
	public void cancel() {
		if (cancelled) return;
		cancelled = true;
	}
	
	/**
	 * @return if this runnable has been cancelled
	 */
	public boolean isCancelled() {
		return cancelled;
	}
	
	/**
	 * Stops this runnable as quickly as possible by interrupting the executing thread.
	 */
	public void stopNow() {
		cancel();
		synchronized (this) {
			if (runner != null) {
				runner.interrupt();
			}
		}
	}
	
	@Override
	public void run() {
		synchronized (this) {
			runner = Thread.currentThread();
		}
		
		try {
			while (!isCancelled()) {
				try {
					action.accept(t);
					Thread.sleep(sleepFunction.applyAsLong(t));
				} catch (InterruptedException ex) {
					continue;
				} catch (Exception ex) {
					Log.log(Level.SEVERE, "Uncaught exception in action", ex);
				}
			}
		} finally {
			if (onStop != null) {
				try {
					onStop.accept(t);
				} catch (Exception ex) {
					Log.log(Level.SEVERE, "Uncaught exception in onStop", ex);
				}
			}
			
			synchronized (this) {
				runner = null;
			}
		}
	}

	/**
	 * Interruptible consumer interface.
	 * @param <T> - the type to accept
	 */
	@FunctionalInterface
	public interface InterruptibleConsumer<T> {
		/**
		 * Performs this operation on the given argument.
		 * @param t - the input argument
		 * @throws InterruptedException - when the method is interrupted
		 */
		public void accept(T t) throws InterruptedException;
	}
}

