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

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.util.Size
import androidx.annotation.AnyThread
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.pdf.PdfDocument
import androidx.pdf.util.RectUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Manages the loading of [Bitmap]s for a single PDF page
 *
 * Bitmaps can be obtained from the [pageBitmaps] property, which will be either a [FullPageBitmap]
 * or a [TileBoard] depending on [maxBitmapSizePx] and the current zoom level as provided to
 * [maybeFetchNewBitmaps].
 *
 * [maybeFetchNewBitmaps] dictates the nature of bitmaps that will be fetched. If it's never called,
 * no bitmaps will be fetched. [pageBitmaps] is eventually consistent with respect to the most
 * recent zoom level and view area provided to [maybeFetchNewBitmaps], so take into account the
 * [PageContents.bitmapScale] when using the bitmaps.
 *
 * This fetcher retains memory-intensive resources, and it should be [close]d when it's no longer
 * needed (e.g. the PDF page it pertains to is not visible). [discardAndCancelTileBitmaps] can be
 * used to release especially heavy weight resources (i.e. high-res bitmaps) while keeping others
 * open (e.g. if the PDF page it pertains to is nearly visible).
 */
@MainThread
internal class BitmapFetcher(
    pageNum: Int,
    private val pageSize: Point,
    pdfDocument: PdfDocument,
    private val backgroundScope: CoroutineScope,
    /**
     * The maximum size of a single bitmap in pixels. If the pageSize * current zoom exceeds this
     * threshold, we will start to use tiled rendering.
     */
    private val maxBitmapSizePx: Point,
    private val onPageUpdate: () -> Unit,
) : AutoCloseable {

    /**
     * The maximum size of a full page bitmap that is used as the background for a tiled rendering.
     * We draw a low-res bitmap behind tiles to avoid blank spaces in the page as high res tiles are
     * being loaded.
     */
    private val maxTileBackgroundSizePx = Point(maxBitmapSizePx.x / 2, maxBitmapSizePx.y / 2)

    private val pageBitmapsLock = Any()

    /** The bitmaps to draw for this page, as [PageContents] */
    @get:AnyThread
    @set:AnyThread
    @GuardedBy("pageBitmapsLock")
    var pageBitmaps: PageContents? = null
        set(value) = synchronized(pageBitmapsLock) { field = value }
        get() = synchronized(pageBitmapsLock) { field }

    /** The [PdfDocument.BitmapSource] from which to obtain [Bitmap]s */
    private val bitmapSource: PdfDocument.BitmapSource = pdfDocument.getPageBitmapSource(pageNum)

    /** The scale, i.e. zoom level for which we're actively fetching [Bitmap]s */
    @VisibleForTesting var currentFetchingScale: Float? = null

    /** The [BitmapRequestHandle] for any ongoing fetch */
    @VisibleForTesting var fetchingWorkHandle: BitmapRequestHandle? = null

    /** Update the view area and scale for which we should be fetching bitmaps */
    fun maybeFetchNewBitmaps(scale: Float, viewArea: Rect) {
        // Scale the provided viewArea, and clip it to the scaled bounds of the page
        // Carefully avoid mutating the provided Rect
        val scaledViewArea = Rect(viewArea)
        RectUtils.scale(scaledViewArea, scale)
        scaledViewArea.intersect(0, 0, (pageSize.x * scale).toInt(), (pageSize.y * scale).toInt())
        if (shouldFetchNewContents(scale)) {
            // Scale has changed, fetch entirely new PageContents
            fetchNewContents(scale, scaledViewArea)
        } else {
            // View area has changed, fetch new tiles and discard obsolete ones IFF we're tiling
            maybeUpdateTiling(scale, scaledViewArea)
        }
    }

    /** Discard all high-res bitmaps in the current tiling and stop fetching new ones */
    fun discardAndCancelTileBitmaps() {
        (pageBitmaps as? TileBoard)?.let { for (tile in it.tiles) tile.bitmap = null }
        fetchingWorkHandle?.cancel()
    }

    private fun maybeUpdateTiling(scale: Float, scaledViewArea: Rect) {
        // Exit early if we're not tiling
        val currentTileBoard = pageBitmaps as? TileBoard ?: return
        val currentTilingWork = fetchingWorkHandle as? TileBoardRequestHandle
        val tileRequests = mutableMapOf<Int, SingleBitmapRequestHandle>()
        var tileJob: Job? = null
        for (tile in currentTileBoard.tiles) {
            val ongoingRequest = currentTilingWork?.tileRequestHandles?.get(tile.index)
            if (
                tile.rectPx.intersects(
                    scaledViewArea.left,
                    scaledViewArea.top,
                    scaledViewArea.right,
                    scaledViewArea.bottom
                )
            ) {
                // Tile is visible, make sure we have, or have requested, a Bitmap for it
                if (ongoingRequest?.isActive == true) {
                    // Continue tracking the active request for this tile
                    tileRequests[tile.index] = ongoingRequest
                } else if (tile.bitmap == null) {
                    // Make a new request for this tile
                    tileJob = fetchBitmap(tile, scale, tileJob)
                    tileRequests[tile.index] = SingleBitmapRequestHandle(tileJob)
                }
            } else {
                // Tile is no longer visible, cancel any active request and clean up the Bitmap
                ongoingRequest?.cancel()
                tile.bitmap = null
            }
        }
        if (tileRequests.isNotEmpty()) {
            fetchingWorkHandle =
                TileBoardRequestHandle(tileRequests, currentTilingWork?.backgroundRequestHandle)
            currentFetchingScale = scale
        }
    }

    /**
     * Notify this fetcher that the zoom level / scale factor of the UI has changed, and that it
     * ought to fetch new bitmaps
     */
    private fun fetchNewContents(scale: Float, scaledViewArea: Rect) {
        fetchingWorkHandle?.cancel()
        fetchingWorkHandle =
            if (needsTiling(scale)) {
                fetchTiles(scale, scaledViewArea)
            } else {
                fetchNewBitmap(scale)
            }
        currentFetchingScale = scale
    }

    /**
     * Returns true if this fetcher should start fetching a net-new [PageContents], i.e. if the
     * scaled has changed since we started or finished fetching the previous set of Bitmaps
     */
    private fun shouldFetchNewContents(scale: Float): Boolean {
        val fetchingAtCurrentScale =
            currentFetchingScale == scale && fetchingWorkHandle?.isActive == true
        val fetchedAtCurrentScale = pageBitmaps?.let { it.bitmapScale == scale } == true

        return !fetchedAtCurrentScale && !fetchingAtCurrentScale
    }

    /**
     * Cancel ongoing work and release resources, including [Bitmap]s and [AutoCloseable]s held by
     * this fetcher
     */
    override fun close() {
        currentFetchingScale = null
        pageBitmaps = null
        fetchingWorkHandle?.cancel()
        fetchingWorkHandle = null
        bitmapSource.close()
    }

    /** Fetch a [FullPageBitmap] */
    private fun fetchNewBitmap(scale: Float): SingleBitmapRequestHandle {
        val job =
            fetchFullPageBitmap(limitBitmapSize(scale, maxBitmapSizePx)) {
                pageBitmaps = FullPageBitmap(it, scale)
                onPageUpdate()
            }
        return SingleBitmapRequestHandle(job)
    }

    /** Fetch a [TileBoard] */
    private fun fetchTiles(scale: Float, scaledViewArea: Rect): TileBoardRequestHandle {
        val pageSizePx = Point((pageSize.x * scale).roundToInt(), (pageSize.y * scale).roundToInt())
        val tileBoard = TileBoard(tileSizePx, pageSizePx, scale)
        // Re-use an existing low-res background if we have one to avoid displaying any blank space
        val prevFullPageBitmap =
            (pageBitmaps as? TileBoard)?.fullPageBitmap ?: (pageBitmaps as? FullPageBitmap)?.bitmap
        tileBoard.fullPageBitmap = prevFullPageBitmap
        pageBitmaps = tileBoard
        // Always fetch a new low-res one background. If zoom decreased this will save memory. If
        // zoom increased this will be a higher res, more suitable background
        val backgroundRequest =
            SingleBitmapRequestHandle(
                fetchFullPageBitmap(limitBitmapSize(scale, maxTileBackgroundSizePx)) {
                    tileBoard.fullPageBitmap = it
                }
            )
        val tileRequests = mutableMapOf<Int, SingleBitmapRequestHandle>()
        // Used to sequence requests so tiles are loaded left-to-right and top-to-bottom
        var tileJob: Job? = null
        for (tile in tileBoard.tiles) {
            val tileRect = tile.rectPx
            if (
                scaledViewArea.intersects(
                    tileRect.left,
                    tileRect.top,
                    tileRect.right,
                    tileRect.bottom
                )
            ) {
                tileJob = fetchBitmap(tile, scale, tileJob)
                tileRequests[tile.index] = SingleBitmapRequestHandle(tileJob)
            }
        }
        return TileBoardRequestHandle(tileRequests.toMap(), backgroundRequest)
    }

    /** Fetch a full page bitmap at [size], and return the [Job] that's doing the fetching */
    private fun fetchFullPageBitmap(size: Size, onReady: (Bitmap) -> Unit): Job {
        return backgroundScope.launch {
            ensureActive()
            val bitmap = bitmapSource.getBitmap(size)
            ensureActive()
            onReady(bitmap)
        }
    }

    /**
     * Fetch a [Bitmap] for this [TileBoard.Tile]
     *
     * @param tile the [TileBoard.Tile] to fetch a bitmap for
     * @param scale the scale factor of the bitmap
     * @param prevJob the [Job] that is fetching a bitmap for the tile left or above [tile], i.e. to
     *   guarantee tiles are loaded left-to-right and top-to-bottom
     */
    private fun fetchBitmap(tile: TileBoard.Tile, scale: Float, prevJob: Job?): Job {
        val job =
            backgroundScope.launch {
                prevJob?.join()
                ensureActive()
                val bitmap =
                    bitmapSource.getBitmap(
                        Size((pageSize.x * scale).roundToInt(), (pageSize.y * scale).roundToInt()),
                        tile.rectPx
                    )
                ensureActive()
                tile.bitmap = bitmap
                onPageUpdate()
            }
        return job
    }

    /** True if the [pageSize] * [scale] exceeds [maxBitmapSizePx] */
    private fun needsTiling(scale: Float): Boolean {
        return ((pageSize.x * scale) >= maxBitmapSizePx.x) ||
            ((pageSize.y * scale) >= maxBitmapSizePx.y)
    }

    /**
     * Returns a size that is as near as possible to [pageSize] * [requestedScale] while being
     * smaller than [maxSize] in both dimensions
     */
    private fun limitBitmapSize(requestedScale: Float, maxSize: Point): Size {
        val finalSize = PointF(pageSize.x * requestedScale, pageSize.y * requestedScale)
        // Reduce final size by 10% in each dimension until the constraints are satisfied
        while (finalSize.x > maxSize.x || finalSize.y > maxSize.y) {
            finalSize.x *= 0.9f
            finalSize.y *= 0.9f
        }
        return Size(finalSize.x.roundToInt(), finalSize.y.roundToInt())
    }

    companion object {
        /** The size of a single tile in pixels, when tiling is used */
        @VisibleForTesting internal val tileSizePx = Point(800, 800)
    }
}

