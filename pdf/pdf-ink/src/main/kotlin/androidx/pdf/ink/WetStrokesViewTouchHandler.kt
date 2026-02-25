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

import android.graphics.Matrix
import android.graphics.RectF
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresExtension
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.input.motionprediction.MotionEventPredictor
import androidx.pdf.annotation.PageInfoProvider
import androidx.pdf.ink.util.InkDefaults

/**
 * Handles touch events on an [InProgressStrokesView] for ink drawing.
 *
 * @param pageInfoProvider Provider for page-specific information like zoom and bounds.
 * @param onStrokeStartedListener A listener that is invoked when a new ink stroke is initiated.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class WetStrokesViewTouchHandler(
    private val pageInfoProvider: PageInfoProvider,
    private val onStrokeStartedListener: OnStrokeStartedListener,
) : View.OnTouchListener {

    private var currentPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var currentStrokeId: InProgressStrokeId? = null
    private var motionEventPredictor: MotionEventPredictor? = null
    private var currentPageInfo: PageInfoProvider.PageInfo? = null
    private var lastValidEvent: MotionEvent? = null
    private val matrixValues = FloatArray(9)

    /** The brush needs to be used for any new Strokes on [InProgressStrokesView]. */
    var brushForInking: Brush = InkDefaults.PEN_BRUSH

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val wetStrokesView = view as? InProgressStrokesView ?: return false
        if (motionEventPredictor == null) {
            motionEventPredictor = MotionEventPredictor.newInstance(wetStrokesView)
        }
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            view.requestUnbufferedDispatch(event)
        }

        motionEventPredictor?.record(event)
        val predictedEvent = motionEventPredictor?.predict()

        try {
            return handleTouchEvent(wetStrokesView, event, predictedEvent)
        } finally {
            predictedEvent?.recycle()
        }
    }

    private fun handleTouchEvent(
        view: InProgressStrokesView,
        event: MotionEvent,
        predictedEvent: MotionEvent?,
    ): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleActionDown(view, event)
            MotionEvent.ACTION_MOVE -> handleActionMove(view, event, predictedEvent)
            MotionEvent.ACTION_UP -> handleActionUp(view, event)
            MotionEvent.ACTION_CANCEL -> handleActionCancel(view, event)
            else -> false
        }
    }

    private fun handleActionDown(view: InProgressStrokesView, event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        currentStrokeId?.let {
            view.cancelStroke(it, event)
            resetStrokeState()
        }

        currentPointerId = pointerId
        currentPageInfo = pageInfoProvider.getPageInfoFromViewCoordinates(event.x, event.y)

        val insetPageBounds = getInsetPageBounds()
        if (insetPageBounds == null || !insetPageBounds.contains(event.x, event.y)) {
            return false
        }

        val pageInfo = currentPageInfo ?: return false
        val strokeToWorldTransform = pageInfo.pageToViewTransform

        currentStrokeId =
            view.startStroke(
                event = event,
                pointerId = pointerId,
                brush = brushForInking,
                strokeToWorldTransform = strokeToWorldTransform,
            )
        onStrokeStartedListener.onStrokeStarted(currentStrokeId!!, pageInfo.pageNum)
        lastValidEvent?.recycle()
        lastValidEvent = MotionEvent.obtain(event)
        return true
    }

    private fun handleActionMove(
        view: InProgressStrokesView,
        event: MotionEvent,
        predictedEvent: MotionEvent?,
    ): Boolean {
        val activeStrokeId = currentStrokeId ?: return false
        if (currentPointerId == MotionEvent.INVALID_POINTER_ID) return false

        val pointerIndex = event.findPointerIndex(currentPointerId)
        if (pointerIndex == MotionEvent.INVALID_POINTER_ID) {
            currentStrokeId?.let { strokeId -> view.cancelStroke(strokeId, event) }
            resetStrokeState()
            return true
        }

        val currentEventX = predictedEvent?.x ?: event.getX(pointerIndex)
        val currentEventY = predictedEvent?.y ?: event.getY(pointerIndex)

        val insetPageBounds = getInsetPageBounds()
        if (insetPageBounds != null && insetPageBounds.contains(currentEventX, currentEventY)) {
            // Pointer is within the current page bounds, add to the stroke.
            view.addToStroke(event, currentPointerId, activeStrokeId, predictedEvent)
            lastValidEvent?.recycle()
            lastValidEvent = MotionEvent.obtain(event)
        } else {
            val eventToFinishWith = lastValidEvent ?: event
            view.finishStroke(eventToFinishWith, currentPointerId, activeStrokeId)
            resetStrokeState()
        }
        return true
    }

    private fun handleActionUp(view: InProgressStrokesView, event: MotionEvent): Boolean {
        if (isEventForActivePointer(event)) {
            currentStrokeId?.let { strokeId ->
                view.finishStroke(event, currentPointerId, strokeId)
            }
            resetStrokeState()
            view.performClick()
        }
        return true
    }

    private fun handleActionCancel(view: InProgressStrokesView, event: MotionEvent): Boolean {
        if (isEventForActivePointer(event)) {
            currentStrokeId?.let { strokeId -> view.cancelStroke(strokeId, event) }
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

    private fun getInsetPageBounds(): RectF? {
        val pageInfo = currentPageInfo ?: return null

        // Extract the zoom (scale) from the transformation matrix
        pageInfo.pageToViewTransform.getValues(matrixValues)
        val zoom = matrixValues[Matrix.MSCALE_X]

        val brushRadius = brushForInking.size / 2
        val totalInset = (brushRadius) * zoom

        // Duplicate page bounds to avoid mutating the original, then apply the inset.
        return RectF(pageInfo.pageBounds).apply { inset(totalInset, totalInset) }
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
}
