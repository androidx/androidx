/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Tests for the {@link SequencedExecutor} class.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class SequencedExecutorTest {
    private static final int TIMEOUT_SECONDS = 1;
    private static final int NUM_THREADS = 5;
    private ExecutorService mInnerExecutorSingleThread;
    private SequencedExecutor mSequencedExecutorSingleThread;
    private ExecutorService mInnerExecutorMultiThread;
    private SequencedExecutor mSequencedExecutorMultiThread;

    private static class MyTestingExecutor implements Executor {
        ArrayDeque<Consumer<Runnable>> mOnExecutes = new ArrayDeque<>();

        // Call before you expect execute() to be called.
        public MyTestingExecutor expectExecute(Consumer<Runnable> onExecute) {
            mOnExecutes.add(onExecute);
            return this;
        }

        @Override
        public void execute(Runnable runnable) {
            Consumer<Runnable> onExecute = mOnExecutes.poll();
            Assert.assertNotNull(onExecute);
            onExecute.accept(runnable);
        }
    }

    /**
     * Single threaded executor: up
     * Multi threaded executor: up
     */
    @Before
    public void setup() {
        mInnerExecutorSingleThread = Executors.newSingleThreadExecutor();
        mSequencedExecutorSingleThread = new SequencedExecutor(mInnerExecutorSingleThread);
        mInnerExecutorMultiThread = Executors.newFixedThreadPool(NUM_THREADS);
        mSequencedExecutorMultiThread = new SequencedExecutor(mInnerExecutorMultiThread);
    }

    /**
     * Single threaded executor: down
     * Multi threaded executor: down
     */
    @After
    public void teardown() throws Throwable {
        if (mInnerExecutorSingleThread != null) {
            mInnerExecutorSingleThread.shutdown();
            Assert.assertTrue("(Single thread) Inner executor did not terminate",
                    mInnerExecutorSingleThread.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        if (mInnerExecutorMultiThread != null) {
            mInnerExecutorMultiThread.shutdown();
            Assert.assertTrue("(Multi thread) Inner executor did not terminate",
                    mInnerExecutorMultiThread.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    /**
     * Verifies that tasks are run in the order they were posted.
     */
    @Test
    public void testExecuteSequence() throws Throwable {
        final int tasks = 1000;
        final ArrayDeque<Integer> executionOrder = new ArrayDeque<>();

        final CountDownLatch latch1 = new CountDownLatch(tasks);
        for (int i = 0; i < tasks; i++) {
            final int task = i;
            mSequencedExecutorSingleThread.execute(() -> {
                executionOrder.add(task);
                latch1.countDown();
            });
        }
        Assert.assertTrue("(Single thread) Tasks did not complete.",
                latch1.await(5 * TIMEOUT_SECONDS, TimeUnit.SECONDS));

        for (int i = 0; i < tasks; i++) {
            Assert.assertEquals("(Single thread) Task executed in wrong order",
                    (Integer) i, executionOrder.poll());
        }
        Assert.assertTrue(executionOrder.isEmpty());

        final CountDownLatch latch2 = new CountDownLatch(tasks);
        for (int i = 0; i < tasks; i++) {
            final int task = i;
            mSequencedExecutorMultiThread.execute(() -> {
                executionOrder.add(task);
                latch2.countDown();
            });
        }
        Assert.assertTrue("(Multi thread) Tasks did not complete.",
                latch2.await(5 * TIMEOUT_SECONDS, TimeUnit.SECONDS));

        for (int i = 0; i < tasks; i++) {
            Assert.assertEquals("(Multi thread) Task executed in wrong order",
                    (Integer) i, executionOrder.poll());
        }
        Assert.assertTrue(executionOrder.isEmpty());
    }

    /**
     * Verifies that a task can be posted from within a task.
     */
    @Test
    public void testExecuteFromTask() throws Throwable {
        final CountDownLatch latch1 = new CountDownLatch(1);
        mSequencedExecutorSingleThread.execute(() -> {
            mSequencedExecutorSingleThread.execute(() -> latch1.countDown());
        });
        Assert.assertTrue("(Single thread) Task did not complete.",
                latch1.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        final CountDownLatch latch2 = new CountDownLatch(1);
        mSequencedExecutorMultiThread.execute(() -> {
            mSequencedExecutorMultiThread.execute(() -> latch2.countDown());
        });
        Assert.assertTrue("(Multi thread) Task did not complete.",
                latch2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    /**
     * Verifies that a direct executor (e.g. Runnable:Run) is compatible with SequencedExecutor.
     */
    @Test
    public void testExecuteDirectExecutor() throws Throwable {
        SequencedExecutor sequencedExecutor = new SequencedExecutor(Runnable::run);
        CountDownLatch latch = new CountDownLatch(2);
        sequencedExecutor.execute(() -> {
            latch.countDown();
            sequencedExecutor.execute(() -> latch.countDown());
        });
        Assert.assertTrue("Tasks did not complete.",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    /**
     * Verifies that posting tasks to a shutdown executor fails the task at runtime.
     * Failing the task at runtime does not throw an error.
     */
    @Test
    @MediumTest
    public void testExecuteExecutorShutdown() throws Throwable {
        mInnerExecutorSingleThread.shutdown();
        mSequencedExecutorSingleThread.execute(() -> {});
    }

    /**
     * Verifies that queued tasks do not run after inner executor is shutdown.
     */
    @Test
    public void testDrainOnInnerExecutorShutdown() throws Throwable {
        MyTestingExecutor innerExecutor = new MyTestingExecutor()
                .expectExecute((task) -> {
                    task.run();
                }).expectExecute((task) -> {
                    throw new RejectedExecutionException();
                });
        SequencedExecutor sequencedExecutor = new SequencedExecutor(innerExecutor);

        final CountDownLatch latch1 = new CountDownLatch(1);
        sequencedExecutor.execute(() -> latch1.countDown());
        Assert.assertTrue("Task did not run before shutdown",
                latch1.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        final CountDownLatch latch2 = new CountDownLatch(1);
        sequencedExecutor.execute(() -> latch2.countDown());
        Assert.assertFalse("Task completed after shutdown",
                latch2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    /**
     * Verifies that the SequencedExecutor can accept new tasks after its queue has been emptied.
     */
    @Test
    public void testDrainOnInnerDrainedAndRestarted() throws Throwable {
        MyTestingExecutor innerExecutor = new MyTestingExecutor()
                .expectExecute((task) -> {
                    task.run();
                }).expectExecute((task) -> {
                    task.run();
                });
        SequencedExecutor sequencedExecutor = new SequencedExecutor(innerExecutor);

        final CountDownLatch latch1 = new CountDownLatch(1);
        sequencedExecutor.execute(() -> latch1.countDown());
        Assert.assertTrue("Task run before empty queue",
                latch1.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        final CountDownLatch latch2 = new CountDownLatch(1);
        sequencedExecutor.execute(() -> latch2.countDown());
        Assert.assertTrue("Task did not run after empty queue",
                latch2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }
}
