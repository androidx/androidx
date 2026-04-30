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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Creates a [LayerOutsets] with the same value for all sides. */
fun LayerOutsets(all: Dp) = LayerOutsets(all, all, all, all)

/**
 * Creates a [LayerOutsets] where the horizontal value is applied to the left and right, and the
 * vertical value is applied to the top and bottom.
 */
fun LayerOutsets(vertical: Dp, horizontal: Dp) =
    LayerOutsets(horizontal, vertical, horizontal, vertical)

/**
 * Represents the outsets of a layer, which define the extra visual space around the layer's
 * content. These outsets can be used to expand the layer's bounds beyond its measured content size.
 *
 * This does not affect the clip or shadows itself and only increases the visual bounds of the
 * layer.
 *
 * All outset values must be non-negative.
 *
 * @param left The outset on the left side.
 * @param top The outset on the top side.
 * @param right The outset on the right side.
 * @param bottom The outset on the bottom side.
 */
@Immutable
class LayerOutsets(
    val left: Dp = 0.dp,
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp,
) {
    init {
        requirePrecondition(left >= 0.dp && right >= 0.dp && top >= 0.dp && bottom >= 0.dp) {
            "Layer outsets must be non-negative"
        }
    }

    companion object {
        /** A [LayerOutsets] with all sides set to zero. */
        val Zero = LayerOutsets()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerOutsets) return false

        if (left != other.left) return false
        if (top != other.top) return false
        if (right != other.right) return false
        if (bottom != other.bottom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        return result
    }

    override fun toString(): String {
        return "LayerOutsets(left=$left, top=$top, right=$right, bottom=$bottom)"
    }
}
