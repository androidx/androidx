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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.geometry.MutableBox
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.InProgressStroke

/**
 * An implementation of [InProgressStrokesRenderHelper] that works on Android versions before
 * [android.os.Build.VERSION_CODES.Q]. This implementation renders in-progress strokes via the
 * [View] hierarchy using a [CanvasStrokeRenderer], where everything occurs on the UI thread.
 * Support of pre-Q Android versions comes with the expense of rendering latency that is higher than
 * it would be with [androidx.graphics.lowlatency.CanvasFrontBufferedRenderer].
 */
@OptIn(ExperimentalLatencyDataApi::class)
@UiThread
internal class CanvasInProgressStrokesRenderHelperV21(
    private val mainView: ViewGroup,
    private val callback: InProgressStrokesRenderHelper.Callback,
    private val renderer: CanvasStrokeRenderer,
) : InProgressStrokesRenderHelper {

    // View hierarchy rendering does not retain its contents between frames, so all contents must be
    // redrawn on every frame.
    override val contentsPreservedBetweenDraws = false

    override val supportsDebounce = false

    override val supportsFlush = false

    override var maskPath: Path? = null

    private val maskPaint =
        Paint().apply {
            color = Color.TRANSPARENT
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

    /** Set during active drawing, unset otherwise. */
    private var canvasForCurrentDraw: Canvas? = null

    private val innerView =
        object : View(mainView.context) {
            override fun onDraw(canvas: Canvas) {
                assertOnUiThread()
                // Just in case save/restores get imbalanced among callbacks
                val originalSaveCount = canvas.saveCount
                canvasForCurrentDraw = canvas
                try {
                    callback.onDraw()
                } finally {
                    // NOMUTANTS -- Defensive programming.
                    canvasForCurrentDraw = null
                }

                // Clear the client-defined masked area.
                maskPath?.let { canvas.drawPath(it, maskPaint) }

                callback.onDrawComplete()
                callback.setCustomLatencyDataField(finishesDrawCallsSetter)
                // TODO: b/316891464 - Use Choreographer to estimate the next frame time.
                callback.handOffAllLatencyData()

                check(canvas.saveCount == originalSaveCount) {
                    "Unbalanced saves and restores. Expected save count of $originalSaveCount, got ${canvas.saveCount}."
                }
            }
        }

    /**
     * Defined as a lambda instead of a member function or companion object function to ensure that
     * no extra allocation takes place when passing this function object into the higher-level
     * callback.
     */
    private val finishesDrawCallsSetter = { data: LatencyData, timeNanos: Long ->
        data.hwuiInProgressStrokesRenderHelperData.finishesDrawCalls = timeNanos
    }

    private val viewListener =
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                addInnerToMainView()
            }

            override fun onViewDetachedFromWindow(v: View) {
                mainView.removeView(innerView)
            }
        }

    init {
        if (mainView.isAttachedToWindow) {
            addInnerToMainView()
        }
        mainView.addOnAttachStateChangeListener(viewListener)
    }

    override fun assertOnRenderThread() = assertOnUiThread()

    private fun assertOnUiThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Expected to be running on UI thread, but instead running on ${Thread.currentThread()}."
        }
    }

    override fun requestDraw() {
        assertOnUiThread()
        // This leads to innerView.onDraw.
        innerView.invalidate()
    }

    override fun prepareToDrawInModifiedRegion(modifiedRegionInMainView: MutableBox) = Unit

    override fun drawInModifiedRegion(
        inProgressStroke: InProgressStroke,
        strokeToMainViewTransform: Matrix,
    ) {
        assertOnUiThread()
        val canvas =
            checkNotNull(canvasForCurrentDraw) { "Can only render during Callback.onDraw." }
        renderer.draw(canvas, inProgressStroke, strokeToMainViewTransform)
    }

    override fun afterDrawInModifiedRegion() = Unit

    override fun clear() {
        // View hierarchy rendering does not retain its buffer contents between frames (all contents
        // must be redrawn with every frame), so clearing takes place automatically by simply not
        // rendering anything in the next innerView.onDraw.
    }

    override fun requestStrokeCohortHandoffToHwui(
        handingOff: Map<InProgressStrokeId, FinishedStroke>
    ) {
        // The callback will ensure that the handoff data is drawn in HWUI in its next frame.
        callback.onStrokeCohortHandoffToHwui(handingOff)
        // Ensure that the next innerView.onDraw, when it calls callback.onDraw, will not result in
        // any
        // calls to drawInModifiedRegion - which will ensure that innerView has no content on the
        // next
        // HWUI frame.
        innerView.invalidate()
        // Because everything is synchronized to HWUI frames, there is no need to delay the next
        // cohort.
        callback.onStrokeCohortHandoffToHwuiComplete()
    }

    private fun addInnerToMainView() {
        assertOnUiThread()
        mainView.addView(
            innerView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }
}
