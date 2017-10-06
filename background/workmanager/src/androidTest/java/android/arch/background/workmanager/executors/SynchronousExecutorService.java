/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.arch.background.workmanager.executors;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A synchronous {@link ScheduledExecutorService}.
 */
public class SynchronousExecutorService implements ScheduledExecutorService {

    private boolean mShutdown;

    @NonNull
    @Override
    public ScheduledFuture<?> schedule(
            @NonNull Runnable command, long delay, @NonNull TimeUnit unit) {
        return doBlocking(command, delay, unit);
    }

    @NonNull
    @Override
    public <V> ScheduledFuture<V> schedule(
            @NonNull Callable<V> callable, long delay, @NonNull TimeUnit unit) {
        try {
            return doBlocking(callable, delay, unit);
        } catch (Exception e) {
            return new SynchronizedScheduledFuture<>();
        }
    }

    @NonNull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
            @NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
            @NonNull Runnable command, long initialDelay, long delay, @NonNull TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
        mShutdown = true;
    }

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        mShutdown = true;
        return new ArrayList<>(0);
    }

    @Override
    public boolean isShutdown() {
        return mShutdown;
    }

    @Override
    public boolean isTerminated() {
        return mShutdown;   // Assume all tasks have completed after shutdown.
    }

    @Override
    public boolean awaitTermination(
            long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return false;
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        try {
            return doBlocking(task, 0, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return new SynchronizedScheduledFuture<>();
        }
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Runnable task, T result) {
        return doBlocking(task, 0, TimeUnit.MILLISECONDS);
    }

    @NonNull
    @Override
    public Future<?> submit(@NonNull Runnable task) {
        return doBlocking(task, 0, TimeUnit.MILLISECONDS);
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(
            @NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit)
            throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(
            @NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(@NonNull Runnable command) {
        doBlocking(command, 0, TimeUnit.MILLISECONDS);
    }

    private <T> ScheduledFuture<T> doBlocking(Runnable command, long delay, TimeUnit unit) {
        try {
            long millis = unit.toMillis(delay);
            if (millis > 0L) {
                Thread.sleep(millis);
            }
        } catch (InterruptedException e) {
            // Do nothing.
        }
        command.run();
        return new SynchronizedScheduledFuture<>();
    }

    private <T> ScheduledFuture<T> doBlocking(Callable<T> command, long delay, TimeUnit unit)
            throws Exception {
        try {
            long millis = unit.toMillis(delay);
            if (millis > 0L) {
                Thread.sleep(millis);
            }
        } catch (InterruptedException e) {
            // Do nothing.
        }
        command.call();
        return new SynchronizedScheduledFuture<>();
    }

    private static class SynchronizedScheduledFuture<V> implements ScheduledFuture<V> {

        @Override
        public long getDelay(@NonNull TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(@NonNull Delayed o) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public V get(long timeout, @NonNull TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
