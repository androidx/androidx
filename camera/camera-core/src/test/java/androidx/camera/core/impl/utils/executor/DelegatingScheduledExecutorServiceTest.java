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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import android.os.Handler;
import android.os.HandlerThread;

import com.google.common.collect.Range;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Config.TARGET_SDK})
public class DelegatingScheduledExecutorServiceTest {

    private static final long DELAY_MS = 100;

    private final QueueingExecutor mDelegate = new QueueingExecutor();
    private HandlerThread mSchedulerThread;
    private Handler mSchedulerHandler;
    private ShadowLooper mSchedulerLooper;
    private ScheduledExecutorService mExecutor;

    @Before
    public void setUp() {
        mSchedulerThread = new HandlerThread("DelegatingSchedulerTest");
        mSchedulerThread.start();
        mSchedulerHandler = new Handler(mSchedulerThread.getLooper());
        mSchedulerLooper = shadowOf(mSchedulerThread.getLooper());
        mExecutor = CameraXExecutors.asScheduledExecutor(mDelegate, mSchedulerHandler);
    }

    @After
    public void tearDown() throws InterruptedException {
        mSchedulerThread.quitSafely();
        mSchedulerThread.join(1000);
    }

    @Test
    public void execute_runsOnDelegateWithoutPostingToScheduler() {
        AtomicInteger runCount = new AtomicInteger();

        mExecutor.execute(runCount::incrementAndGet);

        assertThat(hasPendingTimer()).isFalse();
        assertThat(mDelegate.runAll()).isEqualTo(1);
        assertThat(runCount.get()).isEqualTo(1);
    }

    @Test
    public void scheduleRunnable_dispatchesToDelegateOnlyWhenDelayExpires() {
        AtomicInteger runCount = new AtomicInteger();

        ScheduledFuture<?> future =
                mExecutor.schedule(runCount::incrementAndGet, DELAY_MS, TimeUnit.MILLISECONDS);

        assertThat(hasPendingTimer()).isTrue();
        mSchedulerLooper.idleFor(DELAY_MS - 1, TimeUnit.MILLISECONDS);
        assertThat(mDelegate.size()).isEqualTo(0);

        mSchedulerLooper.idleFor(1, TimeUnit.MILLISECONDS);
        // The timer only hands the task to the delegate; it never runs it on the scheduler.
        assertThat(runCount.get()).isEqualTo(0);
        assertThat(mDelegate.runAll()).isEqualTo(1);
        assertThat(runCount.get()).isEqualTo(1);
        assertThat(future.isDone()).isTrue();
    }

    @Test
    public void scheduleCallable_completesWithResult() throws Exception {
        ScheduledFuture<String> future =
                mExecutor.schedule(() -> "result", DELAY_MS, TimeUnit.MILLISECONDS);

        mSchedulerLooper.idleFor(DELAY_MS, TimeUnit.MILLISECONDS);
        mDelegate.runAll();

        assertThat(future.get()).isEqualTo("result");
    }

    @Test
    public void getDelay_isPositiveAndBasedOnUptime() {
        ScheduledFuture<?> future = mExecutor.schedule(() -> {}, DELAY_MS, TimeUnit.MILLISECONDS);

        assertThat(future.getDelay(TimeUnit.MILLISECONDS)).isIn(Range.openClosed(0L, DELAY_MS));

        mSchedulerLooper.idleFor(DELAY_MS, TimeUnit.MILLISECONDS);

        assertThat(future.getDelay(TimeUnit.MILLISECONDS)).isIn(Range.closed(-DELAY_MS, 0L));
    }

    @Test
    public void schedule_negativeDelay_isDispatchedImmediately() {
        AtomicInteger runCount = new AtomicInteger();

        mExecutor.schedule(runCount::incrementAndGet, -DELAY_MS, TimeUnit.MILLISECONDS);
        mSchedulerLooper.idle();

        assertThat(mDelegate.runAll()).isEqualTo(1);
        assertThat(runCount.get()).isEqualTo(1);
    }

    @Test
    public void schedule_hugeDelay_doesNotOverflowIntoImmediateRun() {
        AtomicInteger runCount = new AtomicInteger();

        ScheduledFuture<?> future =
                mExecutor.schedule(runCount::incrementAndGet, Long.MAX_VALUE, TimeUnit.DAYS);
        mSchedulerLooper.idle();

        assertThat(hasPendingTimer()).isTrue();
        assertThat(mDelegate.size()).isEqualTo(0);
        assertThat(runCount.get()).isEqualTo(0);
        assertThat(future.getDelay(TimeUnit.MILLISECONDS)).isGreaterThan(0L);
        future.cancel(false);
    }

