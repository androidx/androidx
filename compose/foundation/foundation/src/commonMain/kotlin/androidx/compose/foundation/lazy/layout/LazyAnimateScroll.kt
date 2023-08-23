/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.copy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

private class ItemFoundInScroll(
    val itemOffset: Int,
    val previousAnimation: AnimationState<Float, AnimationVector1D>
) : CancellationException()

private val TargetDistance = 2500.dp
private val BoundDistance = 1500.dp
private val MinimumDistance = 50.dp

private const val DEBUG = false
private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("LazyScrolling: ${generateMsg()}")
    }
}

/**
 * A scope to allow customization of animated scroll in LazyLayouts. This scope contains all needed
 * information to perform an animatedScroll in a scrollable LazyLayout.
 */
@ExperimentalFoundationApi
internal interface LazyLayoutAnimateScrollScope {

    /**
     * The index of the first visible item in the lazy layout.
     */
    val firstVisibleItemIndex: Int

    /**
     * The offset of the first visible item.
     */
    val firstVisibleItemScrollOffset: Int

    /**
     * The last visible item in the LazyLayout, lastVisibleItemIndex - firstVisibleItemOffset + 1
     * is the number of visible items.
     */
    val lastVisibleItemIndex: Int

    /**
     * The total item count.
     */
    val itemCount: Int

    /**
     * The average size of visible items.
     */
    val visibleItemsAverageSize: Int

    /**
     * Retrieves the scroll offset for an item that is currently visible.
     */
    fun getVisibleItemScrollOffset(index: Int): Int

    /**
     * Immediately scroll to [index] and settle in [scrollOffset].
     */
    fun ScrollScope.snapToItem(index: Int, scrollOffset: Int)

    /**
     * The "expected" distance to [targetIndex]. This means, how far one needs to scroll to have
     * [targetIndex] be the [firstVisibleItemIndex] and [firstVisibleItemScrollOffset] be
     * [targetItemOffset]. In other words, how far one needs to scroll to reach [targetIndex].
     */
    fun calculateDistanceTo(targetIndex: Int, targetItemOffset: Int): Float

    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [ScrollScope.scrollBy] and [ScrollScope.snapToItem]. All actions that change the logical
     * scroll position must be performed within a [scroll] block (even if they don't call any other
     * methods on this object) in order to guarantee that mutual exclusion is enforced.
     *
     * If [scroll] is called from elsewhere, this will be canceled.
     */
    suspend fun scroll(block: suspend ScrollScope.() -> Unit)
}

@Suppress("PrimitiveInLambda")
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyLayoutAnimateScrollScope.isItemVisible(index: Int): Boolean {
    return index in firstVisibleItemIndex..lastVisibleItemIndex
}

