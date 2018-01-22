package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Iterates over an iterable list in reversed order.
 * @param <T>
 */
public class ReversedIterator<T> implements Iterable<T> {
    private final List<T> original;

    /**
     * Constructor.
     * @param original - the original iterable list.
     */
    public ReversedIterator(List<T> original) {
        this.original = original;
    }

    /**
     * Implementation of the iterator.
     * @return - the iterator.
     */
    public Iterator<T> iterator() {
        final ListIterator<T> i = original.listIterator(original.size());

        return new Iterator<T>() {
            public boolean hasNext() { return i.hasPrevious(); }
            public T next() { return i.previous(); }
            public void remove() { i.remove(); }
        };
    }

    /**
     * Gets a reversed iterator for an iterable list.
     * @param original - the original list
     * @param <T> - the type of the original list
     * @return - the reversed iterable.
     */
    public static <T> ReversedIterator<T> reversed(List<T> original) {
        return new ReversedIterator<T>(original);
    }
}