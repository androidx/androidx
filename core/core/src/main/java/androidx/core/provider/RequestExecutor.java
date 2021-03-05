/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.provider;

import android.os.Handler;
import android.os.Process;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class that provides callback and wait operations for execute actions.
 */
class RequestExecutor {

    private RequestExecutor() {}

    /**
     * Calls the callable, and returns the result as an argument to {@link Consumer#accept}.
     */
    static <T> void execute(
            @NonNull Executor executor,
            @NonNull Callable<T> callable,
            @NonNull Consumer<T> consumer
    ) {
        // TODO check if this handler can be removed
        //  removing the callee handler cause breakage see aosp/1600057
        final Handler calleeHandler = CalleeHandler.create();
        executor.execute(new ReplyRunnable<>(calleeHandler, callable, consumer));
    }

    static <T> T submit(
            @NonNull ExecutorService executor,
            @NonNull final Callable<T> callable,
            @IntRange(from = 0) int timeoutMillis
    ) throws InterruptedException {
        Future<T> future = executor.submit(callable);
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new InterruptedException("timeout");
        }
    }

    static ThreadPoolExecutor createDefaultExecutor(
            @NonNull String threadName,
            int threadPriority,
            @IntRange(from = 0) int keepAliveTimeInMillis
    ) {
        ThreadFactory threadFactory = new DefaultThreadFactory(threadName, threadPriority);
        // allow core thread timeout to timeout the core threads so that
        // when no tasks arrive, the core threads can be killed
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                0 /* corePoolSize */,
                1 /* maximumPoolSize */,
                keepAliveTimeInMillis /* keepAliveTime */,
                TimeUnit.MILLISECONDS /* keepAliveTime TimeUnit */,
                new LinkedBlockingDeque<Runnable>() /* unbounded queue*/,
                threadFactory
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    static Executor createHandlerExecutor(@NonNull Handler handler) {
        return new HandlerExecutor(handler);
    }

    /**
     * An adapter {@link Executor} that posts all executed tasks onto the given {@link Handler}.
     *
     * The same as androidx.core.os.HandlerExecutor, however that class causes build failures.
     * Adding this class temporarily until HandlerExecutor problem is solved.
     */
    private static class HandlerExecutor implements Executor {
        private final Handler mHandler;

        HandlerExecutor(@NonNull Handler handler) {
            mHandler = Preconditions.checkNotNull(handler);
        }

        @Override
        public void execute(@NonNull Runnable command) {
            if (!mHandler.post(Preconditions.checkNotNull(command))) {
                throw new RejectedExecutionException(mHandler + " is shutting down");
            }
        }
    }

    /**
     * Default Runnable to call the callable, and returns the result as an argument to
     * {@link Consumer#accept}.
     */
    private static class ReplyRunnable<T> implements Runnable {
        private @NonNull Callable<T> mCallable;
        private @NonNull Consumer<T> mConsumer;
        private @NonNull Handler mHandler;

        ReplyRunnable(
                @NonNull Handler handler,
                @NonNull Callable<T> callable,
                @NonNull Consumer<T> consumer
        ) {
            mCallable = callable;
            mConsumer = consumer;
            mHandler = handler;
        }

        @Override
        public void run() {
            T t;
            try {
                t = mCallable.call();
            } catch (Exception e) {
                t = null;
            }
            final T result = t;
            final Consumer<T> consumer = mConsumer;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    consumer.accept(result);
                }
            });
        }
    }

    private static class DefaultThreadFactory implements ThreadFactory {
        private String mThreadName;
        private int mPriority;

        DefaultThreadFactory(@NonNull String threadName, int priority) {
            mThreadName = threadName;
            mPriority = priority;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            return new ProcessPriorityThread(runnable, mThreadName, mPriority);
        }

        private static class ProcessPriorityThread extends Thread {
            private final int mPriority;

            ProcessPriorityThread(Runnable target, String name, int priority) {
                super(target, name);
                mPriority = priority;
            }

            @Override
            public void run() {
                Process.setThreadPriority(mPriority);
                super.run();
            }
        }
    }
}

