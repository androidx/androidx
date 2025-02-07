/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.view

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Range
import android.util.SparseArray
import androidx.pdf.PdfDocument
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns and updates all pagination-related state, including the range of pages that are visible in
 * the viewport, the position of each page in the viewport, and the dimensions of each page in
 * content coordinates.
 *
 * Not thread safe
 */
internal class PageLayoutManager(
    private val pdfDocument: PdfDocument,
    private val backgroundScope: CoroutineScope,
    private val pagePrefetchRadius: Int,
    // TODO(b/376299551) - Make page margin configurable via XML attribute
    pageSpacingPx: Int = DEFAULT_PAGE_SPACING_PX,
    internal val paginationModel: PaginationModel =
        PaginationModel(pageSpacingPx, pdfDocument.pageCount)
) {
    /** The 0-indexed maximum page number whose dimensions are known to this model */
    val reach
        get() = paginationModel.reach

    private val _dimensions = MutableSharedFlow<Pair<Int, Point>>(replay = pdfDocument.pageCount)

    /**
     * A [SharedFlow] of PDF page dimensions, represented by a [Pair] whose first value is the page
     * number and whose second value is a [Point] representing the page's dimensions in PDF points
     */
    val dimensions: SharedFlow<Pair<Int, Point>>
        get() = _dimensions

    private val _visiblePages = MutableStateFlow<Range<Int>>(Range(0, 0))

    /**
     * A [StateFlow] representing the [Range] of pages that are currently visible in the window.
     *
     * Values in the range are 0-indexed.
     */
    val visiblePages: StateFlow<Range<Int>>
        get() = _visiblePages

    private val _fullyVisiblePages = MutableStateFlow<Range<Int>>(Range(0, 0))

    /**
     * A [StateFlow] emitting the range of pages considered to be in the viewport.
     *
     * Values in the range are 0-indexed.
     */
    val fullyVisiblePages: StateFlow<Range<Int>>
        get() = _fullyVisiblePages

    /** The 0-indexed maximum page whose dimensions have been requested */
    private var requestedReach: Int = paginationModel.reach

    /**
     * The current [Job] that is handling dimensions loading work
     *
     * In order to ensure dimensions are loaded sequentially, this [Job] is always [Job.join]ed at
     * the beginning of any coroutine to load new dimensions
     */
    private var currentDimensionsJob: Job? = null

    init {
        // If we received a PaginationModel that already has some dimensions, emit those to the View
        // This is the restored instanceState case
        if (paginationModel.reach >= 0) {
            for (i in 0..paginationModel.reach) {
                _dimensions.tryEmit(i to paginationModel.getPageSize(i))
            }
        }

        increaseReach(pagePrefetchRadius - 1)
    }

    /**
     * Returns a [SparseArray] containing [Rect]s indicating the visible region of each visible
     * page, in page coordinates.
     */
    fun getVisiblePageAreas(pages: Range<Int>, viewport: Rect): SparseArray<Rect> {
        val ret = SparseArray<Rect>(pages.upper - pages.lower + 1)
        for (i in pages.lower..pages.upper) {
            ret.put(i, getPageVisibleArea(i, viewport))
        }
        return ret
    }

    private fun getPageVisibleArea(pageNum: Int, viewport: Rect): Rect {
        val pageLocation = getPageLocation(pageNum, viewport)
        val pageWidth = pageLocation.right - pageLocation.left
        val pageHeight = pageLocation.bottom - pageLocation.top
        return Rect(
            maxOf(viewport.left - pageLocation.left, 0),
            maxOf(viewport.top - pageLocation.top, 0),
            minOf(viewport.right - pageLocation.left, pageWidth),
            minOf(viewport.bottom - pageLocation.top, pageHeight),
        )
    }

    /**
     * Returns the current View-coordinate location of a 0-indexed [pageNum] given the [viewport]
     */
    fun getPageLocation(pageNum: Int, viewport: Rect): Rect {
        return paginationModel.getPageLocation(pageNum, viewport)
    }

    /** Returns the size of the page at [pageNum], or null if we don't know that page's size yet */
    fun getPageSize(pageNum: Int): Point? {
        val size = paginationModel.getPageSize(pageNum)
        if (size == PaginationModel.UNKNOWN_SIZE) return null
        return size
    }

    /**
     * Returns the [PdfPoint] that exists at [contentCoordinates], or null if no page content is
     * laid out at [contentCoordinates].
     *
     * @param contentCoordinates the content coordinates to check (View coordinates that are scaled
     *   up or down by the current zoom level)
     * @param viewport the current viewport in content coordinates
     */
    fun getPdfPointAt(contentCoordinates: PointF, viewport: Rect): PdfPoint? {
        val visiblePages = visiblePages.value
        for (pageIndex in visiblePages.lower..visiblePages.upper) {
            val pageBounds = paginationModel.getPageLocation(pageIndex, viewport)
            if (RectF(pageBounds).contains(contentCoordinates.x, contentCoordinates.y)) {
                return PdfPoint(
                    pageIndex,
                    PointF(
                        contentCoordinates.x - pageBounds.left,
                        contentCoordinates.y - pageBounds.top,
                    )
                )
            }
        }
        return null
    }

    /**
     * Returns a View-relative [RectF] corresponding to a page-relative [PdfRect], or null if the
     * page hasn't been laid out
     */
    fun getViewRect(pdfRect: PdfRect, viewport: Rect): RectF? {
        if (pdfRect.pageNum > paginationModel.reach) return null
        val pageBounds = paginationModel.getPageLocation(pdfRect.pageNum, viewport)
        val out = RectF(pdfRect.pageRect)
        out.offset(pageBounds.left.toFloat(), pageBounds.top.toFloat())
        return out
    }

    /**
     * Emits a new [Range] to [visiblePages] based on the current [scrollY], [height], and [zoom] of
     * a [PdfView]
     */
    fun onViewportChanged(scrollY: Int, height: Int, zoom: Float) {
        val contentTop = floor(scrollY / zoom).toInt()
        val contentBottom = ceil((height + scrollY) / zoom).toInt()
        // Try emit will always succeed for MutableStateFlow
        val prevVisiblePages = _visiblePages.value
        val newVisiblePages = paginationModel.getPagesInViewport(contentTop, contentBottom)

        val fullyVisiblePageRange =
            paginationModel.getPagesInViewport(contentTop, contentBottom, includePartial = false)
        if (fullyVisiblePageRange != _fullyVisiblePages.value) {
            _fullyVisiblePages.tryEmit(fullyVisiblePageRange)
        }

        if (prevVisiblePages != newVisiblePages) {
            _visiblePages.tryEmit(newVisiblePages)
            increaseReach(
                minOf(newVisiblePages.upper + pagePrefetchRadius, paginationModel.numPages - 1)
            )
        }
    }

    /**
     * Sequentially enqueues requests for any pages up to [untilPage] that we haven't requested
     * dimensions for
     */
    internal fun increaseReach(untilPage: Int) {
        if (untilPage < requestedReach) return

        for (i in requestedReach + 1..minOf(untilPage, paginationModel.numPages - 1)) {
            loadPageDimensions(i)
        }
    }

    /** Waits for any outstanding dimensions to be loaded, then loads dimensions for [pageNum] */
    private fun loadPageDimensions(pageNum: Int) {
        requestedReach = pageNum
        val previousDimensionsJob = currentDimensionsJob
        currentDimensionsJob =
            backgroundScope.launch {
                previousDimensionsJob?.join()
                val pageMetadata = pdfDocument.getPageInfo(pageNum)
                val size = Point(pageMetadata.width, pageMetadata.height)
                // Add the value to the model before emitting, and on the main thread
                withContext(Dispatchers.Main) { paginationModel.addPage(pageNum, size) }
                _dimensions.emit(pageNum to Point(pageMetadata.width, pageMetadata.height))
            }
    }

    companion object {
        private const val DEFAULT_PAGE_SPACING_PX: Int = 20
        private const val INVALID_ID = -1
    }
}
