package info.kgeorgiy.ja.Ignatov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Ignatov Nikolay
 * This class allows processing of the lists using multiple threads.
 * Each method of it is an associative operation whose execution is iteratively parallelized.
 * @see Thread
 * @see ScalarIP
 * @see ParallelMapper
 */
public class IterativeParallelism implements ScalarIP {

    private final ParallelMapper parallelMapper;

    /**
     * ParallelMapper constructor
     *
     * @param parallelMapper an instance of {@link ParallelMapper}
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Default constructor
     */
    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    /**
     * Allows getting first maximum of the list.
     *
     * @param threads    number or concurrent threads.
     * @param values     list to get maximum of.
     * @param comparator list elements comparator.
     * @return The first maximum of the list. If the list is empty the null value would be returned.
     * Elements with null value are ignored when comparing.
     * @throws InterruptedException when some concurrent threads were interrupted
     * @see List
     * @see Comparator
     */

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return applyThreads(threads, values,
                stream -> filterNulls(stream)
                        .max(comparator).orElse(null),
                stream -> filterNulls(stream)
                        .max(comparator).orElse(null));
    }

    /**
     * Allows getting first minimum of the list.
     *
     * @param threads    number or concurrent threads.
     * @param values     list to get minimum of.
     * @param comparator list elements comparator.
     * @return The first minimum of the list. If the list is empty the null value would be returned.
     * Elements with null value are ignored when comparing.
     * @throws InterruptedException when some concurrent threads were interrupted
     * @see List
     * @see Comparator
     */

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return applyThreads(threads, values,
                stream -> filterNulls(stream)
                        .min(comparator).orElse(null),
                stream -> filterNulls(stream)
                        .min(comparator).orElse(null));
    }

    /**
     * Checks whether the condition is met for all elements.
     *
     * @param threads   number or concurrent threads.
     * @param values    list of the elements to check the predicate for.
     * @param predicate list elements predicate.
     * @return <ul>
     * <li>true, if predicate is met for all non null elements</li>
     * <li>false, if there is the non null element for which the condition is not met </li>
     * </ul>
     * @throws InterruptedException when some concurrent threads were interrupted
     * @see List
     * @see Predicate
     */

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return applyThreads(threads, values,
                stream -> filterNulls(stream)
                        .allMatch(predicate),
                stream -> filterNulls(stream)
                        .allMatch(element -> element));
    }

    /**
     * Checks whether the condition is met for any elements.
     *
     * @param threads   number or concurrent threads.
     * @param values    list of the elements to check predicate for.
     * @param predicate list elements predicate.
     * @return <ul>
     * <li>true, if predicate is met for any non null elements</li>
     * <li>false, if predicate is not met for all non null elements </li>
     * </ul>
     * @throws InterruptedException when some concurrent threads were interrupted
     * @see List
     * @see Predicate
     */

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return applyThreads(threads, values,
                stream -> filterNulls(stream)
                        .anyMatch(predicate),
                stream -> filterNulls(stream)
                        .anyMatch(element -> element));
    }

    private <T, R> R applyThreads(int threads, List<? extends T> values,
                                  Function<Stream<? extends T>, R> applier,
                                  Function<Stream<? extends R>, R> returnApplier) throws InterruptedException {
        final int blockSize = values.size() / threads;
        int rest = values.size() % threads;
        List<Stream<? extends T>> blocks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int factualSize = blockSize;
            if (i < rest) {
                factualSize++;
            }
            blocks.add(values.subList(i * factualSize, Math.min(values.size(), (i + 1) * factualSize)).stream());
        }
        List<R> threadResults;
        if (parallelMapper == null) {
            List<Thread> currentThreads = new ArrayList<>();
            threadResults = new ArrayList<>(Collections.nCopies(threads, null));
            for (int i = 0; i < blocks.size(); i++) {
                final int index = i; // effectively final
                var block = blocks.get(i);
                Thread thread = new Thread(() -> threadResults.set(index, applier.apply(block)));
                currentThreads.add(thread);
            }
            currentThreads.forEach(Thread::start);
            List<InterruptedException> exceptions = new ArrayList<>();
            for (var thread : currentThreads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    exceptions.add(e);
                }
            }
            if (!exceptions.isEmpty()) {
                InterruptedException e = new InterruptedException("Some of the threads were interrupted");
                for (var exception : exceptions) {
                    e.addSuppressed(exception);
                }
                throw e;
            }
            
        } else {
            threadResults = parallelMapper.map(applier, blocks);
        }
        return returnApplier.apply(threadResults.stream());
    }

    private <T> Stream<T> filterNulls(Stream<T> stream) {
        return stream.filter(Objects::nonNull);
    }

}