/** Represents a cancellable handle to a request for one or more [Bitmap]s */
internal sealed interface BitmapRequestHandle {
    /** True if this request is active */
    val isActive: Boolean

    /** Cancel this request completely */
    fun cancel()
}

/** Cancellable [BitmapRequestHandle] for a single [Bitmap] */
internal class SingleBitmapRequestHandle(private val job: Job) : BitmapRequestHandle {
    override val isActive: Boolean
        get() = job.isActive

    override fun cancel() {
        job.cancel()
    }
}

/**
 * Cancellable [BitmapRequestHandle] for a full [TileBoard], composing multiple
 * [SingleBitmapRequestHandle] for the low-res background and each high-res tile
 */
internal class TileBoardRequestHandle(
    /** Map of [TileBoard.Tile.index] to a [BitmapRequestHandle] to fetch that tile's bitmap */
    val tileRequestHandles: Map<Int, SingleBitmapRequestHandle>,
    /**
     * [SingleBitmapRequestHandle] to fetch a low-res background for this tiling, or null if we
     * re-used the background from a previous tiling
     */
    val backgroundRequestHandle: SingleBitmapRequestHandle? = null
) : BitmapRequestHandle {
    override val isActive: Boolean
        get() =
            tileRequestHandles.values.any { it.isActive } ||
                backgroundRequestHandle?.isActive == true

    override fun cancel() {
        tileRequestHandles.values.forEach { it.cancel() }
        backgroundRequestHandle?.cancel()
    }
}

