/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import android.graphics.Matrix
import android.graphics.Path
import android.os.Build
import android.view.MotionEvent
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.authoring.latency.latencyDataEqual
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushTip
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.geometry.MutableBox
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlin.test.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [InProgressStrokesManager].
 *
 * In production, there are two threads: the UI thread and the render thread. Client calls to
 * [InProgressStrokesManager] happen on the UI thread. The manager interacts through the lower-level
 * rendering system via the [InProgressStrokesRenderHelper], which makes callbacks into the manager
 * on the render thread.
 *
 * In contrast, in these tests we have just one thread: the one that each test runs in. So the test
 * setup functions configure the manager and the render helper to work on that single thread, either
 * by running async requests synchronously, or by queueing those async requests for the test code to
 * run at a specific time of the test.
 */
@OptIn(ExperimentalInkCustomBrushApi::class, ExperimentalLatencyDataApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(
    minSdkVersion = Build.VERSION_CODES.N, // Mockito expects Java Stream
    maxSdkVersion = Build.VERSION_CODES.TIRAMISU, // Mockito crash on Jetpack API 34 emulator
)
internal class InProgressStrokesManagerTest {
    private val inProgressStrokesRenderHelper = mock<InProgressStrokesRenderHelper> {}

    /**
     * Returns an [InProgressStrokesManager] instance that runs callbacks synchronously on the
     * test's single thread.
     */
    private fun makeSynchronousInProgressStrokesManager(
        latencyDataRecorder: LatencyDataRecorder,
        clock: FakeClock,
        inProgressStrokePool: FakeInProgressStrokePool = FakeInProgressStrokePool(),
    ): InProgressStrokesManager {
        return InProgressStrokesManager(
            inProgressStrokesRenderHelper,
            // In production, the manager calls postOnAnimation/postToUiThread from the render
            // thread to
            // run callbacks on the UI thread. However, in these tests, the caller of these will be
            // in the
            // single test thread, and we will just run each callback synchronously.
            postOnAnimation = Runnable::run,
            postToUiThread = Runnable::run,
            latencyDataCallback = { data: LatencyData -> latencyDataRecorder.record(data) },
            getNanoTime = { clock.getNextTime() },
            inProgressStrokePool = inProgressStrokePool,
        )
    }

    /**
     * Creates the [InProgressStrokesManager] under test in a way that can simulate its
     * multi-threaded implementation in a single-threaded test. More complex test scenarios will
     * require this approach, but simpler scenarios may be able to use
     * [makeSynchronousInProgressStrokesManager].
     *
     * @return
     *     1. The [InProgressStrokesManager].
     *     2. The [AsyncRenderHelper], which can be used to run the render thread to idle.
     *     3. A function to run the UI thread to the next frame.
     */
    private fun makeAsyncManager(
        latencyDataRecorder: LatencyDataRecorder,
        clock: FakeClock,
        inProgressStrokePool: FakeInProgressStrokePool = FakeInProgressStrokePool(),
    ): Triple<InProgressStrokesManager, FakeAsyncRenderHelper, () -> Boolean> {
        lateinit var manager: InProgressStrokesManager
        // Indirection due to an indirect circular dependency between the manager and the render
        // helper.
        val callback =
            object : InProgressStrokesRenderHelper.Callback {
                override fun onDraw() = manager.onDraw()

                override fun onDrawComplete() = manager.onDrawComplete()

                override fun reportEstimatedPixelPresentationTime(timeNanos: Long) =
                    manager.reportEstimatedPixelPresentationTime(timeNanos)

                override fun setCustomLatencyDataField(setter: (LatencyData, Long) -> Unit) =
                    manager.setCustomLatencyDataField(setter)

                override fun handOffAllLatencyData() = manager.handOffAllLatencyData()

                override fun setPauseStrokeCohortHandoffs(paused: Boolean) =
                    manager.setPauseStrokeCohortHandoffs(paused)

                override fun onStrokeCohortHandoffToHwui(
                    strokeCohort: Map<InProgressStrokeId, FinishedStroke>
                ) = manager.onStrokeCohortHandoffToHwui(strokeCohort)

                override fun onStrokeCohortHandoffToHwuiComplete() =
                    manager.onStrokeCohortHandoffToHwuiComplete()
            }
        val renderHelper = FakeAsyncRenderHelper(callback, clock)
        val uiThreadRunnables = mutableListOf<Runnable>()
        val onAnimationRunnables = mutableListOf<Runnable>()
        val runUiThreadToNextFrame = {
            var ranAny = false
            // Run through next onAnimation.
            if (uiThreadRunnables.isNotEmpty() || onAnimationRunnables.isNotEmpty()) {
                // uiThreadRunnables executing may add more to the list, or to onAnimationRunnables.
                while (uiThreadRunnables.isNotEmpty()) {
                    ranAny = true
                    uiThreadRunnables.removeAt(0).run()
                }
                // Only run the onAnimationRunnables that are currently present, as a newly posted
                // runnable
                // would run at the next onAnimation.
                if (onAnimationRunnables.isNotEmpty()) {
                    ranAny = true
                    val onAnimation = onAnimationRunnables.toList()
                    onAnimationRunnables.clear()
                    onAnimation.forEach(Runnable::run)
                }
            }
            ranAny
        }
        manager =
            InProgressStrokesManager(
                renderHelper,
                // In production, the manager calls postOnAnimation/postToUiThread from the render
                // thread to
                // run callbacks on the UI thread. However, in these tests, there is only a single
                // test
                // thread, so save the callbacks to run later to simulate the UI thread being
                // scheduled.
                postOnAnimation = { onAnimationRunnables.add(it) },
                postToUiThread = { uiThreadRunnables.add(it) },
                latencyDataCallback = { data: LatencyData -> latencyDataRecorder.record(data) },
                getNanoTime = { clock.getNextTime() },
                inProgressStrokePool = inProgressStrokePool,
                blockingAwait = { latch, _, _ ->
                    runToIdle(runUiThreadToNextFrame, renderHelper::runRenderThreadToIdle)
                    // Expect that the latch will have counted down during the execution.
                    latch.count == 0L
                },
            )
        return Triple(manager, renderHelper, runUiThreadToNextFrame)
    }

    private fun runToIdle(
        runUiThreadToIdle: () -> Boolean,
        runRenderThreadToIdle: () -> Boolean,
    ): Boolean {
        var ranAny = false
        while (runUiThreadToIdle() || runRenderThreadToIdle()) {
            ranAny = true
        }
        return ranAny
    }

    private fun makeAsyncInProgressStrokesManager(
        latencyDataRecorder: LatencyDataRecorder,
        clock: FakeClock,
        inProgressStrokePool: FakeInProgressStrokePool = FakeInProgressStrokePool(),
    ): InProgressStrokesManager {
        val uiThreadRunnables = mutableListOf<Runnable>()
        val onAnimationRunnables = mutableListOf<Runnable>()
        return InProgressStrokesManager(
            inProgressStrokesRenderHelper,
            // In production, the manager calls postOnAnimation/postToUiThread from the render
            // thread to
            // run callbacks on the UI thread. However, in these tests, there is only a single test
            // thread, so save the callbacks to run later to simulate the UI thread being scheduled.
            postOnAnimation = { onAnimationRunnables.add(it) },
            postToUiThread = { uiThreadRunnables.add(it) },
            latencyDataCallback = { data: LatencyData -> latencyDataRecorder.record(data) },
            getNanoTime = { clock.getNextTime() },
            inProgressStrokePool = inProgressStrokePool,
            blockingAwait = { latch, _, _ ->
                while (uiThreadRunnables.isNotEmpty() || onAnimationRunnables.isNotEmpty()) {
                    // uiThreadRunnables executing may add more to the list, or ot
                    // onAnimationRunnables.
                    while (uiThreadRunnables.isNotEmpty()) {
                        uiThreadRunnables.removeAt(0).run()
                    }
                    while (onAnimationRunnables.isNotEmpty()) {
                        onAnimationRunnables.removeAt(0).run()
                    }
                    // onAnimationRunnables might have refilled uiThreadRunnables, so try those
                    // again until
                    // both lists are empty.
                }
                // Expect that the latch will have counted down during the
                latch.count == 0L
            },
        )
    }

    /**
     * Sets up the mock [InProgressStrokesRenderHelper] to synchronously call back into the manager
     * on the test's single thread.
     *
     * In production, the manager calls helper.requestDraw from the UI thread, which eventually
     * results in some work in the helper on the render thread. As part of this work, the helper
     * makes a synchronous sequence of calls into the manager: onDraw, onDrawComplete, and various
     * latency tracking calls for the end of drawing. Here we set up the mock instance to run these
     * calls immediately and synchronously on the test's single thread.
     */
    private fun setUpMockInProgressStrokesRenderHelperForSynchronousOperation(
        manager: InProgressStrokesManager,
        clock: FakeClock,
    ) {
        whenever(inProgressStrokesRenderHelper.requestDraw()).then {
            manager.onDraw()
            manager.onDrawComplete()
            manager.setCustomLatencyDataField({ data: LatencyData, timeNanos: Long ->
                data.canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = timeNanos
            })
            manager.reportEstimatedPixelPresentationTime(clock.getNextTime())
            manager.handOffAllLatencyData()
        }
    }

    @Test
    fun latencyDataCallback_getsLatencyDataForPenDown() {
        val latencyDataRecorder = LatencyDataRecorder()
        // Arbitrary start time; there are no checks for consistency between MotionEvents and the
        // clock.
        val clock = FakeClock(777_000L)
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        // Set the pen down at t=321ms. Note that LatencyData results will show this as
        // 321_000_000ns.
        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)

        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )

        assertThat(latencyDataRecorder.recordedData)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(
                listOf(
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.START
                        eventAction = LatencyData.EventAction.DOWN
                        batchSize = 1
                        batchIndex = 0
                        osDetectsEvent = 321_000_000
                        // The clock ticked once to record this time.
                        strokesViewGetsAction = 777_001
                        // And twice more for this one.
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        // And once more for this one.
                        estimatedPixelPresentationTime = 777_004
                    }
                )
            )
    }

    @Test
    fun latencyDataCallback_getsLatencyDataForMove() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        // Set the pen down at t=321ms.
        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        // We already checked the reported LatencyData in a previous test. The clock ticked once.
        latencyDataRecorder.recordedData.clear()
        // Reset the fake clock, since we only care about latency reports from here onward. The
        // specific
        // time doesn't matter; there are no checks for consistency between MotionEvents and the
        // clock.
        clock.timeNanos = 777_000L

        // Now move the pen at t=325ms and t=329ms.
        val moveEvent1 = MotionEvent.obtain(321, 325, MotionEvent.ACTION_MOVE, 12f, 22f, 0)
        val moveEvent2 = MotionEvent.obtain(321, 329, MotionEvent.ACTION_MOVE, 12f, 22f, 0)

        manager.addToStroke(moveEvent1, moveEvent1.getPointerId(0), inProgressStrokeId, null)
        manager.addToStroke(moveEvent2, moveEvent2.getPointerId(0), inProgressStrokeId, null)

        assertThat(latencyDataRecorder.recordedData)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(
                listOf(
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.ADD
                        eventAction = LatencyData.EventAction.MOVE
                        batchSize = 1
                        batchIndex = 0
                        osDetectsEvent = 325_000_000
                        // The clock ticked once to record this time.
                        strokesViewGetsAction = 777_001
                        // And twice for this one - once on the UI thread and once on the render
                        // thread.
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        // And once more for this one.
                        estimatedPixelPresentationTime = 777_004
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.ADD
                        eventAction = LatencyData.EventAction.MOVE
                        batchSize = 1
                        batchIndex = 0
                        osDetectsEvent = 329_000_000
                        // The clock ticked again to get this time.
                        strokesViewGetsAction = 777_005
                        // And twice for this one - once on the UI thread and once on the render
                        // thread.
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_007
                        // And once more for this one.
                        estimatedPixelPresentationTime = 777_008
                    },
                )
            )
    }

    @Test
    fun latencyDataCallback_getsLatencyDataForBatchedMove() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        // Set the pen down at t=321ms.
        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        // We already checked the reported LatencyData in a previous test. The clock ticked once.
        latencyDataRecorder.recordedData.clear()
        // Reset the fake clock, since we only care about latency reports from here onward. The
        // specific
        // time doesn't matter; there are no checks for consistency between MotionEvents and the
        // clock.
        clock.timeNanos = 777_000L

        // Now move the pen at t=325ms and t=329ms. The two events got batched together.
        val moveEvent = MotionEvent.obtain(321, 325, MotionEvent.ACTION_MOVE, 12f, 22f, 0)
        moveEvent.addBatch(329, 14f, 24f, 1f, 1f, 0)

        manager.addToStroke(moveEvent, moveEvent.getPointerId(0), inProgressStrokeId, null)

        assertThat(latencyDataRecorder.recordedData)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(
                listOf(
                    // Since both MOVE events were sent in one call, they were processed together.
                    // So there
                    // was just one clock tick to get the draw call finish time, and one other for
                    // the
                    // estimated pixel presentation time.
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.ADD
                        eventAction = LatencyData.EventAction.MOVE
                        batchSize = 2
                        batchIndex = 0
                        osDetectsEvent = 325_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.ADD
                        eventAction = LatencyData.EventAction.MOVE
                        batchSize = 2
                        batchIndex = 1
                        osDetectsEvent = 329_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                )
            )
    }

    @Test
    fun latencyDataCallback_getsLatencyDataForPredictedMove() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        // Set the pen down at t=321ms.
        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        // We already checked the reported LatencyData in a previous test.
        latencyDataRecorder.recordedData.clear()
        // Reset the fake clock, since we only care about latency reports from here onward. The
        // specific
        // time doesn't matter; there are no checks for consistency between MotionEvents and the
        // clock.
        clock.timeNanos = 777_000L

        // Now move the pen at t=325ms. The platform predicted events for t=329ms and t=333ms.
        val realMoveEvent = MotionEvent.obtain(321, 325, MotionEvent.ACTION_MOVE, 12f, 22f, 0)
        val predictedMoveEvent = MotionEvent.obtain(321, 329, MotionEvent.ACTION_MOVE, 16f, 26f, 0)
        predictedMoveEvent.addBatch(333, 18f, 28f, 1f, 1f, 0)

        manager.addToStroke(
            realMoveEvent,
            realMoveEvent.getPointerId(0),
            inProgressStrokeId,
            predictedMoveEvent,
        )

        // Now we should have latency data for all three input events.
        assertThat(latencyDataRecorder.recordedData)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(
                listOf(
                    // Since all MOVE events (one real, two predicted) were sent in one call, they
                    // were
                    // processed together. So there was just one clock tick to get the draw call
                    // finish time,
                    // and one other for the estimated pixel presentation time.
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.ADD
                        eventAction = LatencyData.EventAction.MOVE
                        batchSize = 1
                        batchIndex = 0
                        osDetectsEvent = 325_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                        eventAction = LatencyData.EventAction.PREDICTED_MOVE
                        batchSize = 2
                        batchIndex = 0
                        osDetectsEvent = 329_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                        eventAction = LatencyData.EventAction.PREDICTED_MOVE
                        batchSize = 2
                        batchIndex = 1
                        osDetectsEvent = 333_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                )
            )
    }

    @Test
    fun latencyDataCallback_getsLatencyDataForBatchedAndPredictedMove() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        // Set the pen down at t=321ms.
        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        // We already checked the reported LatencyData in a previous test.
        latencyDataRecorder.recordedData.clear()
        // Reset the fake clock, since we only care about latency reports from here onward. The
        // specific
        // time doesn't matter; there are no checks for consistency between MotionEvents and the
        // clock.
        clock.timeNanos = 777_000L

        // Now move the pen at t=325ms and t=329ms. The two events got batched together. The
        // platform
        // also predicted events for t=333ms and t=327ms.
        val realMoveEvent = MotionEvent.obtain(321, 325, MotionEvent.ACTION_MOVE, 12f, 22f, 0)
        realMoveEvent.addBatch(329, 14f, 24f, 1f, 1f, 0)
        val predictedMoveEvent = MotionEvent.obtain(321, 333, MotionEvent.ACTION_MOVE, 16f, 26f, 0)
        predictedMoveEvent.addBatch(337, 18f, 28f, 1f, 1f, 0)

        manager.addToStroke(
            realMoveEvent,
            realMoveEvent.getPointerId(0),
            inProgressStrokeId,
            predictedMoveEvent,
        )

        // Now we should have latency data for all three input events.
        assertThat(latencyDataRecorder.recordedData)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(
                listOf(
                    // Since all MOVE events (two real, two predicted) were sent in one call, they
                    // were
                    // processed together. So there were just two clock ticks to get the draw call
                    // finish
                    // time, and one other for the estimated pixel presentation time.
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.ADD
                        eventAction = LatencyData.EventAction.MOVE
                        batchSize = 2
                        batchIndex = 0
                        osDetectsEvent = 325_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.ADD
                        eventAction = LatencyData.EventAction.MOVE
                        batchSize = 2
                        batchIndex = 1
                        osDetectsEvent = 329_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                        eventAction = LatencyData.EventAction.PREDICTED_MOVE
                        batchSize = 2
                        batchIndex = 0
                        osDetectsEvent = 333_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                        eventAction = LatencyData.EventAction.PREDICTED_MOVE
                        batchSize = 2
                        batchIndex = 1
                        osDetectsEvent = 337_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                )
            )
    }

    @Test
    fun latencyDataCallback_getsLatencyDataForAllPredictedInputsEvenWhenOverwrittenByRealInputs() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        // Set the pen down at t=321ms.
        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )

        // We already checked the reported LatencyData in a previous test.
        latencyDataRecorder.recordedData.clear()
        // Reset the fake clock, since we only care about latency reports from here onward. The
        // specific
        // time doesn't matter; there are no checks for consistency between MotionEvents and the
        // clock.
        clock.timeNanos = 777_000L

        // Now move the pen. There are three real inputs: two in one batch and then another. There
        // are
        // also predicted inputs along with both batches of real inputs.
        val realMoveEvent1 = MotionEvent.obtain(321, 325, MotionEvent.ACTION_MOVE, 12f, 22f, 0)
        realMoveEvent1.addBatch(329, 14f, 24f, 1f, 1f, 0)
        val predictedMoveEvent1 = MotionEvent.obtain(321, 333, MotionEvent.ACTION_MOVE, 16f, 26f, 0)
        predictedMoveEvent1.addBatch(337, 18f, 28f, 1f, 1f, 0)
        // The prediction was correct: the next real input exactly matches the last prediction.
        val realMoveEvent2 = MotionEvent.obtain(321, 337, MotionEvent.ACTION_MOVE, 18f, 28f, 0)
        val predictedMoveEvent2 = MotionEvent.obtain(321, 341, MotionEvent.ACTION_MOVE, 20f, 30f, 0)

        manager.addToStroke(
            realMoveEvent1,
            realMoveEvent1.getPointerId(0),
            inProgressStrokeId,
            predictedMoveEvent1,
        )
        manager.addToStroke(
            realMoveEvent2,
            realMoveEvent2.getPointerId(0),
            inProgressStrokeId,
            predictedMoveEvent2,
        )

        // Now we should have latency data for all the input events, even the old predictions that
        // were
        // superseded (in rendering) by the newest real input.
        assertThat(latencyDataRecorder.recordedData)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(
                listOf(
                    // The first addToStroke (two real inputs and two predicted) led to two clock
                    // ticks: one
                    // for the draw call finish time and one for the estimated pixel presentation
                    // time.
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.ADD
                        eventAction = LatencyData.EventAction.MOVE
                        batchSize = 2
                        batchIndex = 0
                        osDetectsEvent = 325_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.ADD
                        eventAction = LatencyData.EventAction.MOVE
                        batchSize = 2
                        batchIndex = 1
                        osDetectsEvent = 329_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                        eventAction = LatencyData.EventAction.PREDICTED_MOVE
                        batchSize = 2
                        batchIndex = 0
                        osDetectsEvent = 333_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                        eventAction = LatencyData.EventAction.PREDICTED_MOVE
                        batchSize = 2
                        batchIndex = 1
                        osDetectsEvent = 337_000_000
                        strokesViewGetsAction = 777_001
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_003
                        estimatedPixelPresentationTime = 777_004
                    },
                    // The second addToStroke (one real input and one predicted) led to two more
                    // clock ticks.
                    // The fact that the real input is the same as the last prediction doesn't
                    // matter at all
                    // for latency reporting.
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.ADD
                        eventAction = LatencyData.EventAction.MOVE
                        batchSize = 1
                        batchIndex = 0
                        osDetectsEvent = 337_000_000
                        strokesViewGetsAction = 777_005
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_007
                        estimatedPixelPresentationTime = 777_008
                    },
                    LatencyData().apply {
                        strokeId = inProgressStrokeId
                        strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                        eventAction = LatencyData.EventAction.PREDICTED_MOVE
                        batchSize = 1
                        batchIndex = 0
                        osDetectsEvent = 341_000_000
                        strokesViewGetsAction = 777_005
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 777_007
                        estimatedPixelPresentationTime = 777_008
                    },
                )
            )
    }

    @Test
    fun latencyDataCallback_getsLatencyDataForPenUp() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        // Set the pen down at t=321ms.
        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )

        // Now move the pen at t=325ms and t=329ms.
        val moveEvent1 = MotionEvent.obtain(321, 325, MotionEvent.ACTION_MOVE, 12f, 22f, 0)
        val moveEvent2 = MotionEvent.obtain(321, 329, MotionEvent.ACTION_MOVE, 12f, 22f, 0)
        manager.addToStroke(moveEvent1, moveEvent1.getPointerId(0), inProgressStrokeId, null)
        manager.addToStroke(moveEvent2, moveEvent2.getPointerId(0), inProgressStrokeId, null)

        // We already checked the reported LatencyData in a previous test.
        latencyDataRecorder.recordedData.clear()

        clock.timeNanos = 334_000_000
        // Finally, lift up the pen at t=333ms.
        val upEvent = MotionEvent.obtain(321, 333, MotionEvent.ACTION_UP, 12f, 22f, 0)

        manager.finishStroke(upEvent, upEvent.getPointerId(0), inProgressStrokeId)

        assertThat(latencyDataRecorder.recordedData)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(
                listOf(
                    LatencyData().apply {
                        eventAction = LatencyData.EventAction.UP
                        strokeAction = LatencyData.StrokeAction.FINISH
                        strokeId = inProgressStrokeId
                        batchSize = 1
                        batchIndex = 0
                        osDetectsEvent = 333_000_000
                        // The clock ticked once to get this time.
                        strokesViewGetsAction = 334_000_001
                        // And thrice for this one - twice on the UI thread and once on the render
                        // thread.
                        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 334_000_004
                        // And once more to get the estimated time.
                        estimatedPixelPresentationTime = 334_000_005
                    }
                )
            )
    }

    @Test
    fun startStroke_whenContentRetained_shouldDrawWithFiniteModifiedRegion() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        whenever(inProgressStrokesRenderHelper.contentsPreservedBetweenDraws).thenReturn(true)

        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)
        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        @Suppress("UNUSED_VARIABLE")
        val unused =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )

        val modifiedRegionCaptor = argumentCaptor<MutableBox>()
        verify(inProgressStrokesRenderHelper)
            .prepareToDrawInModifiedRegion(modifiedRegionCaptor.capture())
        assertThat(modifiedRegionCaptor.firstValue.width).isFinite()
        assertThat(modifiedRegionCaptor.firstValue.height).isFinite()
        verify(inProgressStrokesRenderHelper).drawInModifiedRegion(any<InProgressStroke>(), any())
        verify(inProgressStrokesRenderHelper).afterDrawInModifiedRegion()
    }

    @Test
    fun startStroke_whenContentNotRetained_shouldDrawWithInfiniteModifiedRegion() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        whenever(inProgressStrokesRenderHelper.contentsPreservedBetweenDraws).thenReturn(false)

        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)
        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        @Suppress("UNUSED_VARIABLE")
        val unused =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )

        val modifiedRegionCaptor = argumentCaptor<MutableBox>()
        verify(inProgressStrokesRenderHelper)
            .prepareToDrawInModifiedRegion(modifiedRegionCaptor.capture())
        assertThat(modifiedRegionCaptor.firstValue.xMin).isNegativeInfinity()
        assertThat(modifiedRegionCaptor.firstValue.xMax).isPositiveInfinity()
        assertThat(modifiedRegionCaptor.firstValue.yMin).isNegativeInfinity()
        assertThat(modifiedRegionCaptor.firstValue.yMax).isPositiveInfinity()
        verify(inProgressStrokesRenderHelper).drawInModifiedRegion(any<InProgressStroke>(), any())
        verify(inProgressStrokesRenderHelper).afterDrawInModifiedRegion()
    }

    @Test
    fun addToStroke_whenInSameLocationAndContentNotRetained_shouldRedrawWithInfiniteModifiedRegion() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        whenever(inProgressStrokesRenderHelper.contentsPreservedBetweenDraws).thenReturn(false)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)
        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        // The specifics of this are validated in a test focused on startStroke.
        verify(inProgressStrokesRenderHelper).prepareToDrawInModifiedRegion(any())
        verify(inProgressStrokesRenderHelper).drawInModifiedRegion(any<InProgressStroke>(), any())
        verify(inProgressStrokesRenderHelper).afterDrawInModifiedRegion()

        val moveEvent = MotionEvent.obtain(321, 325, MotionEvent.ACTION_MOVE, 10f, 20f, 0)
        manager.addToStroke(moveEvent, moveEvent.getPointerId(0), inProgressStrokeId, null)

        // Each being called a second time - the first time was from startStroke.
        val modifiedRegionCaptor = argumentCaptor<MutableBox>()
        verify(inProgressStrokesRenderHelper, times(2))
            .prepareToDrawInModifiedRegion(modifiedRegionCaptor.capture())
        assertThat(modifiedRegionCaptor.firstValue.xMin).isNegativeInfinity()
        assertThat(modifiedRegionCaptor.firstValue.xMax).isPositiveInfinity()
        assertThat(modifiedRegionCaptor.firstValue.yMin).isNegativeInfinity()
        assertThat(modifiedRegionCaptor.firstValue.yMax).isPositiveInfinity()
        verify(inProgressStrokesRenderHelper, times(2))
            .drawInModifiedRegion(any<InProgressStroke>(), any())
        verify(inProgressStrokesRenderHelper, times(2)).afterDrawInModifiedRegion()
    }

    @Test
    fun onHandoff_whenContentRetained_shouldCallRenderHelperClear() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock().apply { timeNanos = 334_000_000 }
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        whenever(inProgressStrokesRenderHelper.contentsPreservedBetweenDraws).thenReturn(true)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        val upEvent = MotionEvent.obtain(321, 333, MotionEvent.ACTION_UP, 12f, 22f, 0)
        manager.finishStroke(upEvent, upEvent.getPointerId(0), inProgressStrokeId)
        manager.onStrokeCohortHandoffToHwuiComplete() // Unpause input processing to finish handoff.

        verify(inProgressStrokesRenderHelper).clear()
    }

    @Test
    fun onHandoff_whenContentNotRetained_shouldNotCallRenderHelperClear() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock().apply { timeNanos = 334_000_000 }
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        whenever(inProgressStrokesRenderHelper.contentsPreservedBetweenDraws).thenReturn(false)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        val upEvent = MotionEvent.obtain(321, 333, MotionEvent.ACTION_UP, 12f, 22f, 0)
        manager.finishStroke(upEvent, upEvent.getPointerId(0), inProgressStrokeId)
        manager.onStrokeCohortHandoffToHwuiComplete() // Unpause input processing to finish handoff.

        verify(inProgressStrokesRenderHelper, never()).clear()
    }

    @Test
    fun startStroke_shouldObtainInProgressStroke() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock().apply { timeNanos = 334_000_000 }
        val inProgressStrokePool = FakeInProgressStrokePool()
        val manager =
            makeSynchronousInProgressStrokesManager(
                latencyDataRecorder,
                clock,
                inProgressStrokePool
            )
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        @Suppress("UNUSED_VARIABLE")
        val unused =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )

        assertThat(inProgressStrokePool.obtainCount).isEqualTo(1)
        assertThat(inProgressStrokePool.recycleCount).isEqualTo(0)
        assertThat(inProgressStrokePool.trimToSizeLastValue).isNull()
    }

    @Test
    fun onHandoff_shouldRecycleInProgressStroke() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock().apply { timeNanos = 334_000_000 }
        val inProgressStrokePool = FakeInProgressStrokePool()
        val manager =
            makeSynchronousInProgressStrokesManager(
                latencyDataRecorder,
                clock,
                inProgressStrokePool
            )
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        val upEvent = MotionEvent.obtain(321, 333, MotionEvent.ACTION_UP, 12f, 22f, 0)
        manager.finishStroke(upEvent, upEvent.getPointerId(0), inProgressStrokeId)
        manager.onStrokeCohortHandoffToHwuiComplete() // Unpause input processing to finish handoff.

        assertThat(inProgressStrokePool.obtainCount).isEqualTo(1)
        assertThat(inProgressStrokePool.recycleCount).isEqualTo(1)
        assertThat(inProgressStrokePool.trimToSizeLastValue).isEqualTo(1)
    }

    @Test
    fun onHandoff_afterMultipleHandoffs_shouldTrimInProgressStrokePoolToMaxCohortSize() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock().apply { timeNanos = 334_000_000 }
        val inProgressStrokePool = FakeInProgressStrokePool()
        val manager =
            makeSynchronousInProgressStrokesManager(
                latencyDataRecorder,
                clock,
                inProgressStrokePool
            )
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        val cohortSizesForHandoffs = listOf(2, 9, 3, 8, 4, 7, 5, 6, 1, 1, 1, 1, 1, 1)
        val maxOfLast10CohortSizes = listOf(2, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 8, 8, 7)
        check(maxOfLast10CohortSizes.size == cohortSizesForHandoffs.size)

        for (handoffIndex in cohortSizesForHandoffs.indices) {
            val cohortSize = cohortSizesForHandoffs[handoffIndex]
            val cohortStrokeIds = mutableListOf<InProgressStrokeId>()
            repeat(cohortSize) {
                val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
                cohortStrokeIds.add(
                    manager.startStroke(
                        downEvent,
                        downEvent.getPointerId(0),
                        Matrix(),
                        Matrix(),
                        makeBrush(),
                        0f,
                    )
                )
                assertThat(inProgressStrokePool.obtainCount).isEqualTo(it + 1) // it is 0-based
            }
            assertThat(inProgressStrokePool.obtainCount).isEqualTo(cohortSize)
            assertThat(inProgressStrokePool.recycleCount).isEqualTo(0)
            assertThat(inProgressStrokePool.trimToSizeLastValue).isNull()
            for (strokeId in cohortStrokeIds) {
                // None are recycled until the last one is finished and the entire cohort is handed
                // off
                // together.
                assertThat(inProgressStrokePool.recycleCount).isEqualTo(0)
                val upEvent = MotionEvent.obtain(321, 333, MotionEvent.ACTION_UP, 12f, 22f, 0)
                manager.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
            }
            manager.onStrokeCohortHandoffToHwuiComplete() // Unpause input processing to finish
            // handoff.
            assertThat(inProgressStrokePool.obtainCount).isEqualTo(cohortSize)
            assertThat(inProgressStrokePool.recycleCount).isEqualTo(cohortSize)
            assertThat(inProgressStrokePool.trimToSizeLastValue)
                .isEqualTo(maxOfLast10CohortSizes[handoffIndex])
            // Check obtain/recycle counts and trimToSize separately for each cohort.
            inProgressStrokePool.resetTestData()
        }
    }

    @Test
    fun finishStroke_shouldCallStrokesFinishedListener() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val (manager, renderHelper, runUiThreadToEndOfFrame) =
            makeAsyncManager(latencyDataRecorder, clock)
        val finishedStrokes = mutableListOf<InProgressStrokeId>()
        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    finishedStrokes.addAll(strokes.keys)
                }
            }
        )

        val downTime = clock.getNextMillisTime()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val strokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        val upEvent =
            MotionEvent.obtain(
                downTime,
                clock.getNextMillisTime(),
                MotionEvent.ACTION_UP,
                12f,
                22f,
                0
            )
        manager.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()

        assertThat(finishedStrokes).containsExactly(strokeId)
    }

    @Test
    fun onAllStrokesFinished_getsFinishedStrokeWithAllInputs_motionEventApi() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val (manager, renderHelper, runUiThreadToEndOfFrame) =
            makeAsyncManager(latencyDataRecorder, clock)
        val finishedStrokeIds = mutableListOf<InProgressStrokeId>()
        val finishedStrokes = mutableListOf<FinishedStroke>()
        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    finishedStrokeIds.addAll(strokes.keys)
                    finishedStrokes.addAll(strokes.values)
                }
            }
        )

        val downTime = clock.getNextMillisTime()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val strokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        clock.advanceByMillis(1000)
        val moveEvent =
            MotionEvent.obtain(
                downTime,
                clock.getNextMillisTime(),
                MotionEvent.ACTION_MOVE,
                30f,
                40f,
                0
            )
        manager.addToStroke(moveEvent, moveEvent.getPointerId(0), strokeId, null)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        clock.advanceByMillis(1000)
        val upEvent =
            MotionEvent.obtain(
                downTime,
                clock.getNextMillisTime(),
                MotionEvent.ACTION_UP,
                50f,
                60f,
                0
            )
        manager.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()

        assertThat(finishedStrokeIds).containsExactly(strokeId)
        assertThat(finishedStrokes).hasSize(1)
        val stroke = finishedStrokes[0].stroke
        assertThat(stroke.inputs.size).isEqualTo(3)
        assertThat(stroke.inputs[0])
            .isEqualTo(
                StrokeInput.create(
                    x = 10f,
                    y = 20f,
                    toolType = InputToolType.TOUCH,
                    elapsedTimeMillis = 0
                )
            )
        assertThat(stroke.inputs[1])
            .isEqualTo(
                StrokeInput.create(
                    x = 30f,
                    y = 40f,
                    toolType = InputToolType.TOUCH,
                    elapsedTimeMillis = 1000,
                )
            )
        assertThat(stroke.inputs[2])
            .isEqualTo(
                StrokeInput.create(
                    x = 50f,
                    y = 60f,
                    toolType = InputToolType.TOUCH,
                    elapsedTimeMillis = 2000,
                )
            )
        // The MotionEvent API records latency data at each step.
        assertThat(latencyDataRecorder.recordedData).hasSize(3)
    }

    @Test
    fun onAllStrokesFinished_getsFinishedStrokeWithAllInputs_strokeInputApi() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val (manager, renderHelper, runUiThreadToEndOfFrame) =
            makeAsyncManager(latencyDataRecorder, clock)
        val finishedStrokeIds = mutableListOf<InProgressStrokeId>()
        val finishedStrokes = mutableListOf<FinishedStroke>()
        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    finishedStrokeIds.addAll(strokes.keys)
                    finishedStrokes.addAll(strokes.values)
                }
            }
        )

        val downInput =
            StrokeInput.create(
                x = 10f,
                y = 20f,
                toolType = InputToolType.TOUCH,
                elapsedTimeMillis = 0
            )
        val strokeId = manager.startStroke(downInput, makeBrush())
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        clock.advanceByMillis(1000)
        val moveInputs =
            MutableStrokeInputBatch()
                .apply {
                    addOrThrow(
                        StrokeInput.create(
                            x = 30f,
                            y = 40f,
                            toolType = InputToolType.TOUCH,
                            elapsedTimeMillis = 1000,
                        )
                    )
                }
                .asImmutable()
        manager.addToStroke(moveInputs, strokeId, ImmutableStrokeInputBatch.EMPTY)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        clock.advanceByMillis(1000)
        val upInput =
            StrokeInput.create(
                x = 50f,
                y = 60f,
                toolType = InputToolType.TOUCH,
                elapsedTimeMillis = 2000
            )
        manager.finishStroke(upInput, strokeId)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()

        assertThat(finishedStrokeIds).containsExactly(strokeId)
        assertThat(finishedStrokes).hasSize(1)
        val stroke = finishedStrokes[0].stroke
        assertThat(stroke.inputs.size).isEqualTo(3)
        assertThat(stroke.inputs[0])
            .isEqualTo(
                StrokeInput.create(
                    x = 10f,
                    y = 20f,
                    toolType = InputToolType.TOUCH,
                    elapsedTimeMillis = 0
                )
            )
        assertThat(stroke.inputs[1])
            .isEqualTo(
                StrokeInput.create(
                    x = 30f,
                    y = 40f,
                    toolType = InputToolType.TOUCH,
                    elapsedTimeMillis = 1000,
                )
            )
        assertThat(stroke.inputs[2])
            .isEqualTo(
                StrokeInput.create(
                    x = 50f,
                    y = 60f,
                    toolType = InputToolType.TOUCH,
                    elapsedTimeMillis = 2000,
                )
            )
        // The StrokeInput[Batch] API doesn't record latency data.
        assertThat(latencyDataRecorder.recordedData).isEmpty()
    }

    @Test
    fun startStroke_withNonInvertibleStrokeToWorldTransform_throwsException() {
        val clock = FakeClock()
        val (manager, _, _) = makeAsyncManager(LatencyDataRecorder(), clock)
        val downTime = clock.getNextMillisTime()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 10f, 20f, 0)

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                manager.startStroke(
                    downEvent,
                    pointerId = downEvent.getPointerId(0),
                    motionEventToWorldTransform = Matrix(),
                    strokeToWorldTransform = Matrix().apply { setScale(0f, 0f) },
                    brush = makeBrush(),
                    strokeUnitLengthCm = 0f,
                )
            }
        assertThat(error).hasMessageThat().contains("strokeToWorldTransform must be invertible")
    }

    @Test
    fun startStroke_withInvalidPointerId_throwsException() {
        val clock = FakeClock()
        val (manager, _, _) = makeAsyncManager(LatencyDataRecorder(), clock)
        val downTime = clock.getNextMillisTime()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 10f, 20f, 0)

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                manager.startStroke(
                    downEvent,
                    pointerId = 10,
                    motionEventToWorldTransform = Matrix(),
                    strokeToWorldTransform = Matrix(),
                    brush = makeBrush(),
                    strokeUnitLengthCm = 1f,
                )
            }
        assertThat(error).hasMessageThat().contains("Pointer id 10 is not present in event.")
    }

    @Test
    fun addToStroke_withInvalidPointerId_throwsException() {
        val clock = FakeClock()
        val (manager, _, _) = makeAsyncManager(LatencyDataRecorder(), clock)
        val downTime = clock.getNextMillisTime()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 10f, 20f, 0)

        val strokeId =
            manager.startStroke(
                downEvent,
                pointerId = downEvent.getPointerId(0),
                motionEventToWorldTransform = Matrix(),
                strokeToWorldTransform = Matrix(),
                brush = makeBrush(),
                strokeUnitLengthCm = 1f,
            )
        val moveEvent =
            MotionEvent.obtain(downTime, downTime + 1000L, MotionEvent.ACTION_MOVE, 10f, 20f, 0)
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                manager.addToStroke(
                    moveEvent,
                    pointerId = 10,
                    strokeId = strokeId,
                    prediction = null
                )
            }
        assertThat(error).hasMessageThat().contains("Pointer id 10 is not present in event.")
    }

    @Test
    fun addToStroke_withMissingStrokeId_throwsException() {
        val clock = FakeClock()
        val (manager, _, _) = makeAsyncManager(LatencyDataRecorder(), clock)
        val downTime = clock.getNextMillisTime()
        val moveEvent =
            MotionEvent.obtain(downTime, downTime + 1000L, MotionEvent.ACTION_MOVE, 10f, 20f, 0)
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                manager.addToStroke(
                    moveEvent,
                    pointerId = moveEvent.getPointerId(0),
                    strokeId = InProgressStrokeId(),
                    prediction = null,
                )
            }
        assertThat(error).hasMessageThat().startsWith("Stroke with ID")
    }

    @Test
    fun addToStroke_withMissingStrokeId_strokeInputBatchApi_throwsException() {
        val clock = FakeClock()
        val (manager, _, _) = makeAsyncManager(LatencyDataRecorder(), clock)
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                manager.addToStroke(
                    ImmutableStrokeInputBatch.EMPTY,
                    InProgressStrokeId(),
                    ImmutableStrokeInputBatch.EMPTY,
                )
            }
        assertThat(error).hasMessageThat().startsWith("Stroke with ID")
    }

    @Test
    fun finishStroke_withMissingStrokeId_isIgnored() {
        val clock = FakeClock()
        val (manager, _, _) = makeAsyncManager(LatencyDataRecorder(), clock)
        val downTime = clock.getNextMillisTime()
        val moveEvent =
            MotionEvent.obtain(downTime, downTime + 1000L, MotionEvent.ACTION_MOVE, 10f, 20f, 0)
        // No error is thrown.
        manager.finishStroke(
            moveEvent,
            pointerId = moveEvent.getPointerId(0),
            strokeId = InProgressStrokeId(),
        )
    }

    @Test
    fun finishStroke_withMissingStrokeId_strokeInputBatchApi_isIgnored() {
        val clock = FakeClock()
        val (manager, _, _) = makeAsyncManager(LatencyDataRecorder(), clock)
        manager.finishStroke(StrokeInput(), InProgressStrokeId())
    }

    @Test
    fun finishStroke_withInvalidPointerId_throwsException() {
        val clock = FakeClock()
        val (manager, _, _) = makeAsyncManager(LatencyDataRecorder(), clock)
        val downTime = clock.getNextMillisTime()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 10f, 20f, 0)

        val strokeId =
            manager.startStroke(
                downEvent,
                pointerId = downEvent.getPointerId(0),
                motionEventToWorldTransform = Matrix(),
                strokeToWorldTransform = Matrix(),
                brush = makeBrush(),
                strokeUnitLengthCm = 1f,
            )
        val upEvent =
            MotionEvent.obtain(downTime, downTime + 1000L, MotionEvent.ACTION_UP, 10f, 20f, 0)
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                manager.finishStroke(upEvent, pointerId = 10, strokeId = strokeId)
            }
        assertThat(error).hasMessageThat().contains("Pointer id 10 is not present in event.")
    }

    @Test
    fun startStroke_shouldCombineTransformsCorrectly() {
        val clock = FakeClock()
        val (manager, renderHelper, runUiThreadToEndOfFrame) =
            makeAsyncManager(LatencyDataRecorder(), clock)
        val finishedStrokes = mutableListOf<FinishedStroke>()
        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    finishedStrokes.addAll(strokes.values)
                }
            }
        )

        val motionEventToWorldTransform = Matrix().apply { setScale(2f, 2f) }
        val strokeToWorldTransform = Matrix().apply { setTranslate(1f, 3f) }

        // Create a stroke at (10, 20) in MotionEvent space.
        val downTime = clock.getNextMillisTime()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val strokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                motionEventToWorldTransform,
                strokeToWorldTransform,
                makeBrush(),
                strokeUnitLengthCm = 0f,
            )
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        val upEvent =
            MotionEvent.obtain(
                downTime,
                clock.getNextMillisTime(),
                MotionEvent.ACTION_UP,
                10f,
                20f,
                0
            )
        manager.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()

        assertThat(finishedStrokes).hasSize(1)
        assertThat(finishedStrokes[0].stroke).isNotNull()

        // Given the above transforms, the stroke should be at (20, 40) in world space, and at (19,
        // 37)
        // in stroke space.
        val stroke = checkNotNull(finishedStrokes[0].stroke)
        assertThat(stroke.inputs.get(0).x).isEqualTo(19f)
        assertThat(stroke.inputs.get(0).y).isEqualTo(37f)

        assertThat(finishedStrokes[0].strokeToViewTransform)
            .isEqualTo(
                Matrix().apply {
                    setTranslate(1f, 3f)
                    postScale(0.5f, 0.5f)
                }
            )
    }

    @Test
    fun finishStroke_whenHandoffsPaused_shouldNotCallStrokesFinishedListenerUntilUnpaused() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val (manager, renderHelper, runUiThreadToEndOfFrame) =
            makeAsyncManager(latencyDataRecorder, clock)
        manager.setPauseStrokeCohortHandoffs(true)
        val finishedStrokes = mutableListOf<InProgressStrokeId>()
        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    finishedStrokes.addAll(strokes.keys)
                }
            }
        )

        val downTime = clock.getNextMillisTime()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val strokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        val upEvent =
            MotionEvent.obtain(
                downTime,
                clock.getNextMillisTime(),
                MotionEvent.ACTION_UP,
                12f,
                22f,
                0
            )
        manager.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        assertThat(finishedStrokes).isEmpty()

        manager.setPauseStrokeCohortHandoffs(false)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        assertThat(finishedStrokes).containsExactly(strokeId)
    }

    @Test
    fun finishStroke_withTimeSinceBrushBehavior_doesNotHandOffUntilBehaviorFinishes() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val (manager, renderHelper, runUiThreadToEndOfFrame) =
            makeAsyncManager(latencyDataRecorder, clock)
        val finishedStrokes = mutableListOf<InProgressStrokeId>()
        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    finishedStrokes.addAll(strokes.keys)
                }
            }
        )

        // Create a brush with a time-since behavior that takes 250ms to complete.
        val behavior =
            BrushBehavior(
                source = BrushBehavior.Source.TIME_SINCE_INPUT_IN_SECONDS,
                target = BrushBehavior.Target.SIZE_MULTIPLIER,
                sourceValueRangeLowerBound = 0f,
                sourceValueRangeUpperBound = 0.25f,
                targetModifierRangeLowerBound = 1.25f,
                targetModifierRangeUpperBound = 1f,
            )
        val brush =
            Brush(
                family = BrushFamily(BrushTip(behaviors = listOf(behavior))),
                size = 10f,
                epsilon = 0.1f,
            )

        // Start a new stroke with the above brush.
        val downTime = clock.getNextMillisTime()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val strokeId =
            manager.startStroke(downEvent, downEvent.getPointerId(0), Matrix(), Matrix(), brush, 0f)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()

        // Finish inputs for the stroke. Because the stroke has a time-since behavior that hasn't
        // yet
        // completed, the stroke should not be dry yet.
        val upEvent =
            MotionEvent.obtain(
                downTime,
                clock.getNextMillisTime(),
                MotionEvent.ACTION_UP,
                12f,
                22f,
                0
            )
        manager.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        assertThat(finishedStrokes).isEmpty()

        // The time-since behavior takes 250ms to complete, so after 150ms, it should still not be
        // done
        // yet.
        clock.advanceByMillis(150)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        assertThat(finishedStrokes).isEmpty()

        // After a second 150ms, the 250ms behavior should be complete and the stroke should now be
        // finished drying.
        clock.advanceByMillis(150)
        renderHelper.runRenderThreadToIdle()
        runUiThreadToEndOfFrame()
        assertThat(finishedStrokes).containsExactly(strokeId)
    }

    @Test
    fun cancelStroke_shouldNotCallStrokesFinishedListener() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock()
        val manager = makeSynchronousInProgressStrokesManager(latencyDataRecorder, clock)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)
        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    fail("Should never be called")
                }
            }
        )

        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        val moveEvent = MotionEvent.obtain(321, 325, MotionEvent.ACTION_MOVE, 10f, 20f, 0)
        manager.addToStroke(moveEvent, moveEvent.getPointerId(0), inProgressStrokeId, null)
        val cancelEvent = MotionEvent.obtain(321, 333, MotionEvent.ACTION_CANCEL, 12f, 22f, 0)
        manager.cancelStroke(inProgressStrokeId, cancelEvent)
    }

    @Test
    fun flush_whenNoStrokesInProgress_returnsWithoutCallingStrokesFinishedListener() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock().apply { timeNanos = 334_000_000 }
        val inProgressStrokePool = FakeInProgressStrokePool()
        whenever(inProgressStrokesRenderHelper.supportsFlush).thenReturn(true)

        val manager =
            makeAsyncInProgressStrokesManager(latencyDataRecorder, clock, inProgressStrokePool)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    fail("Expected no callbacks to this function.")
                }
            }
        )

        assertThat(manager.flush(1000, TimeUnit.MILLISECONDS, cancelAllInProgress = false)).isTrue()
    }

    @Test
    fun flush_whenUnfinishedStrokesFinished_shouldFinishAllAndCallStrokesFinishedListener() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock().apply { timeNanos = 334_000_000 }
        val inProgressStrokePool = FakeInProgressStrokePool()
        whenever(inProgressStrokesRenderHelper.supportsFlush).thenReturn(true)
        val manager =
            makeAsyncInProgressStrokesManager(latencyDataRecorder, clock, inProgressStrokePool)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        val finishedStrokes = mutableListOf<InProgressStrokeId>()
        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    finishedStrokes.addAll(strokes.keys)
                }
            }
        )

        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId1 =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        val inProgressStrokeId2 =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )

        assertThat(manager.flush(1000, TimeUnit.MILLISECONDS, cancelAllInProgress = false)).isTrue()

        assertThat(finishedStrokes).containsExactly(inProgressStrokeId1, inProgressStrokeId2)
    }

    @Test
    fun flush_whenUnfinishedStrokesCanceled_shouldCancelAllAndNotCallStrokesFinishedListener() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock().apply { timeNanos = 334_000_000 }
        val inProgressStrokePool = FakeInProgressStrokePool()
        whenever(inProgressStrokesRenderHelper.supportsFlush).thenReturn(true)
        val manager =
            makeAsyncInProgressStrokesManager(latencyDataRecorder, clock, inProgressStrokePool)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)

        val finishedStrokes = mutableListOf<InProgressStrokeId>()
        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    finishedStrokes.addAll(strokes.keys)
                }
            }
        )

        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        @Suppress("UNUSED_VARIABLE")
        val unused1 =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        @Suppress("UNUSED_VARIABLE")
        val unused2 =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )

        assertThat(manager.flush(1000, TimeUnit.MILLISECONDS, cancelAllInProgress = true)).isTrue()

        assertThat(finishedStrokes).isEmpty()
    }

    @Test
    fun flush_whenFinishedStrokesAreDebouncedAndPaused_shouldCallStrokesFinishedListener() {
        val latencyDataRecorder = LatencyDataRecorder()
        val clock = FakeClock().apply { timeNanos = 334_000_000 }
        val inProgressStrokePool = FakeInProgressStrokePool()
        whenever(inProgressStrokesRenderHelper.supportsFlush).thenReturn(true)
        whenever(inProgressStrokesRenderHelper.supportsDebounce).thenReturn(true)
        val manager =
            makeAsyncInProgressStrokesManager(latencyDataRecorder, clock, inProgressStrokePool)
        setUpMockInProgressStrokesRenderHelperForSynchronousOperation(manager, clock)
        manager.setHandoffDebounceTimeMs(5000)
        manager.setPauseStrokeCohortHandoffs(true)

        val finishedStrokes = mutableListOf<InProgressStrokeId>()
        manager.addListener(
            object : InProgressStrokesManager.Listener {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke>
                ) {
                    finishedStrokes.addAll(strokes.keys)
                }
            }
        )

        val downEvent = MotionEvent.obtain(321, 321, MotionEvent.ACTION_DOWN, 10f, 20f, 0)
        val inProgressStrokeId1 =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        val inProgressStrokeId2 =
            manager.startStroke(
                downEvent,
                downEvent.getPointerId(0),
                Matrix(),
                Matrix(),
                makeBrush(),
                0f
            )
        val upEvent = MotionEvent.obtain(321, 333, MotionEvent.ACTION_UP, 12f, 22f, 0)
        manager.finishStroke(upEvent, upEvent.getPointerId(0), inProgressStrokeId1)
        manager.finishStroke(upEvent, upEvent.getPointerId(0), inProgressStrokeId2)

        // These strokes aren't still in progress, they just haven't been handed off yet, so they
        // shouldn't be canceled.
        assertThat(manager.flush(1000, TimeUnit.MILLISECONDS, cancelAllInProgress = true)).isTrue()

        assertThat(finishedStrokes).containsExactly(inProgressStrokeId1, inProgressStrokeId2)
    }

    private fun makeBrush() = Brush(family = StockBrushes.markerLatest, size = 10f, epsilon = 0.1f)
}

