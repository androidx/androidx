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
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Path
import androidx.ui.unit.Dp
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.px

/**
 * A shape describing the rectangle with cut corners.
 * Corner size is representing the cut length - the size of both legs of the cut's right triangle.
 *
 * @param topLeft a size of the top left corner
 * @param topRight a size of the top right corner
 * @param bottomRight a size of the bottom left corner
 * @param bottomLeft a size of the bottom right corner
 */
data class CutCornerShape(
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
    ) = Outline.Generic(Path().apply {
        var cornerSize = topLeft.value
        moveTo(0f, cornerSize)
        lineTo(cornerSize, 0f)
        cornerSize = topRight.value
        lineTo(size.width.value - cornerSize, 0f)
        lineTo(size.width.value, cornerSize)
        cornerSize = bottomRight.value
        lineTo(size.width.value, size.height.value - cornerSize)
        lineTo(size.width.value - cornerSize, size.height.value)
        cornerSize = bottomLeft.value
        lineTo(cornerSize, size.height.value)
        lineTo(0f, size.height.value - cornerSize)
        close()
    })
}

/**
 * Creates [CutCornerShape] with the same size applied for all four corners.
 * @param corner [CornerSize] to apply.
 */
/*inline*/ fun CutCornerShape(corner: CornerSize) = CutCornerShape(corner, corner, corner, corner)

/**
 * Creates [CutCornerShape] with the same size applied for all four corners.
 * @param size Size in [Dp] to apply.
 */
/*inline*/ fun CutCornerShape(size: Dp) = CutCornerShape(CornerSize(size))

/**
 * Creates [CutCornerShape] with the same size applied for all four corners.
 * @param size Size in [Px] to apply.
 */
/*inline*/ fun CutCornerShape(size: Px) = CutCornerShape(CornerSize(size))

/**
 * Creates [CutCornerShape] with the same size applied for all four corners.
 * @param percent Size in percents to apply.
 */
/*inline*/ fun CutCornerShape(percent: Int) = CutCornerShape(CornerSize(percent))

/**
 * Creates [CutCornerShape] with sizes defined in [Dp].
 */
/*inline*/ fun CutCornerShape(
    topLeft: Dp = 0.dp,
    topRight: Dp = 0.dp,
    bottomRight: Dp = 0.dp,
    bottomLeft: Dp = 0.dp
) = CutCornerShape(
    CornerSize(topLeft),
    CornerSize(topRight),
    CornerSize(bottomRight),
    CornerSize(bottomLeft)
)

/**
 * Creates [CutCornerShape] with sizes defined in [Px].
 */
/*inline*/ fun CutCornerShape(
    topLeft: Px = 0.px,
    topRight: Px = 0.px,
    bottomRight: Px = 0.px,
    bottomLeft: Px = 0.px
) = CutCornerShape(
    CornerSize(topLeft),
    CornerSize(topRight),
    CornerSize(bottomRight),
    CornerSize(bottomLeft)
)

/**
 * Creates [CutCornerShape] with sizes defined in percents of the shape's smaller side.
 */
/*inline*/ fun CutCornerShape(
    @IntRange(from = 0, to = 50) topLeftPercent: Int = 0,
    @IntRange(from = 0, to = 50) topRightPercent: Int = 0,
    @IntRange(from = 0, to = 50) bottomRightPercent: Int = 0,
    @IntRange(from = 0, to = 50) bottomLeftPercent: Int = 0
) = CutCornerShape(
    CornerSize(topLeftPercent),
    CornerSize(topRightPercent),
    CornerSize(bottomRightPercent),
    CornerSize(bottomLeftPercent)
)
