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
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.os.HandlerCompat
import androidx.pdf.PdfDocument
import androidx.pdf.R
import androidx.pdf.annotation.PageInfoProvider
import androidx.pdf.annotation.highlights.models.HighlightState
import androidx.pdf.annotation.highlights.models.InProgressHighlightId
import androidx.pdf.annotation.highlights.utils.calculateHighlightRects
import androidx.pdf.annotation.highlights.utils.computeBoundingBox
import androidx.pdf.annotation.highlights.utils.toPathPdfObjects
import androidx.pdf.annotation.models.StampAnnotation
import androidx.pdf.exceptions.RequestFailedException
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

    /** The PDF document used to query text boundaries. */
    var pdfDocument: PdfDocument? = null
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
    private val inProgressTextHighlightsListeners =
        mutableListOf<InProgressTextHighlightsListener>()
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
                processRequest(request.block)
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

    /** Adds a listener to be notified of highlight gesture events. */
    fun addInProgressTextHighlightsListener(listener: InProgressTextHighlightsListener) {
        inProgressTextHighlightsListeners.add(listener)
    }

    /** Removes a listener that was previously added via [addInProgressTextHighlightsListener]. */
    fun removeInProgressTextHighlightsListener(listener: InProgressTextHighlightsListener) {
        inProgressTextHighlightsListeners.remove(listener)
    }

    /** Starts a highlight gesture. Invokes callbacks based on whether text is found. */
    fun startTextHighlight(
        id: InProgressHighlightId,
        pageNum: Int,
        startPdfPoint: PointF,
        startViewPoint: PointF,
        pageToViewTransform: Matrix,
    ) {
        val doc = pdfDocument ?: return

        activeHighlights[id] =
            HighlightState(pageNum, highlightColor, pageToViewTransform, startPdfPoint, emptyList())

        tryHighlighting {
            val pageRects = doc.calculateHighlightRects(pageNum, startPdfPoint, startPdfPoint)

            if (pageRects.isNotEmpty()) {
                val viewRects =
                    pageRects.map { pageRect ->
                        RectF().apply { pageToViewTransform.mapRect(this, pageRect) }
                    }

                // If the gesture hasn't been canceled, update its state and notify listeners.
                activeHighlights[id]?.let { currentState ->
                    activeHighlights[id] = currentState.copy(selectionRects = viewRects)
                    inProgressTextHighlightsListeners.forEach {
                        it.onTextHighlightStarted(startViewPoint, id)
                    }
                }
                invalidate()
            } else {
                activeHighlights.remove(id)
                inProgressTextHighlightsListeners.forEach {
                    it.onTextHighlightRejected(startViewPoint)
                }
            }
        }
    }

    /** Updates an existing highlight gesture. */
    fun addToTextHighlight(id: InProgressHighlightId, currentPdfPoint: PointF) {
        val doc = pdfDocument ?: return
        activeHighlights[id]?.let { currentState ->
            updateRequests.trySend(
                HighlightRequest({
                    val pageRects =
                        doc.calculateHighlightRects(
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
                })
            )
        }
    }

    /** Finalizes the highlight gesture, converting it to a stamp annotation. */
    fun finishTextHighlight(id: InProgressHighlightId, finalPdfPoint: PointF) {
        val doc = pdfDocument ?: return
        activeHighlights[id]?.let { currentState ->
            tryHighlighting {
                val pageRects =
                    doc.calculateHighlightRects(
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
                    inProgressTextHighlightsListeners.forEach {
                        it.onTextHighlightFinished(mapOf(id to annotation))
                    }
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

    private fun tryHighlighting(block: suspend () -> Unit) {
        viewScope?.launch { processRequest(block) }
    }

    private suspend fun processRequest(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: RequestFailedException) {
            inProgressTextHighlightsListeners.forEach { it.onTextHighlightError(e) }
        }
    }

    @JvmInline private value class HighlightRequest(val block: suspend () -> Unit)
}
