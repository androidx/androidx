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

package androidx.camera.core;

import android.os.Process;

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.CameraFactory;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A camera executor class that executes camera operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CameraExecutor implements Executor, ScheduledExecutorService {
    private static final String TAG = "CameraExecutor";
    private static final int DEFAULT_CORE_THREADS = 1;

    /**
     * Camera threads are given higher priority due to the realtime nature of camera operations.
     * These are utilized for camera device and capture session operations as well as their
     * respective callbacks. This is set to have slightly (1) lower priority than the display
     * rendering thread should have.
     */
    private static final int CAMERA_THREAD_PROCESS_PRIORITY =
            Process.THREAD_PRIORITY_DISPLAY + Process.THREAD_PRIORITY_LESS_FAVORABLE;
    private static final int CAMERA_THREAD_JAVA_PRIORITY = 7;

    private final Object mExecutorLock = new Object();
    @GuardedBy("mExecutorLock")
    private @NonNull ScheduledThreadPoolExecutor mThreadPoolExecutor = createExecutor();

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private static final String THREAD_NAME_STEM =
                CameraXThreads.TAG + "core_camera_%d";
        private final AtomicInteger mThreadId = new AtomicInteger(0);

        @SuppressWarnings("ThreadPriorityCheck") // call to t.setPriority
        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread t = new Thread(() -> {
                Process.setThreadPriority(CAMERA_THREAD_PROCESS_PRIORITY);
                runnable.run();
            });
            t.setPriority(CAMERA_THREAD_JAVA_PRIORITY);
            t.setName(
                    String.format(
                            Locale.US,
                            THREAD_NAME_STEM,
                            mThreadId.getAndIncrement()));
            return t;
        }
    };

    /**
     * Initialize the CameraExecutor.
     *
     * @param cameraFactory the cameraFactory which provides camera information.
     */
    void init(@NonNull CameraFactory cameraFactory) {
        Preconditions.checkNotNull(cameraFactory);

        ThreadPoolExecutor executor;
        synchronized (mExecutorLock) {
            if (mThreadPoolExecutor.isShutdown()) {
                mThreadPoolExecutor = createExecutor();
            }
            executor = mThreadPoolExecutor;
        }

        int cameraNumber = cameraFactory.getAvailableCameraIds().size();
        // According to the document of ThreadPoolExecutor, "If there are more than corePoolSize
        // but less than maximumPoolSize threads running, a new thread will be created only if
        // the queue is full."
        // Because we use LinkedBlockingQueue which is never full, we have to set max pool size
        // as core pool size to make the executor can serve n-task simultaneously.
        int corePoolSize = Math.max(1, cameraNumber);
        executor.setCorePoolSize(corePoolSize);
    }

    /**
     * De-initialize the CameraExecutor.
     */
    void deinit() {
        synchronized (mExecutorLock) {
            if (!mThreadPoolExecutor.isShutdown()) {
                mThreadPoolExecutor.shutdown();
            }
        }
    }

    /**
     * Executes the runnable.
     *
     * @param runnable the runnable
     */
    @Override
    public void execute(@NonNull Runnable runnable) {
        Preconditions.checkNotNull(runnable);

        synchronized (mExecutorLock) {
            mThreadPoolExecutor.execute(runnable);
        }
    }

    private static ScheduledThreadPoolExecutor createExecutor() {
        ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(
                DEFAULT_CORE_THREADS, THREAD_FACTORY);
        threadPoolExecutor.setKeepAliveTime(0, TimeUnit.MILLISECONDS);

        threadPoolExecutor.setRejectedExecutionHandler((runnable, executor) -> Logger.w(TAG,
                "A rejected execution occurred in CameraExecutor!"));

        return threadPoolExecutor;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.schedule(command, delay, unit);
        }
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.schedule(callable, delay, unit);
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
            TimeUnit unit) {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
        }
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
            long delay, TimeUnit unit) {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }
    }

    @Override
    public void shutdown() {
        synchronized (mExecutorLock) {
            mThreadPoolExecutor.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.shutdownNow();
        }
    }

    @Override
    public boolean isShutdown() {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.isShutdown();
        }
    }

    @Override
    public boolean isTerminated() {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.isTerminated();
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.awaitTermination(timeout, unit);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.submit(task);
        }
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.submit(task, result);
        }
    }

    @Override
    public Future<?> submit(Runnable task) {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.submit(task);
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.invokeAll(tasks);
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
            TimeUnit unit) throws InterruptedException {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.invokeAll(tasks, timeout, unit);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws ExecutionException, InterruptedException {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.invokeAny(tasks);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException {
        synchronized (mExecutorLock) {
            return mThreadPoolExecutor.invokeAny(tasks, timeout, unit);
        }
    }
}
