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

package androidx.pdf.compose

import android.graphics.PointF
import android.graphics.RectF
import android.util.SparseArray
import androidx.annotation.IntRange
import androidx.collection.MutableIntObjectMap
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.core.util.keyIterator
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.Highlight
import androidx.pdf.PdfPoint
import androidx.pdf.compose.PdfViewerState.Companion.GESTURE_STATE_IDLE
import androidx.pdf.compose.PdfViewerState.Companion.GESTURE_STATE_INTERACTING
import androidx.pdf.compose.PdfViewerState.Companion.GESTURE_STATE_SETTLING
import androidx.pdf.selection.Selection
import androidx.pdf.view.PdfView
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation

/** Scope used for suspending scroll blocks */
public interface PdfZoomScrollScope {
    /**
     * Attempts to scroll forward by [delta] px.
     *
     * @return the amount of requested scroll that was consumed (i.e. how far it scrolled)
     */
    public fun scrollBy(delta: Offset): Offset

    /** Instantly sets the zoom level to [zoomLevel] */
    public fun zoomTo(zoomLevel: Float)
}

/** Creates and remembers a [PdfViewerState]. */
@Composable
@ExperimentalPdfApi
public fun rememberPdfViewerState(): PdfViewerState {
    return remember { PdfViewerState() }
}

/**
 * A state object that can be hoisted to observe and control [PdfViewer] zoom, scroll, and content
 * position.
 */
@ExperimentalPdfApi
public class PdfViewerState() {
    private val zoomScrollMutex = MutatorMutex()

    internal var pdfView: PdfView? = null
        set(value) {
            if (field === value) return
            pdfViewObserver?.let {
                field?.removeOnViewportChangedListener(it)
                field?.removeOnSelectionChangedListener(it)
            }
            firstVisiblePage = pdfView?.firstVisiblePage ?: 0
            visiblePagesCount = pdfView?.visiblePagesCount ?: 0
            gestureState = pdfView?.gestureState ?: GESTURE_STATE_IDLE
            zoom = pdfView?.zoom ?: PdfView.DEFAULT_INIT_ZOOM
            visiblePageOffsets.clear()
            currentSelection = pdfView?.currentSelection
            field = value
            field?.let { pdfView ->
                pdfViewObserver =
                    PdfViewObserver().also { observer ->
                        pdfView.addOnViewportChangedListener(observer)
                        pdfView.addOnSelectionChangedListener(observer)
                    }
                pdfViewPositioner = PdfViewPositioner(pdfView)
            }
        }

    private var pdfViewPositioner: PdfViewPositioner? = null
    private var pdfViewObserver: PdfViewObserver? = null

    /** The first page in the viewport, including partially-visible pages. 0-indexed. */
    public var firstVisiblePage: Int by mutableIntStateOf(0)
        private set

    /** The [Offset] of the first visible page in the viewport in view coordinates. */
    @get:FrequentlyChangingValue
    public var firstVisiblePageOffset: Offset by mutableStateOf(Offset(0F, 0F))
        private set

    /** The number of pages visible in the viewport, including partially visible pages. */
    public var visiblePagesCount: Int by mutableIntStateOf(0)
        private set

    /**
     * The zoom level of this view, as a factor of the content's natural size with when 1 pixel is
     * equal to 1 PDF point.
     */
    @get:FrequentlyChangingValue
    public var zoom: Float by mutableFloatStateOf(PdfView.DEFAULT_INIT_ZOOM)
        private set

    /**
     * State regarding whether the user is interacting with the PDF, one of [GESTURE_STATE_IDLE],
     * [GESTURE_STATE_SETTLING], or [GESTURE_STATE_INTERACTING]
     */
    public var gestureState: Int by mutableIntStateOf(GESTURE_STATE_IDLE)
        internal set

    /** The currently-selected content in the PDF, or null if nothing is selected */
    public var currentSelection: Selection? by mutableStateOf(null)
        private set

    private val visiblePageOffsets = MutableIntObjectMap<Offset>()

    /**
     * Returns the [PdfPoint] corresponding to [offset] in Compose coordinates, or null if no PDF
     * content has been laid out at this Offset.
     *
     * Returns null if this [PdfViewerState] is not yet associated with a [PdfViewer], or if the
     * [PdfViewer] is not associated with a [androidx.pdf.PdfDocument]
     */
    public fun visibleOffsetToPdfPoint(offset: Offset): PdfPoint? {
        return pdfView?.viewToPdfPoint(PointF(offset.x, offset.y))
    }

    /**
     * Returns the View coordinate location of [pdfPoint], or [Offset.Unspecified] if that PDF
     * content has not been laid out yet.
     *
     * Returns [Offset.Unspecified] if this [PdfViewerState] is not yet associated with a
     * [PdfViewer], or if the [PdfViewer] is not associated with a [androidx.pdf.PdfDocument]
     */
    public fun pdfPointToVisibleOffset(pdfPoint: PdfPoint): Offset {
        return pdfView?.pdfToViewPoint(pdfPoint)?.toOffset() ?: Offset.Unspecified
    }

