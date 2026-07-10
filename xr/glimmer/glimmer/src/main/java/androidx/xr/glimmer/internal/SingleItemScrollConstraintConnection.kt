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

package androidx.xr.glimmer.internal

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerLayoutInfo
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * A [NestedScrollConnection] that anchors the gesture start position and prevents scrolling beyond
 * the immediate neighbor items.
 */
internal class SingleItemScrollConstraintConnection(private val pagerState: PagerState) :
    NestedScrollConnection {

    /** Item where the current gesture (drag + fling) started, null meaning no active gesture. */
    private var anchorItem: Int? = null

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // We only restrict direct user input and allow programmatic scroll.
        if (source != NestedScrollSource.UserInput) {
            // Reset the anchor item if this is a scroll for the snapping animation.
            anchorItem = null
            return Offset.Zero
        }

        val layoutInfo = pagerState.layoutInfo
        val delta = if (layoutInfo.isVertical()) available.y else available.x
        if (delta == 0f) return Offset.Zero

        // Reset the anchor if we are not already scrolling.
        if (!pagerState.isScrollInProgress) anchorItem = null

        // Latch the anchor item on the first scroll event of a gesture.
        val anchor = anchorItem ?: pagerState.currentPage.also { anchorItem = it }

        val itemSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        if (itemSize <= 0) return Offset.Zero

        // How much the scroll is intending to move in item sizes.
        val deltaInItems = delta / (-itemSize.toFloat())

        val currentPosition = calculateCurrentPosition()
        val targetPosition = currentPosition + deltaInItems

        val minItem = (anchor - 1).toFloat()
        val maxItem = (anchor + 1).toFloat()

        // If the target position is outside bounds, clamp it.
        if (targetPosition !in minItem..maxItem) {
            val clampedPosition = targetPosition.coerceIn(minItem, maxItem)

            val allowedDeltaInItems = clampedPosition - currentPosition
            val allowedPixels = allowedDeltaInItems * (-itemSize)

            // Consume the excess delta.
            val consumedDelta = delta - allowedPixels
            return if (layoutInfo.isVertical()) Offset(x = 0f, y = consumedDelta)
            else Offset(x = consumedDelta, y = 0f)
        }

        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val anchor = anchorItem ?: return Velocity.Zero
        val currentPosition = calculateCurrentPosition()

        val minItem = (anchor - 1).toFloat()
        val maxItem = (anchor + 1).toFloat()

        val layoutInfo = pagerState.layoutInfo
        val velocity = if (layoutInfo.isVertical()) available.y else available.x

        // If the user is already at the limit (hit the wall) and tries to fling further in that
        // direction, we kill the velocity to stop the Pager from trying to snap to the next page.
        if (isFlingingOutOfBounds(currentPosition, minItem, maxItem, velocity)) {
            return available // Consume all velocity.
        }

        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        // The gesture (Drag + Fling) is complete. Reset the anchor for the next interaction.
        anchorItem = null
        return Velocity.Zero
    }

    private fun calculateCurrentPosition(): Float =
        pagerState.currentPage + pagerState.currentPageOffsetFraction

    private fun isFlingingOutOfBounds(
        currentPosition: Float,
        minItem: Float,
        maxItem: Float,
        velocity: Float,
    ): Boolean {
        return (currentPosition >= maxItem && velocity < 0) ||
            (currentPosition <= minItem && velocity > 0)
    }

    private fun PagerLayoutInfo.isVertical(): Boolean = orientation == Orientation.Vertical
}
