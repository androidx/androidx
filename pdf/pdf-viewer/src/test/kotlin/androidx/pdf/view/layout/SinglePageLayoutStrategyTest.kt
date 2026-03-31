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
class SinglePageLayoutStrategyTest {
    private lateinit var singlePageLayoutStrategy: SinglePageLayoutStrategy

    @Before
    fun setUp() {
        singlePageLayoutStrategy =
            SinglePageLayoutStrategy(
                pageCount = 10,
                verticalPageSpacingPx = 10f,
                topPageMarginPx = 5f,
            )
    }

    @Test
    fun testInitialization_withZeroPageCount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SinglePageLayoutStrategy(pageCount = 0, verticalPageSpacingPx = 10f)
        }
    }

    @Test
    fun testInitialization_withNegativePageCount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SinglePageLayoutStrategy(pageCount = -1, verticalPageSpacingPx = 10f)
        }
    }

    @Test
    fun testInitialization_withNegativeVerticalPageSpacing_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SinglePageLayoutStrategy(pageCount = 10, verticalPageSpacingPx = -10f)
        }
    }

    @Test
    fun testSetPagePositions() {
        val dimension1 = Dimension(100, 200)
        val dimension2 = Dimension(120, 180)

        singlePageLayoutStrategy.setPagePositions(0, dimension1)
        assertThat(singlePageLayoutStrategy.lastKnownPage).isEqualTo(0)
        assertThat(singlePageLayoutStrategy.maxWidth).isEqualTo(100f)

        singlePageLayoutStrategy.setPagePositions(1, dimension2)
        assertThat(singlePageLayoutStrategy.lastKnownPage).isEqualTo(1)
        assertThat(singlePageLayoutStrategy.maxWidth).isEqualTo(120f)
    }

    @Test
    fun testSetPagePositions_jumpForward_approximatesLayout() {
        // Load the first page.
        singlePageLayoutStrategy.setPagePositions(0, Dimension(100, 200))

        // Jump forward to page 3, creating a gap for pages 1 and 2.
        val dimension3 = Dimension(100, 250)
        singlePageLayoutStrategy.setPagePositions(3, dimension3)

        // Verify the layout for the intermediate pages is approximated.
        // The approximation should use the height of the newly loaded page (page 3).
        val viewport = RectF(0f, 0f, 100f, 600f)
        val expectedPage0Location = RectF(0f, 5f, 100f, 205f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 0, Dimension(100, 200)))
            .isEqualTo(expectedPage0Location)

        val expectedPage3Location = RectF(0f, 735f, 100f, 985f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 3, Dimension(100, 250)))
            .isEqualTo(expectedPage3Location)
    }

    @Test
    fun testSetPagePositions_lateLoadForPreviousPage_correctsLayout() {
        // Create an approximated layout by loading page 3 first.
        singlePageLayoutStrategy.setPagePositions(3, Dimension(110, 250))

        // Load page 1 out of order with a different, taller dimension.
        singlePageLayoutStrategy.setPagePositions(1, Dimension(110, 300))

        // Verify the layout of page 0 is the same.
        val viewport = RectF(0f, 0f, 110f, 600f)
        val expectedPage0Location = RectF(0f, 5f, 110f, 255f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 0, Dimension(110, 250)))
            .isEqualTo(expectedPage0Location)

        // Verify the layout of page 3 is corrected.
        val expectedPage3Location = RectF(0f, 835f, 110f, 1085f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 3, Dimension(110, 250)))
            .isEqualTo(expectedPage3Location)
    }

    @Test
    fun testGetVisiblePages_includePartial() {
        setupPageDimensions()
        val viewport = RectF(0f, 200f, 100f, 500f)
        val visiblePages = singlePageLayoutStrategy.getVisiblePages(viewport, includePartial = true)
        assertThat(visiblePages.pages).isEqualTo(Range(0, 2))
    }

    @Test
    fun testGetVisiblePages_excludePartial() {
        setupPageDimensions()
        val viewport = RectF(0f, 200f, 100f, 500f)
        val visiblePages =
            singlePageLayoutStrategy.getVisiblePages(viewport, includePartial = false)
        assertThat(visiblePages.pages).isEqualTo(Range(1, 1))
    }

    @Test
    fun testGetPageLocation_pageSmallerThanViewport_isCentered() {
        setupPageDimensions()
        val viewport = RectF(0f, 0f, 500f, 800f)
        val pageDimensions = createPageDimensions()
        val location = singlePageLayoutStrategy.getPageLocation(viewport, 0, pageDimensions[0])

        val expectedLeft = (viewport.width() - pageDimensions[0].x) / 2f
        assertThat(location.left).isEqualTo(expectedLeft)
        assertThat(location.width()).isEqualTo(pageDimensions[0].x)
    }

    @Test
    fun testGetPageLocation_pageWiderThanViewport_isScrolled() {
        setupPageDimensions(pageWidth = 600)
        val viewport = RectF(50f, 0f, 550f, 800f)
        val pageDimensions = createPageDimensions(pageWidth = 600)

        // The layout's maxPageWidth will be 600. Viewport width is 500.
        // Max scroll is 100. Current scroll is 50.
        // Proportional scroll for a page of width 600 is 50 * (600 - 600) / 100 = 0.
        // So this page should not be scrolled.
        val location = singlePageLayoutStrategy.getPageLocation(viewport, 0, pageDimensions[0])
        assertThat(location.left).isEqualTo(0f)
    }

    @Test
    fun getVisiblePages_viewportAboveAllPages() {
        val pageSize = Dimension(100, 200)
        singlePageLayoutStrategy.setPagePositions(0, pageSize)
        singlePageLayoutStrategy.setPagePositions(1, pageSize)
        singlePageLayoutStrategy.setPagePositions(2, pageSize)

        val visiblePages =
            singlePageLayoutStrategy.getVisiblePages(RectF(0f, -100f, 100f, 0f), true)

        // When the viewport is above the top of this model, we expect an empty range at the
        // beginning of this model
        assertThat(visiblePages.pages.upper).isEqualTo(0)
        assertThat(visiblePages.pages.lower).isEqualTo(0)
    }

    @Test
    fun getVisiblePages_viewportBelowAllPages() {
        val pageSize = Dimension(100, 200)
        singlePageLayoutStrategy.setPagePositions(0, pageSize)
        singlePageLayoutStrategy.setPagePositions(1, pageSize)
        singlePageLayoutStrategy.setPagePositions(2, pageSize)
        val contentBottom = 200 * 3 + 10 * 5

        val visiblePages =
            singlePageLayoutStrategy.getVisiblePages(RectF(0f, 660f, 100f, 750f), true)

        // When the viewport is below the end of this model, we expect an empty range at the last
        // known page
        assertThat(visiblePages.pages.upper).isEqualTo(2)
        assertThat(visiblePages.pages.lower).isEqualTo(2)
    }

    @Test
    fun getVisiblePages_allPagesVisible() {
        val pageSize = Dimension(100, 200)
        singlePageLayoutStrategy.setPagePositions(0, pageSize)
        singlePageLayoutStrategy.setPagePositions(1, pageSize)
        singlePageLayoutStrategy.setPagePositions(2, pageSize)

        val visiblePages = singlePageLayoutStrategy.getVisiblePages(RectF(0f, 0f, 100f, 660f), true)

        assertThat(visiblePages.pages.upper).isEqualTo(2)
        assertThat(visiblePages.pages.lower).isEqualTo(0)
    }

    @Test
    fun getVisiblePages_onePagePartiallyVisible() {
        val pageSize = Dimension(100, 200)
        singlePageLayoutStrategy.setPagePositions(0, pageSize)
        singlePageLayoutStrategy.setPagePositions(1, pageSize)
        singlePageLayoutStrategy.setPagePositions(2, pageSize)

        val visiblePages =
            singlePageLayoutStrategy.getVisiblePages(RectF(0f, 235f, 100f, 335f), true)

        assertThat(visiblePages.pages.upper).isEqualTo(1)
        assertThat(visiblePages.pages.lower).isEqualTo(1)
    }

    @Test
    fun getVisiblePages_twoPagesPartiallyVisible() {
        val pageSize = Dimension(100, 200)
        singlePageLayoutStrategy.setPagePositions(0, pageSize)
        singlePageLayoutStrategy.setPagePositions(1, pageSize)
        singlePageLayoutStrategy.setPagePositions(2, pageSize)

        val visiblePages =
            singlePageLayoutStrategy.getVisiblePages(RectF(0f, 235f, 100f, 455f), true)

        assertThat(visiblePages.pages.upper).isEqualTo(2)
        assertThat(visiblePages.pages.lower).isEqualTo(1)
    }

    @Test
    fun getVisiblePages_multiplePagesVisible() {
        val pageSize = Dimension(100, 200)
        singlePageLayoutStrategy.setPagePositions(0, pageSize)
        singlePageLayoutStrategy.setPagePositions(1, pageSize)
        singlePageLayoutStrategy.setPagePositions(2, pageSize)
        singlePageLayoutStrategy.setPagePositions(3, pageSize)

        val visiblePages =
            singlePageLayoutStrategy.getVisiblePages(RectF(0f, 210f, 100f, 840f), true)

        assertThat(visiblePages.pages.upper).isEqualTo(3)
        assertThat(visiblePages.pages.lower).isEqualTo(1)
    }

    /**
     * Add 3 pages of differing sizes to the model. Set the visible area to cover the whole model.
     * Largest page should span (0, model width). Smaller pages should be placed in the middle of
     * the model horizontally. Pages should get consistent vertical spacing.
     */
    @Test
    fun getPageLocation_viewportCoversAllPages() {
        val smallSize = Dimension(200, 100)
        val mediumSize = Dimension(400, 200)
        val largeSize = Dimension(800, 400)
        singlePageLayoutStrategy.setPagePositions(0, smallSize)
        singlePageLayoutStrategy.setPagePositions(1, mediumSize)
        singlePageLayoutStrategy.setPagePositions(2, largeSize)
        val viewport = RectF(0f, 0f, 800f, 800f)

        val expectedSmLocation = RectF(300f, 5f, 500f, 105f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 0, smallSize))
            .isEqualTo(expectedSmLocation)

        val expectedMdLocation = RectF(200f, 115f, 600f, 315f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 1, mediumSize))
            .isEqualTo(expectedMdLocation)

        val expectedLgLocation = RectF(0f, 325f, 800f, 725f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 2, largeSize))
            .isEqualTo(expectedLgLocation)
    }

    /**
     * Add 3 pages of differing sizes to the model. Set the visible area to the bottom left corner
     * of this model. Page 0 is not visible, page 1 should shift left to fit the maximum amount of
     * content on-screen, and page 2 should span [0, model width]
     */
    @Test
    fun getPageLocation_shiftPagesLargerThanViewportLeft() {
        val smallSize = Dimension(200, 100)
        val mediumSize = Dimension(400, 200)
        val largeSize = Dimension(800, 400)
        singlePageLayoutStrategy.setPagePositions(0, smallSize)
        singlePageLayoutStrategy.setPagePositions(1, mediumSize)
        singlePageLayoutStrategy.setPagePositions(2, largeSize)
        // A 300x200 section in the bottom-left corner of this model
        val viewport = RectF(0f, 250f, 200f, 800f)

        val expectedMdLocation = RectF(0f, 115f, 400f, 315f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 1, mediumSize))
            .isEqualTo(expectedMdLocation)

        val expectedLgLocation = RectF(0f, 325f, 800f, 725f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 2, largeSize))
            .isEqualTo(expectedLgLocation)
    }

    /**
     * Add 3 pages of differing sizes to the model. Set the visible area to the bottom right corner
     * of this model. Page 0 is not visible, page 1 should shift right to fit the maximum amount of
     * content on-screen, and page 2 should span [0, model width]
     */
    @Test
    fun getPageLocation_shiftPagesLargerThanViewportRight() {
        val smallSize = Dimension(200, 100)
        val mediumSize = Dimension(400, 200)
        val largeSize = Dimension(800, 400)
        singlePageLayoutStrategy.setPagePositions(0, smallSize)
        singlePageLayoutStrategy.setPagePositions(1, mediumSize)
        singlePageLayoutStrategy.setPagePositions(2, largeSize)
        // A 300x200 section in the bottom-right corner of this model
        val viewport = RectF(600f, 250f, 800f, 800f)

        val expectedMdLocation = RectF(400f, 115f, 800f, 315f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 1, mediumSize))
            .isEqualTo(expectedMdLocation)

        val expectedLgLocation = RectF(0f, 325f, 800f, 725f)
        assertThat(singlePageLayoutStrategy.getPageLocation(viewport, 2, largeSize))
            .isEqualTo(expectedLgLocation)
    }

    @Test
    fun testParcelable_restoresStateCorrectly() {
        // 1. Set up the initial strategy with some data.
        setupPageDimensions(pageWidth = 150)

        // 2. Write the original strategy to a parcel.
        val parcel = Parcel.obtain()
        singlePageLayoutStrategy.writeToParcel(parcel, singlePageLayoutStrategy.describeContents())

        // 3. Reset the parcel for reading and create a new strategy from it.
        parcel.setDataPosition(0)
        val restoredStrategy = SinglePageLayoutStrategy.CREATOR.createFromParcel(parcel)

        // 4. Assert that the restored strategy's state matches the original.
        assertThat(restoredStrategy.pageCount).isEqualTo(singlePageLayoutStrategy.pageCount)
        assertThat(restoredStrategy.verticalPageSpacingPx)
            .isEqualTo(singlePageLayoutStrategy.verticalPageSpacingPx)
        assertThat(restoredStrategy.lastKnownPage).isEqualTo(singlePageLayoutStrategy.lastKnownPage)
        assertThat(restoredStrategy.maxWidth).isEqualTo(singlePageLayoutStrategy.maxWidth)

        // Check the layout results.
        val viewport = RectF(0f, 0f, 500f, 800f)
        val pageDimensions = createPageDimensions(pageWidth = 150)

        val originalLocation =
            singlePageLayoutStrategy.getPageLocation(viewport, 5, pageDimensions[5])
        val restoredLocation = restoredStrategy.getPageLocation(viewport, 5, pageDimensions[5])
        assertThat(restoredLocation).isEqualTo(originalLocation)

        parcel.recycle()
    }

    private fun setupPageDimensions(pageWidth: Int = 100) {
        val pageDimensions = createPageDimensions(pageWidth)
        for (i in pageDimensions.indices) {
            singlePageLayoutStrategy.setPagePositions(i, pageDimensions[i])
        }
    }

    private fun createPageDimensions(pageWidth: Int = 100): Array<Dimension> {
        return Array(10) { Dimension(pageWidth, 200) }
    }
}