    /**
     * Returns the [Offset] of the page at [visiblePageNumber], or null if the provided page number
     * is not currently visible.
     */
    public fun getVisiblePageOffset(@IntRange(from = 0) visiblePageNumber: Int): Offset? {
        return visiblePageOffsets[visiblePageNumber]
    }

    /** Centers the page at [pageNum] in the viewport. */
    public suspend fun scrollToPage(@IntRange(from = 0) pageNum: Int) {
        zoomScrollMutex.mutate { pdfView?.scrollToPage(pageNum) }
    }

    /** Centers the location described by [position] in the viewport */
    public suspend fun scrollToPosition(position: PdfPoint) {
        zoomScrollMutex.mutate { pdfView?.scrollToPosition(position) }
    }

    internal suspend fun cancelOngoingNavigations() {
        zoomScrollMutex.mutate(priority = MutatePriority.PreventUserInput) {}
    }

    internal suspend fun lockZoomScrollOnUserInteraction() {
        zoomScrollMutex.mutate(priority = MutatePriority.UserInput) {
            // it's canceled  when the user stops interacting with the PDF. This releases the mutex
            // and unblocks programmatic scrolling.
            awaitCancellation()
        }
    }

    /**
     * Call this function to take control of zoom and scroll, and gain the ability to send zoom and
     * / or scroll events via [PdfZoomScrollScope]. All actions that change the logical zoom and /
     * or scroll position must be performed within a [zoomScroll] block, even if they don't call
     * other methods on this object in order to guarantee that mutual exclusion is enforced.
     *
     * If [zoomScroll] is called from elsewhere, this will be cancelled.
     */
    public suspend fun zoomScroll(block: PdfZoomScrollScope.() -> Unit) {
        pdfViewPositioner?.let { zoomScrollMutex.mutateWith(it) { block() } }
    }

    /** Clears the current selection, if one exists. No-op if there is no current [Selection] */
    public fun clearCurrentSelection() {
        pdfView?.clearCurrentSelection()
    }

    /**
     * Applies a set of [Highlight] to be drawn over this PDF. Each [Highlight] may be a different
     * color. This overrides any previous highlights, there is no merging of new and previous
     * values. [highlights] are defensively copied and the list or its contents may be modified
     * after providing it here.
     */
    public fun setHighlights(highlights: List<Highlight>) {
        pdfView?.setHighlights(highlights)
    }

    /** Listens to [PdfView] state to update the containing PdfViewerState. */
    private inner class PdfViewObserver() :
        PdfView.OnViewportChangedListener, PdfView.OnSelectionChangedListener {

        override fun onViewportChanged(
            firstVisiblePage: Int,
            visiblePagesCount: Int,
            pageLocations: SparseArray<RectF>,
            zoomLevel: Float,
        ) {
            this@PdfViewerState.firstVisiblePage = firstVisiblePage
            this@PdfViewerState.visiblePagesCount = visiblePagesCount
            firstVisiblePageOffset = pageLocations.get(firstVisiblePage).toOffset()
            zoom = zoomLevel

            // Clear no longer visible pages
            visiblePageOffsets.forEachKey { page ->
                if (pageLocations.indexOfKey(page) < 0) {
                    visiblePageOffsets.remove(page)
                }
            }
            // Add or update new or existing pages
            for (page in pageLocations.keyIterator()) {
                visiblePageOffsets.put(page, pageLocations.get(page).toOffset())
            }
        }

        override fun onSelectionChanged(newSelection: Selection?) {
            currentSelection = newSelection
        }
    }

    public companion object {
        /**
         * [PdfViewer] is not currently being affected by an outside input, e.g. user touch
         *
         * @see [PdfViewerState.gestureState]
         */
        public const val GESTURE_STATE_IDLE: Int = PdfView.GESTURE_STATE_IDLE

        /**
         * [PdfViewer] is currently being affected by an outside input, e.g. user touch
         *
         * @see [PdfViewerState.gestureState]
         */
        public const val GESTURE_STATE_INTERACTING: Int = PdfView.GESTURE_STATE_INTERACTING

        /**
         * [PdfViewer] is currently animating to a final position while not under outside control,
         * e.g. settling on a final position following a fling gesture.
         *
         * @see [PdfViewerState.gestureState]
         */
        public const val GESTURE_STATE_SETTLING: Int = PdfView.GESTURE_STATE_SETTLING
    }
}

/** [PdfZoomScrollScope] implementation that uses the [PdfView] underlying [PdfViewer] */
private class PdfViewPositioner(private val pdfView: PdfView) : PdfZoomScrollScope {
    override fun scrollBy(delta: Offset): Offset {
        val beforeX = pdfView.scrollX
        val beforeY = pdfView.scrollY
        pdfView.scrollBy(delta.x.roundToInt(), delta.y.roundToInt())
        val afterX = pdfView.scrollX
        val afterY = pdfView.scrollY
        return Offset((afterX - beforeX).toFloat(), (afterY - beforeY).toFloat())
    }

    override fun zoomTo(zoomLevel: Float) {
        pdfView.zoom = zoomLevel
    }
}

private fun PointF.toOffset() = Offset(this.x, this.y)

private fun RectF.toOffset() = Offset(this.left, this.top)
