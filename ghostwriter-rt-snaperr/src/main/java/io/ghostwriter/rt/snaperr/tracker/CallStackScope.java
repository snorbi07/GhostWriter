package io.ghostwriter.rt.snaperr.tracker;

import io.ghostwriter.rt.snaperr.api.TrackedScope;
import io.ghostwriter.rt.snaperr.api.TrackedValue;

import java.util.*;

/**
 * Stack implementation holding {@link TrackedScope} instances. Upon
 * {@link #pop()} the stack does not remove the TrackedScope instance from the
 * underlying list to reduce GC overhead, only it's values are set null.
 * <p>
 * <p>
 * Based on {@link ArrayList} with an initial capacity of
 * {@link #INITIAL_CAPACITY}. While addition and deletion to the end of a list
 * is faster in LinkedList because there is no automatic reallocation of the
 * list, our implementation needs fast indexing for retrieving and inserting
 * elements in the middle of the list.
 *
 */
class CallStackScope implements Iterable<TrackedScope> {

    static final int INITIAL_CAPACITY = 256;

    /**
     * It's important to use ArrayList because it has O(1) cost for indexing
     * elements in the middle of the list
     */
    private final List<TrackedScope> stack = new ArrayList<>(INITIAL_CAPACITY);

    /**
     * Index of last inserted element within <i>stack</i>
     */
    private int lastInsertedIndex = -1;

    void push(Object source, String methodName, Map<String, TrackedValue> references) {
        cleanup();

        int stackLastElementIndex = stack.size() - 1;
        if (stackLastElementIndex == lastInsertedIndex) {
            /*
             * We reached the end of the list, so we create and append a new
             * TrackedScope element to
             */
            stack.add(new TrackedScope(source, methodName, references));
            lastInsertedIndex++;
        } else if (stackLastElementIndex > lastInsertedIndex) {
            /*
             * The lastIndex points to the middle of the list, so we set the
             * values to the existing TrackedScope element
             */
            TrackedScope last = stack.get(++lastInsertedIndex);
            last.setSource(source);
            last.setMethodName(methodName);
            last.setReferences(references);
        } else {
            throw new IllegalStateException("lastIndex '" + lastInsertedIndex
                    + "' must not be greater than stack size '" + stack.size());
        }
    }

    /**
     * Removes the last element from the list.
     */
    void pop() {
        TrackedScope peek = peek();
        peek.setMethodName(null);
        peek.setReferences(null);
        peek.setSource(null);
        lastInsertedIndex--;
    }

    /**
     * If you want to use {@link #pop()} after {@link #peek()}, make a copy of
     * the returned value because <i>pop()</i> will set all the values of
     * {@link TrackedScope} to NULL
     *
     * @return Last inserted element
     */
    TrackedScope peek() {
        TrackedScope trackedScope = stack.get(lastInsertedIndex);
        return trackedScope;
    }

    /**
     * @return <li>true if empty
     * <li>false if not empty
     */
    boolean isEmpty() {
        return lastInsertedIndex < 0;
    }

    /**
     * Cleans up unused {@link #stack} elements
     */
    private void cleanup() {
        // TODO: check what to do here
    }

    /**
     * Reverse order iterator
     */
    @Override
    public Iterator<TrackedScope> iterator() {
        final int toIndexExclusive = lastInsertedIndex + 1;
        List<TrackedScope> notNullElements = stack.subList(0, toIndexExclusive);
        return new ScopeStackReverseIterator(notNullElements);
    }

    public static class ScopeStackReverseIterator implements Iterator<TrackedScope> {

        final Iterator<TrackedScope> listIterator;

        private ScopeStackReverseIterator(List<TrackedScope> stack) {
            /*
             * Create a new list, we take a snapshot to avoid concurrent
             * modification
             */
            List<TrackedScope> stackCopy = new LinkedList<>(stack);

            // Revert it, ListIterator#hasPrevious did not seem to work
            Collections.reverse(stackCopy);
            listIterator = stackCopy.iterator();
        }

        @Override
        public boolean hasNext() {
            return listIterator.hasNext();
        }

        @Override
        public TrackedScope next() {
            return listIterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                    "You cannot remove elements from the actual Tracked Scope stack");
        }
    }

}
