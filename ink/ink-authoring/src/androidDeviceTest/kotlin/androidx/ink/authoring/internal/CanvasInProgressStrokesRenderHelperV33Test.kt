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
import android.os.Build
import android.os.SystemClock
import android.view.SurfaceView
import androidx.graphics.surface.SurfaceControlCompat
import androidx.ink.authoring.ExperimentalCustomShapeWorkflowApi
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InkInProgressShape
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV33.Bounds
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.StockBrushes
import androidx.ink.geometry.MutableBox
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Logic test of [CanvasInProgressStrokesRenderHelperV33]. Runs on an emulator to avoid issues with
 * APIs like SurfaceControl and SurfaceControl.Transaction, which don't have Robolectric shadows or
 * other fakes. The interactions with those objects can't really be verified, so this test will
 * focus on its public API rather than system side effects - namely, calling functions and verifying
 * that the appropriate callbacks are executed. Although this is an emulator test, it does not do
 * any screenshot comparison. That is the role of [InProgressStrokesViewTest], which is a bit higher
 * level but covers the functionality of [CanvasInProgressStrokesRenderHelperV33] in a different way
 * than this test.
 */
@OptIn(
    ExperimentalLatencyDataApi::class,
    ExperimentalInkCustomBrushApi::class,
    ExperimentalCustomShapeWorkflowApi::class,
)
@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(
    maxSdkVersion = Build.VERSION_CODES.TIRAMISU,
    minSdkVersion = Build.VERSION_CODES.TIRAMISU,
)
class CanvasInProgressStrokesRenderHelperV33Test {

    @get:Rule
    val activityScenarioRule =
        ActivityScenarioRule(CanvasInProgressStrokesRenderHelperV33TestActivity::class.java)

    lateinit internal var mockCallback: InProgressStrokesRenderHelper.Callback<Stroke>
    lateinit internal var mockRenderer: CanvasStrokeRenderer

    @Before
    fun setUp() {
        activityScenarioRule.scenario.onActivity { activity ->
            mockCallback = activity.mockCallback
            mockRenderer = activity.mockRenderer
        }
        // Complete initialization and assert that initialization logic is run on both UI and render
        // threads.
        var ranAnyOnUiThread = false
        var ranAnyOnRenderThread = false
        val awaitResumeLatch = CountDownLatch(1)
        withActivity { activity ->
            activity.renderHelper.countDownAfterHandoffsResumedTestLatch = awaitResumeLatch
        }
        val backgroundExecutor = Executors.newSingleThreadExecutor()
        var awaitingInitialization = true
        backgroundExecutor.execute {
            while (awaitingInitialization) {
                withActivity { activity ->
                    if (activity.fakeThreads.runUiThreadOnce()) {
                        ranAnyOnUiThread = true
                    }
                    if (activity.fakeThreads.runRenderThreadOnce()) {
                        ranAnyOnRenderThread = true
                    }
                }
            }
        }
        try {
            assertThat(awaitResumeLatch.await(10, TimeUnit.SECONDS)).isTrue()
        } finally {
            awaitingInitialization = false
            // shutdownNow() _could_ do more to stop the existing task (e.g. interrupt the thread),
            // but
            // it's not guaranteed to and doesn't seem to on at least some of the emulators this
            // test is
            // running on.
            backgroundExecutor.shutdown()
            assertThat(backgroundExecutor.awaitTermination(10, TimeUnit.SECONDS)).isTrue()
        }
        assertThat(ranAnyOnUiThread).isTrue()
        assertThat(ranAnyOnRenderThread).isTrue()
    }

    @Test
    fun init_shouldAddSurfaceView() {
        withActivity { activity ->
            assertThat(activity.mainView.childCount).isEqualTo(1)
            assertThat(activity.mainView.getChildAt(0)).isInstanceOf(SurfaceView::class.java)
        }
    }

    @Test
    fun requestDraw_whenNotInitialized_schedulesTaskOnRenderThread() {
        withActivity { activity ->
            activity.renderHelper.requestDraw()
            assertThat(activity.fakeThreads.runRenderThreadToIdle()).isTrue()
        }
    }

    @Test
    fun requestDraw_runsCallbackOnDrawAndOnDrawComplete() {
        withActivity { activity ->
            activity.renderHelper.requestDraw()

            assertThat(activity.fakeThreads.runRenderThreadToIdle()).isTrue()
            verify(mockCallback).onDraw()
            verify(mockCallback).onDrawComplete()
        }
    }

