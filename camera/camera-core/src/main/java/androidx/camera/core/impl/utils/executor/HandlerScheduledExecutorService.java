/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl.utils.executor;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of {@link ScheduledExecutorService} which delegates all scheduled task to
 * the given {@link Handler}.
 *
 * <p>Currently, can only be used to schedule future non-repeating tasks.
 */
final class HandlerScheduledExecutorService extends AbstractExecutorService implements
        ScheduledExecutorService {

    private static ThreadLocal<ScheduledExecutorService> sThreadLocalInstance =
            new ThreadLocal<ScheduledExecutorService>() {
                @Override
                public ScheduledExecutorService initialValue() {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        return CameraXExecutors.mainThreadExecutor();
                    } else if (Looper.myLooper() != null) {
                        Handler handler = new Handler(Looper.myLooper());
                        return new HandlerScheduledExecutorService(handler);
                    }

                    return null;
                }
            };

    private final Handler mHandler;

    HandlerScheduledExecutorService(@NonNull Handler handler) {
        mHandler = handler;
    }

    /**
     * Retrieves a cached executor derived from the current thread's looper.
     */
    static ScheduledExecutorService currentThreadExecutor() {
        ScheduledExecutorService executor = sThreadLocalInstance.get();
        if (executor == null) {
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalStateException("Current thread has no looper!");
            }

            executor = new HandlerScheduledExecutorService(new Handler(looper));
            sThreadLocalInstance.set(executor);
        }

        return executor;
    }

    @Override
    public ScheduledFuture<?> schedule(
            @NonNull final Runnable command,
            long delay,
            @NonNull TimeUnit unit) {
        Callable<Void> wrapper = new Callable<Void>() {
            @Override
            public Void call() {
                command.run();
                return null;
            }
        };
        return schedule(wrapper, delay, unit);
    }

    @Override
    @NonNull
    public <V> ScheduledFuture<V> schedule(
            @NonNull Callable<V> callable,
            long delay,
            @NonNull TimeUnit unit) {
        long runAtMillis = SystemClock.uptimeMillis() + TimeUnit.MILLISECONDS.convert(delay, unit);
        HandlerScheduledFuture<V> future = new HandlerScheduledFuture<>(mHandler, runAtMillis,
                callable);
        if (mHandler.postAtTime(future, runAtMillis)) {
            return future;
        }

        return Futures.immediateFailedScheduledFuture(createPostFailedException());
    }

    @Override
    @NonNull
    public ScheduledFuture<?> scheduleAtFixedRate(
            @NonNull Runnable command,
            long initialDelay,
            long period,
            @NonNull TimeUnit unit) {
        throw new UnsupportedOperationException(
                HandlerScheduledExecutorService.class.getSimpleName()
                        + " does not yet support fixed-rate scheduling.");
    }

    @Override
    @NonNull
    public ScheduledFuture<?> scheduleWithFixedDelay(@NonNull Runnable command, long initialDelay,
            long delay, @NonNull TimeUnit unit) {
        throw new UnsupportedOperationException(
                HandlerScheduledExecutorService.class.getSimpleName()
                        + " does not yet support fixed-delay scheduling.");
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException(
                HandlerScheduledExecutorService.class.getSimpleName()
                        + " cannot be shut down. Use Looper.quitSafely().");
    }

    @Override
    @NonNull
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException(
                HandlerScheduledExecutorService.class.getSimpleName()
                        + " cannot be shut down. Use Looper.quitSafely().");
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) {
        throw new UnsupportedOperationException(
                HandlerScheduledExecutorService.class.getSimpleName()
                        + " cannot be shut down. Use Looper.quitSafely().");
    }

    @Override
    public void execute(@NonNull Runnable command) {
        if (!mHandler.post(command)) {
            throw createPostFailedException();
        }
    }

    private RejectedExecutionException createPostFailedException() {
        return new RejectedExecutionException(mHandler + " is shutting down");
    }

    private static class HandlerScheduledFuture<V> implements RunnableScheduledFuture<V> {

        final AtomicReference<CallbackToFutureAdapter.Completer<V>>
                mCompleter = new AtomicReference<>(null);
        private final long mRunAtMillis;
        private final Callable<V> mTask;
        private final ListenableFuture<V> mDelegate;

        HandlerScheduledFuture(final Handler handler, long runAtMillis, final Callable<V> task) {
            mRunAtMillis = runAtMillis;
            mTask = task;
            mDelegate = CallbackToFutureAdapter.getFuture(
                    new CallbackToFutureAdapter.Resolver<V>() {

                        @Override
                        public Object attachCompleter(
                                @NonNull CallbackToFutureAdapter.Completer<V> completer) throws
                                RejectedExecutionException {

                            completer.addCancellationListener(new Runnable() {
                                @Override
                                public void run() {
                                    // Remove the completer if we're cancelled so the task won't
                                    // run.
                                    if (mCompleter.getAndSet(null) != null) {
                                        handler.removeCallbacks(HandlerScheduledFuture.this);
                                    }
                                }
                            }, CameraXExecutors.directExecutor());

                            mCompleter.set(completer);
                            return "HandlerScheduledFuture-" + task.toString();
                        }
                    });
        }

        @Override
        public boolean isPeriodic() {
            return false;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(mRunAtMillis - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public void run() {
            // If completer is null, it has already run or is cancelled.
            CallbackToFutureAdapter.Completer<V> completer = mCompleter.getAndSet(null);
            if (completer != null) {
                try {
                    completer.set(mTask.call());
                } catch (Exception e) {
                    completer.setException(e);
                }
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return mDelegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return mDelegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return mDelegate.isDone();
        }

        @Override
        public V get() throws ExecutionException, InterruptedException {
            return mDelegate.get();
        }

        @Override
        public V get(long timeout, @NonNull TimeUnit unit)
                throws ExecutionException, InterruptedException, TimeoutException {
            return mDelegate.get(timeout, unit);
        }
    }
}
