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

package androidx.core.provider;

import static androidx.core.provider.SelfDestructiveThread.ReplyCallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.os.Process;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.GuardedBy;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tests for {@link SelfDestructiveThread}
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class SelfDestructiveThreadTest {
    private static final int DEFAULT_TIMEOUT = 1000;

    private void waitUntilDestruction(SelfDestructiveThread thread, long timeoutMs) {
        if (!thread.isRunning()) {
            return;
        }
        final long deadlineNanoTime =
                System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        final long timeSliceMs = 50;
        do {
            try {
                Thread.sleep(timeSliceMs);
            } catch (InterruptedException e) {
                // ignore.
            }
            if (!thread.isRunning()) {
                return;
            }
        } while (System.nanoTime() < deadlineNanoTime);
        throw new RuntimeException("Timeout for waiting thread destruction.");
    }

    private void waitMillis(long ms) {
        final long deadlineNanoTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ms);
        for (;;) {
            final long now = System.nanoTime();
            if (deadlineNanoTime < now) {
                return;
            }
            try {
                Thread.sleep(TimeUnit.NANOSECONDS.toMillis(deadlineNanoTime - now));
            } catch (InterruptedException e) {
                // ignore.
            }
        }
    }

    @Test
    public void testDestruction() throws InterruptedException {
        final int destructAfterLastActivityInMs = 300;
        final SelfDestructiveThread thread = new SelfDestructiveThread(
                "test", Process.THREAD_PRIORITY_BACKGROUND, destructAfterLastActivityInMs);
        thread.postAndWait(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        }, DEFAULT_TIMEOUT);
        waitUntilDestruction(thread, DEFAULT_TIMEOUT);
        assertFalse(thread.isRunning());
    }

    @Test
    public void testReconstruction() throws InterruptedException {
        final int destructAfterLastActivityInMs = 300;
        final SelfDestructiveThread thread = new SelfDestructiveThread(
                "test", Process.THREAD_PRIORITY_BACKGROUND, destructAfterLastActivityInMs);
        Integer generation = thread.postAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return thread.getGeneration();
            }
        }, DEFAULT_TIMEOUT);
        assertNotNull(generation);
        waitUntilDestruction(thread, DEFAULT_TIMEOUT);
        Integer nextGeneration = thread.postAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return thread.getGeneration();
            }
        }, DEFAULT_TIMEOUT);
        assertNotNull(nextGeneration);
        assertNotEquals(generation.intValue(), nextGeneration.intValue());
    }

    @Test
    public void testReuseSameThread() throws InterruptedException {
        final int destructAfterLastActivityInMs = 300;
        final SelfDestructiveThread thread = new SelfDestructiveThread(
                "test", Process.THREAD_PRIORITY_BACKGROUND, destructAfterLastActivityInMs);
        Integer generation = thread.postAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return thread.getGeneration();
            }
        }, DEFAULT_TIMEOUT);
        assertNotNull(generation);
        Integer nextGeneration = thread.postAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return thread.getGeneration();
            }
        }, DEFAULT_TIMEOUT);
        assertNotNull(nextGeneration);
        waitUntilDestruction(thread, DEFAULT_TIMEOUT);
        assertEquals(generation.intValue(), nextGeneration.intValue());
    }

    @LargeTest
    @Test
    public void testReuseSameThread_Multiple() throws InterruptedException {
        final int destructAfterLastActivityInMs = 300;
        final SelfDestructiveThread thread = new SelfDestructiveThread(
                "test", Process.THREAD_PRIORITY_BACKGROUND, destructAfterLastActivityInMs);
        Integer generation = thread.postAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return thread.getGeneration();
            }
        }, DEFAULT_TIMEOUT);
        assertNotNull(generation);
        int firstGeneration = generation.intValue();
        for (int i = 0; i < 10; ++i) {
            // Less than renewal duration, so that the same thread must be used.
            waitMillis(destructAfterLastActivityInMs / 2);
            Integer nextGeneration = thread.postAndWait(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return thread.getGeneration();
                }
            }, DEFAULT_TIMEOUT);
            assertNotNull(nextGeneration);
            assertEquals(firstGeneration, nextGeneration.intValue());
        }
        waitUntilDestruction(thread, DEFAULT_TIMEOUT);
    }

    @Test
    public void testTimeout() {
        final int destructAfterLastActivityInMs = 300;
        final SelfDestructiveThread thread = new SelfDestructiveThread(
                "test", Process.THREAD_PRIORITY_BACKGROUND, destructAfterLastActivityInMs);

        final int timeoutMs = 300;
        try {
            thread.postAndWait(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    waitMillis(timeoutMs * 3);  // Wait longer than timeout.
                    return new Object();
                }
            }, timeoutMs);
            fail();
        } catch (InterruptedException e) {
             // pass
        }
    }

    private class WaitableReplyCallback implements ReplyCallback<Integer> {
        private final ReentrantLock mLock = new ReentrantLock();
        private final Condition mCond = mLock.newCondition();

        @GuardedBy("mLock")
        private Integer mValue;

        private static final int NOT_STARTED = 0;
        private static final int WAITING = 1;
        private static final int FINISHED = 2;
        private static final int TIMEOUT = 3;
        @GuardedBy("mLock")
        int mState = NOT_STARTED;

        @Override
        public void onReply(Integer value) {
            mLock.lock();
            try {
                if (mState != TIMEOUT) {
                    mValue = value;
                    mState = FINISHED;
                }
                mCond.signalAll();
            } finally {
                mLock.unlock();
            }
        }

        public Integer waitUntil(long timeoutMillis) {
            mLock.lock();
            try {
                if (mState == FINISHED) {
                    return mValue;
                }
                mState = WAITING;
                long remaining = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
                while (mState == WAITING) {
                    try {
                        remaining = mCond.awaitNanos(remaining);
                    } catch (InterruptedException e) {
                        // Ignore.
                    }
                    if (mState == FINISHED) {
                        return mValue;
                    }
                    if (remaining <= 0) {
                        mState = TIMEOUT;
                        fail("Timeout");
                    }
                }
                throw new IllegalStateException("mState becomes unexpected state");
            } finally {
                mLock.unlock();
            }
        }
    }

    @Test
    public void testPostAndReply() {
        final int destructAfterLastActivityInMs = 300;
        final Integer expectedResult = 123;

        final Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return expectedResult;
            }
        };
        final WaitableReplyCallback reply = new WaitableReplyCallback();
        final SelfDestructiveThread thread = new SelfDestructiveThread(
                "test", Process.THREAD_PRIORITY_BACKGROUND, destructAfterLastActivityInMs);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                thread.postAndReply(callable, reply);
            }
        });

        assertEquals(expectedResult, reply.waitUntil(DEFAULT_TIMEOUT));
    }
}
