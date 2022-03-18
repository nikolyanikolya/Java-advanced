package info.kgeorgiy.ja.Ignatov.arrayset;

import java.util.*;

/**
 * @author Ignatov Nikolay
 */
public class ArraySet<E extends Comparable<E>> extends AbstractSet<E> implements SortedSet<E> {
    private final List<E> elements;
    private final Comparator<? super E> comparator;
    private final Comparator<? super E> comparatorUsed;

    public ArraySet() {
        elements = List.of();
        comparator = null;
        comparatorUsed = Comparator.naturalOrder();
    }

    private ArraySet(final List<E> elements, final Comparator<? super E> comparator,
                     final Comparator<? super E> comparatorUsed) {
        this.elements = elements;
        this.comparator = comparator;
        this.comparatorUsed = comparatorUsed;
    }

    public ArraySet(final Collection<? extends E> collection, final Comparator<? super E> comparator) {
        this.comparator = comparator;
        comparatorUsed = comparator == null ? Comparator.naturalOrder() : comparator;
        elements = new ArrayList<>();

        final List<E> sortedListOfElements = new ArrayList<>(collection);
        sortedListOfElements.sort(comparator);
        for (final E sortedListOfElement : sortedListOfElements) {
            if (!elements.isEmpty()) {
                if (comparatorUsed.compare(sortedListOfElement, elements.get(elements.size() - 1)) == 0) {
                    continue;
                }
            }
            elements.add(sortedListOfElement);
        }
    }

    public ArraySet(final Collection<? extends E> collection) {
        this(collection, null);
    }

    private int lowerBound(final E key, boolean inclusive) {
        // :NOTE: Collections.binarySearch(elements, key);
        final var index = Collections.binarySearch(elements, key, comparatorUsed);
        if (index < 0) {
            return -index - 2;
        }
        return inclusive ? index : index - 1;
    }


    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableCollection(elements).iterator();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object a) {
        return Collections.binarySearch(elements, (E) a, comparator) >= 0;
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> headSet(final E toElement) {
        return new ArraySet<>(elements.subList(0, lowerBound(toElement, false) + 1), comparator, comparatorUsed);
    }

    @Override
    public SortedSet<E> tailSet(final E fromElement) {
        return new ArraySet<>(elements.subList(lowerBound(fromElement, false) + 1, size()), comparator, comparatorUsed);
    }

    @Override
    public E first() throws NoSuchElementException {
        if (!isEmpty()) {
            return elements.get(0);
        }
        throw new NoSuchElementException("first element does not exist");
    }

    @Override
    public E last() throws NoSuchElementException {
        if (!isEmpty()) {
            return elements.get(size() - 1);
        }
        throw new NoSuchElementException("last element does not exist");
    }

    @Override
    public SortedSet<E> subSet(final E fromElement, final E toElement)
            throws IllegalArgumentException {
        if (comparatorUsed.compare(toElement, fromElement) >= 0) {
            return headSet(toElement).tailSet(fromElement);
        }
        throw new IllegalArgumentException("illegal arguments for subset");
    }
}
