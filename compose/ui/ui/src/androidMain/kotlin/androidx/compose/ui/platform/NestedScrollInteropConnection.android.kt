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

@file:JvmName("NestedScrollInteropConnectionKt")

package androidx.compose.ui.platform

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ComposeUiFlags.isNestedScrollInteropIntegerPropagationEnabled
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.TYPE_NON_TOUCH
import androidx.core.view.ViewCompat.TYPE_TOUCH
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Adapts nested scroll from View to Compose. This class is used by [ComposeView] to bridge nested
 * scrolling across View and Compose. It acts as both:
 * 1) [androidx.core.view.NestedScrollingChild3] by using an instance of
 *    [NestedScrollingChildHelper] to dispatch scroll deltas up to a consuming parent on the view
 *    side.
 * 2) [NestedScrollingChildHelper] by implementing this interface it should be able to receive
 *    deltas from dispatching children on the Compose side.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class NestedScrollInteropConnection(
    private val view: View,
    private val minFlingVelocity: Float,
) : NestedScrollConnection {

    private val nestedScrollChildHelper =
        NestedScrollingChildHelper(view).apply { isNestedScrollingEnabled = true }

    private val consumedScrollCache = IntArray(2)

    init {
        // Enables nested scrolling for the root view [AndroidComposeView].
        // Like in Compose, nested scrolling is a default implementation
        ViewCompat.setNestedScrollingEnabled(view, true)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Using the return of startNestedScroll to determine if nested scrolling will happen.
        if (nestedScrollChildHelper.startNestedScroll(available.scrollAxes, source.toViewType())) {
            // reuse
            consumedScrollCache.fill(0)

            val dx = composeToViewOffset(available.x)
            val dy = composeToViewOffset(available.y)
            nestedScrollChildHelper.dispatchNestedPreScroll(
                dx,
                dy,
                consumedScrollCache,
                null,
                source.toViewType(),
            )

            return toOffset(dx, dy, consumedScrollCache, available)
        }

        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        // Using the return of startNestedScroll to determine if nested scrolling will happen.
        if (nestedScrollChildHelper.startNestedScroll(available.scrollAxes, source.toViewType())) {
            consumedScrollCache.fill(0)
            val dx = composeToViewOffset(available.x)
            val dy = composeToViewOffset(available.y)

            nestedScrollChildHelper.dispatchNestedScroll(
                composeToViewOffset(consumed.x),
                composeToViewOffset(consumed.y),
                dx,
                dy,
                null,
                source.toViewType(),
                consumedScrollCache,
            )

            return toOffset(dx, dy, consumedScrollCache, available)
        }

        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val result =
            if (
                !nestedScrollChildHelper.dispatchNestedPreFling(
                    available.x.toViewVelocity(),
                    available.y.toViewVelocity(),
                )
            ) {
                val consumed =
                    nestedScrollChildHelper.dispatchNestedFling(
                        available.x.toViewVelocity(),
                        available.y.toViewVelocity(),
                        true,
                    )
                // Someone consume during onNestedFling
                if (consumed) available else Velocity.Zero
            } else {
                available // someone above consumed during onNestedPreFling
            }

        return result
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        // All nested fling methods in the view world happen during the PreFling phase of Compose.
        // Some examples of this are in ScrollView and RecyclerView.
        // When a fling happens in a child, the child will call dispatchNestedPreFling and if
        // nothing consumes it will immediately call dispatchNestedFling. Only then the child
        // will fling itself with any remaining velocity.

        // finalize fling process by declaring the end of nested scrolling.
        stopNestedScrolls()
        return Velocity.Zero
    }

    private fun stopNestedScrolls() {
        if (nestedScrollChildHelper.hasNestedScrollingParent(TYPE_TOUCH)) {
            nestedScrollChildHelper.stopNestedScroll(TYPE_TOUCH)
        }

        if (nestedScrollChildHelper.hasNestedScrollingParent(TYPE_NON_TOUCH)) {
            nestedScrollChildHelper.stopNestedScroll(TYPE_NON_TOUCH)
        }
    }
}

// Relative ceil for rounding. Ceiling away from zero to avoid missing scrolling deltas to rounding
// issues.
private fun Float.ceilAwayFromZero(): Float = if (this >= 0) ceil(this) else floor(this)

/**
 * Views deal with integer pixels and Compose uses floating point pixels. We will use a similar
 * approach that RecyclerView uses to avoid rounding issues.
 */
private fun Float.extractIntegerPixels(): Int = this.roundToInt()

