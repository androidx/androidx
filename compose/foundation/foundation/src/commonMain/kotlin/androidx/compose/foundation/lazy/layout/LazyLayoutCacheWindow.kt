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

package androidx.compose.foundation.lazy.layout

import androidx.annotation.FloatRange
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Represents an out of viewport area of a Lazy Layout where items should be cached. Items will be
 * prepared in the Lazy Layout Cache Window area in advance to improve scroll performance.
 */
@Stable
interface LazyLayoutCacheWindow {
    /**
     * Determines whether the cache window populates after a non-scroll related trigger. When set to
     * `true`, the cache window will use non-scroll triggers to start the caching process. For
     * example, if layout data changes and causes a cache purge, the ahead window will be refilled
     * while the layout remains idle. Other common scenarios in which the ahead window fills include
     * initial composition and item reordering, during which the layout is in a non-scroll state,
     * providing the opportunity to populate the window.
     */
    @get:Suppress("GetterSetterNames")
    val enableNonScrollCaching: Boolean
        get() = true

    /**
     * Calculates the prefetch window area in pixels for prefetching on the scroll direction, "ahead
     * window". The prefetch window strategy will prepare items in the ahead area in advance so they
     * are ready to be used when they become visible.
     *
     * @param viewport The size of the viewport in this Lazy Layout in pixels.
     */
    fun Density.calculateAheadWindow(viewport: Int): Int = 0

    /**
     * Calculates the window area in pixels for keeping items in the scroll counter direction,
     * "behind window". Items in the behind window will not be disposed and can be accessed more
     * quickly if they become visible again.
     *
     * @param viewport The size of the viewport in this Lazy Layout in pixels.
     */
    fun Density.calculateBehindWindow(viewport: Int): Int = 0
}

/**
 * A Dp-based [LazyLayoutCacheWindow].
 *
 * @param ahead The size of the ahead window to be used as per
 *   [LazyLayoutCacheWindow.calculateAheadWindow].
 * @param behind The size of the behind window to be used as per
 *   [LazyLayoutCacheWindow.calculateBehindWindow].
 * @param enableNonScrollCaching whether the cache window populates after a non-scroll related
 *   trigger. When set to `true`, the cache window will use non-scroll triggers to start the caching
 *   process. For example, if layout data changes and causes a cache purge, the ahead window will be
 *   refilled while the layout remains idle. Other common scenarios in which the ahead window fills
 *   include initial composition and item reordering, during which the layout is in a non-scroll
 *   state, providing the opportunity to populate the window.
 */
fun LazyLayoutCacheWindow(
    ahead: Dp = 0.dp,
    behind: Dp = 0.dp,
    enableNonScrollCaching: Boolean = true,
): LazyLayoutCacheWindow {
    return DpLazyLayoutCacheWindow(
        ahead = ahead,
        behind = behind,
        enableNonScrollCaching = enableNonScrollCaching,
    )
}

private class DpLazyLayoutCacheWindow(
    val ahead: Dp,
    val behind: Dp,
    override val enableNonScrollCaching: Boolean,
) : LazyLayoutCacheWindow {
    override fun Density.calculateAheadWindow(viewport: Int): Int = ahead.roundToPx()

    override fun Density.calculateBehindWindow(viewport: Int): Int = behind.roundToPx()

    override fun hashCode(): Int {
        return 31 * ahead.hashCode() + behind.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is DpLazyLayoutCacheWindow) {
            other.ahead == this.ahead && other.behind == this.behind
        } else {
            false
        }
    }
}

/**
 * Creates a [LazyLayoutCacheWindow] based off a fraction of the viewport.
 *
 * @param aheadFraction The fraction of the viewport to be used for the ahead window.
 * @param behindFraction The fraction of the viewport to be used for the behind window.
 * @param enableNonScrollCaching whether the cache window populates after a non-scroll related
 *   trigger. When set to `true`, the cache window will use non-scroll triggers to start the caching
 *   process. For example, if layout data changes and causes a cache purge, the ahead window will be
 *   refilled while the layout remains idle. Other common scenarios in which the ahead window fills
 *   include initial composition and item reordering, during which the layout is in a non-scroll
 *   state, providing the opportunity to populate the window.
 */
fun LazyLayoutCacheWindow(
    @FloatRange(from = 0.0) aheadFraction: Float = 0.0f,
    @FloatRange(from = 0.0) behindFraction: Float = 0.0f,
    enableNonScrollCaching: Boolean = true,
): LazyLayoutCacheWindow =
    FractionLazyLayoutCacheWindow(
        aheadFraction = aheadFraction,
        behindFraction = behindFraction,
        enableNonScrollCaching = enableNonScrollCaching,
    )

private class FractionLazyLayoutCacheWindow(
    val aheadFraction: Float,
    val behindFraction: Float,
    override val enableNonScrollCaching: Boolean,
) : LazyLayoutCacheWindow {
    override fun Density.calculateAheadWindow(viewport: Int): Int =
        (viewport * aheadFraction).roundToInt()

    override fun Density.calculateBehindWindow(viewport: Int): Int =
        (viewport * behindFraction).roundToInt()

    override fun hashCode(): Int {
        return 31 * aheadFraction.hashCode() + behindFraction.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is FractionLazyLayoutCacheWindow) {
            other.aheadFraction == this.aheadFraction && other.behindFraction == this.behindFraction
        } else {
            false
        }
    }
}
