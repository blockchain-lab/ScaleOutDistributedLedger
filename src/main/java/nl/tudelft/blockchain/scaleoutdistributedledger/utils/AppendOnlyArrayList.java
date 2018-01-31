package nl.tudelft.blockchain.scaleoutdistributedledger.utils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * An ArrayList to which you can only append.
 * The iterators of this class all reflect the list from the moment of creation. Any elements
 * appended to the list after the iterator was created will not be reflected.
 * 
 * @param <E> - the type of elements in the list
 */
public class AppendOnlyArrayList<E> extends ArrayList<E> {
	private static final long serialVersionUID = 1L;

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void removeRange(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<E> iterator() {
		return listIterator();
	}

	@Override
	public ListIterator<E> listIterator() {
		return new AppendOnlyArrayListIterator(0);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new AppendOnlyArrayListIterator(index);
	}

	/**
	 * This method returns a list iterator that provides a view from index 0 to the given size,
	 * starting at the given index.
	 * @param index - the index where the list iterator starts
	 * @param size  - the size that the list iterator will use as maximum
	 * @return - a new List Iterator starting at the given index
	 */
	public ListIterator<E> listIterator(int index, int size) {
		return new AppendOnlyArrayListIterator(index, size);
	}

	/**
	 * List iterator for this class.
	 */
	private class AppendOnlyArrayListIterator implements ListIterator<E> {
		private int index;
		private int size;

		/**
		 * @param index - the index to start at
		 */
		AppendOnlyArrayListIterator(int index) {
			this(index, size());
		}

		/**
		 * @param index - the index to start at
		 * @param size  - the size to use
		 */
		AppendOnlyArrayListIterator(int index, int size) {
			this.index = index;
			this.size = size;

			if (index < 0 || index > size) {
				throw new IndexOutOfBoundsException("Index: " + index);
			}
		}

		@Override
		public boolean hasNext() {
			return index != size;
		}

		@Override
		public E next() {
			int i = index;
			if (i >= size) throw new NoSuchElementException();
			
			E element = get(i);
			index = i + 1;
			return element;
		}

		@Override
		public boolean hasPrevious() {
			return index != 0;
		}

		@Override
		public E previous() {
			int i = index - 1;
			if (i < 0) throw new NoSuchElementException();
			
			E element = get(i);
			index = i;
			return element;
		}

		@Override
		public int nextIndex() {
			return index;
		}

		@Override
		public int previousIndex() {
			return index - 1;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(E e) {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		if (toIndex > size()) throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		if (fromIndex > toIndex) throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
		return new SubList(0, fromIndex, toIndex);
	}

	/**
	 * Sublist implementation for AppendOnlyArrayList.
	 */
	private class SubList extends AbstractList<E> implements RandomAccess {
		private final int offset;
		private final int size;

		SubList(int offset, int fromIndex, int toIndex) {
			this.offset = offset + fromIndex;
			this.size = toIndex - fromIndex;
		}

		@Override
		public E set(int index, E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public E get(int index) {
			if (index < 0 || index >= this.size) throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size);
			return AppendOnlyArrayList.this.get(offset + index);
		}

		@Override
		public int size() {
			return this.size;
		}

		@Override
		public void add(int index, E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public E remove(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void removeRange(int fromIndex, int toIndex) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(int index, Collection<? extends E> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<E> iterator() {
			return listIterator();
		}

		@Override
		public ListIterator<E> listIterator(final int index) {
			if (index < 0 || index > this.size) throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size);
			final int offset = this.offset;

			return new ListIterator<E>() {
				int cursor = index;

				@Override
				public boolean hasNext() {
					return cursor != SubList.this.size;
				}

				@Override
				public E next() {
					int i = cursor;
					if (i >= SubList.this.size) throw new NoSuchElementException();
					cursor = i + 1;
					return AppendOnlyArrayList.this.get(offset + i);
				}

				@Override
				public boolean hasPrevious() {
					return cursor != 0;
				}

				@Override
				public E previous() {
					int i = cursor - 1;
					if (i < 0) throw new NoSuchElementException();
					cursor = i;
					return AppendOnlyArrayList.this.get(offset + i);
				}

				@Override
				public int nextIndex() {
					return cursor;
				}

				@Override
				public int previousIndex() {
					return cursor - 1;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public void set(E e) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void add(E e) {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public List<E> subList(int fromIndex, int toIndex) {
			if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
			if (toIndex > size) throw new IndexOutOfBoundsException("toIndex = " + toIndex);
			if (fromIndex > toIndex) throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
			return new SubList(offset, fromIndex, toIndex);
		}
	}
}
