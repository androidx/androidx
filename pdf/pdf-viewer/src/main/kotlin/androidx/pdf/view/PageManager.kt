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

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.RectF
import android.util.Range
import android.util.SparseArray
import androidx.core.util.isEmpty
import androidx.core.util.keyIterator
import androidx.core.util.valueIterator
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.models.FormWidgetInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Manages a collection of [Page]s, each representing a single PDF page. Receives events to update
 * pages' internal state, and produces events via a [SharedFlow] of type [Unit] to signal the host
 * [PdfView] to invalidate itself when any page needs to be redrawn. Operations like drawing pages
 * and handling touch events on pages may be delegated to this manager.
 *
 * Not thread safe
 */
internal class PageManager(
    private val pdfDocument: PdfDocument,
    private val backgroundScope: CoroutineScope,
    /**
     * The maximum size of any single [android.graphics.Bitmap] we render for a page, i.e. the
     * threshold for tiled rendering
     */
    private val maxBitmapSizePx: Point,
    /** Error flow for propagating error occurred while processing to [PdfView]. */
    private val errorFlow: MutableSharedFlow<Throwable>,
    isAccessibilityEnabled: Boolean,
) {
    /**
     * Replay at least 1 value in case of an invalidation signal issued while [PdfView] is not
     * collecting
     */
    private val _invalidationSignalFlow = MutableSharedFlow<Unit>(replay = 1)

    /**
     * This [SharedFlow] serves as an event bus of sorts to signal our host [PdfView] to invalidate
     * itself in a decoupled way. It is of type [Unit] because the reason for invalidation is
     * inconsequential. The model is: we update the data that affects what will be drawn, we signal
     * [PdfView] to invalidate itself, and the relevant changes in state will be reflected in the
     * next call to [PdfView.onDraw]
     */
    val invalidationSignalFlow: SharedFlow<Unit>
        get() = _invalidationSignalFlow

    internal val pages = SparseArray<Page>()

    private val _pageTextReadyFlow = MutableSharedFlow<Int>(replay = 1)
    val pageTextReadyFlow: SharedFlow<Int>
        get() = _pageTextReadyFlow

    internal var isAccessibilityEnabled: Boolean = isAccessibilityEnabled
        set(value) {
            field = value
            for (page in pages.valueIterator()) {
                page.isAccessibilityEnabled = value
            }
        }

    internal fun areAllVisiblePagesFullyRendered(
        visiblePageRange: Range<Int>,
        zoom: Float,
        visiblePageAreas: SparseArray<RectF>?,
    ): Boolean =
        (visiblePageRange.lower..visiblePageRange.upper).all { pageNum ->
            pages[pageNum]?.isFullyRendered(zoom, visiblePageAreas?.get(pageNum)) ?: false
        }

    /**
     * [Highlight]s supplied by the developer to be drawn along with the pages they belong to
     *
     * We store these in a map keyed by page number for more efficient lookup at drawing time, even
     * though each [Highlight] contains its own page number.
     */
    private val highlights: MutableMap<Int, MutableList<Highlight>> = mutableMapOf()

    /**
     * Updates the visibility state of [Page]s owned by this manager.
     *
     * @param visiblePageAreas the visible area of each visible page, in page coordinates
     * @param currentZoomLevel the current zoom level
     * @param stablePosition true if we don't believe our position is actively changing
     * @param pauseBitmapFetch true if we should avoid fetching Bitmaps, regardless of current
     *   visibility
     */
    fun updatePageVisibilities(
        visiblePageAreas: SparseArray<RectF>,
        currentZoomLevel: Float,
        stablePosition: Boolean,
        pauseBitmapFetch: Boolean,
    ) {
        if (visiblePageAreas.isEmpty()) return
        // Start preparing UI for visible pages
        visiblePageAreas.keyIterator().forEach { pageNum ->
            pages[pageNum]?.setVisible(
                currentZoomLevel,
                visiblePageAreas.get(pageNum),
                stablePosition,
                pauseBitmapFetch,
            )
        }

        // We put pages that are near the viewport in a "nearly visible" state where some data is
        // retained. We release all data from pages well outside the viewport
        val nearPages =
            Range(
                maxOf(0, visiblePageAreas.keyAt(0) - PAGE_RETENTION_RADIUS),
                minOf(
                    visiblePageAreas.keyAt(visiblePageAreas.size() - 1) + PAGE_RETENTION_RADIUS,
                    pdfDocument.pageCount - 1,
                ),
            )
        for (pageNum in pages.keyIterator()) {
            if (pageNum < nearPages.lower || pageNum > nearPages.upper) {
                pages[pageNum]?.setInvisible()
            } else if (!visiblePageAreas.contains(pageNum)) {
                pages[pageNum]?.setNearlyVisible()
            }
        }
    }

    /**
     * Invalidates the given [areasToUpdate] for the [Page] at [pageNum].
     *
     * This function checks if the union of [areasToUpdate] intersects with the [visibleArea]. If
     * there's an intersection, it updates the specific page to invalidate the intersecting area.
     */
    fun maybeInvalidateAreas(
        pageNum: Int,
        visibleArea: RectF,
        currentZoom: Float,
        areasToUpdate: List<RectF>,
    ) {
        val invalidatedArea = areasToUpdate.union()
        if (invalidatedArea.intersect(visibleArea)) {
            // If there is some intersection in the visible area and the invalidated area,
            // invalidatedArea is updated to hold the intersection.
            pages[pageNum]?.maybeInvalidateAreas(currentZoom, invalidatedArea)
        }
    }

    /**
     * Updates the set of [Page]s owned by this manager when a new Page's dimensions are loaded.
     * Dimensions are the minimum data required to instantiate a page.
     */
    fun addPage(
        pageNum: Int,
        size: Point,
        currentZoomLevel: Float,
        stablePosition: Boolean,
        viewArea: RectF? = null,
        pauseBitmapFetch: Boolean,
        pdfFormFillingConfig: PdfFormFillingConfig,
        formWidgetInfos: List<FormWidgetInfo>? = null,
    ) {
        if (pages.contains(pageNum)) return
        val page =
            Page(
                    pageNum,
                    size,
                    pdfDocument,
                    backgroundScope,
                    maxBitmapSizePx,
                    onPageUpdate = { _invalidationSignalFlow.tryEmit(Unit) },
                    onPageTextReady = { pageNumber -> _pageTextReadyFlow.tryEmit(pageNumber) },
                    errorFlow = errorFlow,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    pdfFormFillingConfig = pdfFormFillingConfig,
                    formWidgetInfos = formWidgetInfos,
                )
                .apply {
                    // If the page is visible, let it know
                    if (viewArea != null) {
                        setVisible(currentZoomLevel, viewArea, stablePosition, pauseBitmapFetch)
                    }
                }
        pages.put(pageNum, page)
    }

    fun maybeLoadFormWidgetMetadata(formWidgetMetadataLoader: FormWidgetMetadataLoader) {
        pages.valueIterator().forEach {
            if (it.formWidgetInfos == null) {
                it.maybeUpdateFormWidgetInfos(formWidgetMetadataLoader)
            }
        }
    }

    /** Updates the form widget information in the given [pageNum] when a edit is applied. */
    fun maybeUpdateFormWidgetMetadata(
        pageNum: Int,
        formWidgetMetadataLoader: FormWidgetMetadataLoader,
    ) {
        pages[pageNum]?.maybeUpdateFormWidgetInfos(formWidgetMetadataLoader)
    }

    /** Adds [newHighlights]s to this manager to be drawn along with the pages they belong to */
    fun setHighlights(newHighlights: List<Highlight>) {
        highlights.clear()
        for (highlight in newHighlights) {
            highlights.getOrPut(highlight.area.pageNum) { mutableListOf() }.add(highlight)
        }
        _invalidationSignalFlow.tryEmit(Unit)
    }

    /** Draws the [Page] at [pageNum] to the canvas at [locationInView] */
    fun drawPage(pageNum: Int, canvas: Canvas, locationInView: RectF) {
        val highlightsForPage = highlights.getOrDefault(pageNum, EMPTY_HIGHLIGHTS)
        pages.get(pageNum)?.draw(canvas, locationInView, highlightsForPage)
    }

    /**
     * Sets all [Page]s owned by this manager to invisible, i.e. to reduce memory when the host
     * [PdfView] is not in an interactive state.
     */
    fun cleanup() {
        for (page in pages.valueIterator()) {
            page.setInvisible()
        }
    }

    fun getLinkAtTapPoint(pdfPoint: PdfPoint): PdfDocument.PdfPageLinks? {
        return pages[pdfPoint.pageNum]?.links
    }

    fun getWidgetAtTapPoint(pdfPoint: PdfPoint): List<FormWidgetInfo>? {
        return pages[pdfPoint.pageNum]?.formWidgetInfos
    }

    private fun List<RectF>.union(): RectF {
        if (isEmpty()) return RectF()
        val unionRect = RectF()
        for (rect in this) {
            unionRect.union(rect)
        }
        return unionRect
    }
}

/** Constant empty list to avoid allocations during drawing */
private val EMPTY_HIGHLIGHTS = listOf<Highlight>()

private val PAGE_RETENTION_RADIUS = 2
