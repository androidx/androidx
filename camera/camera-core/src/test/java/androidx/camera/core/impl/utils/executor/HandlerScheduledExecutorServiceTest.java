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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Config.TARGET_SDK})
public class HandlerScheduledExecutorServiceTest {

    private static final long DELAY_MS = 100;

    private HandlerThread mHandlerThread;
    private ShadowLooper mShadowLooper;
    private ScheduledExecutorService mExecutor;

    @Before
    public void setUp() {
        mHandlerThread = new HandlerThread("HandlerScheduledExecutorTest");
        mHandlerThread.start();
        Handler handler = new Handler(mHandlerThread.getLooper());
        mShadowLooper = shadowOf(mHandlerThread.getLooper());
        mShadowLooper.pause();
        mExecutor = CameraXExecutors.newHandlerExecutor(handler);
    }

    @After
    public void tearDown() throws InterruptedException {
        mShadowLooper.unPause();
        mHandlerThread.quitSafely();
        mHandlerThread.join(1000);
    }

    @Test
    public void getDelay_isPositiveUptimeBasedDelayBeforeDeadline() {
        ScheduledFuture<?> future = mExecutor.schedule(() -> {}, DELAY_MS, TimeUnit.MILLISECONDS);

        assertThat(future.getDelay(TimeUnit.MILLISECONDS)).isIn(Range.openClosed(0L, DELAY_MS));
        assertThat(future.getDelay(TimeUnit.NANOSECONDS)).isGreaterThan(0L);

        mShadowLooper.idleFor(DELAY_MS, TimeUnit.MILLISECONDS);

        assertThat(future.getDelay(TimeUnit.MILLISECONDS)).isIn(Range.closed(-DELAY_MS, 0L));
        assertThat(future.isDone()).isTrue();
    }

    @Test
    public void schedule_negativeDelay_clampsWithoutOvertakingQueuedExecuteTasks() {
        mShadowLooper.idleFor(DELAY_MS * 2, TimeUnit.MILLISECONDS);
        List<String> executionOrder = new ArrayList<>();

        mExecutor.execute(() -> executionOrder.add("execute"));
        mExecutor.schedule(
                () -> executionOrder.add("negativeSchedule"), -DELAY_MS, TimeUnit.MILLISECONDS);
        mShadowLooper.idle();

        assertThat(executionOrder).containsExactly("execute", "negativeSchedule").inOrder();
    }

    @Test
    public void schedule_hugeDelay_doesNotOverflowIntoImmediateRunAndCancelsCleanly() {
        AtomicInteger runCount = new AtomicInteger();

        ScheduledFuture<?> future =
                mExecutor.schedule(runCount::incrementAndGet, Long.MAX_VALUE, TimeUnit.DAYS);
        mShadowLooper.idle();

        assertThat(runCount.get()).isEqualTo(0);
        assertThat(future.getDelay(TimeUnit.MILLISECONDS)).isGreaterThan(0L);
        assertThat(future.cancel(false)).isTrue();
        assertThat(future.isCancelled()).isTrue();

        mShadowLooper.idleFor(1, TimeUnit.DAYS);
        assertThat(runCount.get()).isEqualTo(0);
    }

    @Test
    public void compareTo_ordersSameTypeAndExternalDelayedConsistently() {
        ScheduledFuture<?> sooner = mExecutor.schedule(() -> {}, DELAY_MS, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> later =
                mExecutor.schedule(() -> {}, DELAY_MS * 2, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> clampedMax =
                mExecutor.schedule(() -> {}, Long.MAX_VALUE, TimeUnit.DAYS);

        try {
            assertThat(sooner.compareTo(sooner)).isEqualTo(0);
            assertThat(clampedMax.compareTo(clampedMax)).isEqualTo(0);

            assertThat(sooner.compareTo(later)).isLessThan(0);
            assertThat(later.compareTo(sooner)).isGreaterThan(0);
            assertThat(later.compareTo(clampedMax)).isLessThan(0);
            assertThat(clampedMax.compareTo(later)).isGreaterThan(0);

            Delayed middleExternal = fixedMillisDelayed(DELAY_MS + 50);
            assertThat(sooner.compareTo(middleExternal)).isLessThan(0);
            assertThat(later.compareTo(middleExternal)).isGreaterThan(0);

            Delayed halfMaxExternal = fixedMillisDelayed(Long.MAX_VALUE / 2);
            assertThat(clampedMax.compareTo(halfMaxExternal)).isGreaterThan(0);
        } finally {
            sooner.cancel(false);
            later.cancel(false);
            clampedMax.cancel(false);
        }
    }

    @Test
    public void schedule_onQuitLooper_returnsFailedScheduledFuture() throws InterruptedException {
        HandlerThread quitThread = new HandlerThread("QuitHandlerScheduledExecutorTest");
        quitThread.start();
        Handler quitHandler = new Handler(quitThread.getLooper());
        quitThread.quit();
        quitThread.join(1000);
        ScheduledExecutorService executor = CameraXExecutors.newHandlerExecutor(quitHandler);

        ScheduledFuture<?> future = executor.schedule(() -> {}, DELAY_MS, TimeUnit.MILLISECONDS);

        assertThat(future.isDone()).isTrue();
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertThat(exception).hasCauseThat().isInstanceOf(RejectedExecutionException.class);
    }

    @Test
    public void shutdown_throwsUnsupportedOperationException() {
        assertThat(mExecutor.isShutdown()).isFalse();
        assertThat(mExecutor.isTerminated()).isFalse();
        assertThrows(UnsupportedOperationException.class, mExecutor::shutdown);
        assertThrows(UnsupportedOperationException.class, mExecutor::shutdownNow);
    }

    private static Delayed fixedMillisDelayed(long delayMillis) {
        return new Delayed() {
            @Override
            public long getDelay(@NonNull TimeUnit unit) {
                return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
            }

            @Override
            public int compareTo(@NonNull Delayed other) {
                return Long.compare(
                        getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
            }
        };
    }
}