@OptIn(ExperimentalFoundationApi::class)
internal suspend fun LazyLayoutAnimateScrollScope.animateScrollToItem(
    index: Int,
    scrollOffset: Int,
    numOfItemsForTeleport: Int,
    density: Density
) {
    scroll {
        require(index >= 0f) { "Index should be non-negative ($index)" }

        try {
            val targetDistancePx = with(density) { TargetDistance.toPx() }
            val boundDistancePx = with(density) { BoundDistance.toPx() }
            val minDistancePx = with(density) { MinimumDistance.toPx() }
            var loop = true
            var anim = AnimationState(0f)

            if (isItemVisible(index)) {
                val targetItemInitialOffset = getVisibleItemScrollOffset(index)
                // It's already visible, just animate directly
                throw ItemFoundInScroll(targetItemInitialOffset, anim)
            }
            val forward = index > firstVisibleItemIndex

            fun isOvershot(): Boolean {
                // Did we scroll past the item?
                @Suppress("RedundantIf") // It's way easier to understand the logic this way
                return if (forward) {
                    if (firstVisibleItemIndex > index) {
                        true
                    } else if (
                        firstVisibleItemIndex == index &&
                        firstVisibleItemScrollOffset > scrollOffset
                    ) {
                        true
                    } else {
                        false
                    }
                } else { // backward
                    if (firstVisibleItemIndex < index) {
                        true
                    } else if (
                        firstVisibleItemIndex == index &&
                        firstVisibleItemScrollOffset < scrollOffset
                    ) {
                        true
                    } else {
                        false
                    }
                }
            }

            var loops = 1
            while (loop && itemCount > 0) {
                val expectedDistance = calculateDistanceTo(index, scrollOffset)
                val target = if (abs(expectedDistance) < targetDistancePx) {
                    val absTargetPx = maxOf(abs(expectedDistance), minDistancePx)
                    if (forward) absTargetPx else -absTargetPx
                } else {
                    if (forward) targetDistancePx else -targetDistancePx
                }

                debugLog {
                    "Scrolling to index=$index offset=$scrollOffset from " +
                        "index=$firstVisibleItemIndex offset=$firstVisibleItemScrollOffset with " +
                        "calculated target=$target"
                }

                anim = anim.copy(value = 0f)
                var prevValue = 0f
                anim.animateTo(
                    target,
                    sequentialAnimation = (anim.velocity != 0f)
                ) {
                    // If we haven't found the item yet, check if it's visible.
                    if (!isItemVisible(index)) {
                        // Springs can overshoot their target, clamp to the desired range
                        val coercedValue = if (target > 0) {
                            value.coerceAtMost(target)
                        } else {
                            value.coerceAtLeast(target)
                        }
                        val delta = coercedValue - prevValue
                        debugLog {
                            "Scrolling by $delta (target: $target, coercedValue: $coercedValue)"
                        }

                        val consumed = scrollBy(delta)
                        if (isItemVisible(index)) {
                            debugLog { "Found the item after performing scrollBy()" }
                        } else if (!isOvershot()) {
                            if (delta != consumed) {
                                debugLog { "Hit end without finding the item" }
                                cancelAnimation()
                                loop = false
                                return@animateTo
                            }
                            prevValue += delta
                            if (forward) {
                                if (value > boundDistancePx) {
                                    debugLog { "Struck bound going forward" }
                                    cancelAnimation()
                                }
                            } else {
                                if (value < -boundDistancePx) {
                                    debugLog { "Struck bound going backward" }
                                    cancelAnimation()
                                }
                            }

                            if (forward) {
                                if (
                                    loops >= 2 &&
                                    index - lastVisibleItemIndex > numOfItemsForTeleport
                                ) {
                                    // Teleport
                                    debugLog { "Teleport forward" }
                                    snapToItem(
                                        index = index - numOfItemsForTeleport,
                                        scrollOffset = 0
                                    )
                                }
                            } else {
                                if (
                                    loops >= 2 &&
                                    firstVisibleItemIndex - index > numOfItemsForTeleport
                                ) {
                                    // Teleport
                                    debugLog { "Teleport backward" }
                                    snapToItem(
                                        index = index + numOfItemsForTeleport,
                                        scrollOffset = 0
                                    )
                                }
                            }
                        }
                    }

                    // We don't throw ItemFoundInScroll when we snap, because once we've snapped to
                    // the final position, there's no need to animate to it.
                    if (isOvershot()) {
                        debugLog {
                            "Overshot, " +
                                "item $firstVisibleItemIndex at  $firstVisibleItemScrollOffset," +
                                " target is $scrollOffset"
                        }
                        snapToItem(index = index, scrollOffset = scrollOffset)
                        loop = false
                        cancelAnimation()
                        return@animateTo
                    } else if (isItemVisible(index)) {
                        val targetItemOffset = getVisibleItemScrollOffset(index)
                        debugLog { "Found item" }
                        throw ItemFoundInScroll(targetItemOffset, anim)
                    }
                }

                loops++
            }
        } catch (itemFound: ItemFoundInScroll) {
            // We found it, animate to it
            // Bring to the requested position - will be automatically stopped if not possible
            val anim = itemFound.previousAnimation.copy(value = 0f)
            val target = (itemFound.itemOffset + scrollOffset).toFloat()
            var prevValue = 0f
            debugLog {
                "Seeking by $target at velocity ${itemFound.previousAnimation.velocity}"
            }
            anim.animateTo(
                target,
                sequentialAnimation = (anim.velocity != 0f)
            ) {
                // Springs can overshoot their target, clamp to the desired range
                val coercedValue = when {
                    target > 0 -> {
                        value.coerceAtMost(target)
                    }

                    target < 0 -> {
                        value.coerceAtLeast(target)
                    }

                    else -> {
                        debugLog { "WARNING: somehow ended up seeking 0px, this shouldn't happen" }
                        0f
                    }
                }
                val delta = coercedValue - prevValue
                debugLog { "Seeking by $delta (coercedValue = $coercedValue)" }
                val consumed = scrollBy(delta)
                if (delta != consumed /* hit the end, stop */ ||
                    coercedValue != value /* would have overshot, stop */
                ) {
                    cancelAnimation()
                }
                prevValue += delta
            }
            // Once we're finished the animation, snap to the exact position to account for
            // rounding error (otherwise we tend to end up with the previous item scrolled the
            // tiniest bit onscreen)
            // TODO: prevent temporarily scrolling *past* the item
            snapToItem(index = index, scrollOffset = scrollOffset)
        }
    }
}
