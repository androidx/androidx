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
package androidx.wear.compose.foundation.lazy

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.copy
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastSumBy
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

// TODO: b/373832623 - Migrate to `LazyLayoutScrollScope` when `animateScrollToItem` is available.
internal class TransformingLazyColumnScrollScope(
    private val state: TransformingLazyColumnState,
    scrollScope: ScrollScope
) : ScrollScope by scrollScope {
    val firstVisibleItemIndex: Int
        get() = state.layoutInfo.visibleItems.firstOrNull()?.index ?: 0

    val firstVisibleItemScrollOffset: Int
        get() = state.layoutInfo.visibleItems.firstOrNull()?.offset ?: 0

    val lastVisibleItemIndex: Int
        get() = state.layoutInfo.visibleItems.lastOrNull()?.index ?: 0

    val anchorItemIndex: Int
        get() = state.layoutInfoState.value.anchorItemIndex

    val anchorItemScrollOffset: Int
        get() = state.layoutInfoState.value.anchorItemScrollOffset

    val itemCount: Int
        get() = state.layoutInfo.totalItemsCount

    fun snapToItem(index: Int, offset: Int = 0) {
        state.snapToItemIndexInternal(index, offset)
    }

    fun approximateDistanceTo(targetIndex: Int, targetOffset: Int = 0): Int {
        val layoutInfo = state.layoutInfoState.value
        if (layoutInfo.visibleItems.isEmpty()) return 0
        return if (!isItemVisible(targetIndex)) {
            val averageSize =
                calculateVisibleItemsAverageHeight(layoutInfo) + layoutInfo.itemSpacing
            val indexesDiff = targetIndex - layoutInfo.anchorItemIndex
            (averageSize * indexesDiff) - layoutInfo.anchorItemScrollOffset
        } else {
            val visibleItem = layoutInfo.visibleItems.fastFirstOrNull { it.index == targetIndex }
            visibleItem?.offset ?: 0
        } + targetOffset
    }

    private fun calculateVisibleItemsAverageHeight(
        measureResult: TransformingLazyColumnMeasureResult
    ): Int {
        val visibleItems = measureResult.visibleItems
        return visibleItems.fastSumBy { it.measuredHeight } / visibleItems.size
    }

    internal fun TransformingLazyColumnScrollScope.isItemVisible(index: Int): Boolean {
        return index in firstVisibleItemIndex..lastVisibleItemIndex
    }
}

private class ItemFoundInScroll(
    val itemOffset: Int,
    val previousAnimation: AnimationState<Float, AnimationVector1D>
) : CancellationException()

private val TargetDistance = 500.dp
private val BoundDistance = 300.dp
private val MinimumDistance = 10.dp

private const val DEBUG = false

private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("LazyScrolling: ${generateMsg()}")
    }
}

internal suspend fun TransformingLazyColumnScrollScope.animateScrollToItem(
    index: Int,
    scrollOffset: Int,
    numOfItemsForTeleport: Int,
    density: Density,
    scrollScope: ScrollScope
) {
    with(scrollScope) {
        try {
            val targetDistancePx = with(density) { TargetDistance.toPx() }
            val boundDistancePx = with(density) { BoundDistance.toPx() }
            val minDistancePx = with(density) { MinimumDistance.toPx() }
            var loop = true
            var anim = AnimationState(0f)
            if (isItemVisible(index)) {
                val targetItemInitialOffset = approximateDistanceTo(index)
                // It's already visible, just animate directly
                throw ItemFoundInScroll(targetItemInitialOffset, anim)
            }
            val forward = index > anchorItemIndex
            fun isOvershot(): Boolean {
                // Did we scroll past the item?
                @Suppress("RedundantIf") // It's way easier to understand the logic this way
                return if (forward) {
                    if (anchorItemIndex > index) {
                        true
                    } else if (anchorItemIndex == index && anchorItemScrollOffset > scrollOffset) {
                        true
                    } else {
                        false
                    }
                } else { // backward
                    if (anchorItemIndex < index) {
                        true
                    } else if (anchorItemIndex == index && anchorItemScrollOffset < scrollOffset) {
                        true
                    } else {
                        false
                    }
                }
            }
            var loops = 1
            while (loop && itemCount > 0) {
                val expectedDistance = approximateDistanceTo(index) + scrollOffset
                val target =
                    if (abs(expectedDistance) < targetDistancePx) {
                        maxOf(abs(expectedDistance.toFloat()), minDistancePx)
                    } else {
                        targetDistancePx
                    } * if (forward) 1 else -1
                debugLog {
                    "Scrolling to index=$index offset=$scrollOffset from " +
                        "index=$firstVisibleItemIndex offset=$firstVisibleItemScrollOffset with " +
                        "calculated target=$target"
                }
                anim = anim.copy(value = 0f)
                var prevValue = 0f
                anim.animateTo(target, sequentialAnimation = (anim.velocity != 0f)) {
                    // If we haven't found the item yet, check if it's visible.
                    debugLog { "firstVisibleItemIndex=$firstVisibleItemIndex" }
                    if (!isItemVisible(index)) {
                        // Springs can overshoot their target, clamp to the desired range
                        val coercedValue =
                            if (target > 0) {
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
                                    snapToItem(index = index - numOfItemsForTeleport, offset = 0)
                                }
                            } else {
                                if (
                                    loops >= 2 &&
                                        firstVisibleItemIndex - index > numOfItemsForTeleport
                                ) {
                                    // Teleport
                                    debugLog { "Teleport backward" }
                                    snapToItem(index = index + numOfItemsForTeleport, offset = 0)
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
                        snapToItem(index = index, offset = scrollOffset)
                        loop = false
                        cancelAnimation()
                        return@animateTo
                    } else if (isItemVisible(index)) {
                        val targetItemOffset = approximateDistanceTo(index)
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
            debugLog { "Seeking by $target at velocity ${itemFound.previousAnimation.velocity}" }
            anim.animateTo(target, sequentialAnimation = (anim.velocity != 0f)) {
                // Springs can overshoot their target, clamp to the desired range
                val coercedValue =
                    when {
                        target > 0 -> {
                            value.coerceAtMost(target)
                        }
                        target < 0 -> {
                            value.coerceAtLeast(target)
                        }
                        else -> {
                            debugLog {
                                "WARNING: somehow ended up seeking 0px, this shouldn't happen"
                            }
                            0f
                        }
                    }
                val delta = coercedValue - prevValue
                debugLog { "Seeking by $delta (coercedValue = $coercedValue)" }
                val consumed = scrollBy(delta)
                if (
                    delta != consumed /* hit the end, stop */ ||
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
            snapToItem(index = index, offset = scrollOffset)
        }
    }
}
