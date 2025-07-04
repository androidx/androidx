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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.DeadObjectException
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.pdf.PdfDocument
import androidx.pdf.exceptions.RequestFailedException
import androidx.pdf.exceptions.RequestMetadata
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.util.PAGE_CONTENTS_REQUEST_NAME
import androidx.pdf.util.PAGE_LINKS_REQUEST_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** A single PDF page that knows how to render and draw itself */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@MainThread
internal class Page(
    /** The 0-based index of this page in the PDF */
    private val pageNum: Int,
    /** The size of this PDF page, in content coordinates */
    private val pageSize: Point,
    /** The [PdfDocument] this [Page] belongs to */
    private val pdfDocument: PdfDocument,
    /** The [CoroutineScope] to use for background work */
    private val backgroundScope: CoroutineScope,
    /**
     * The maximum size of any single [android.graphics.Bitmap] we render for a page, i.e. the
     * threshold for tiled rendering
     */
    private val maxBitmapSizePx: Point,
    /** A function to call when the [PdfView] hosting this [Page] ought to invalidate itself */
    private val onPageUpdate: () -> Unit,
    /** A function to call when page text is ready (invoked with page number). */
    private val onPageTextReady: ((Int) -> Unit),
    /** Error flow for propagating error occurred while processing to [PdfView]. */
    private val errorFlow: MutableSharedFlow<Throwable>,
    isAccessibilityEnabled: Boolean,
    /** A list represent the [FormWidgetInfo] present on the page. */
    formWidgetInfos: List<FormWidgetInfo>? = null,
    private val pdfFormFillingConfig: PdfFormFillingConfig,
) {
    init {
        require(pageNum >= 0) { "Invalid negative page" }
    }

    /** Handles rendering bitmaps for this page using [PdfDocument] */
    private var bitmapFetcher: BitmapFetcher? = null
    // Pre-allocated values to avoid allocations at drawing time
    private val highlightPaint =
        Paint().apply {
            style = Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            alpha = 255
            isAntiAlias = true
            isDither = true
        }
    private val highlightRect = RectF()
    private val formWidgetHighlightRect = RectF()
    private val tileLocationRect = RectF()

    private var fetchPageTextJob: Job? = null
    internal var pageText: String? = null
        private set

    private var fetchLinksJob: Job? = null
    internal var links: PdfDocument.PdfPageLinks? = null
        private set

    private var fetchFormWidgetInfoJob: Job? = null

    internal var isAccessibilityEnabled: Boolean = isAccessibilityEnabled
        set(value) {
            field = value
            if (value) {
                maybeFetchPageText()
            }
        }

    internal var formWidgetInfos: List<FormWidgetInfo>? = formWidgetInfos
        private set(value) {
            field = value
            formWidgetIndexToInfoMap = value?.associateBy { it.widgetIndex }
        }

    internal var formWidgetIndexToInfoMap: Map<Int, FormWidgetInfo>? =
        formWidgetInfos?.associateBy { it.widgetIndex }
        private set

    //  Checks if the content of this page within the specified visible area is fully rendered.
    internal fun isFullyRendered(zoom: Float, viewArea: RectF?): Boolean {
        return bitmapFetcher?.isFullyRendered(zoom, viewArea) ?: false
    }

    /**
     * Puts this page into a "visible" state, and / or updates various properties related to the
     * page's visible state
     *
     * @param zoom the current scale
     * @param viewArea the portion of the page that's visible, in content coordinates
     * @param stablePosition true if position is not actively changing, e.g. during a fling
     * @param pauseBitmapFetch true if we should wait to fetch Bitmaps
     */
    fun setVisible(
        zoom: Float,
        viewArea: RectF,
        stablePosition: Boolean = true,
        pauseBitmapFetch: Boolean = false,
    ) {
        if (bitmapFetcher == null) {
            bitmapFetcher =
                BitmapFetcher(
                    pageNum,
                    pageSize,
                    pdfDocument,
                    backgroundScope,
                    maxBitmapSizePx,
                    onPageUpdate,
                    errorFlow,
                )
        }
        if (!pauseBitmapFetch) {
            bitmapFetcher?.maybeFetchNewBitmaps(zoom, viewArea)
        }
        if (stablePosition) {
            maybeFetchLinks()
            if (isAccessibilityEnabled) {
                maybeFetchPageText()
            }
        }
    }

    /**
     * Invalidates and fetches new bitmaps for the [invalidatedArea] of the page.
     *
     * @param zoom the current scale
     * @param invalidatedArea visible portion of the page which has been invalidated
     */
    fun maybeInvalidateAreas(zoom: Float, invalidatedArea: RectF) {
        bitmapFetcher?.maybeFetchNewBitmaps(zoom, invalidatedArea, hasFormStateChanged = true)
    }

    /**
     * Puts this page into a "nearly visible" state, discarding only high res bitmaps and retaining
     * lighter weight data in case the page becomes visible again
     */
    fun setNearlyVisible() {
        bitmapFetcher?.discardAndCancelTileBitmaps()
    }

    /** Puts this page into an "invisible" state, i.e. retaining only the minimum data required */
    fun setInvisible() {
        bitmapFetcher?.close()
        bitmapFetcher = null
        pageText = null
        fetchPageTextJob?.cancel()
        fetchPageTextJob = null
        links = null
        fetchLinksJob?.cancel()
        fetchLinksJob = null
    }

    private fun maybeFetchPageText() {
        if (fetchPageTextJob?.isActive == true || pageText != null) return

        fetchPageTextJob =
            backgroundScope
                .launch {
                    ensureActive()
                    try {
                        pageText =
                            pdfDocument.getPageContent(pageNum)?.textContents?.joinToString {
                                it.text
                            }
                        onPageTextReady.invoke(pageNum)
                    } catch (e: DeadObjectException) {
                        val exception =
                            RequestFailedException(
                                requestMetadata =
                                    RequestMetadata(
                                        requestName = PAGE_CONTENTS_REQUEST_NAME,
                                        pageRange = pageNum..pageNum,
                                    ),
                                throwable = e,
                            )
                        errorFlow.emit(exception)
                    }
                }
                .also { it.invokeOnCompletion { fetchPageTextJob = null } }
    }

    /** Updates the [formWidgetInfos] associated with the page. */
    internal fun maybeUpdateFormWidgetInfos(formWidgetMetadataLoader: FormWidgetMetadataLoader) {
        val previousJob = fetchFormWidgetInfoJob

        fetchFormWidgetInfoJob =
            backgroundScope.launch {
                // Cancel the previous job, since we want to fetch the latest set of widgets
                previousJob?.cancelAndJoin()
                ensureActive()
                formWidgetInfos = formWidgetMetadataLoader.loadFormWidgetInfos(pageNum)
            }
    }

    fun draw(canvas: Canvas, locationInView: RectF, highlights: List<Highlight>) {
        val pageBitmaps = bitmapFetcher?.pageBitmaps
        if (pageBitmaps == null || pageBitmaps.needsWhiteBackground) {
            canvas.drawRect(locationInView, BLANK_PAINT)
        }
        if (pageBitmaps is FullPageBitmap) {
            draw(pageBitmaps, canvas, locationInView)
        } else if (pageBitmaps is TileBoard) {
            draw(pageBitmaps, canvas, locationInView)
        }
        for (highlight in highlights) {
            // Highlight locations are defined in content coordinates, compute their location
            // in View coordinates using locationInView
            highlightRect.set(
                highlight.area.left,
                highlight.area.top,
                highlight.area.right,
                highlight.area.bottom,
            )
            highlightRect.offset(locationInView.left, locationInView.top)
            highlightPaint.color = highlight.color
            canvas.drawRect(highlightRect, highlightPaint)
        }

        if (pdfFormFillingConfig.isFormFillingEnabled()) {
            formWidgetInfos
                ?.filter { !it.readOnly }
                ?.forEach {
                    formWidgetHighlightRect.set(it.widgetRect)
                    formWidgetHighlightRect.offset(locationInView.left, locationInView.top)
                    highlightPaint.color = pdfFormFillingConfig.formFieldsHighlightColor
                    canvas.drawRect(formWidgetHighlightRect, highlightPaint)
                }
        }
    }

    private fun maybeFetchLinks() {
        if (fetchLinksJob?.isActive == true || links != null) return
        fetchLinksJob =
            backgroundScope
                .launch {
                    ensureActive()
                    try {
                        links = pdfDocument.getPageLinks(pageNum)
                    } catch (e: DeadObjectException) {
                        val exception =
                            RequestFailedException(
                                requestMetadata =
                                    RequestMetadata(
                                        requestName = PAGE_LINKS_REQUEST_NAME,
                                        pageRange = pageNum..pageNum,
                                    ),
                                throwable = e,
                            )
                        errorFlow.emit(exception)
                    }
                }
                .also { it.invokeOnCompletion { fetchLinksJob = null } }
    }

    private fun draw(fullPageBitmap: FullPageBitmap, canvas: Canvas, locationInView: RectF) {
        canvas.drawBitmap(fullPageBitmap.bitmap, /* src= */ null, locationInView, BMP_PAINT)
    }

    private fun draw(tileBoard: TileBoard, canvas: Canvas, locationInView: RectF) {
        tileBoard.fullPageBitmap?.let {
            canvas.drawBitmap(it, /* src= */ null, locationInView, BMP_PAINT)
        }
        for (tile in tileBoard.tiles) {
            tile.bitmap?.let { bitmap ->
                canvas.drawBitmap(
                    bitmap, /* src */
                    null,
                    locationForTile(tile, tileBoard.bitmapScale, locationInView),
                    BMP_PAINT,
                )
            }
        }
    }

    private fun locationForTile(
        tile: TileBoard.Tile,
        renderedScale: Float,
        locationInView: RectF,
    ): RectF {
        val tileOffsetPx = tile.offsetPx
        // The tile describes its own location in pixels, i.e. scaled coordinates, however
        // our Canvas is already scaled by the zoom factor, so we need to describe the tile's
        // location to the Canvas in unscaled coordinates
        val left = locationInView.left + tileOffsetPx.x / renderedScale
        val top = locationInView.top + tileOffsetPx.y / renderedScale
        val exactSize = tile.exactSizePx
        tileLocationRect.set(
            left,
            top,
            left + exactSize.x / renderedScale,
            top + exactSize.y / renderedScale,
        )
        return tileLocationRect
    }
}

/** Constant [Paint]s used in drawing */
@VisibleForTesting internal val BMP_PAINT = Paint(Paint.FILTER_BITMAP_FLAG)

@VisibleForTesting
internal val BLANK_PAINT =
    Paint().apply {
        color = Color.WHITE
        style = Style.FILL
    }
