package com.developer.harshul.pinvoke;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.lang.reflect.Field;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * A JUnit Test Rule that swaps the background and main thread executors in AppExecutors
 * with synchronous executors, allowing background operations to execute synchronously
 * during unit testing.
 */
public class SynchronousExecutorRule extends TestWatcher {

    private AppExecutors originalExecutors;

    @Override
    protected void starting(Description description) {
        super.starting(description);
        
        // Mock a synchronous AppExecutors instance using reflection to overwrite the singleton
        try {
            Field instanceField = AppExecutors.class.getDeclaredField("sInstance");
            instanceField.setAccessible(true);
            originalExecutors = (AppExecutors) instanceField.get(null);

            ExecutorService syncExecutorService = new SynchronousExecutorService();
            Executor syncExecutor = Runnable::run;

            // Use reflection again to construct the private constructor (or mock it)
            // Simpler approach for testing: just mock the getters or create a test subclass if possible.
            // But AppExecutors constructor is private, so we use reflection to create a synchronous one.
            java.lang.reflect.Constructor<AppExecutors> constructor = 
                AppExecutors.class.getDeclaredConstructor(ExecutorService.class, ExecutorService.class, Executor.class);
            constructor.setAccessible(true);
            AppExecutors syncExecutors = constructor.newInstance(syncExecutorService, syncExecutorService, syncExecutor);

            instanceField.set(null, syncExecutors);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to set synchronous AppExecutors", e);
        }
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        
        // Restore original executors
        try {
            Field instanceField = AppExecutors.class.getDeclaredField("sInstance");
            instanceField.setAccessible(true);
            instanceField.set(null, originalExecutors);
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore AppExecutors", e);
        }
    }

    /**
     * A very simple synchronous ExecutorService for testing.
     */
    private static class SynchronousExecutorService extends java.util.concurrent.AbstractExecutorService {
        private boolean isShutdown = false;

        @Override
        public void shutdown() { isShutdown = true; }

        @Override
        public java.util.List<Runnable> shutdownNow() { isShutdown = true; return java.util.Collections.emptyList(); }

        @Override
        public boolean isShutdown() { return isShutdown; }

        @Override
        public boolean isTerminated() { return isShutdown; }

        @Override
        public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) { return true; }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
