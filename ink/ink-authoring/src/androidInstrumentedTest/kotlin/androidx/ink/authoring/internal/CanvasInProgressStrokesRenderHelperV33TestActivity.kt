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

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.geometry.AffineTransform
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke
import java.util.concurrent.TimeUnit

/** An [Activity] to support [CanvasInProgressStrokesRenderHelperV33]. */
@OptIn(ExperimentalLatencyDataApi::class)
@SuppressLint("UseSdkSuppress") // SdkSuppress is on the test class.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class CanvasInProgressStrokesRenderHelperV33TestActivity : Activity() {

    lateinit var rootView: ViewGroup
    lateinit var mainView: ViewGroup
    internal lateinit var renderHelper: CanvasInProgressStrokesRenderHelperV33

    internal var fakeThreads = FakeThreads()

    internal var callback: InProgressStrokesRenderHelper.Callback? = null
    var renderer: CanvasStrokeRenderer? = null

    private val delegatingCallback =
        object : InProgressStrokesRenderHelper.Callback {
            override fun onDraw() {
                callback?.onDraw()
            }

            override fun onDrawComplete() {
                callback?.onDrawComplete()
            }

            override fun reportEstimatedPixelPresentationTime(timeNanos: Long) {
                callback?.reportEstimatedPixelPresentationTime(timeNanos)
            }

            override fun setCustomLatencyDataField(setter: (LatencyData, Long) -> Unit) {
                callback?.setCustomLatencyDataField(setter)
            }

            override fun handOffAllLatencyData() {
                callback?.handOffAllLatencyData()
            }

            override fun setPauseStrokeCohortHandoffs(paused: Boolean) {
                callback?.setPauseStrokeCohortHandoffs(paused)
            }

            override fun onStrokeCohortHandoffToHwui(
                strokeCohort: Map<InProgressStrokeId, FinishedStroke>
            ) {
                callback?.onStrokeCohortHandoffToHwui(strokeCohort)
            }

            override fun onStrokeCohortHandoffToHwuiComplete() {
                callback?.onStrokeCohortHandoffToHwuiComplete()
            }
        }

    private val delegatingRenderer =
        object : CanvasStrokeRenderer {
            override fun draw(
                canvas: Canvas,
                stroke: Stroke,
                strokeToScreenTransform: AffineTransform
            ) {
                renderer?.draw(canvas, stroke, strokeToScreenTransform)
            }

            override fun draw(canvas: Canvas, stroke: Stroke, strokeToScreenTransform: Matrix) {
                renderer?.draw(canvas, stroke, strokeToScreenTransform)
            }

            override fun draw(
                canvas: Canvas,
                inProgressStroke: InProgressStroke,
                strokeToScreenTransform: AffineTransform,
            ) {
                renderer?.draw(canvas, inProgressStroke, strokeToScreenTransform)
            }

            override fun draw(
                canvas: Canvas,
                inProgressStroke: InProgressStroke,
                strokeToScreenTransform: Matrix,
            ) {
                renderer?.draw(canvas, inProgressStroke, strokeToScreenTransform)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootView = FrameLayout(this)
        mainView = FrameLayout(this)
        rootView.addView(mainView)
        setContentView(rootView)

        renderHelper =
            CanvasInProgressStrokesRenderHelperV33(
                mainView,
                delegatingCallback,
                delegatingRenderer,
                fakeThreads.uiThreadExecutors,
                fakeThreads.renderThreadExecutors,
            )
    }

    internal class FakeThreads {
        enum class ThreadId {
            TEST,
            UI,
            RENDER,
        }

        private var currentThreadId = ThreadId.TEST

        val clock =
            FakeClock(1000) { newTimeMillis ->
                _uiThreadExecutors.onNewTime(newTimeMillis)
                _renderThreadExecutors.onNewTime(newTimeMillis)
            }

        private val _uiThreadExecutors = FakeScheduledExecutor(ThreadId.UI)
        val uiThreadExecutors: CanvasInProgressStrokesRenderHelperV33.ScheduledExecutor =
            _uiThreadExecutors

        private val _renderThreadExecutors = FakeScheduledExecutor(ThreadId.RENDER)
        val renderThreadExecutors: CanvasInProgressStrokesRenderHelperV33.ScheduledExecutor =
            _renderThreadExecutors

        fun uiThreadReadyTaskCount() = _uiThreadExecutors.tasks.size

        fun renderThreadReadyTaskCount() = _renderThreadExecutors.tasks.size

        fun uiThreadDelayedTaskCount() = _uiThreadExecutors.delayedTasks.size

        fun renderThreadDelayedTaskCount() = _renderThreadExecutors.delayedTasks.size

        fun runUiThreadOnce() = runFakeThreadOnce(_uiThreadExecutors)

        fun runRenderThreadOnce() = runFakeThreadOnce(_renderThreadExecutors)

        fun runUiThreadToIdle() = runFakeThreadToIdle(_uiThreadExecutors)

        fun runRenderThreadToIdle() = runFakeThreadToIdle(_renderThreadExecutors)

        fun runOnUiThread(block: () -> Unit) {
            val previousThreadId = currentThreadId
            currentThreadId = ThreadId.UI
            block()
            currentThreadId = previousThreadId
        }

        fun runOnRenderThread(block: () -> Unit) {
            val previousThreadId = currentThreadId
            currentThreadId = ThreadId.RENDER
            block()
            currentThreadId = previousThreadId
        }

        private fun runFakeThreadOnce(fakeThread: FakeScheduledExecutor): Boolean {
            val previousThreadId = currentThreadId
            currentThreadId = fakeThread.threadId
            var ranAny = false
            if (fakeThread.tasks.isNotEmpty()) {
                fakeThread.tasks.removeAt(0).run()
                ranAny = true
            }
            currentThreadId = previousThreadId
            return ranAny
        }

        private fun runFakeThreadToIdle(fakeThread: FakeScheduledExecutor): Boolean {
            val previousThreadId = currentThreadId
            currentThreadId = fakeThread.threadId
            var ranAny = false
            while (fakeThread.tasks.isNotEmpty()) {
                fakeThread.tasks.removeAt(0).run()
                ranAny = true
            }
            currentThreadId = previousThreadId
            return ranAny
        }

        private inner class FakeScheduledExecutor(val threadId: ThreadId) :
            CanvasInProgressStrokesRenderHelperV33.ScheduledExecutor {
            val tasks = mutableListOf<Runnable>()
            /** Each element is a delayed runtime (in the [clock] time space) with its task. */
            val delayedTasks = mutableListOf<Pair<Long, Runnable>>()

            override fun onThread() = currentThreadId == threadId

            override fun execute(command: Runnable) {
                tasks.add(command)
            }

            override fun executeDelayed(
                command: Runnable,
                delayTime: Long,
                delayTimeUnit: TimeUnit
            ) {
                if (delayTime == 0L) {
                    execute(command)
                } else {
                    delayedTasks.add(
                        Pair(clock.currentTimeMillis + delayTimeUnit.toMillis(delayTime), command)
                    )
                }
            }

            fun onNewTime(timeMillis: Long) {
                val potentialTasks = delayedTasks.iterator()
                for ((taskTimeMillis, task) in potentialTasks) {
                    if (taskTimeMillis <= timeMillis) {
                        potentialTasks.remove()
                        tasks.add(task)
                    }
                }
            }
        }
    }

    internal class FakeClock(initialTimeMillis: Long, private var onNewTime: (Long) -> Unit) {
        var currentTimeMillis = initialTimeMillis
            set(value) {
                field = value
                onNewTime(field)
            }
    }
}