    @Test
    fun requestDraw_whenCalledAgainBeforeDrawFinished_nextDrawIsQueuedAndBothHandOffLatencyData() {
        withActivity { activity ->
            // Run pending initialization tasks.
            activity.fakeThreads.runRenderThreadToIdle()

            // Two draw requests, with a render thread executions for each of their top level render
            // thread scheduled tasks.
            activity.renderHelper.requestDraw()
            activity.renderHelper.requestDraw()
            assertThat(activity.fakeThreads.runRenderThreadOnce()).isTrue()
            assertThat(activity.fakeThreads.runRenderThreadOnce()).isTrue()

            // onDraw and onDrawComplete executed just for the first draw request.
            verify(mockCallback, times(1)).onDraw()
            verify(mockCallback, times(1)).onDrawComplete()
            verify(mockCallback, never()).setCustomLatencyDataField(any())
            verify(mockCallback, never()).handOffAllLatencyData()
        }

        // The draw request may be async outside of our code's control, so wait for it to finish,
        // and
        // run any render thread tasks that it enqueues.
        var ranAnyOnRenderThread = false
        for (i in 0 until 3) {
            onIdle()
            withActivity { activity ->
                if (activity.fakeThreads.runRenderThreadToIdle()) {
                    ranAnyOnRenderThread = true
                }
            }
            SystemClock.sleep(1000)
        }
        assertThat(ranAnyOnRenderThread).isTrue()

        // Now the second draw was able to execute onDraw and onDrawComplete.
        verify(mockCallback, times(2)).onDraw()
        verify(mockCallback, times(2)).onDrawComplete()
        verify(mockCallback, times(2)).setCustomLatencyDataField(any())
        verify(mockCallback, times(2)).handOffAllLatencyData()
    }

    @Test
    fun drawInModifiedRegion_callsRenderer() {
        withActivity { activity ->
            whenever(mockCallback.onDraw()).then {
                activity.renderHelper.prepareToDrawInModifiedRegion(MutableBox())
                activity.renderHelper.drawInModifiedRegion(InkInProgressShape(), Matrix())
                activity.renderHelper.afterDrawInModifiedRegion()
            }

            activity.renderHelper.requestDraw()
            assertThat(activity.fakeThreads.runRenderThreadToIdle()).isTrue()

            verify(mockCallback, times(1)).onDraw()
            verify(mockRenderer, times(1))
                .draw(any(), any<InProgressStroke>(), any<Matrix>(), any<Float>())
            verify(mockCallback, times(1)).onDrawComplete()
        }
    }

    @Test
    fun requestStrokeCohortHandoffToHwui_shouldExecuteCallbackHandoffAndPauseHandoffs() {
        // Handoffs are paused during initialization and unpaused when the viewport is initialized,
        // to
        // ensure that unpause happens if the preivous viewport was removed mid-handoff.
        verify(mockCallback).pauseStrokeCohortHandoffs()
        verify(mockCallback).resumeStrokeCohortHandoffs()
        clearInvocations(mockCallback)

        withActivity { activity ->
            val brush = Brush(family = StockBrushes.marker(), size = 10f, epsilon = 0.1f)
            val stroke = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)
            val cohort = listOf(FinishedStroke(InProgressStrokeId(), stroke, Matrix()))
            // This kicks off the handoff process which goes back and forth between the render and
            // UI
            // threads. resumeStrokeCohortHandoffs() is called at the end when the inactive buffer
            // is
            // cleared and ready for the next handoff.
            activity.renderHelper.requestStrokeCohortHandoffToHwui(cohort)
            verify(mockCallback).pauseStrokeCohortHandoffs()
            verify(mockCallback).onStrokeCohortHandoffToHwui(cohort)
            verify(mockCallback).onStrokeCohortHandoffToHwuiComplete()
            assertThat(activity.fakeThreads.uiThreadDelayedTaskCount()).isEqualTo(0)
            assertThat(activity.fakeThreads.uiThreadReadyTaskCount()).isEqualTo(0)
        }

        // The next draw happens outside of our fake threads, so we need to wait for the transaction
        // to
        // be committed, running any tasks enqueued on the fakes.
        run {
            for (i in 0 until 3) {
                var delayedClearQueued = false
                onIdle()
                withActivity { activity ->
                    // Not running the UI thread because handoff needs to be able to complete and
                    // handoffs be unpaused while the UI thread is waiting during flush.
                    activity.fakeThreads.runRenderThreadToIdle()
                    delayedClearQueued = activity.fakeThreads.renderThreadDelayedTaskCount() > 0
                }
                if (delayedClearQueued) {
                    break
                }
                SystemClock.sleep(250)
            }
        }

