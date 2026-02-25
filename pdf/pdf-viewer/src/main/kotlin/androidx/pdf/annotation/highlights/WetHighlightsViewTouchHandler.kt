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

package androidx.pdf.annotation.highlights

import android.graphics.PointF
import android.view.MotionEvent
import androidx.pdf.annotation.PageInfoProvider
import androidx.pdf.annotation.highlights.models.InProgressHighlightId
import androidx.pdf.annotation.highlights.utils.applyTransform

/** Handles touch events on an [InProgressHighlightsView] to create text highlight annotations. */
internal class WetHighlightsViewTouchHandler(private val pageInfoProvider: PageInfoProvider) {
    private var currentPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var currentAnnotationId: InProgressHighlightId? = null
    private var lastValidPdfPoint: PointF? = null

    fun handleTouchEvent(view: InProgressHighlightsView, event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Request unbuffered dispatch for low-latency processing of this gesture.
                view.requestUnbufferedDispatch(event)
                handleActionDown(view, event)
            }
            MotionEvent.ACTION_MOVE -> handleActionMove(view, event)
            MotionEvent.ACTION_UP -> handleActionUp(view, event)
            MotionEvent.ACTION_CANCEL -> handleActionCancel(view, event)
            else -> false
        }
    }

    private fun handleActionDown(view: InProgressHighlightsView, event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        // If there's an existing annotation being tracked, cancel it.
        currentAnnotationId?.let {
            view.cancelTextHighlight(it)
            resetAnnotationState()
        }

        currentPointerId = pointerId
        val viewPoint = PointF(event.getX(pointerIndex), event.getY(pointerIndex))
        val pageInfo =
            pageInfoProvider.getPageInfoFromViewCoordinates(viewPoint.x, viewPoint.y)
                ?: return false
        val startPdfPoint = viewPoint.applyTransform(pageInfo.viewToPageTransform)
        lastValidPdfPoint = startPdfPoint

        val newAnnotationId = InProgressHighlightId.create()
        currentAnnotationId = newAnnotationId

        view.startTextHighlight(
            id = newAnnotationId,
            pageNum = pageInfo.pageNum,
            startPdfPoint = startPdfPoint,
            startViewPoint = viewPoint,
            pageToViewTransform = pageInfo.pageToViewTransform,
        )
        return true
    }

    private fun handleActionMove(view: InProgressHighlightsView, event: MotionEvent): Boolean {
        val activeAnnotationId = currentAnnotationId ?: return false

        val pointerIndex = event.findPointerIndex(currentPointerId)
        if (pointerIndex == MotionEvent.INVALID_POINTER_ID) {
            view.cancelTextHighlight(activeAnnotationId)
            resetAnnotationState()
            return true
        }

        val viewPoint = PointF(event.getX(pointerIndex), event.getY(pointerIndex))
        val pageInfo =
            pageInfoProvider.getPageInfoFromViewCoordinates(viewPoint.x, viewPoint.y) ?: return true
        val currentPdfPoint = viewPoint.applyTransform(pageInfo.viewToPageTransform)
        lastValidPdfPoint = currentPdfPoint

        view.addToTextHighlight(id = activeAnnotationId, currentPdfPoint = currentPdfPoint)
        return true
    }

    private fun handleActionUp(view: InProgressHighlightsView, event: MotionEvent): Boolean {
        val activeAnnotationId = currentAnnotationId ?: return false
        if (currentPointerId == MotionEvent.INVALID_POINTER_ID) return false

        try {
            if (isEventForActivePointer(event)) {
                val pointerIndex = event.findPointerIndex(currentPointerId)
                val viewPoint = PointF(event.getX(pointerIndex), event.getY(pointerIndex))
                val pageInfo =
                    pageInfoProvider.getPageInfoFromViewCoordinates(viewPoint.x, viewPoint.y)
                val finalPdfPoint =
                    if (pageInfo != null) {
                        viewPoint.applyTransform(pageInfo.viewToPageTransform)
                    } else {
                        // When the gesture ends outside of the page bounds, use the last valid
                        // point to end the gesture.
                        lastValidPdfPoint ?: return true
                    }
                view.finishTextHighlight(activeAnnotationId, finalPdfPoint)
                view.performClick()
            }
        } finally {
            resetAnnotationState()
        }
        return true
    }

    private fun handleActionCancel(view: InProgressHighlightsView, event: MotionEvent): Boolean {
        val activeAnnotationId = currentAnnotationId ?: return false
        try {
            if (isEventForActivePointer(event)) {
                view.cancelTextHighlight(activeAnnotationId)
            }
        } finally {
            resetAnnotationState()
        }
        return true
    }

    private fun resetAnnotationState() {
        currentPointerId = MotionEvent.INVALID_POINTER_ID
        currentAnnotationId = null
    }

    private fun isEventForActivePointer(event: MotionEvent): Boolean {
        return event.getPointerId(event.actionIndex) == currentPointerId
    }
}
