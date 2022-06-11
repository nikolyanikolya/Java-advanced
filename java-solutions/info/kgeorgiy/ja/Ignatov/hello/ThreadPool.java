package info.kgeorgiy.ja.Ignatov.hello;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
    private static final int TIMEOUT = 20;
    private final ExecutorService executors;
    private final ExecutorService beginner;
    private final int threads;
    protected ThreadPool(int threads) {
        this.threads = threads;
        executors = Executors.newFixedThreadPool(threads);
        beginner = Executors.newSingleThreadExecutor();
    }
    protected void close(){
        executors.shutdown();
        beginner.shutdown();
        try {
            if (!executors.awaitTermination(
                    (long) threads * TIMEOUT, TimeUnit.SECONDS)
                    || !beginner.awaitTermination(TIMEOUT, TimeUnit.SECONDS)) {
                System.err.println("Pool did not terminate...");
            }
        } catch (InterruptedException e) {
            System.err.println("Some threads were interrupted. " + e.getMessage());
        }
    }
    protected ExecutorService getBeginner(){
        return beginner;
    }
    protected ExecutorService getExecutors(){
        return executors;
    }
}
