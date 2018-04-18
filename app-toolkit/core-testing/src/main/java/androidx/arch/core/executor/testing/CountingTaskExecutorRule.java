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

package androidx.arch.core.executor.testing;

import android.os.SystemClock;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.DefaultTaskExecutor;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A JUnit Test Rule that swaps the background executor used by the Architecture Components with a
 * different one which counts the tasks as they are start and finish.
 * <p>
 * You can use this rule for your host side tests that use Architecture Components.
 */
public class CountingTaskExecutorRule extends TestWatcher {
    private final Object mCountLock = new Object();
    private int mTaskCount = 0;

    @Override
    protected void starting(Description description) {
        super.starting(description);
        ArchTaskExecutor.getInstance().setDelegate(new DefaultTaskExecutor() {
            @Override
            public void executeOnDiskIO(Runnable runnable) {
                super.executeOnDiskIO(new CountingRunnable(runnable));
            }

            @Override
            public void postToMainThread(Runnable runnable) {
                super.postToMainThread(new CountingRunnable(runnable));
            }
        });
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    private void increment() {
        synchronized (mCountLock) {
            mTaskCount++;
        }
    }

    private void decrement() {
        synchronized (mCountLock) {
            mTaskCount--;
            if (mTaskCount == 0) {
                onIdle();
                mCountLock.notifyAll();
            }
        }
    }

    /**
     * Called when the number of awaiting tasks reaches to 0.
     *
     * @see #isIdle()
     */
    protected void onIdle() {

    }

    /**
     * Returns false if there are tasks waiting to be executed, true otherwise.
     *
     * @return False if there are tasks waiting to be executed, true otherwise.
     *
     * @see #onIdle()
     */
    public boolean isIdle() {
        synchronized (mCountLock) {
            return mTaskCount == 0;
        }
    }

    /**
     * Waits until all active tasks are finished.
     *
     * @param time The duration to wait
     * @param timeUnit The time unit for the {@code time} parameter
     *
     * @throws InterruptedException If thread is interrupted while waiting
     * @throws TimeoutException If tasks cannot be drained at the given time
     */
    public void drainTasks(int time, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException {
        long end = SystemClock.uptimeMillis() + timeUnit.toMillis(time);
        synchronized (mCountLock) {
            while (mTaskCount != 0) {
                long now = SystemClock.uptimeMillis();
                long remaining = end - now;
                if (remaining > 0) {
                    mCountLock.wait(remaining);
                } else {
                    throw new TimeoutException("could not drain tasks");
                }
            }
        }
    }

    class CountingRunnable implements Runnable {
        final Runnable mWrapped;

        CountingRunnable(Runnable wrapped) {
            mWrapped = wrapped;
            increment();
        }

        @Override
        public void run() {
            try {
                mWrapped.run();
            } finally {
                decrement();
            }
        }
    }
}