/** Represents the [Bitmap] or [Bitmap]s used to render this page */
internal sealed interface PageContents {
    val bitmapScale: Float
}

/** A singular [Bitmap] depicting the full page, when full page rendering is used */
internal class FullPageBitmap(val bitmap: Bitmap, override val bitmapScale: Float) : PageContents

/**
 * A set of [Bitmap]s that depict the full page as a rectangular grid of individual bitmap tiles.
 * This [PageContents] is mutable; it's updated with new Bitmaps as tiles are loaded incrementally
 */
internal class TileBoard(
    val tileSizePx: Point,
    val pageSizePx: Point,
    override val bitmapScale: Float
) : PageContents {

    /** The low res background [Bitmap] for this [TileBoard] */
    var fullPageBitmap: Bitmap? = null

    /** The number of rows in the current tiling */
    private val numRows
        get() = (1 + (pageSizePx.y - 1) / tileSizePx.y)

    /** The number of columns in the current tiling */
    private val numCols
        get() = (1 + (pageSizePx.x - 1) / tileSizePx.x)

    /** The [Tile]s in this board */
    val tiles = Array(numRows * numCols) { index -> Tile(index) }

    /** An individual [Tile] in this [TileBoard] */
    inner class Tile(val index: Int) {
        /** The x position of this tile in the tile board */
        private val rowIdx = index / numCols

        /** The y position of this tile in the tile board */
        private val colIdx = index % numCols

        /**
         * The offset of this [Tile] from the origin of the page in pixels, used in computations
         * where an exact pixel size is expected, e.g. fetching bitmaps
         */
        val offsetPx = Point(colIdx * tileSizePx.x, rowIdx * tileSizePx.y)

        /** The size of this [Tile] in pixels */
        val exactSizePx =
            Point(
                minOf(tileSizePx.x, pageSizePx.x - offsetPx.x),
                minOf(tileSizePx.y, pageSizePx.y - offsetPx.y),
            )

        /** The exact pixel location of this tile in the scaled page */
        val rectPx =
            Rect(offsetPx.x, offsetPx.y, offsetPx.x + exactSizePx.x, offsetPx.y + exactSizePx.y)

        /** The high res [Bitmap] for this [Tile] */
        var bitmap: Bitmap? = null
    }
}
