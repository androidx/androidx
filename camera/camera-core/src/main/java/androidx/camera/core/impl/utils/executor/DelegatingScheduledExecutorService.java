/*
 * Copyright 2026 The Android Open Source Project
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
import android.os.SystemClock;

import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A one-shot {@link ScheduledExecutorService} which runs all tasks on a delegate {@link Executor}
 * and uses a {@link Handler} only as a timer, matching {@code
 * CameraThreadConfig#getSchedulerHandler()}.
 *
 * <p>This wrapper does not own the lifecycle of the delegate {@link Executor} or scheduler {@link
 * Handler} and cannot be shut down directly. If the delegate {@link ExecutorService} is shut down
 * or the scheduler {@link Handler} is shutting down, {@code schedule} throws {@link
 * RejectedExecutionException} so callers such as {@code kotlinx.coroutines} cancel the awaiting job
 * instead of leaving {@code delay()} or {@code withTimeout()} suspended indefinitely.
 */
final class DelegatingScheduledExecutorService extends AbstractExecutorService
        implements ScheduledExecutorService {

    private final Executor mDelegate;
    private final Handler mSchedulerHandler;

    DelegatingScheduledExecutorService(
            @NonNull Executor delegate, @NonNull Handler schedulerHandler) {
        mDelegate = Preconditions.checkNotNull(delegate);
        mSchedulerHandler = Preconditions.checkNotNull(schedulerHandler);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        Preconditions.checkNotNull(command);
        mDelegate.execute(command);
    }

    @Override
    public @NonNull ScheduledFuture<?> schedule(
            @NonNull Runnable command, long delay, @NonNull TimeUnit unit) {
        Preconditions.checkNotNull(command);
        Preconditions.checkNotNull(unit);
        return schedule(
                new DelegatingScheduledFuture<Void>(command, triggerTimeMillis(delay, unit)));
    }

    @Override
    public <V> @NonNull ScheduledFuture<V> schedule(
            @NonNull Callable<V> callable, long delay, @NonNull TimeUnit unit) {
        Preconditions.checkNotNull(callable);
        Preconditions.checkNotNull(unit);
        return schedule(new DelegatingScheduledFuture<>(callable, triggerTimeMillis(delay, unit)));
    }

    @Override
    public @NonNull ScheduledFuture<?> scheduleAtFixedRate(
            @NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit) {
        throw new UnsupportedOperationException(
                DelegatingScheduledExecutorService.class.getSimpleName()
                        + " does not yet support fixed-rate scheduling.");
    }

    @Override
    public @NonNull ScheduledFuture<?> scheduleWithFixedDelay(
            @NonNull Runnable command, long initialDelay, long delay, @NonNull TimeUnit unit) {
        throw new UnsupportedOperationException(
                DelegatingScheduledExecutorService.class.getSimpleName()
                        + " does not yet support fixed-delay scheduling.");
    }

    private <V> @NonNull ScheduledFuture<V> schedule(@NonNull DelegatingScheduledFuture<V> future) {
        if (isShutdown()) {
            throw new RejectedExecutionException(mDelegate + " is shutting down");
        }
        if (!future.postTrigger()) {
            // kotlinx.coroutines cancels the job on RejectedExecutionException; returning a
            // never-firing future would leave delay()/withTimeout() suspended forever.
            throw new RejectedExecutionException(mSchedulerHandler + " is shutting down");
        }
        return future;
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException(
                DelegatingScheduledExecutorService.class.getSimpleName()
                        + " cannot be shut down. Shut down the delegate executor instead.");
    }

    @Override
    public @NonNull List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException(
                DelegatingScheduledExecutorService.class.getSimpleName()
                        + " cannot be shut down. Shut down the delegate executor instead.");
    }

    @Override
    public boolean isShutdown() {
        return mDelegate instanceof ExecutorService && ((ExecutorService) mDelegate).isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return mDelegate instanceof ExecutorService && ((ExecutorService) mDelegate).isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) {
        throw new UnsupportedOperationException(
                DelegatingScheduledExecutorService.class.getSimpleName()
                        + " cannot be shut down. Shut down the delegate executor instead.");
    }

    private static long triggerTimeMillis(long delay, @NonNull TimeUnit unit) {
        long delayMillis = Math.max(0L, unit.toMillis(delay));
        long sum = SystemClock.uptimeMillis() + delayMillis;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }

    private final class DelegatingScheduledFuture<V> extends FutureTask<V>
            implements RunnableScheduledFuture<V> {

        private final long mTriggerTimeMillis;
        private final Runnable mTrigger = this::dispatchToDelegate;

        DelegatingScheduledFuture(@NonNull Runnable runnable, long triggerTimeMillis) {
            super(runnable, null);
            mTriggerTimeMillis = triggerTimeMillis;
        }

        DelegatingScheduledFuture(@NonNull Callable<V> callable, long triggerTimeMillis) {
            super(callable);
            mTriggerTimeMillis = triggerTimeMillis;
        }

        boolean postTrigger() {
            return mSchedulerHandler.postAtTime(mTrigger, mTriggerTimeMillis);
        }

        private void dispatchToDelegate() {
            if (isDone()) {
                return;
            }
            try {
                mDelegate.execute(this);
            } catch (RuntimeException e) {
                // Never let a rejection escape onto the scheduler thread, which would crash the
                // process. Fail the future instead so that waiters are released.
                setException(e);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (cancelled) {
                mSchedulerHandler.removeCallbacks(mTrigger);
            }
            return cancelled;
        }

        @Override
        public boolean isPeriodic() {
            return false;
        }

        @Override
        public long getDelay(@NonNull TimeUnit unit) {
            return unit.convert(
                    mTriggerTimeMillis - SystemClock.uptimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@NonNull Delayed other) {
            if (other == this) {
                return 0;
            }
            if (other instanceof DelegatingScheduledFuture<?>) {
                return Long.compare(
                        mTriggerTimeMillis,
                        ((DelegatingScheduledFuture<?>) other).mTriggerTimeMillis);
            }
            return Long.compare(
                    getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
        }
    }
}
