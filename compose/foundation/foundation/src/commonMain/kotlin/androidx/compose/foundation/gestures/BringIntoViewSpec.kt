/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlin.math.abs

/**
 * A composition local to customize the focus scrolling behavior used by some scrollable containers.
 * [LocalBringIntoViewSpec] has a platform defined default behavior.
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
expect val LocalBringIntoViewSpec: ProvidableCompositionLocal<BringIntoViewSpec>

/**
 * The configuration of how a scrollable reacts to bring into view requests.
 *
 * Check the following sample for a use case usage of this API:
 *
 * @sample androidx.compose.foundation.samples.FocusScrollingInLazyRowSample
 */
@Stable
interface BringIntoViewSpec {

    /**
     * An Animation Spec to be used as the animation to run to fulfill the BringIntoView requests.
     */
    @Deprecated("Animation spec customization is no longer supported.")
    @get:Deprecated("Animation spec customization is no longer supported.")
    val scrollAnimationSpec: AnimationSpec<Float>
        get() = DefaultScrollAnimationSpec

    /**
     * Calculate the offset needed to bring one of the scrollable container's child into view. This
     * will be called for every frame of the scrolling animation. This means that, as the animation
     * progresses, the offset will naturally change to fulfill the scroll request.
     *
     * All distances below are represented in pixels.
     *
     * @param offset from the side closest to the start of the container.
     * @param size is the child size.
     * @param containerSize Is the main axis size of the scrollable container.
     * @return The necessary amount to scroll to satisfy the bring into view request. Returning zero
     *   from here means that the request was satisfied and the scrolling animation should stop.
     */
    fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        defaultCalculateScrollDistance(offset, size, containerSize)

    companion object {

        /**
         * The default animation spec used by [Modifier.scrollable] to run Bring Into View requests.
         */
        internal val DefaultScrollAnimationSpec: AnimationSpec<Float> = spring()

        internal val DefaultBringIntoViewSpec = object : BringIntoViewSpec {}

        internal fun defaultCalculateScrollDistance(
            offset: Float,
            size: Float,
            containerSize: Float
        ): Float {
            val trailingEdge = offset + size
            @Suppress("UnnecessaryVariable") val leadingEdge = offset
            return when {

                // If the item is already visible, no need to scroll.
                leadingEdge >= 0 && trailingEdge <= containerSize -> 0f

                // If the item is visible but larger than the parent, we don't scroll.
                leadingEdge < 0 && trailingEdge > containerSize -> 0f

                // Find the minimum scroll needed to make one of the edges coincide with the
                // parent's
                // edge.
                abs(leadingEdge) < abs(trailingEdge - containerSize) -> leadingEdge
                else -> trailingEdge - containerSize
            }
        }
    }
}