    @Test
    public void cancel_removesPendingTimerFromScheduler() {
        ScheduledFuture<?> future = mExecutor.schedule(() -> {}, DELAY_MS, TimeUnit.MILLISECONDS);
        assertThat(hasPendingTimer()).isTrue();

        assertThat(future.cancel(false)).isTrue();

        assertThat(hasPendingTimer()).isFalse();
        mSchedulerLooper.idleFor(DELAY_MS, TimeUnit.MILLISECONDS);
        assertThat(mDelegate.size()).isEqualTo(0);
        assertThat(future.isCancelled()).isTrue();
    }

    @Test
    public void schedule_onQuitLooper_throwsRejectedExecutionException()
            throws InterruptedException {
        HandlerThread thread = new HandlerThread("QuitScheduler");
        thread.start();
        Handler quitHandler = new Handler(thread.getLooper());
        thread.quit();
        thread.join(1000);
        ScheduledExecutorService executor =
                CameraXExecutors.asScheduledExecutor(mDelegate, quitHandler);

        assertThrows(
                RejectedExecutionException.class,
                () -> executor.schedule(() -> {}, DELAY_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void schedule_onShutdownDelegateExecutorService_throwsRejectedExecutionException()
            throws InterruptedException {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        delegate.shutdown();
        delegate.awaitTermination(1, TimeUnit.SECONDS);
        ScheduledExecutorService executor =
                CameraXExecutors.asScheduledExecutor(delegate, mSchedulerHandler);

        assertThat(executor.isShutdown()).isTrue();
        assertThat(executor.isTerminated()).isTrue();
        assertThrows(
                RejectedExecutionException.class,
                () -> executor.schedule(() -> {}, DELAY_MS, TimeUnit.MILLISECONDS));
        assertThat(hasPendingTimer()).isFalse();
    }

    @Test
    public void delegateRejects_failsFutureWithoutCrashingScheduler() {
        ScheduledExecutorService executor =
                CameraXExecutors.asScheduledExecutor(
                        command -> {
                            throw new RejectedExecutionException("Delegate is shut down");
                        },
                        mSchedulerHandler);
        ScheduledFuture<?> future = executor.schedule(() -> {}, DELAY_MS, TimeUnit.MILLISECONDS);

        // An exception escaping onto the scheduler thread would be rethrown here.
        mSchedulerLooper.idleFor(DELAY_MS, TimeUnit.MILLISECONDS);

        assertThat(future.isDone()).isTrue();
        ExecutionException e = assertThrows(ExecutionException.class, future::get);
        assertThat(e).hasCauseThat().isInstanceOf(RejectedExecutionException.class);
    }

    @Test
    public void periodicScheduling_isUnsupported() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> mExecutor.scheduleAtFixedRate(() -> {}, 0, DELAY_MS, TimeUnit.MILLISECONDS));
        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        mExecutor.scheduleWithFixedDelay(
                                () -> {}, 0, DELAY_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shutdown_isUnsupported() {
        assertThat(mExecutor.isShutdown()).isFalse();
        assertThat(mExecutor.isTerminated()).isFalse();
        assertThrows(UnsupportedOperationException.class, mExecutor::shutdown);
        assertThrows(UnsupportedOperationException.class, mExecutor::shutdownNow);
        assertThrows(
                UnsupportedOperationException.class,
                () -> mExecutor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void asScheduledExecutor_returnsExistingScheduledExecutorAsIs() {
        assertThat(CameraXExecutors.asScheduledExecutor(mExecutor, mSchedulerHandler))
                .isSameInstanceAs(mExecutor);
    }

    @Test
    public void asScheduledExecutor_wrapsPlainExecutor() {
        ScheduledExecutorService executor =
                CameraXExecutors.asScheduledExecutor(mDelegate, mSchedulerHandler);
        AtomicInteger runCount = new AtomicInteger();

        executor.schedule(runCount::incrementAndGet, DELAY_MS, TimeUnit.MILLISECONDS);
        mSchedulerLooper.idleFor(DELAY_MS, TimeUnit.MILLISECONDS);

        assertThat(mDelegate.runAll()).isEqualTo(1);
        assertThat(runCount.get()).isEqualTo(1);
    }

    private boolean hasPendingTimer() {
        // Callbacks posted to a Handler are stored as messages with what == 0.
        return mSchedulerHandler.hasMessages(0);
    }

    /** An {@link Executor} that queues tasks until explicitly run by the test. */
    private static final class QueueingExecutor implements Executor {
        private final Queue<Runnable> mTasks = new ArrayDeque<>();

        @Override
        public void execute(@NonNull Runnable command) {
            mTasks.add(command);
        }

        int size() {
            return mTasks.size();
        }

        /** Runs all queued tasks and returns how many were run. */
        int runAll() {
            int count = 0;
            Runnable task;
            while ((task = mTasks.poll()) != null) {
                task.run();
                count++;
            }
            return count;
        }
    }
}
