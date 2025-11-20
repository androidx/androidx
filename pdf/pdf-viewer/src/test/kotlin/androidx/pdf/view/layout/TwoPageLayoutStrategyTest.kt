/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.pdf.view.layout

import android.graphics.RectF
import android.os.Parcel
import android.util.Range
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class TwoPageLayoutStrategyTest {
    private lateinit var twoPageLayoutStrategy: TwoPageLayoutStrategy

    @Before
    fun setUp() {
        twoPageLayoutStrategy =
            TwoPageLayoutStrategy(
                pageCount = 10,
                verticalPageSpacingPx = 10f,
                horizontalPageSpacingPx = 5f,
                topPageMarginPx = 5f,
            )
    }

    @Test
    fun testInitialization_withNegativePageCount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            TwoPageLayoutStrategy(
                pageCount = -1,
                verticalPageSpacingPx = 10f,
                horizontalPageSpacingPx = 5f,
            )
        }
    }

    @Test
    fun testInitialization_withNegativeVerticalPageSpacing_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            TwoPageLayoutStrategy(
                pageCount = 10,
                verticalPageSpacingPx = -10f,
                horizontalPageSpacingPx = 5f,
            )
        }
    }

    @Test
    fun testInitialization_withNegativeHorizontalPageSpacing_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            TwoPageLayoutStrategy(
                pageCount = 10,
                verticalPageSpacingPx = 10f,
                horizontalPageSpacingPx = -5f,
            )
        }
    }

    @Test
    fun testSetPagePositions() {
        val dimension1 = Dimension(100, 200)
        val dimension2 = Dimension(120, 180)

        twoPageLayoutStrategy.setPagePositions(0, dimension1)
        assertThat(twoPageLayoutStrategy.lastKnownPage).isEqualTo(0)
        assertThat(twoPageLayoutStrategy.maxWidth).isEqualTo(100f)

        twoPageLayoutStrategy.setPagePositions(1, dimension2)
        assertThat(twoPageLayoutStrategy.lastKnownPage).isEqualTo(1)
        assertThat(twoPageLayoutStrategy.maxWidth).isEqualTo(100f + 5f + 120f)
    }

    @Test
    fun testGetVisiblePages_includePartial() {
        setupPageDimensions()
        val viewport = RectF(0f, 200f, 100f, 500f)
        val visiblePages = twoPageLayoutStrategy.getVisiblePages(viewport, includePartial = true)
        assertThat(visiblePages.pages).isEqualTo(Range(0, 5))
    }

    @Test
    fun testGetVisiblePages_excludePartial() {
        setupPageDimensions()
        val viewport = RectF(0f, 200f, 100f, 500f)
        val visiblePages = twoPageLayoutStrategy.getVisiblePages(viewport, includePartial = false)
        assertThat(visiblePages.pages).isEqualTo(Range(2, 3))
    }

    @Test
    fun testGetPageLocation() {
        setupPageDimensions()
        val viewport = RectF(0f, 0f, 500f, 800f)
        val pageDimensions = createPageDimensions()

        // Test even page
        val location0 = twoPageLayoutStrategy.getPageLocation(viewport, 0, pageDimensions[0])
        val expectedOffset = (viewport.width() - twoPageLayoutStrategy.maxWidth) / 2f
        assertThat(location0.left).isEqualTo(expectedOffset)
        assertThat(location0.width()).isEqualTo(pageDimensions[0].x)

        // Test odd page
        val location1 = twoPageLayoutStrategy.getPageLocation(viewport, 1, pageDimensions[1])
        assertThat(location1.left).isEqualTo(expectedOffset + 100f + 5f)
        assertThat(location1.width()).isEqualTo(pageDimensions[1].x)
    }

    @Test
    fun testParcelable_restoresStateCorrectly() {
        // 1. Set up the initial strategy with some data.
        setupPageDimensions(pageWidth = 150)

        // 2. Write the original strategy to a parcel.
        val parcel = Parcel.obtain()
        twoPageLayoutStrategy.writeToParcel(parcel, twoPageLayoutStrategy.describeContents())

        // 3. Reset the parcel for reading and create a new strategy from it.
        parcel.setDataPosition(0)
        val restoredStrategy = TwoPageLayoutStrategy.CREATOR.createFromParcel(parcel)

        // 4. Assert that the restored strategy's state matches the original.
        assertThat(restoredStrategy.pageCount).isEqualTo(twoPageLayoutStrategy.pageCount)
        assertThat(restoredStrategy.verticalPageSpacingPx)
            .isEqualTo(twoPageLayoutStrategy.verticalPageSpacingPx)
        assertThat(restoredStrategy.horizontalPageSpacingPx)
            .isEqualTo(twoPageLayoutStrategy.horizontalPageSpacingPx)
        assertThat(restoredStrategy.lastKnownPage).isEqualTo(twoPageLayoutStrategy.lastKnownPage)
        assertThat(restoredStrategy.maxWidth).isEqualTo(twoPageLayoutStrategy.maxWidth)

        // Check the layout results.
        val viewport = RectF(0f, 0f, 500f, 800f)
        val pageDimensions = createPageDimensions(pageWidth = 150)
        val originalLocation = twoPageLayoutStrategy.getPageLocation(viewport, 5, pageDimensions[5])
        val restoredLocation = restoredStrategy.getPageLocation(viewport, 5, pageDimensions[5])
        assertThat(restoredLocation).isEqualTo(originalLocation)
        parcel.recycle()
    }

    private fun setupPageDimensions(pageWidth: Int = 100) {
        val pageDimensions = createPageDimensions(pageWidth)
        for (i in pageDimensions.indices) {
            twoPageLayoutStrategy.setPagePositions(i, pageDimensions[i])
        }
    }

    private fun createPageDimensions(pageWidth: Int = 100): Array<Dimension> {
        return Array(10) { Dimension(pageWidth, 200) }
    }
}
