package info.kgeorgiy.ja.Ignatov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;


import java.util.*;
import java.util.function.Function;

/**
 * Class includes functionality of {@link ParallelMapper} interface
 *
 * @author Ignatov Nikolay
 * @see ParallelMapper
 * @see Thread
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final List<Thread> currentThreads;

    /**
     * Creates an instance with specified number of working threads which can be used for parallelization.
     *
     * @param threads number of used threads, must be positive
     */
    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Illegal threads number");
        }
        currentThreads = new ArrayList<>();
        Runnable run = () -> {
            try {
                while (!Thread.interrupted()) {
                    runSynchronizedTask();
                }
            } catch (InterruptedException e) {
                // ignore
            } finally {
                Thread.currentThread().interrupt(); // recovering interrupting flag
            }
        };
        for (int i = 0; i < threads; i++) {
            currentThreads.add(new Thread(run));
        }
        for (var currentThread : currentThreads) {
            currentThread.start();
        }
    }

    /**
     * Applies the specified function to each argument in the list in multiple threads.
     *
     * @param f    function to apply
     * @param args arguments
     * @return the list of elements obtained as a result of the function application
     * @throws InterruptedException when some concurrent threads were suddenly interrupted
     * @see Function
     * @see Thread
     * @see InterruptedException
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        synchronized (ParallelMapper.class) {
            List<InterruptedException> exceptions = new ArrayList<>();
            InnerRunner<R> runner = new InnerRunner<>(args.size());
            int i = 0;
            for (var arg : args) {
                final int index = i++;
                try {
                    produceSynchronizedTask(() ->
                            runner.addValue(f.apply(arg), index)
                    );
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
            var answer = runner.getList();
            tasks.clear();
            return answer;
        }
    }

    /**
     * Stops all working threads.
     * The state of unfinished threads is undefined.
     */
    @Override
    public void close() {
        // :NOTE: synchronized(this)
        synchronized (ParallelMapperImpl.class) {
            for (var currentThread : currentThreads) {
                currentThread.interrupt();
            }
            for (var currentThread : currentThreads) {
                try {
                    currentThread.join();
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        }
    }

    private void produceSynchronizedTask(final Runnable task) throws InterruptedException { // producer
        synchronized (tasks) {
            tasks.add(task);
            tasks.notify(); // there are only consumers in waiting
        }
    }

    private void runSynchronizedTask() throws InterruptedException { // consumer
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
            tasks.notify();
        }
        task.run();
    }

    private static class InnerRunner<R> {
        private final List<R> results;
        private int tasksCompleted = 0;

        private InnerRunner(int size) {
            results = new ArrayList<>(Collections.nCopies(size, null));
        }

        private synchronized void addValue(final R value, int taskNumber) {
            tasksCompleted++;
            if (tasksCompleted == results.size()) {
                notify();  // only consumers in waiting
            }
            results.set(taskNumber, value);
        }

        private synchronized List<R> getList() throws InterruptedException {
            while (tasksCompleted < results.size()) {
                wait();
            }
            return results;
        }
    }

}
