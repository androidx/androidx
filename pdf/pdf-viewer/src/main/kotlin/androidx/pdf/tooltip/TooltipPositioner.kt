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
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.view.View
import androidx.pdf.R

internal class TooltipPositioner(private val context: Context) {
    fun computePosition(
        viewToPosition: View,
        anchorPositionCoordinates: PointF,
        anchorViewDimensions: Point,
        yOffset: Int,
    ): Pair<Point, Rect> {
        val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        viewToPosition.measure(measureSpec, measureSpec)

        val (startX, horizontalAlignment) =
            computeHorizontalAlignment(
                viewToPosition.measuredWidth,
                anchorPositionCoordinates.x,
                anchorViewDimensions.x,
            )
        val (startY, verticalAlignment) =
            computeVerticalAlignment(
                viewToPosition.measuredHeight,
                anchorPositionCoordinates.y.toInt(),
                anchorViewDimensions.y,
                yOffset,
            )

        return Pair(
            Point(horizontalAlignment, verticalAlignment),
            Rect(
                startX,
                startY,
                startX + viewToPosition.measuredWidth,
                startY + viewToPosition.measuredHeight,
            ),
        )
    }

    private fun computeHorizontalAlignment(
        tooltipWidth: Int,
        anchorPositionX: Float,
        anchorViewWidth: Int,
    ): Pair<Int, Int> {
        val tooltipHeadOffsetFromEdge =
            context.resources.getDimensionPixelOffset(R.dimen.tooltip_head_offset_from_edge)

        return if (
            anchorPositionX - tooltipWidth / 2 >= 0 &&
                anchorPositionX + tooltipWidth / 2 <= anchorViewWidth
        ) {
            val startPosition = (anchorPositionX - tooltipWidth / 2).toInt()
            Pair(startPosition, TooltipView.BEAK_ALIGNMENT_CENTER)
        } else if (anchorPositionX - tooltipWidth / 2 < 0) {
            val startPosition = (anchorPositionX - tooltipHeadOffsetFromEdge).toInt()
            Pair(startPosition, TooltipView.BEAK_ALIGNMENT_LEFT)
        } else {
            val startPosition =
                (anchorPositionX - (tooltipWidth / 2 - tooltipHeadOffsetFromEdge)).toInt()
            Pair(startPosition, TooltipView.BEAK_ALIGNMENT_RIGHT)
        }
    }

    private fun computeVerticalAlignment(
        tooltipHeight: Int,
        anchorPositionY: Int,
        anchorViewHeight: Int,
        yOffset: Int,
    ): Pair<Int, Int> {
        return if (anchorPositionY + tooltipHeight + yOffset <= anchorViewHeight) {
            val startPosition = anchorPositionY + yOffset
            Pair(startPosition, TooltipView.BEAK_ALIGNMENT_TOP)
        } else {
            val startPosition = anchorPositionY - tooltipHeight - yOffset
            Pair(startPosition, TooltipView.BEAK_ALIGNMENT_BOTTOM)
        }
    }
}
