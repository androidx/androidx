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

package androidx.pdf.tooltip

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.view.View
import androidx.pdf.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class TooltipPositionerTest {

    @Mock private lateinit var mockViewToPosition: View

    @Mock private lateinit var mockContext: Context

    @Mock private lateinit var mockResources: Resources

    private lateinit var positioner: TooltipPositioner

    private val tooltipHeadOffsetFromEdge = 20

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.resources).thenReturn(mockResources)
        `when`(mockResources.getDimensionPixelOffset(R.dimen.tooltip_head_offset_from_edge))
            .thenReturn(tooltipHeadOffsetFromEdge)
        positioner = TooltipPositioner(mockContext)
    }

    @Test
    fun computePositionTopCenterTooltip() {
        `when`(mockViewToPosition.measuredWidth).thenReturn(TOOLTIP_WIDTH)
        `when`(mockViewToPosition.measuredHeight).thenReturn(TOOLTIP_HEIGHT)

        val anchorPosition = PointF(500f, 200f)
        val anchorViewDimensions = Point(1000, 1000)
        val yOffset = 10

        val (alignment, bounds) =
            positioner.computePosition(
                mockViewToPosition,
                anchorPosition,
                anchorViewDimensions,
                yOffset,
            )
        // The positioner tries to position the beak exactly at the anchorPosition
        // while keeping the tooltip within the bounds of the parent view. For vertical
        // positioning we try to first place the tooltip beneath the anchor. For this test case
        // both conditions comply. Hence tooltip has beak at the top center.

        assertThat(alignment.x).isEqualTo(TooltipView.BEAK_ALIGNMENT_CENTER)
        assertThat(alignment.y).isEqualTo(TooltipView.BEAK_ALIGNMENT_TOP)
        assertThat(bounds)
            .isEqualTo(
                Rect(
                    anchorPosition.x.toInt() - TOOLTIP_WIDTH / 2,
                    anchorPosition.y.toInt() + yOffset,
                    anchorPosition.x.toInt() + TOOLTIP_WIDTH / 2,
                    anchorPosition.y.toInt() + yOffset + TOOLTIP_HEIGHT,
                )
            )
    }

    @Test
    fun computePositionBottomLeftTooltip() {
        `when`(mockViewToPosition.measuredWidth).thenReturn(TOOLTIP_WIDTH)
        `when`(mockViewToPosition.measuredHeight).thenReturn(TOOLTIP_HEIGHT)

        // Anchor near left edge, tooltip should align left
        val anchorPosition = PointF(50f, 900f)
        val anchorViewDimensions = Point(1000, 1000)
        val yOffset = 10

        val (alignment, bounds) =
            positioner.computePosition(
                mockViewToPosition,
                anchorPosition,
                anchorViewDimensions,
                yOffset,
            )

        assertThat(alignment.x).isEqualTo(TooltipView.BEAK_ALIGNMENT_LEFT)
        assertThat(alignment.y).isEqualTo(TooltipView.BEAK_ALIGNMENT_BOTTOM)
        // startX = 50 - 20 (tooltipHeadOffsetFromEdge) = 30
        // startY = 900 - 100 - 10 = 790
        assertThat(bounds).isEqualTo(Rect(30, 790, 230, 890))
    }

    @Test
    fun computePositionTopRightTooltip() {
        `when`(mockViewToPosition.measuredWidth).thenReturn(TOOLTIP_WIDTH)
        `when`(mockViewToPosition.measuredHeight).thenReturn(TOOLTIP_HEIGHT)

        // Anchor near right edge, tooltip should align right
        val anchorPosition = PointF(950f, 300f)
        val anchorViewDimensions = Point(1000, 1000)
        val yOffset = 10

        val (alignment, bounds) =
            positioner.computePosition(
                mockViewToPosition,
                anchorPosition,
                anchorViewDimensions,
                yOffset,
            )

        assertThat(alignment.x).isEqualTo(TooltipView.BEAK_ALIGNMENT_RIGHT)
        assertThat(alignment.y).isEqualTo(TooltipView.BEAK_ALIGNMENT_TOP)
        // startX = 950 - (200/2 - 20) = 950 - 80 = 870
        // startY = 300 + 10 = 310
        assertThat(bounds).isEqualTo(Rect(870, 310, 1070, 410))
    }

    @Test
    fun computePositionBottomCenterTooltip() {
        `when`(mockViewToPosition.measuredWidth).thenReturn(TOOLTIP_WIDTH)
        `when`(mockViewToPosition.measuredHeight).thenReturn(TOOLTIP_HEIGHT)

        val anchorPosition = PointF(500f, 900f) // Tooltip should be above
        val anchorViewDimensions = Point(1000, 1000)
        val yOffset = 10

        val (alignment, bounds) =
            positioner.computePosition(
                mockViewToPosition,
                anchorPosition,
                anchorViewDimensions,
                yOffset,
            )

        assertThat(alignment.x).isEqualTo(TooltipView.BEAK_ALIGNMENT_CENTER)
        assertThat(alignment.y).isEqualTo(TooltipView.BEAK_ALIGNMENT_BOTTOM)
        // startX = 500 - 100 = 400
        // startY = 900 - 100 - 10 = 790
        assertThat(bounds).isEqualTo(Rect(400, 790, 600, 890))
    }
}

private const val TOOLTIP_WIDTH = 200
private const val TOOLTIP_HEIGHT = 100
