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

package androidx.javascriptengine;

import androidx.javascriptengine.common.SequencedExecutor;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Shared thread pool for use by the JavaScriptEngine library.
 *
 * This thread pool lives for the lifetime of the application.
 */
@ThreadSafe
final class ThreadPool {
    // The defaults of newCachedThreadPool are: corePoolSize=0, maximumPoolSize=Integer.MAX_VALUE,
    // keepAliveTime=60 seconds. We might want to consider tweaking these in future.
    private final ExecutorService mThreadPoolTaskExecutor =
            Executors.newCachedThreadPool(new ThreadFactory() {
                private final AtomicInteger mCount = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "JSE" + mCount.getAndIncrement());
                }
            });

    private static final ThreadPool sInstance = new ThreadPool();

    /**
     * Returns the singleton instance of the thread pool.
     */
    @NonNull
    public static ThreadPool getInstance() {
        return sInstance;
    }

    private ThreadPool() {}

    /**
     * Returns an Executor for background tasks.
     */
    @NonNull
    public Executor getExecutor() {
        return mThreadPoolTaskExecutor;
    }

    /**
     * Returns a new {@link SequencedExecutor} that executes on background threads.
     */
    @NonNull
    public SequencedExecutor createSequentialExecutor() {
        return new SequencedExecutor(mThreadPoolTaskExecutor);
    }
}