private class FakeClock(var timeNanos: Long = 0L) {
    /** Increments `timeNanos` by 1 nanosecond and returns it. */
    fun getNextTime(): Long {
        timeNanos += 1L
        return timeNanos
    }

    fun getNextMillisTime() = getNextTime() / 1_000_000

    fun advanceByMillis(durationMillis: Long) {
        timeNanos += durationMillis * 1_000_000
    }
}

@OptIn(ExperimentalLatencyDataApi::class)
private class LatencyDataRecorder() {
    val recordedData = mutableListOf<LatencyData>()

    fun record(data: LatencyData) {
        val copy =
            LatencyData().apply {
                eventAction = data.eventAction
                strokeAction = data.strokeAction
                strokeId = data.strokeId
                batchSize = data.batchSize
                batchIndex = data.batchIndex
                osDetectsEvent = data.osDetectsEvent
                strokesViewGetsAction = data.strokesViewGetsAction
                strokesViewFinishesDrawCalls = data.strokesViewFinishesDrawCalls
                estimatedPixelPresentationTime = data.estimatedPixelPresentationTime
                canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls =
                    data.canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls
                hwuiInProgressStrokesRenderHelperData.finishesDrawCalls =
                    data.hwuiInProgressStrokesRenderHelperData.finishesDrawCalls
            }
        recordedData.add(copy)
    }
}

