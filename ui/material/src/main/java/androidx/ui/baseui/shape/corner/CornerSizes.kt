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

package androidx.ui.baseui.shape.corner

import androidx.annotation.IntRange
import androidx.ui.core.Dp
import androidx.ui.core.Px
import androidx.ui.core.dp
import androidx.ui.core.px

/**
 * Contains sizes of all four corner sizes for a shape.
 *
 * @param topLeft a size for the top left corner
 * @param topRight a size for the top right corner
 * @param bottomRight a size for the bottom left corner
 * @param bottomLeft a size for the bottom right corner
 */
data class CornerSizes(
    val topLeft: CornerSize,
    val topRight: CornerSize,
    val bottomRight: CornerSize,
    val bottomLeft: CornerSize
)

/**
 * Creates [CornerSizes] with the same size applied for all four corners.
 */
/*inline*/ fun CornerSizes(allCornersSize: CornerSize) = CornerSizes(
    allCornersSize,
    allCornersSize,
    allCornersSize,
    allCornersSize
)

/**
 * Creates [CornerSizes] with the same size applied for all four corners.
 */
/*inline*/ fun CornerSizes(size: Dp) = CornerSizes(CornerSize(size))

/**
 * Creates [CornerSizes] with the same size applied for all four corners.
 */
/*inline*/ fun CornerSizes(size: Px) = CornerSizes(CornerSize(size))

/**
 * Creates [CornerSizes] with the same size applied for all four corners.
 */
/*inline*/ fun CornerSizes(percent: Int) = CornerSizes(CornerSize(percent))

/**
 * Creates [CornerSizes] with sizes defined by [Dp].
 */
/*inline*/ fun CornerSizes(
    topLeft: Dp = 0.dp,
    topRight: Dp = 0.dp,
    bottomRight: Dp = 0.dp,
    bottomLeft: Dp = 0.dp
) = CornerSizes(
    CornerSize(topLeft),
    CornerSize(topRight),
    CornerSize(bottomRight),
    CornerSize(bottomLeft)
)

/**
 * Creates [CornerSizes] with sizes defined by [Px].
 */
/*inline*/ fun CornerSizes(
    topLeft: Px = 0.px,
    topRight: Px = 0.px,
    bottomRight: Px = 0.px,
    bottomLeft: Px = 0.px
) = CornerSizes(
    CornerSize(topLeft),
    CornerSize(topRight),
    CornerSize(bottomRight),
    CornerSize(bottomLeft)
)

/**
 * Creates [CornerSizes] with sizes defined by percents of the shape's smaller side.
 */
/*inline*/ fun CornerSizes(
    @IntRange(from = 0, to = 50) topLeftPercent: Int = 0,
    @IntRange(from = 0, to = 50) topRightPercent: Int = 0,
    @IntRange(from = 0, to = 50) bottomRightPercent: Int = 0,
    @IntRange(from = 0, to = 50) bottomLeftPercent: Int = 0
) = CornerSizes(
    CornerSize(topLeftPercent),
    CornerSize(topRightPercent),
    CornerSize(bottomRightPercent),
    CornerSize(bottomLeftPercent)
)
