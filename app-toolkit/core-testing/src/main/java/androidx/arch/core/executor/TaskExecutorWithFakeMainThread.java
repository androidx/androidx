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

package androidx.arch.core.executor;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A TaskExecutor that has a real thread for main thread operations and can wait for execution etc.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TaskExecutorWithFakeMainThread extends TaskExecutor {
    private List<Throwable> mCaughtExceptions = Collections.synchronizedList(new ArrayList
            <Throwable>());

    private ExecutorService mIOService;

    private Thread mMainThread;
    private final int mIOThreadCount;

    private ExecutorService mMainThreadService =
            Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull final Runnable r) {
                    mMainThread = new LoggingThread(r);
                    return mMainThread;
                }
            });

    public TaskExecutorWithFakeMainThread(int ioThreadCount) {
        mIOThreadCount = ioThreadCount;
        mIOService = Executors.newFixedThreadPool(ioThreadCount, new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new LoggingThread(r);
            }
        });
    }

    @Override
    public void executeOnDiskIO(Runnable runnable) {
        mIOService.execute(runnable);
    }

    @Override
    public void postToMainThread(Runnable runnable) {
        // Tasks in SingleThreadExecutor are guaranteed to execute sequentially,
        // and no more than one task will be active at any given time.
        // So if we call this method from the main thread, new task will be scheduled,
        // which is equivalent to post.
        mMainThreadService.execute(runnable);
    }

    @Override
    public boolean isMainThread() {
        return Thread.currentThread() == mMainThread;
    }

    List<Throwable> getErrors() {
        return mCaughtExceptions;
    }

    @SuppressWarnings("SameParameterValue")
    void shutdown(int timeoutInSeconds) throws InterruptedException {
        mMainThreadService.shutdown();
        mIOService.shutdown();
        mMainThreadService.awaitTermination(timeoutInSeconds, TimeUnit.SECONDS);
        mIOService.awaitTermination(timeoutInSeconds, TimeUnit.SECONDS);
    }

    /**
     * Drains tasks at the given time limit
     * @param seconds Number of seconds to wait
     * @throws InterruptedException
     */
    public void drainTasks(int seconds) throws InterruptedException {
        if (isMainThread()) {
            throw new IllegalStateException();
        }
        final CountDownLatch enterLatch = new CountDownLatch(mIOThreadCount);
        final CountDownLatch exitLatch = new CountDownLatch(1);
        for (int i = 0; i < mIOThreadCount; i++) {
            executeOnDiskIO(new Runnable() {
                @Override
                public void run() {
                    enterLatch.countDown();
                    try {
                        exitLatch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        final CountDownLatch mainLatch = new CountDownLatch(1);
        postToMainThread(new Runnable() {
            @Override
            public void run() {
                mainLatch.countDown();
            }
        });
        if (!enterLatch.await(seconds, TimeUnit.SECONDS)) {
            throw new AssertionError("Could not drain IO tasks in " + seconds
                    + " seconds");
        }
        exitLatch.countDown();
        if (!mainLatch.await(seconds, TimeUnit.SECONDS)) {
            throw new AssertionError("Could not drain UI tasks in " + seconds
                    + " seconds");
        }
    }

    @SuppressWarnings("WeakerAccess")
    class LoggingThread extends Thread {
        LoggingThread(final Runnable target) {
            super(new Runnable() {
                @Override
                public void run() {
                    try {
                        target.run();
                    } catch (Throwable t) {
                        mCaughtExceptions.add(t);
                    }
                }
            });
        }
    }
}