private class FakeInProgressStrokePool : InProgressStrokePool {
    private val real = InProgressStrokePool.create()
    var obtainCount = 0
        private set

    var recycleCount = 0
        private set

    var trimToSizeLastValue: Int? = null
        private set

    fun resetTestData() {
        obtainCount = 0
        recycleCount = 0
        trimToSizeLastValue = null
    }

    override fun obtain(): InProgressStroke {
        obtainCount++
        return real.obtain()
    }

    override fun recycle(inProgressStroke: InProgressStroke) {
        recycleCount++
        real.recycle(inProgressStroke)
    }

    override fun trimToSize(maxSize: Int) {
        trimToSizeLastValue = maxSize
        real.trimToSize(maxSize)
    }
}

/**
 * A fake for [InProgressStrokesRenderHelper] which simulates its typically multi-threaded nature in
 * a single-threaded test by providing hooks to run the queued "render thread" jobs.
 */
@OptIn(ExperimentalLatencyDataApi::class)
private class FakeAsyncRenderHelper(
    private val callback: InProgressStrokesRenderHelper.Callback,
    private val clock: FakeClock,
    override val contentsPreservedBetweenDraws: Boolean = true,
    override val supportsDebounce: Boolean = true,
    override val supportsFlush: Boolean = true,
) : InProgressStrokesRenderHelper {
    private var drawRequestCount = 0
    private var onRenderThread = false
    override var maskPath: Path? = null

    fun runRenderThreadToIdle(): Boolean {
        var ranAny = false
        onRenderThread = true
        while (drawRequestCount > 0) {
            drawRequestCount--
            ranAny = true
            callback.onDraw()
            callback.onDrawComplete()
            callback.setCustomLatencyDataField { data: LatencyData, timeNanos: Long ->
                data.canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = timeNanos
            }
            callback.reportEstimatedPixelPresentationTime(clock.getNextTime())
            callback.handOffAllLatencyData()
        }
        onRenderThread = false
        return ranAny
    }

    override fun assertOnRenderThread() {
        check(onRenderThread)
    }

    override fun requestDraw() {
        check(!onRenderThread)
        drawRequestCount++
    }

    override fun prepareToDrawInModifiedRegion(modifiedRegionInMainView: MutableBox) {
        assertOnRenderThread()
    }

    override fun drawInModifiedRegion(
        inProgressStroke: InProgressStroke,
        strokeToMainViewTransform: Matrix,
    ) {
        assertOnRenderThread()
    }

    override fun afterDrawInModifiedRegion() {
        assertOnRenderThread()
    }

    override fun clear() {
        assertOnRenderThread()
    }

    override fun requestStrokeCohortHandoffToHwui(
        handingOff: Map<InProgressStrokeId, FinishedStroke>
    ) {
        check(!onRenderThread)
        callback.onStrokeCohortHandoffToHwui(handingOff)
        callback.onStrokeCohortHandoffToHwuiComplete()
    }
}
