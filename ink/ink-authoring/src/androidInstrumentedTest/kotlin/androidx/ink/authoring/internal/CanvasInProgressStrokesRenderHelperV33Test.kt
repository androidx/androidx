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
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV33.Bounds
import androidx.ink.brush.Brush
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
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
@OptIn(ExperimentalLatencyDataApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(
    maxSdkVersion = Build.VERSION_CODES.TIRAMISU,
    minSdkVersion = Build.VERSION_CODES.TIRAMISU
)
class CanvasInProgressStrokesRenderHelperV33Test {

    @get:Rule
    val activityScenarioRule =
        ActivityScenarioRule(CanvasInProgressStrokesRenderHelperV33TestActivity::class.java)

    private val renderer = mock<CanvasStrokeRenderer> {}
    private val callback = mock<InProgressStrokesRenderHelper.Callback> {}

    @Test
    fun init_shouldAddSurfaceViewAndRunUiThreadTasks() {
        withActivity { activity ->
            assertThat(activity.mainView.childCount).isEqualTo(1)
            assertThat(activity.mainView.getChildAt(0)).isInstanceOf(SurfaceView::class.java)
        }
        var ranAnyOnUiThread = false
        for (i in 0 until 3) {
            onIdle()
            withActivity { activity ->
                if (activity.fakeThreads.runUiThreadToIdle()) {
                    ranAnyOnUiThread = true
                }
            }
            SystemClock.sleep(1000)
        }
        assertThat(ranAnyOnUiThread).isTrue()
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
        var ranAnyOnUiThread = false
        for (i in 0 until 3) {
            onIdle()
            withActivity { activity ->
                if (activity.fakeThreads.runUiThreadToIdle()) {
                    ranAnyOnUiThread = true
                }
            }
            SystemClock.sleep(1000)
        }
        // Make sure it fully initialized.
        assertThat(ranAnyOnUiThread).isTrue()

        withActivity { activity ->
            activity.renderHelper.requestDraw()

            assertThat(activity.fakeThreads.runRenderThreadToIdle()).isTrue()
            verify(callback).onDraw()
            verify(callback).onDrawComplete()
        }
    }

    @Test
    fun requestDraw_whenCalledAgainBeforeDrawFinished_nextDrawIsQueuedAndBothHandOffLatencyData() {
        var ranAnyOnUiThread = false
        for (i in 0 until 3) {
            onIdle()
            withActivity { activity ->
                if (activity.fakeThreads.runUiThreadToIdle()) {
                    ranAnyOnUiThread = true
                }
            }
            SystemClock.sleep(1000)
        }
        // Make sure it fully initialized.
        assertThat(ranAnyOnUiThread).isTrue()

        withActivity { activity ->
            // Two draw requests, with a render thread executions for each of their top level render
            // thread scheduled tasks.
            activity.renderHelper.requestDraw()
            activity.renderHelper.requestDraw()
            assertThat(activity.fakeThreads.runRenderThreadOnce()).isTrue()
            assertThat(activity.fakeThreads.runRenderThreadOnce()).isTrue()

            // onDraw and onDrawComplete executed just for the first draw request.
            verify(callback, times(1)).onDraw()
            verify(callback, times(1)).onDrawComplete()
            verify(callback, never()).setCustomLatencyDataField(any())
            verify(callback, never()).handOffAllLatencyData()
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
        verify(callback, times(2)).onDraw()
        verify(callback, times(2)).onDrawComplete()
        verify(callback, times(2)).setCustomLatencyDataField(any())
        verify(callback, times(2)).handOffAllLatencyData()
    }

    @Test
    fun drawInModifiedRegion_callsRenderer() {
        var ranAnyOnUiThread = false
        for (i in 0 until 3) {
            onIdle()
            withActivity { activity ->
                if (activity.fakeThreads.runUiThreadToIdle()) {
                    ranAnyOnUiThread = true
                }
            }
            SystemClock.sleep(1000)
        }
        // Make sure it fully initialized.
        assertThat(ranAnyOnUiThread).isTrue()

        withActivity { activity ->
            whenever(callback.onDraw()).then {
                activity.renderHelper.prepareToDrawInModifiedRegion(MutableBox())
                activity.renderHelper.drawInModifiedRegion(InProgressStroke(), Matrix())

                activity.renderHelper.afterDrawInModifiedRegion()
            }

            activity.renderHelper.requestDraw()
            assertThat(activity.fakeThreads.runRenderThreadOnce()).isTrue()

            // onDraw and onDrawComplete executed just for the first draw request.
            verify(callback, times(1)).onDraw()
            // The [InProgressStroke] above is expected to be drawn by the [renderer], and the
            // legacy
            // [LegacyStrokeBuilder] is expected to be drawn by the [legacyRenderer].
            verify(renderer, times(1)).draw(any(), any<InProgressStroke>(), any<Matrix>())

            verify(callback, times(1)).onDrawComplete()
        }
    }

    @Test
    fun requestStrokeCohortHandoffToHwui_shouldExecuteCallbackHandoffAndPauseHandoffs() {
        run {
            var ranAnyOnUiThread = false
            for (i in 0 until 3) {
                onIdle()
                withActivity { activity ->
                    if (activity.fakeThreads.runUiThreadToIdle()) {
                        ranAnyOnUiThread = true
                    }
                }
                SystemClock.sleep(1000)
            }
            // Make sure it fully initialized.
            assertThat(ranAnyOnUiThread).isTrue()
        }

        withActivity { activity ->
            val brush = Brush(family = StockBrushes.markerLatest, size = 10f, epsilon = 0.1f)
            val stroke = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)
            val handingOff =
                mapOf(
                    InProgressStrokeId() to
                        FinishedStroke(
                            stroke,
                            Matrix(),
                        )
                )
            activity.renderHelper.requestStrokeCohortHandoffToHwui(handingOff)
            verify(callback).setPauseStrokeCohortHandoffs(true)
            verify(callback).onStrokeCohortHandoffToHwui(handingOff)
            verify(callback).onStrokeCohortHandoffToHwuiComplete()

            activity.fakeThreads.runOnRenderThread { activity.renderHelper.clear() }
            assertThat(activity.fakeThreads.uiThreadDelayedTaskCount()).isEqualTo(1)
            assertThat(activity.fakeThreads.uiThreadReadyTaskCount()).isEqualTo(0)

            activity.fakeThreads.clock.currentTimeMillis += 1000
            assertThat(activity.fakeThreads.uiThreadDelayedTaskCount()).isEqualTo(0)
            assertThat(activity.fakeThreads.uiThreadReadyTaskCount()).isEqualTo(1)
            assertThat(activity.fakeThreads.runUiThreadToIdle()).isTrue()
        }

        // The draw request may be async outside of our code's control, so wait for it to finish,
        // and
        // run any UI thread tasks that it enqueues.
        run {
            for (i in 0 until 3) {
                onIdle()
                withActivity { activity -> activity.fakeThreads.runUiThreadToIdle() }
                SystemClock.sleep(250)
            }
        }

        verify(callback).setPauseStrokeCohortHandoffs(false)
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
            assertThat(bufferTransform).isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY)
            assertThat(bufferTransformInverse)
                .isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY)
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
            assertThat(bufferTransform).isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180)
            assertThat(bufferTransformInverse)
                .isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180)
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
            assertThat(bufferTransform).isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90)
            assertThat(bufferTransformInverse)
                .isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270)
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
            assertThat(bufferTransform).isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270)
            assertThat(bufferTransformInverse)
                .isEqualTo(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90)
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
            assertThat(bufferTransform).isNull()
            assertThat(bufferTransformInverse).isNull()
        }
    }

    private fun withActivity(block: (CanvasInProgressStrokesRenderHelperV33TestActivity) -> Unit) {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.renderer = renderer
            activity.callback = callback
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
