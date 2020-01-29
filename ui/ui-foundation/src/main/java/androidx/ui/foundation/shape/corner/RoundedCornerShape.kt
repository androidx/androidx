/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation.shape.corner

import androidx.annotation.IntRange
import androidx.ui.geometry.RRect
import androidx.ui.geometry.Radius
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Shape
import androidx.ui.unit.Dp
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.px
import androidx.ui.unit.toRect

/**
 * A shape describing the rectangle with rounded corners.
 *
 * @param topLeft a size of the top left corner
 * @param topRight a size of the top right corner
 * @param bottomRight a size of the bottom left corner
 * @param bottomLeft a size of the bottom right corner
 */
data class RoundedCornerShape(
    val topLeft: CornerSize,
    val topRight: CornerSize,
    val bottomRight: CornerSize,
    val bottomLeft: CornerSize
) : CornerBasedShape(topLeft, topRight, bottomRight, bottomLeft) {

    override fun createOutline(
        size: PxSize,
        topLeft: Px,
        topRight: Px,
        bottomRight: Px,
        bottomLeft: Px
    ) = Outline.Rounded(
            RRect(
                rect = size.toRect(),
                topLeft = topLeft.toRadius(),
                topRight = topRight.toRadius(),
                bottomRight = bottomRight.toRadius(),
                bottomLeft = bottomLeft.toRadius()
            )
        )

    private /*inline*/ fun Px.toRadius() = Radius.circular(this.value)
}

/**
 * Circular [Shape] with all the corners sized as the 50 percent of the shape size.
 */
val CircleShape = RoundedCornerShape(50)

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 * @param corner [CornerSize] to apply.
 */
/*inline*/ fun RoundedCornerShape(corner: CornerSize) =
    RoundedCornerShape(corner, corner, corner, corner)

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 * @param size Size in [Dp] to apply.
 */
/*inline*/ fun RoundedCornerShape(size: Dp) = RoundedCornerShape(CornerSize(size))

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 * @param size Size in [Px] to apply.
 */
/*inline*/ fun RoundedCornerShape(size: Px) = RoundedCornerShape(CornerSize(size))

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 * @param percent Size in percents to apply.
 */
/*inline*/ fun RoundedCornerShape(percent: Int) = RoundedCornerShape(CornerSize(percent))

/**
 * Creates [RoundedCornerShape] with sizes defined in [Dp].
 */
/*inline*/ fun RoundedCornerShape(
    topLeft: Dp = 0.dp,
    topRight: Dp = 0.dp,
    bottomRight: Dp = 0.dp,
    bottomLeft: Dp = 0.dp
) = RoundedCornerShape(
    CornerSize(topLeft),
    CornerSize(topRight),
    CornerSize(bottomRight),
    CornerSize(bottomLeft)
)

/**
 * Creates [RoundedCornerShape] with sizes defined in [Px].
 */
/*inline*/ fun RoundedCornerShape(
    topLeft: Px = 0.px,
    topRight: Px = 0.px,
    bottomRight: Px = 0.px,
    bottomLeft: Px = 0.px
) = RoundedCornerShape(
    CornerSize(topLeft),
    CornerSize(topRight),
    CornerSize(bottomRight),
    CornerSize(bottomLeft)
)

/**
 * Creates [RoundedCornerShape] with sizes defined in percents of the shape's smaller side.
 */
/*inline*/ fun RoundedCornerShape(
    @IntRange(from = 0, to = 50) topLeftPercent: Int = 0,
    @IntRange(from = 0, to = 50) topRightPercent: Int = 0,
    @IntRange(from = 0, to = 50) bottomRightPercent: Int = 0,
    @IntRange(from = 0, to = 50) bottomLeftPercent: Int = 0
) = RoundedCornerShape(
    CornerSize(topLeftPercent),
    CornerSize(topRightPercent),
    CornerSize(bottomRightPercent),
    CornerSize(bottomLeftPercent)
)
