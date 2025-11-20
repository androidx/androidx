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

package androidx.pdf.ink

import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresExtension
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.input.motionprediction.MotionEventPredictor
import androidx.pdf.ink.EditablePdfViewerFragment.PageInfoProvider

/**
 * Handles touch events on an [InProgressStrokesView] for ink drawing.
 *
 * @param wetStrokesView The view for drawing strokes.
 * @param pageInfoProvider Provider for page-specific information like zoom and bounds.
 * @param onStrokeStartedListener A listener that is invoked when a new ink stroke is initiated.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class WetStrokesViewTouchHandler(
    private val wetStrokesView: InProgressStrokesView,
    private val pageInfoProvider: PageInfoProvider,
    private val onStrokeStartedListener: OnStrokeStartedListener,
) : View.OnTouchListener {

    private var currentPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var currentStrokeId: InProgressStrokeId? = null
    private val motionEventPredictor = MotionEventPredictor.newInstance(wetStrokesView)
    private var currentPageInfo: PageInfoProvider.PageInfo? = null
    private var lastValidEvent: MotionEvent? = null

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            view.requestUnbufferedDispatch(event)
        }

        motionEventPredictor.record(event)
        val predictedEvent = motionEventPredictor.predict()

        try {
            return handleTouchEvent(view, event, predictedEvent)
        } finally {
            predictedEvent?.recycle()
        }
    }

    private fun handleTouchEvent(
        view: View,
        event: MotionEvent,
        predictedEvent: MotionEvent?,
    ): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleActionDown(event)
            MotionEvent.ACTION_MOVE -> handleActionMove(event, predictedEvent)
            MotionEvent.ACTION_UP -> handleActionUp(view, event)
            MotionEvent.ACTION_CANCEL -> handleActionCancel(event)
            else -> false
        }
    }

    private fun handleActionDown(event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        currentStrokeId?.let {
            wetStrokesView.cancelStroke(it, event)
            resetStrokeState()
        }

        currentPointerId = pointerId
        currentPageInfo = pageInfoProvider.getCurrentPageInfo(event.x, event.y)

        val pageInfo = currentPageInfo ?: return false

        val strokeToWorldTransform =
            Matrix().apply {
                postScale(pageInfo.zoom, pageInfo.zoom)
                postTranslate(pageInfo.bounds.left, pageInfo.bounds.top)
            }
        currentStrokeId =
            wetStrokesView.startStroke(
                event = event,
                pointerId = pointerId,
                brush = DEFAULT_BRUSH,
                strokeToWorldTransform = strokeToWorldTransform,
            )
        onStrokeStartedListener.onStrokeStarted(currentStrokeId!!, pageInfo.pageNum)
        lastValidEvent?.recycle()
        lastValidEvent = MotionEvent.obtain(event)
        return true
    }

    private fun handleActionMove(event: MotionEvent, predictedEvent: MotionEvent?): Boolean {
        val activeStrokeId = currentStrokeId ?: return true
        if (currentPointerId == MotionEvent.INVALID_POINTER_ID) return true

        val pointerIndex = event.findPointerIndex(currentPointerId)
        if (pointerIndex == MotionEvent.INVALID_POINTER_ID) {
            currentStrokeId?.let { strokeId -> wetStrokesView.cancelStroke(strokeId, event) }
            resetStrokeState()
            return true
        }

        val currentEventX = event.getX(pointerIndex)
        val currentEventY = event.getY(pointerIndex)
        val pageBounds = currentPageInfo?.bounds

        if (pageBounds != null && pageBounds.contains(currentEventX, currentEventY)) {
            // Pointer is within the current page bounds, add to the stroke.
            wetStrokesView.addToStroke(event, currentPointerId, activeStrokeId, predictedEvent)
            lastValidEvent?.recycle()
            lastValidEvent = MotionEvent.obtain(event)
        } else {
            val eventToFinishWith = lastValidEvent ?: event
            wetStrokesView.finishStroke(eventToFinishWith, currentPointerId, activeStrokeId)
            resetStrokeState()
        }
        return true
    }

    private fun handleActionUp(view: View, event: MotionEvent): Boolean {
        if (isEventForActivePointer(event)) {
            currentStrokeId?.let { strokeId ->
                wetStrokesView.finishStroke(event, currentPointerId, strokeId)
            }
            resetStrokeState()
            view.performClick()
        }
        return true
    }

    private fun handleActionCancel(event: MotionEvent): Boolean {
        if (isEventForActivePointer(event)) {
            currentStrokeId?.let { strokeId -> wetStrokesView.cancelStroke(strokeId, event) }
            resetStrokeState()
        }
        return true
    }

    private fun isEventForActivePointer(event: MotionEvent): Boolean =
        event.getPointerId(event.actionIndex) == currentPointerId

    private fun resetStrokeState() {
        currentPointerId = MotionEvent.INVALID_POINTER_ID
        currentStrokeId = null
        currentPageInfo = null
        lastValidEvent?.recycle()
        lastValidEvent = null
    }

    /** Callback invoked when a new ink stroke is started on a page. */
    internal fun interface OnStrokeStartedListener {
        /**
         * Called when a new ink stroke has been successfully initiated.
         *
         * @param strokeId The unique identifier for the newly started in-progress stroke.
         * @param pageNum The page number (0-indexed) on which the stroke was started.
         */
        fun onStrokeStarted(strokeId: InProgressStrokeId, pageNum: Int)
    }

    companion object {
        internal val DEFAULT_BRUSH: Brush =
            Brush.createWithColorIntArgb(
                family = StockBrushes.pressurePen(),
                colorIntArgb = Color.GREEN,
                size = 5F,
                epsilon = 0.15F,
            )
    }
}
