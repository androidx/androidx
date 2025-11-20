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