// Compose coordinate system is the opposite of view's system
@OptIn(ExperimentalComposeUiApi::class)
internal fun composeToViewOffset(offset: Float): Int =
    if (isNestedScrollInteropIntegerPropagationEnabled) {
        offset.extractIntegerPixels() * -1
    } else {
        offset.ceilAwayFromZero().toInt() * -1
    }

// Compose scrolling sign system is the opposite of view's system
private fun Int.reverseAxis(): Float = this * -1f

private fun Float.toViewVelocity(): Float = this * -1f

/**
 * Converts the view world array into compose [Offset] entity. This is bound by the values in the
 * available [Offset] in order to account for rounding errors produced by the Int to Float
 * conversions.
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun toOffset(dx: Int, dy: Int, consumed: IntArray, available: Offset): Offset {
    /**
     * Since our conversion from Float to Int may result in overflow not being reported correctly we
     * need to re-add the overflow when passing the consumption data back to compose. We will assume
     * that the overflow was also consumed.
     */
    val overflowX =
        if (isNestedScrollInteropIntegerPropagationEnabled) {
            available.x - dx.reverseAxis()
        } else {
            0f
        }

    val overflowY =
        if (isNestedScrollInteropIntegerPropagationEnabled) {
            available.y - dy.reverseAxis()
        } else {
            0f
        }

    val offsetX =
        if (available.x >= 0) {
            (consumed[0].reverseAxis() + overflowX).coerceAtMost(available.x)
        } else {
            (consumed[0].reverseAxis() + overflowX).coerceAtLeast(available.x)
        }

    val offsetY =
        if (available.y >= 0) {
            (consumed[1].reverseAxis() + overflowY).coerceAtMost(available.y)
        } else {
            (consumed[1].reverseAxis() + overflowY).coerceAtLeast(available.y)
        }

    return Offset(offsetX, offsetY)
}

private fun NestedScrollSource.toViewType(): Int =
    when (this) {
        NestedScrollSource.UserInput -> TYPE_TOUCH
        else -> TYPE_NON_TOUCH
    }

// TODO (levima) Maybe use a more accurate threshold?
private const val ScrollingAxesThreshold = 0.5f

/**
 * Make an assumption that the scrolling axes is determined by a threshold of 0.5 on either
 * direction.
 */
private val Offset.scrollAxes: Int
    get() {
        var axes = ViewCompat.SCROLL_AXIS_NONE
        if (x.absoluteValue >= ScrollingAxesThreshold) {
            axes = axes or ViewCompat.SCROLL_AXIS_HORIZONTAL
        }
        if (y.absoluteValue >= ScrollingAxesThreshold) {
            axes = axes or ViewCompat.SCROLL_AXIS_VERTICAL
        }
        return axes
    }

/** Make an assumption that the scrolling axes is determined by a min fling velocity */
private fun Velocity.scrollAxes(minFlingVelocity: Float): Int {
    var axes = ViewCompat.SCROLL_AXIS_NONE
    if (x.absoluteValue >= minFlingVelocity) {
        axes = axes or ViewCompat.SCROLL_AXIS_HORIZONTAL
    }
    if (y.absoluteValue >= minFlingVelocity) {
        axes = axes or ViewCompat.SCROLL_AXIS_VERTICAL
    }
    return axes
}

/**
 * Create and [remember] the [NestedScrollConnection] that enables Nested Scroll Interop between a
 * View parent that implements [androidx.core.view.NestedScrollingParent3] and a Compose child. This
 * should be used in conjunction with a [androidx.compose.ui.input.nestedscroll.nestedScroll]
 * modifier. Nested Scroll is enabled by default on the compose side and you can use this connection
 * to enable both nested scroll on the view side and to add glue logic between View and compose.
 *
 * Note that this only covers the use case where a cooperating parent is used. A cooperating parent
 * is one that implements NestedScrollingParent3, a key layout that does that is
 * [androidx.coordinatorlayout.widget.CoordinatorLayout].
 *
 * @param hostView The View that hosts the compose scrollable, this is usually a ComposeView.
 *
 * Learn how to enable nested scroll interop:
 *
 * @sample androidx.compose.ui.samples.ComposeInCooperatingViewNestedScrollInteropSample
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun rememberNestedScrollInteropConnection(
    hostView: View = LocalView.current
): NestedScrollConnection {
    val viewConfiguration = LocalViewConfiguration.current
    return remember(hostView, viewConfiguration) {
        NestedScrollInteropConnection(hostView, viewConfiguration.minimumFlingVelocity)
    }
}
