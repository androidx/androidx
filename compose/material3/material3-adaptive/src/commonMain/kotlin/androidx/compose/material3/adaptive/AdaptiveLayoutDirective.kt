/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive

import androidx.compose.ui.unit.Dp

/**
 * Top-level directives about how an adaptive layout should be arranged and spaced, like how many
 * partitions the layout can be split into and what should be the gutter size.
 */
class AdaptiveLayoutDirective(
    /** How many partitions along the horizontal axis the respective layout can be split into. */
    val maxHorizontalPartitions: Int,
    /** The gutter size of the respective layout should preserve. */
    val gutterSizes: GutterSizes,
    /**
     * How many partitions along the vertical axis the respective layout can be split into.
     * The default value is 1.
     */
    val maxVerticalPartitions: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdaptiveLayoutDirective) return false
        if (maxHorizontalPartitions != other.maxHorizontalPartitions) return false
        if (gutterSizes != other.gutterSizes) return false
        if (maxVerticalPartitions != other.maxVerticalPartitions) return false
        return true
    }

    override fun hashCode(): Int {
        var result = maxHorizontalPartitions
        result = 31 * result + gutterSizes.hashCode()
        result = 31 * result + maxVerticalPartitions
        return result
    }
}

/**
 * Denotes the gutter sizes of an adaptive layout. Gutters of an adaptive layouts include margins
 * between panes ([innerVertical] and [innerHorizontal]) and paddings of the layout itself
 * ([outerVertical] and [outerHorizontal]). Usually we will expect larger gutter sizes to be set
 * when the layout is larger and more panes are shown in the layout.
 */
class GutterSizes(
    /**
     * Size of the outer vertical gutters. It's similar to left/right paddings of a normal layout.
     */
    val outerVertical: Dp,
    /**
     * Size of the inner vertical gutters. It's similar to left/right margins of the layout's
     * children.
     */
    val innerVertical: Dp,
    /**
     * Size of the outer horizontal gutters. It's similar to top/bottom paddings of a normal layout.
     */
    val outerHorizontal: Dp = outerVertical,
    /**
     * Size of the inner horizontal gutters. It's similar to top/bottom margins of the layout's
     * children.
     */
    val innerHorizontal: Dp = innerVertical
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GutterSizes) return false
        if (outerVertical != other.outerVertical) return false
        if (innerVertical != other.innerVertical) return false
        if (outerHorizontal != other.outerHorizontal) return false
        if (innerHorizontal != other.innerHorizontal) return false
        return true
    }

    override fun hashCode(): Int {
        var result = outerVertical.hashCode()
        result = 31 * result + innerVertical.hashCode()
        result = 31 * result + outerHorizontal.hashCode()
        result = 31 * result + innerHorizontal.hashCode()
        return result
    }
}