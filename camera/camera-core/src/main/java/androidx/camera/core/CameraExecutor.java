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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraFactory;
import androidx.core.util.Preconditions;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A camera executor class that executes camera operations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CameraExecutor implements Executor {
    private static final String TAG = "CameraExecutor";
    private static final int DEFAULT_CORE_THREADS = 1;
    private static final int DEFAULT_MAX_THREADS = DEFAULT_CORE_THREADS;

    private final Object mExecutorLock = new Object();
    @GuardedBy("mExecutorLock")
    @NonNull
    private ThreadPoolExecutor mThreadPoolExecutor = createExecutor();

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private static final String THREAD_NAME_STEM =
                CameraXThreads.TAG + "core_camera_%d";
        private final AtomicInteger mThreadId = new AtomicInteger(0);

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread t = new Thread(runnable);
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
        executor.setMaximumPoolSize(corePoolSize);
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

    private static ThreadPoolExecutor createExecutor() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(DEFAULT_CORE_THREADS,
                DEFAULT_MAX_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                THREAD_FACTORY);

        threadPoolExecutor.setRejectedExecutionHandler((runnable, executor) -> Logger.e(TAG,
                "A rejected execution occurred in CameraExecutor!"));

        return threadPoolExecutor;
    }
}
