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

package androidx.ui.layout

import androidx.annotation.FloatRange
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.ModifierScope
import androidx.ui.core.satisfiedBy
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite

/**
 * A layout modifier that attempts to size a layout to match a specified aspect ratio. The layout
 * modifier will try to match one of the incoming constraints, in the following order: maxWidth,
 * maxHeight, minWidth, minHeight. The size in the other dimension will then be computed
 * according to the aspect ratio. Note that the provided aspectRatio will always correspond to
 * the width/height ratio.
 *
 * If a valid size that satisfies the constraints is found this way, the modifier will size the
 * target layout to it: the layout will be measured with the tight constraints to match the size.
 * If a child is present, it will be measured with tight constraints to match the size.
 * If no valid size is found, the aspect ratio will not be satisfied, and the target layout will
 * be measured with the original constraints.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleAspectRatio
 *
 * @param aspectRatio A positive non-zero value representing the aspect ratio.
 */
data class LayoutAspectRatio(
    @FloatRange(from = 0.0, fromInclusive = false)
    val aspectRatio: Float
) : LayoutModifier {
    init {
        require(aspectRatio > 0) { "aspectRatio $aspectRatio must be > 0" }
    }

    override fun ModifierScope.modifyConstraints(constraints: Constraints): Constraints {
        val size = constraints.findSizeWith(aspectRatio)
        return if (size != null)
            Constraints.fixed(size.width, size.height)
        else
            constraints
    }

    override fun ModifierScope.minIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx {
        return if (height == IntPx.Infinity) measurable.minIntrinsicWidth(height)
        else height * aspectRatio
    }

    override fun ModifierScope.maxIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx {
        return if (height == IntPx.Infinity) measurable.maxIntrinsicWidth(height)
        else height * aspectRatio
    }

    override fun ModifierScope.minIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx {
        return if (width == IntPx.Infinity) measurable.minIntrinsicHeight(width)
        else width / aspectRatio
    }

    override fun ModifierScope.maxIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx {
        return if (width == IntPx.Infinity) measurable.maxIntrinsicHeight(width)
        else width / aspectRatio
    }
}

private fun Constraints.findSizeWith(aspectRatio: Float): IntPxSize? {
    return listOf(
        IntPxSize(this.maxWidth, this.maxWidth / aspectRatio),
        IntPxSize(this.maxHeight * aspectRatio, this.maxHeight),
        IntPxSize(this.minWidth, this.minWidth / aspectRatio),
        IntPxSize(this.minHeight * aspectRatio, this.minHeight)
    ).find {
        this.satisfiedBy(it) &&
                it.width != 0.ipx && it.height != 0.ipx &&
                it.width.isFinite() && it.height.isFinite()
    }
}