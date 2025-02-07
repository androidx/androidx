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
import android.graphics.Rect
import androidx.pdf.PdfDocument
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapFetcherTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val pdfDocument =
        mock<PdfDocument> {
            on { getPageBitmapSource(any()) } doAnswer
                { invocation ->
                    FakeBitmapSource(invocation.getArgument(0))
                }
        }

    private var invalidationCounter = 0
    private val invalidationTracker: () -> Unit = { invalidationCounter++ }

    private val maxBitmapSizePx = Point(2048, 2048)
    private val pageSize = Point(512, 512)
    private val fullPageViewArea = Rect(0, 0, pageSize.x, pageSize.y)

    private lateinit var bitmapFetcher: BitmapFetcher
    private lateinit var tileSizePx: Point

    @Before
    fun setup() {
        testDispatcher.cancelChildren()
        invalidationCounter = 0

        bitmapFetcher =
            BitmapFetcher(
                pageNum = 0,
                pageSize,
                pdfDocument,
                testScope,
                maxBitmapSizePx,
                invalidationTracker,
            )
        tileSizePx = BitmapFetcher.tileSizePx
    }

    @Test
    fun close_cancelsWorkAndFreesBitmaps() {
        bitmapFetcher.maybeFetchNewBitmaps(1.5f, fullPageViewArea)
        assertThat(bitmapFetcher.fetchingWorkHandle?.isActive).isTrue()

        bitmapFetcher.close()
        assertThat(bitmapFetcher.fetchingWorkHandle).isNull()
        assertThat(bitmapFetcher.pageBitmaps).isNull()
    }

    @Test
    fun lowScale_fullPageViewArea_fetchesFullPageBitmap() {
        bitmapFetcher.maybeFetchNewBitmaps(1.5f, fullPageViewArea)

        testDispatcher.scheduler.runCurrent()

        val pageBitmaps = bitmapFetcher.pageBitmaps
        assertThat(pageBitmaps).isInstanceOf(FullPageBitmap::class.java)
        assertThat(pageBitmaps?.bitmapScale).isEqualTo(1.5f)
        pageBitmaps as FullPageBitmap // Make smartcast work nicely below
        assertThat(pageBitmaps.bitmap.width).isEqualTo((pageSize.x * 1.5f).roundToInt())
        assertThat(pageBitmaps.bitmap.height).isEqualTo((pageSize.y * 1.5f).roundToInt())
        assertThat(invalidationCounter).isEqualTo(1)
    }

    @Test
    fun lowScale_partialPageViewArea_fetchesFullPageBitmap() {
        // 1.5 scale, viewing the lower right half of the page
        bitmapFetcher.maybeFetchNewBitmaps(
            1.5f,
            viewArea = Rect(pageSize.x / 2, pageSize.y / 2, pageSize.x, pageSize.y)
        )

        testDispatcher.scheduler.runCurrent()

        val pageBitmaps = bitmapFetcher.pageBitmaps
        assertThat(pageBitmaps).isInstanceOf(FullPageBitmap::class.java)
        assertThat(pageBitmaps?.bitmapScale).isEqualTo(1.5f)
        pageBitmaps as FullPageBitmap // Make smartcast work nicely below
        assertThat(pageBitmaps.bitmap.width).isEqualTo((pageSize.x * 1.5f).roundToInt())
        assertThat(pageBitmaps.bitmap.height).isEqualTo((pageSize.y * 1.5f).roundToInt())
        assertThat(invalidationCounter).isEqualTo(1)
    }

    @Test
    fun highScale_fullPageViewArea_fetchesTileBoard() {
        bitmapFetcher.maybeFetchNewBitmaps(5.0f, fullPageViewArea)

        testDispatcher.scheduler.runCurrent()

        val pageBitmaps = bitmapFetcher.pageBitmaps
        assertThat(pageBitmaps).isInstanceOf(TileBoard::class.java)
        assertThat(pageBitmaps?.bitmapScale).isEqualTo(5.0f)
        pageBitmaps as TileBoard // Make smartcast work nicely below

        // Check the properties of an arbitrary full-size tile
        val row0Col2 = pageBitmaps.tiles[2]
        assertThat(row0Col2.bitmap?.width).isEqualTo(tileSizePx.x)
        assertThat(row0Col2.bitmap?.height).isEqualTo(tileSizePx.y)
        assertThat(row0Col2.offsetPx).isEqualTo(Point(tileSizePx.x * 2, 0))
        assertThat(row0Col2.exactSizePx).isEqualTo(Point(tileSizePx.x, tileSizePx.y))

        // Check the properties of the last tile in the bottom right corner
        val row3Col3 = pageBitmaps.tiles[15]
        val pageSizePx = Point(pageSize.x * 5, pageSize.y * 5)
        // This tile is not the same size as the others b/c it's cut off on both edges
        val row3Col3Size = Point(pageSizePx.x - tileSizePx.x * 3, pageSizePx.y - tileSizePx.y * 3)
        assertThat(row3Col3.bitmap?.width).isEqualTo(row3Col3Size.x)
        assertThat(row3Col3.bitmap?.height).isEqualTo(row3Col3Size.y)
        assertThat(row3Col3.offsetPx).isEqualTo(Point(tileSizePx.x * 3, tileSizePx.y * 3))
        assertThat(row3Col3.exactSizePx).isEqualTo(row3Col3Size)

        for (tile in pageBitmaps.tiles) {
            assertThat(tile.bitmap).isNotNull()
        }

        // 1 for each tile * 16 tiles
        assertThat(invalidationCounter).isEqualTo(16)
    }

    @Test
    fun highScale_partialPageViewArea_fetchesPartialTileBoard() {
        // 1.5 scale, viewing the lower right half of the page
        bitmapFetcher.maybeFetchNewBitmaps(
            5.0f,
            viewArea = Rect(pageSize.x / 2, pageSize.y / 2, pageSize.x, pageSize.y)
        )

        testDispatcher.scheduler.runCurrent()

        val pageBitmaps = bitmapFetcher.pageBitmaps
        assertThat(pageBitmaps).isInstanceOf(TileBoard::class.java)
        assertThat(pageBitmaps?.bitmapScale).isEqualTo(5.0f)
        pageBitmaps as TileBoard // Make smartcast work nicely below
        // This is all tiles in row >= 0 && col >= 0 (row 1 and col 1 are partially visible)
        val expectedVisibleIndices = setOf(5, 6, 7, 9, 10, 11, 13, 14, 15)

        for (tile in pageBitmaps.tiles) {
            if (tile.index in expectedVisibleIndices) {
                assertThat(tile.bitmap).isNotNull()
            } else {
                assertThat(tile.bitmap).isNull()
            }
        }

        // 1 for each visible tile * 9 visible tiles
        assertThat(invalidationCounter).isEqualTo(9)
    }

    @Test
    fun changeScale_toFetchedValue_noNewWork() {
        bitmapFetcher.maybeFetchNewBitmaps(1.5f, fullPageViewArea)
        testDispatcher.scheduler.runCurrent()
        val firstBitmaps = bitmapFetcher.pageBitmaps
        bitmapFetcher.maybeFetchNewBitmaps(1.5f, fullPageViewArea)

        // We shouldn't have started a new Job the second time onScaleChanged to the same value
        assertThat(bitmapFetcher.fetchingWorkHandle?.isActive).isFalse()
        // And we should still have the same bitmaps
        assertThat(bitmapFetcher.pageBitmaps).isEqualTo(firstBitmaps)
        // 1 total invalidation
        assertThat(invalidationCounter).isEqualTo(1)
    }

    @Test
    fun changeScale_toFetchingValue_noNewWork() {
        bitmapFetcher.maybeFetchNewBitmaps(1.5f, fullPageViewArea)
        val firstJob = bitmapFetcher.fetchingWorkHandle
        bitmapFetcher.maybeFetchNewBitmaps(1.5f, fullPageViewArea)

        // This should be the same Job we started the first time onScaleChanged
        assertThat(bitmapFetcher.fetchingWorkHandle).isEqualTo(firstJob)
        // 0 invalidations because we're still rendering
        assertThat(invalidationCounter).isEqualTo(0)
    }

    @Test
    fun changeScale_lowToHigh_fullPageToTiling() {
        bitmapFetcher.maybeFetchNewBitmaps(1.5f, fullPageViewArea)
        testDispatcher.scheduler.runCurrent()
        val fullPageBitmap = bitmapFetcher.pageBitmaps
        assertThat(fullPageBitmap).isInstanceOf(FullPageBitmap::class.java)
        assertThat(fullPageBitmap?.bitmapScale).isEqualTo(1.5f)
        assertThat(invalidationCounter).isEqualTo(1)

        bitmapFetcher.maybeFetchNewBitmaps(5.0f, fullPageViewArea)
        testDispatcher.scheduler.runCurrent()
        val tileBoard = bitmapFetcher.pageBitmaps
        assertThat(tileBoard).isInstanceOf(TileBoard::class.java)
        assertThat(tileBoard?.bitmapScale).isEqualTo(5.0f)
        // 1 invalidation for the previous full page bitmap + (1 for each tile * 16 tiles)
        assertThat(invalidationCounter).isEqualTo(17)
    }

    @Test
    fun changeScale_highToLow_tilingToFullPage() {
        bitmapFetcher.maybeFetchNewBitmaps(5.0f, fullPageViewArea)
        testDispatcher.scheduler.runCurrent()
        val tileBoard = bitmapFetcher.pageBitmaps
        assertThat(tileBoard).isInstanceOf(TileBoard::class.java)
        assertThat(tileBoard?.bitmapScale).isEqualTo(5.0f)
        // 1 for each tile * 16 tiles
        assertThat(invalidationCounter).isEqualTo(16)

        bitmapFetcher.maybeFetchNewBitmaps(1.5f, fullPageViewArea)
        testDispatcher.scheduler.runCurrent()
        val fullPageBitmap = bitmapFetcher.pageBitmaps
        assertThat(fullPageBitmap).isInstanceOf(FullPageBitmap::class.java)
        assertThat(fullPageBitmap?.bitmapScale).isEqualTo(1.5f)
        // 1 additional invalidation for the new full page bitmap
        assertThat(invalidationCounter).isEqualTo(17)
    }

    @Test
    fun changeViewArea_overlapWithPrevious() {
        // 5.0 scale, viewing the lower right half of the page
        bitmapFetcher.maybeFetchNewBitmaps(
            5.0f,
            Rect(pageSize.x / 2, pageSize.y / 2, pageSize.x, pageSize.y)
        )
        testDispatcher.scheduler.runCurrent()
        val originalTileBoard = bitmapFetcher.pageBitmaps
        assertThat(originalTileBoard).isInstanceOf(TileBoard::class.java)
        // This is all tiles in row > 0 && col > 0 (row 1 and col 1 are partially visible)
        val originalVisibleIndices = setOf(5, 6, 7, 9, 10, 11, 13, 14, 15)
        val originalBitmaps = mutableMapOf<Int, Bitmap>()
        for (tile in (originalTileBoard as TileBoard).tiles) {
            if (tile.index in originalVisibleIndices) {
                assertThat(tile.bitmap).isNotNull()
                originalBitmaps[tile.index] = requireNotNull(tile.bitmap)
            } else {
                assertThat(tile.bitmap).isNull()
            }
        }
        // 1 for each visible tile
        val originalInvalidations = invalidationCounter
        assertThat(originalInvalidations).isEqualTo(originalVisibleIndices.size)

        // 5.0 scale, viewing the middle of the page offset by 1/4 of the page's dimensions
        bitmapFetcher.maybeFetchNewBitmaps(
            5.0f,
            Rect(pageSize.x / 4, pageSize.y / 4, pageSize.x * 3 / 4, pageSize.y * 3 / 4)
        )
        testDispatcher.scheduler.runCurrent()
        val newTileBoard = bitmapFetcher.pageBitmaps
        // We should re-use the previous tile board
        assertThat(newTileBoard).isEqualTo(originalTileBoard)
        // This is all tiles in row < 3 and col < 3 (row 0 and 2, col 0 and 2 are partially visible)
        val newVisibleIndices = setOf(0, 1, 2, 4, 5, 6, 8, 9, 10)
        // This is all tiles that are visible in both view areas (original and new)
        val expectedRetainedIndices = originalVisibleIndices.intersect(newVisibleIndices)
        for (tile in (newTileBoard as TileBoard).tiles) {
            if (tile.index in expectedRetainedIndices) {
                // We should have re-used the previous tile Bitmap
                assertThat(tile.bitmap).isNotNull()
                assertThat(tile.bitmap).isEqualTo(originalBitmaps[tile.index])
            } else if (tile.index in newVisibleIndices) {
                // We should have fetched a new tile Bitmap
                assertThat(tile.bitmap).isNotNull()
            } else {
                // We should have cleaned up the Bitmap
                assertThat(tile.bitmap).isNull()
            }
        }

        // Invalidations before the view area changed + 1 for each *newly* visible tile
        assertThat(invalidationCounter)
            .isEqualTo(
                originalInvalidations + (newVisibleIndices.size - expectedRetainedIndices.size)
            )
    }
}
