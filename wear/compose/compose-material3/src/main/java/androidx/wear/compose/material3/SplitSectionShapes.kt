/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.material3.tokens.ShapeTokens

/**
 * Shape of the inner corners between the sections of split components (e.g. SplitCheckboxButton,
 * SplitSwitchButton, SplitRadioButton).
 */
private val SPLIT_SECTIONS_SHAPE = ShapeTokens.CornerExtraSmall

/**
 * Returns the shape of the start section: the outer (start) corners of this component [Shape], and
 * the inner (end) corners of [innerShape]. Falls back to [innerShape] if this is not a
 * [CornerBasedShape].
 */
internal fun Shape.startSection(innerShape: CornerBasedShape = SPLIT_SECTIONS_SHAPE): Shape {
    if (this !is CornerBasedShape) return innerShape
    return SplitSectionShape(copy(topEnd = innerShape.topEnd, bottomEnd = innerShape.bottomEnd))
}

/**
 * Returns the shape of the end section: the outer (end) corners of this component [Shape], and the
 * inner (start) corners of [innerShape]. Falls back to [innerShape] if this is not a
 * [CornerBasedShape].
 */
internal fun Shape.endSection(innerShape: CornerBasedShape = SPLIT_SECTIONS_SHAPE): Shape {
    if (this !is CornerBasedShape) return innerShape
    return SplitSectionShape(
        copy(topStart = innerShape.topStart, bottomStart = innerShape.bottomStart)
    )
}

/**
 * Wraps a [CornerBasedShape] for a horizontal split section so that corners resolve against
 * [Size.height] rather than [Size.minDimension] when a section is narrower than it is tall (such as
 * the 50% outer corners on [Slider] buttons).
 */
private data class SplitSectionShape(private val shape: CornerBasedShape) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val heightBasedSize = Size(size.height, size.height)
        val maxRadius = size.height / 2f

        return shape.createOutline(
            size = size,
            topStart = shape.topStart.toPx(heightBasedSize, density).coerceAtMost(maxRadius),
            topEnd = shape.topEnd.toPx(heightBasedSize, density).coerceAtMost(maxRadius),
            bottomEnd = shape.bottomEnd.toPx(heightBasedSize, density).coerceAtMost(maxRadius),
            bottomStart = shape.bottomStart.toPx(heightBasedSize, density).coerceAtMost(maxRadius),
            layoutDirection = layoutDirection,
        )
    }
}