        withActivity { activity ->
            // Delayed handoff to clear happens on the render thread.
            assertThat(activity.fakeThreads.uiThreadDelayedTaskCount()).isEqualTo(0)
            assertThat(activity.fakeThreads.uiThreadReadyTaskCount()).isEqualTo(0)
            assertThat(activity.fakeThreads.renderThreadDelayedTaskCount()).isEqualTo(1)
            assertThat(activity.fakeThreads.renderThreadReadyTaskCount()).isEqualTo(0)
            activity.fakeThreads.clock.currentTimeMillis += 1000
            assertThat(activity.fakeThreads.renderThreadDelayedTaskCount()).isEqualTo(0)
            assertThat(activity.fakeThreads.renderThreadReadyTaskCount()).isEqualTo(1)
            assertThat(activity.fakeThreads.runRenderThreadToIdle()).isTrue()
        }

        // Once again have to wait for the actual draw that does the clear, handoffs are unpaused in
        // a
        // callback after the draw finishes.
        for (i in 0 until 3) {
            onIdle()
            withActivity { activity ->
                // Again, want to make sure this isn't dependent on doing anything on the UI thread,
                // so
                // it can get all the way to `resumeStrokeCohortHandoffs` while the UI thread is
                // blocked.
                activity.fakeThreads.runRenderThreadToIdle()
            }
            SystemClock.sleep(250)
        }

        // At the end of the process
        verify(mockCallback).resumeStrokeCohortHandoffs()
    }

    @Test
    fun onViewDetachedFromWindow_shouldRemoveSurfaceView() {
        withActivity { activity ->
            activity.rootView.removeView(activity.mainView)
            assertThat(activity.mainView.childCount).isEqualTo(0)
        }
    }

    @Test
    fun boundsInit_handlesAllRotationTransformHints() {
        val mainViewWidth = 111
        val mainViewHeight = 444

        with(
            Bounds(
                mainViewWidth = mainViewWidth,
                mainViewHeight = mainViewHeight,
                mainViewTransformHint = SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY,
            )
        ) {
            assertThat(bufferWidth).isEqualTo(mainViewWidth)
            assertThat(bufferHeight).isEqualTo(mainViewHeight)
            assertThat(rendererTransform).isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY)
            assertThat(surfaceTransform).isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY)
        }

        with(
            Bounds(
                mainViewWidth = mainViewWidth,
                mainViewHeight = mainViewHeight,
                mainViewTransformHint = SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180,
            )
        ) {
            assertThat(bufferWidth).isEqualTo(mainViewWidth)
            assertThat(bufferHeight).isEqualTo(mainViewHeight)
            assertThat(rendererTransform)
                .isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180)
            assertThat(surfaceTransform).isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180)
        }

        with(
            Bounds(
                mainViewWidth = mainViewWidth,
                mainViewHeight = mainViewHeight,
                mainViewTransformHint = SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90,
            )
        ) {
            assertThat(bufferWidth).isEqualTo(mainViewHeight)
            assertThat(bufferHeight).isEqualTo(mainViewWidth)
            assertThat(rendererTransform).isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90)
            assertThat(surfaceTransform).isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270)
        }

        with(
            Bounds(
                mainViewWidth = mainViewWidth,
                mainViewHeight = mainViewHeight,
                mainViewTransformHint = SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270,
            )
        ) {
            assertThat(bufferWidth).isEqualTo(mainViewHeight)
            assertThat(bufferHeight).isEqualTo(mainViewWidth)
            assertThat(rendererTransform)
                .isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270)
            assertThat(surfaceTransform).isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90)
        }

        with(
            Bounds(
                mainViewWidth = mainViewWidth,
                mainViewHeight = mainViewHeight,
                // Unsupported value
                mainViewTransformHint = SurfaceControlCompat.BUFFER_TRANSFORM_MIRROR_HORIZONTAL,
            )
        ) {
            assertThat(bufferWidth).isEqualTo(mainViewWidth)
            assertThat(bufferHeight).isEqualTo(mainViewHeight)
            assertThat(rendererTransform).isNull()
            assertThat(surfaceTransform).isNull()
        }
    }

    @Test
    fun executeOnRenderThread_shouldExecute() {
        var executed = false
        withActivity { activity ->
            val callback = Runnable {
                activity.renderHelper.assertOnRenderThread()
                executed = true
            }
            activity.renderHelper.executeOnRenderThread(callback)
            assertThat(executed).isFalse()
            assertThat(activity.fakeThreads.runRenderThreadToIdle()).isTrue()
            assertThat(executed).isTrue()
        }
    }

    private fun withActivity(block: (CanvasInProgressStrokesRenderHelperV33TestActivity) -> Unit) {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.fakeThreads.runOnUiThread {
                // Code run within onActivity can be considered to be on the UI thread for
                // assertions. There
                // is no equivalent of this for the render thread, since the render thread is only
                // accessed
                // by scheduling tasks on the render thread executors, while the UI thread is used
                // by all
                // many standard system callbacks.
                block(activity)
            }
        }
    }
}
