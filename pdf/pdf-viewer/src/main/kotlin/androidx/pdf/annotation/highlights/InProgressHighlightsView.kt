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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.RemoteException
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.os.HandlerCompat
import androidx.pdf.R
import androidx.pdf.annotation.OnAnnotationEditListener
import androidx.pdf.annotation.OnGestureClaimListener
import androidx.pdf.annotation.PageInfoProvider
import androidx.pdf.annotation.TextBoundsProvider
import androidx.pdf.annotation.highlights.models.HighlightState
import androidx.pdf.annotation.highlights.models.InProgressHighlightId
import androidx.pdf.annotation.highlights.utils.computeBoundingBox
import androidx.pdf.annotation.highlights.utils.createTextBoundsRequestFailedException
import androidx.pdf.annotation.highlights.utils.toPathPdfObjects
import androidx.pdf.annotation.models.StampAnnotation
import androidx.pdf.util.ExceptionUtils.isHandledRemoteException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** A [View] that renders in-progress "wet" text highlights over PDF content. */
internal class InProgressHighlightsView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    /** A provider interface that abstracts the retrieval of text boundary information. */
    var textBoundsProvider: TextBoundsProvider? = null
        set(value) {
            if (field == value) return
            field = value
            activeHighlights.clear()
            invalidate()
        }

    var pageInfoProvider: PageInfoProvider? = null
        set(value) {
            if (field == value) return
            field = value
            touchHandler = value?.let { WetHighlightsViewTouchHandler(it) }
        }

    var highlightColor: Int = context.getColor(R.color.default_highlight_color)

    private var viewScope: CoroutineScope? = null
    private var touchHandler: WetHighlightsViewTouchHandler? = null

    private var onGestureClaimListener: OnGestureClaimListener? = null
    private val onAnnotationEditListeners = mutableListOf<OnAnnotationEditListener>()

    private val activeHighlights = mutableMapOf<InProgressHighlightId, HighlightState>()
    private val updateRequests = Channel<HighlightRequest>(Channel.CONFLATED)

    private val paint = Paint().apply { style = Paint.Style.FILL }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewScope =
            CoroutineScope(
                SupervisorJob() + HandlerCompat.createAsync(handler.looper).asCoroutineDispatcher()
            )
        viewScope?.launch {
            for (request in updateRequests) {
                processRequest(request.pageNum, request.block)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope?.coroutineContext?.get(Job)?.cancel()
        viewScope = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return touchHandler?.handleTouchEvent(this, event) ?: super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (activeHighlights.isEmpty()) return

        activeHighlights.values.forEach { state ->
            paint.color = state.color
            state.selectionRects.forEach { viewRect -> canvas.drawRect(viewRect, paint) }
        }
    }

    /** Sets a listener for generic gesture coordination events. */
    fun setOnGestureClaimListener(listener: OnGestureClaimListener?) {
        onGestureClaimListener = listener
    }

    /** Adds a listener for annotation result events. */
    fun addOnAnnotationEditListener(listener: OnAnnotationEditListener) {
        onAnnotationEditListeners.add(listener)
    }

    /** Removes a listener for annotation result events. */
    fun removeOnAnnotationEditListener(listener: OnAnnotationEditListener) {
        onAnnotationEditListeners.remove(listener)
    }

    /** Starts a highlight gesture. Invokes callbacks based on whether text is found. */
    fun startTextHighlight(
        id: InProgressHighlightId,
        pageNum: Int,
        startPdfPoint: PointF,
        pageToViewTransform: Matrix,
    ) {
        val localTextBoundsProvider = textBoundsProvider ?: return

        activeHighlights[id] =
            HighlightState(pageNum, highlightColor, pageToViewTransform, startPdfPoint, emptyList())

        tryHighlighting(pageNum) {
            val pageRects =
                localTextBoundsProvider.getTextBoundsBetweenPoints(
                    pageNum,
                    startPdfPoint,
                    startPdfPoint,
                )

            if (pageRects.isNotEmpty()) {
                val viewRects =
                    pageRects.map { pageRect ->
                        RectF().apply { pageToViewTransform.mapRect(this, pageRect) }
                    }

                // If the gesture hasn't been canceled, update its state and notify listeners.
                activeHighlights[id]?.let { currentState ->
                    activeHighlights[id] =
                        currentState.copy(selectionRects = viewRects, isClaimed = true)
                    onGestureClaimListener?.onGestureClaimed()
                }
                invalidate()
            } else {
                activeHighlights.remove(id)
                onGestureClaimListener?.onGestureAbandoned()
            }
        }
    }

    /** Updates an existing highlight gesture. */
    fun addToTextHighlight(id: InProgressHighlightId, currentPdfPoint: PointF) {
        val localTextBoundsProvider = textBoundsProvider ?: return
        activeHighlights[id]?.let { currentState ->
            if (!currentState.isClaimed) return

            updateRequests.trySend(
                HighlightRequest(currentState.pageNum) {
                    val pageRects =
                        localTextBoundsProvider.getTextBoundsBetweenPoints(
                            currentState.pageNum,
                            currentState.startPdfPoint,
                            currentPdfPoint,
                        )
                    val newViewRects =
                        pageRects.map { pageRect ->
                            RectF().apply {
                                currentState.pageToViewTransform.mapRect(this, pageRect)
                            }
                        }

                    // Check if the highlight is still active before updating the viewRects.
                    if (activeHighlights.contains(id)) {
                        activeHighlights[id] = currentState.copy(selectionRects = newViewRects)
                        invalidate()
                    }
                }
            )
        }
    }

    /** Finalizes the highlight gesture, converting it to a stamp annotation. */
    fun finishTextHighlight(id: InProgressHighlightId, finalPdfPoint: PointF) {
        val localTextBoundsProvider = textBoundsProvider ?: return
        activeHighlights[id]?.let { currentState ->
            tryHighlighting(currentState.pageNum) {
                val pageRects =
                    localTextBoundsProvider.getTextBoundsBetweenPoints(
                        currentState.pageNum,
                        currentState.startPdfPoint,
                        finalPdfPoint,
                    )
                if (pageRects.isNotEmpty()) {
                    val boundingBox = pageRects.computeBoundingBox()
                    val pathObjects = pageRects.toPathPdfObjects(currentState.color)
                    val annotation =
                        StampAnnotation(
                            pageNum = currentState.pageNum,
                            bounds = boundingBox,
                            pdfObjects = pathObjects,
                        )
                    onAnnotationEditListeners.forEach { it.onAnnotationCreated(annotation) }
                }
                activeHighlights.remove(id)
                invalidate()
            }
        }
    }

    internal fun cancelTextHighlight(id: InProgressHighlightId) {
        if (activeHighlights.remove(id) != null) {
            invalidate()
        }
    }

    private fun tryHighlighting(pageNum: Int, block: suspend () -> Unit) {
        viewScope?.launch { processRequest(pageNum, block) }
    }

    private suspend fun processRequest(pageNum: Int, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: RemoteException) {
            if (!e.isHandledRemoteException) throw e

            val errorToNotify = createTextBoundsRequestFailedException(pageNum, e)
            onAnnotationEditListeners.forEach { it.onAnnotationError(errorToNotify) }
        }
    }

    private data class HighlightRequest(val pageNum: Int, val block: suspend () -> Unit)
}
