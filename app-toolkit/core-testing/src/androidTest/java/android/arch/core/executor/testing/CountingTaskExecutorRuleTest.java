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

package android.arch.core.executor.testing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.core.executor.AppToolkitTaskExecutor;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class CountingTaskExecutorRuleTest {
    private final Semaphore mOnIdleCount = new Semaphore(0);

    @Rule
    public CountingTaskExecutorRule mRule = new CountingTaskExecutorRule() {
        @Override
        protected void onIdle() {
            super.onIdle();
            mOnIdleCount.release(1);
        }
    };

    @Test
    public void initialIdle() {
        assertThat(mRule.isIdle(), is(true));
    }

    @Test
    public void busyIO() throws InterruptedException {
        LatchRunnable task = runOnIO();
        singleTaskTest(task);
    }

    @Test
    public void busyMain() throws InterruptedException {
        LatchRunnable task = runOnMain();
        singleTaskTest(task);
    }

    @Test
    public void multipleTasks() throws InterruptedException {
        List<LatchRunnable> latches = new ArrayList<>(10);
        for (int i = 0; i < 5; i++) {
            latches.add(runOnIO());
            latches.add(runOnMain());
        }
        assertNotIdle();
        for (int i = 0; i < 9; i++) {
            latches.get(i).start();
        }
        for (int i = 0; i < 9; i++) {
            latches.get(i).await();
        }
        assertNotIdle();

        LatchRunnable another = runOnIO();
        latches.get(9).startAndFinish();
        assertNotIdle();

        another.startAndFinish();
        assertBecomeIdle();

        LatchRunnable oneMore = runOnMain();

        assertNotIdle();

        oneMore.startAndFinish();
        assertBecomeIdle();
    }

    private void assertNotIdle() throws InterruptedException {
        assertThat(mOnIdleCount.tryAcquire(300, TimeUnit.MILLISECONDS), is(false));
        assertThat(mRule.isIdle(), is(false));
    }

    private void assertBecomeIdle() throws InterruptedException {
        assertThat(mOnIdleCount.tryAcquire(1, TimeUnit.SECONDS), is(true));
        assertThat(mRule.isIdle(), is(true));
    }

    private void singleTaskTest(LatchRunnable task)
            throws InterruptedException {
        assertNotIdle();
        task.startAndFinish();
        assertBecomeIdle();
    }

    private LatchRunnable runOnIO() {
        LatchRunnable latchRunnable = new LatchRunnable();
        AppToolkitTaskExecutor.getInstance().executeOnDiskIO(latchRunnable);
        return latchRunnable;
    }

    private LatchRunnable runOnMain() {
        LatchRunnable latchRunnable = new LatchRunnable();
        AppToolkitTaskExecutor.getInstance().executeOnMainThread(latchRunnable);
        return latchRunnable;
    }

    @Test
    public void drainFailure() throws InterruptedException {
        runOnIO();
        try {
            mRule.drainTasks(300, TimeUnit.MILLISECONDS);
            throw new AssertionError("drain should fail");
        } catch (TimeoutException ignored) {
        }
    }

    @Test
    public void drainSuccess() throws TimeoutException, InterruptedException {
        final LatchRunnable task = runOnIO();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                }
                task.start();
            }
        }).start();
        mRule.drainTasks(1, TimeUnit.SECONDS);
    }

    private static class LatchRunnable implements Runnable {
        private final CountDownLatch mStart = new CountDownLatch(1);
        private final CountDownLatch mEnd = new CountDownLatch(1);

        @Override
        public void run() {
            try {
                mStart.await(10, TimeUnit.SECONDS);
                mEnd.countDown();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }

        void await() throws InterruptedException {
            mEnd.await(10, TimeUnit.SECONDS);
        }

        void start() {
            mStart.countDown();
        }

        private void startAndFinish() throws InterruptedException {
            start();
            await();
        }
    }
}
