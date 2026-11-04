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

package androidx.wear.compose.foundation.lazy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.wear.compose.foundation.isRoundDevice
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Identifies a snap point: the [pointIndex]-th snap point of the item at [itemIndex]. Keys are
 * ordered as snap points are laid out. Packed in a [Long], as keys are created for every snap point
 * on every layout while the list scrolls.
 */
@JvmInline
internal value class TransformingLazyColumnSnapPointKey
private constructor(private val packed: Long) {
    constructor(
        itemIndex: Int,
        pointIndex: Int,
    ) : this((itemIndex.toLong() shl 32) or (pointIndex.toLong() and 0xFFFFFFFFL))

    val itemIndex: Int
        get() = (packed shr 32).toInt()

    val pointIndex: Int
        get() = packed.toInt()

    operator fun compareTo(other: TransformingLazyColumnSnapPointKey): Int =
        if (itemIndex != other.itemIndex) {
            itemIndex.compareTo(other.itemIndex)
        } else {
            pointIndex.compareTo(other.pointIndex)
        }

    override fun toString(): String = "SnapPointKey($itemIndex, $pointIndex)"
}

/** A snap point, at [position] px from the start of the viewport. */
internal class TransformingLazyColumnSnapPoint(
    val key: TransformingLazyColumnSnapPointKey,
    val position: Float,
)

/**
 * The snap points of one layout of a [TransformingLazyColumn], in layout order, and the [snapLine]
 * they snap to. Positions are offsets from the start of the viewport in the layout direction, as in
 * the layout info, so they also apply to reverse layouts. [fallbackStepSize] is the rotary step
 * when the nearest snap point has no neighbour, for example a single visible item kept whole.
 *
 * [unclampedPoints] are the snap points in rest geometry before clamping to the scroll range: when
 * [canScrollBefore] or [canScrollAfter], points beyond the first or last of [points] are only out
 * of reach while items are shrunk, and may become reachable as the list scrolls.
 */
internal class TransformingLazyColumnSnapPoints(
    val points: List<TransformingLazyColumnSnapPoint>,
    val snapLine: Float,
    private val hasItemsBefore: Boolean = false,
    private val hasItemsAfter: Boolean = false,
    private val fallbackStepSize: Float = 0f,
    private val unclampedPoints: List<TransformingLazyColumnSnapPoint> = points,
    canScrollBefore: Boolean = false,
    canScrollAfter: Boolean = false,
) {
    private val firstUnclampedIndex: Int =
        if (unclampedPoints === points || points.isEmpty()) {
            0
        } else {
            indexOfKey(unclampedPoints, points.first().key).coerceAtLeast(0)
        }

    private val lastUnclampedIndex: Int =
        if (unclampedPoints === points || points.isEmpty()) {
            unclampedPoints.lastIndex
        } else {
            val index = indexOfKey(unclampedPoints, points.last().key)
            if (index >= 0) index else unclampedPoints.lastIndex
        }

    private val collapsedBefore: Int = if (canScrollBefore) firstUnclampedIndex else 0

    private val collapsedAfter: Int =
        if (canScrollAfter) {
            (unclampedPoints.lastIndex - lastUnclampedIndex).coerceAtLeast(0)
        } else {
            0
        }

    private val nearestIndex: Int = run {
        var nearest = -1
        var nearestDistance = Float.MAX_VALUE
        for (i in points.indices) {
            val distance = abs(points[i].position - snapLine)
            if (distance < nearestDistance) {
                nearest = i
                nearestDistance = distance
            }
        }
        nearest
    }

    /** The snap point closest to the [snapLine]. */
    val nearest: TransformingLazyColumnSnapPoint?
        get() = points.getOrNull(nearestIndex)

    fun indexOf(key: TransformingLazyColumnSnapPointKey): Int = indexOfKey(points, key)

    /** Scroll needed to bring [point] onto the [snapLine]. */
    fun distanceTo(point: TransformingLazyColumnSnapPoint): Float = point.position - snapLine

    /** Average distance from the [nearest] snap point to its neighbours: one rotary step. */
    fun stepSize(): Float {
        val i = nearestIndex
        if (i < 0) return 0f
        val previous = points.getOrNull(i - 1)?.let { stepBetween(it, points[i]) }
        val next = points.getOrNull(i + 1)?.let { stepBetween(points[i], it) }
        return if (previous != null && next != null) {
            (previous + next) / 2f
        } else {
            previous ?: next ?: fallbackStepSize
        }
    }

    /**
     * Step distance between adjacent [a] and [b] in [points], floored by their spacing in
     * [unclampedPoints] so clamping to a nearby scroll bound does not collapse the step size.
     */
    internal fun stepBetween(
        a: TransformingLazyColumnSnapPoint,
        b: TransformingLazyColumnSnapPoint,
    ): Float {
        val clamped = abs(b.position - a.position)
        if (unclampedPoints === points) return clamped
        val ia = indexOfKey(unclampedPoints, a.key)
        val ib = indexOfKey(unclampedPoints, b.key)
        return if (ia >= 0 && ib >= 0) {
            max(clamped, abs(unclampedPoints[ib].position - unclampedPoints[ia].position))
        } else {
            clamped
        }
    }

    /** Whether there are items that are not laid out in [direction] (1 forward, -1 backward). */
    fun hasItemsBeyond(direction: Int): Boolean =
        if (direction > 0) hasItemsAfter else hasItemsBefore

    /**
     * Snap points collapsed into the edge snap point in [direction] (1 forward, -1 backward), while
     * the list can still scroll there and items may still grow.
     */
    fun collapsedPointsBeyond(direction: Int): Int =
        if (direction > 0) collapsedAfter else collapsedBefore

    /**
     * The snap point nearest to where the fling would stop ([decayDistance]). With a [direction],
     * only snap points ahead are considered, so that a short fling still moves on within a tall
     * item. When the target is an edge snap point representing collapsed points, re-picks among
     * them by rest geometry at the edge position so a fast fling is not stopped early.
     */
    fun flingTarget(decayDistance: Float, direction: Int): TransformingLazyColumnSnapPoint? {
        val target = flingTargetIn(points, 0, points.size, decayDistance, direction) ?: return null
        val unclamped =
            when {
                target === points.first() && collapsedBefore > 0 ->
                    flingTargetIn(
                        unclampedPoints,
                        0,
                        firstUnclampedIndex + 1,
                        decayDistance,
                        direction,
                    )
                target === points.last() && collapsedAfter > 0 ->
                    flingTargetIn(
                        unclampedPoints,
                        lastUnclampedIndex,
                        unclampedPoints.size,
                        decayDistance,
                        direction,
                    )
                else -> null
            } ?: return target
        return if (unclamped.key == target.key) {
            target
        } else {
            TransformingLazyColumnSnapPoint(unclamped.key, target.position)
        }
    }

    private fun flingTargetIn(
        list: List<TransformingLazyColumnSnapPoint>,
        from: Int,
        to: Int,
        decayDistance: Float,
        direction: Int,
    ): TransformingLazyColumnSnapPoint? {
        var nearest: TransformingLazyColumnSnapPoint? = null
        var nearestDiff = Float.MAX_VALUE
        var nearestAhead: TransformingLazyColumnSnapPoint? = null
        var nearestAheadDiff = Float.MAX_VALUE
        for (i in from until to) {
            val point = list[i]
            val distance = distanceTo(point)
            val diff = abs(distance - decayDistance)
            if (diff < nearestDiff) {
                nearest = point
                nearestDiff = diff
            }
            if (
                direction != 0 && distance * direction > AheadTolerancePx && diff < nearestAheadDiff
            ) {
                nearestAhead = point
                nearestAheadDiff = diff
            }
        }
        return nearestAhead ?: nearest
    }

    /**
     * The snap point with [key]. If it is not laid out, the first or last snap point when [key] is
     * before or after all of them at the start or end of the list, for example an edge snap point
     * whose key changed as the list scrolled, or null.
     */
    fun pointFor(key: TransformingLazyColumnSnapPointKey): TransformingLazyColumnSnapPoint? {
        points.getOrNull(indexOf(key))?.let {
            return it
        }
        val first = points.firstOrNull() ?: return null
        val last = points.last()
        return when {
            key < first.key && !hasItemsBefore -> first
            key > last.key && !hasItemsAfter -> last
            else -> null
        }
    }

    /** Scroll to the snap point for [key] (see [pointFor]), null if there is none. */
    fun distanceTo(key: TransformingLazyColumnSnapPointKey): Float? =
        pointFor(key)?.let { distanceTo(it) }

    private companion object {
        const val AheadTolerancePx = 0.5f

        fun indexOfKey(
            points: List<TransformingLazyColumnSnapPoint>,
            key: TransformingLazyColumnSnapPointKey,
        ): Int {
            for (i in points.indices) {
                if (points[i].key == key) return i
            }
            return -1
        }
    }
}

/**
 * Rotary snap target: [pendingSteps] snap points beyond [key]. Steps beyond the laid out snap
 * points are aimed at [stepPx] each until [resolve] moves them onto snap points.
 */
internal data class TransformingLazyColumnSnapTarget(
    val key: TransformingLazyColumnSnapPointKey,
    val pendingSteps: Int = 0,
    val stepPx: Float = 0f,
) {
    /**
     * Moves pending steps onto laid out [snapPoints], keeping at most the collapsed ones at an edge
     * of the list and dropping those past it. A [key] before or after the laid out snap points
     * moves to the first or last of them.
     */
    fun resolve(snapPoints: TransformingLazyColumnSnapPoints): TransformingLazyColumnSnapTarget {
        var target =
            snapPoints.pointFor(key)?.let { if (it.key == key) this else copy(key = it.key) }
                ?: return this
        while (target.pendingSteps != 0) {
            val direction = target.pendingSteps.sign
            val pos = snapPoints.indexOf(target.key)
            if (pos < 0) return target
            val current = snapPoints.points[pos]
            val next =
                snapPoints.points.getOrNull(pos + direction)
                    ?: return if (snapPoints.hasItemsBeyond(direction)) {
                        target
                    } else {
                        val maxPending = snapPoints.collapsedPointsBeyond(direction)
                        target.copy(
                            pendingSteps =
                                (target.pendingSteps * direction).coerceAtMost(maxPending) *
                                    direction
                        )
                    }
            target =
                TransformingLazyColumnSnapTarget(
                    key = next.key,
                    pendingSteps = target.pendingSteps - direction,
                    stepPx = snapPoints.stepBetween(current, next),
                )
        }
        return target
    }

    /** Remaining scroll to this target, null if it is not laid out. */
    fun distance(snapPoints: TransformingLazyColumnSnapPoints): Float? {
        val pos = snapPoints.indexOf(key)
        if (pos < 0) return null
        return snapPoints.distanceTo(snapPoints.points[pos]) + pendingSteps * stepPx
    }

    /** Whether one more step in [direction] has a snap point. */
    fun canStep(direction: Int, snapPoints: TransformingLazyColumnSnapPoints): Boolean {
        val pos = snapPoints.indexOf(key)
        if (pos < 0) return true
        val pointsBeyond =
            (if (direction > 0) snapPoints.points.size - 1 - pos else pos) +
                snapPoints.collapsedPointsBeyond(direction)
        return snapPoints.hasItemsBeyond(direction) || pointsBeyond > pendingSteps * direction
    }
}

/**
 * Snap points of the visible items of a [TransformingLazyColumn]. An item no taller than its snap
 * segment threshold ([TransformingLazyColumnItemScope.snapSegmentThreshold], or
 * [defaultSnapSegmentThresholdPx]) has one snap point at its center, as with item based snapping; a
 * taller item has one at the center of each of its `ceil(height / threshold)` equal segments.
 *
 * @param snapOffsetPx Distance from the viewport center to the snap line, as in item based
 *   snapping.
 * @param screenDiameterPx Null if the screen is not round. The default threshold assumes that the
 *   list fills the screen and is centered on it.
 * @param cornerRadiusPx Corner radius assumed for items.
 */
internal class TransformingLazyColumnSnapPointsProvider(
    private val state: TransformingLazyColumnState,
    private val snapOffsetPx: Int,
    private val screenDiameterPx: Float?,
    private val cornerRadiusPx: Float = 0f,
) {
    private var cachedLayout: TransformingLazyColumnMeasureResult? = null
    private var cachedPoints: TransformingLazyColumnSnapPoints? = null

    /** The snap points of the current layout, computed once per layout. */
    fun current(): TransformingLazyColumnSnapPoints {
        val layout = state.layoutInfoState.value
        cachedPoints?.let { if (layout === cachedLayout) return it }
        return snapPointsOf(layout).also {
            cachedLayout = layout
            cachedPoints = it
        }
    }

    private fun snapPointsOf(
        layout: TransformingLazyColumnMeasureResult
    ): TransformingLazyColumnSnapPoints {
        val viewportHeight = layout.viewportSize.height
        val snapLine = viewportHeight / 2f - snapOffsetPx
        val visibleItems = layout.visibleItems
        // No snap points needed when the list cannot scroll.
        if (visibleItems.isEmpty() || (!layout.canScrollBackward && !layout.canScrollForward)) {
            return TransformingLazyColumnSnapPoints(emptyList(), snapLine)
        }
        val points = ArrayList<TransformingLazyColumnSnapPoint>(visibleItems.size)
        var totalHeight = 0
        // Geometry of the items once scrolled to the center: scrolling moves the anchor item
        // linearly, and the other items grow back to their measured height as they reach the
        // center, so items are laid out from the anchor item with their measured heights, as
        // with item based snapping. Using the transformed geometry would make targets drift.
        // Items before the anchor item are laid out back from it.
        var top = 0f
        var anchorFound = false
        for (i in visibleItems.indices) {
            val item = visibleItems[i]
            if (item.index == layout.anchorItemIndex) {
                top = item.offset - top
                anchorFound = true
                break
            }
            top += item.measuredHeight + layout.itemSpacing
        }
        if (!anchorFound) top = visibleItems.first().offset.toFloat()
        // Items with a size, for the fallback step size.
        var sizedItems = 0
        visibleItems.fastForEach { item ->
            // An empty item, for example collapsed or not shown, is not a snap point: the list
            // would rest on nothing, and snap points of consecutive empty items would coincide.
            if (item.measuredHeight <= 0) {
                // Laid out as below: at its transformed height if it is the anchor item.
                val emptyHeight =
                    if (item.index == layout.anchorItemIndex) item.transformedHeight else 0
                top += emptyHeight + layout.itemSpacing
                return@fastForEach
            }
            sizedItems++
            totalHeight += item.measuredHeight
            val measuredItem = item as? TransformingLazyColumnMeasuredItem
            val measuredWidth = measuredItem?.placeable?.width
            // Off a round screen, the whole viewport is visible.
            val defaultThreshold =
                if (screenDiameterPx == null) {
                    viewportHeight
                } else {
                    defaultSnapSegmentThresholdPx(
                        measuredWidthPx = measuredWidth,
                        screenDiameterPx = screenDiameterPx,
                        cornerRadiusPx = cornerRadiusPx,
                        snapOffsetPx = snapOffsetPx,
                    )
                }
            // Measured size, so that the count does not change while the item is transformed.
            val count =
                snapSegmentCount(
                    measuredHeightPx = item.measuredHeight,
                    requestedThresholdPx = measuredItem?.snapSegmentThresholdPx,
                    defaultThresholdPx = defaultThreshold,
                    viewportHeightPx = viewportHeight,
                )
            // The anchor item is where it is, at its transformed height: its measured height
            // would put its segments outside of it while it is transformed. The snap behaviors
            // re-aim as the anchor item changes.
            val height =
                if (item.index == layout.anchorItemIndex) {
                    item.transformedHeight
                } else {
                    item.measuredHeight
                }
            val pointSpacing = height.toFloat() / count
            for (k in 0 until count) {
                points +=
                    TransformingLazyColumnSnapPoint(
                        TransformingLazyColumnSnapPointKey(item.index, k),
                        top + (k + 0.5f) * pointSpacing,
                    )
            }
            top += height + layout.itemSpacing
        }
        // Points are in layout order: visible items are in index order.
        val first = visibleItems.first()
        val last = visibleItems.last()
        // Where the snap line is when the list is scrolled to its start or end. Current geometry,
        // not rest frames: the list stops where its current layout ends.
        val start =
            if (first.index == 0) {
                snapLine + first.offset - layout.beforeContentPadding
            } else null
        val end =
            if (last.index == layout.totalItemsCount - 1) {
                snapLine + last.offset + last.transformedHeight + layout.afterContentPadding -
                    viewportHeight
            } else null
        return TransformingLazyColumnSnapPoints(
            points = clampedToScrollRange(points, start, end),
            snapLine = snapLine,
            hasItemsBefore = first.index > 0,
            hasItemsAfter = last.index < layout.totalItemsCount - 1,
            // As with item based snapping: the average item size, of the items with a size.
            fallbackStepSize =
                if (sizedItems > 0) totalHeight.toFloat() / sizedItems + layout.itemSpacing else 0f,
            unclampedPoints = points,
            canScrollBefore =
                layout.canScrollBackward && (start == null || start < snapLine - EdgeTolerancePx),
            canScrollAfter =
                layout.canScrollForward && (end == null || end > snapLine + EdgeTolerancePx),
        )
    }

    internal companion object {
        private const val EdgeTolerancePx = 1f

        /**
         * Replaces the snap points beyond the [start] or [end] of the list, which cannot be
         * reached, with one snap point where the list stops. As with item based snapping, no snap
         * point is added on the content padding (e.g. an EdgeButton) otherwise. Null edges are not
         * laid out.
         */
        fun clampedToScrollRange(
            points: List<TransformingLazyColumnSnapPoint>,
            start: Float?,
            end: Float?,
        ): List<TransformingLazyColumnSnapPoint> {
            // The content fits the viewport and the list does not scroll: no snap points, so that
            // turning the crown does not give snap haptics.
            if (start != null && end != null && start > end) return emptyList()
            // Points are in layout order, so those beyond the edges are at the ends of the list.
            var from = 0
            if (start != null) {
                while (from < points.size && points[from].position < start - EdgeTolerancePx) {
                    from++
                }
            }
            var to = points.size
            if (end != null) {
                while (to > from && points[to - 1].position > end + EdgeTolerancePx) {
                    to--
                }
            }
            if (from == 0 && to == points.size) return points
            val result = ArrayList<TransformingLazyColumnSnapPoint>(to - from + 2)
            if (start != null && from > 0) {
                if (from == to || points[from].position - start > EdgeTolerancePx) {
                    result += TransformingLazyColumnSnapPoint(points[from - 1].key, start)
                }
            }
            for (i in from until to) {
                result += points[i]
            }
            if (end != null && to < points.size) {
                val last = result.lastOrNull()
                if (last == null || end - last.position > EdgeTolerancePx) {
                    result += TransformingLazyColumnSnapPoint(points[to].key, end)
                }
            }
            return result
        }

        /**
         * Cap on the item width for the default threshold, as a fraction of the diameter: a full
         * width Material3 card, inset by the 5.2% horizontal margins of its screen.
         */
        private const val ReferenceItemWidthFraction = 1f - 2 * 0.052f

        /**
         * Height of the band around the center of a round screen of diameter D in which an item of
         * width w with corners of radius r is fully visible: `2r + 2 * sqrt((D/2 - r)^2 - (w/2 -
         * r)^2)`, with w capped by [ReferenceItemWidthFraction], less `2 * |snapOffsetPx|` as
         * segments are centered on the snap line rather than on the screen.
         */
        fun defaultSnapSegmentThresholdPx(
            measuredWidthPx: Int?,
            screenDiameterPx: Float,
            cornerRadiusPx: Float,
            snapOffsetPx: Int = 0,
        ): Int {
            val referenceWidth = screenDiameterPx * ReferenceItemWidthFraction
            val itemWidth =
                measuredWidthPx?.let { minOf(it.toFloat(), referenceWidth) } ?: referenceWidth
            val halfD = screenDiameterPx / 2f
            val halfW = itemWidth.coerceIn(0f, screenDiameterPx) / 2f
            val r = cornerRadiusPx.coerceIn(0f, halfW)
            val arcX = halfW - r
            val arcR = halfD - r
            val band = floor(2f * r + 2f * sqrt((arcR * arcR - arcX * arcX).coerceAtLeast(0f)))
            return (band.toInt() - 2 * abs(snapOffsetPx)).coerceAtLeast(0)
        }

        /**
         * Number of segments of an item. A [requestedThresholdPx] at least [measuredHeightPx] keeps
         * it whole; 0 or less, or null, uses [defaultThresholdPx]; other values are coerced to
         * between [MinThresholdFraction] of [viewportHeightPx] and [viewportHeightPx].
         */
        fun snapSegmentCount(
            measuredHeightPx: Int,
            requestedThresholdPx: Int?,
            defaultThresholdPx: Int,
            viewportHeightPx: Int,
        ): Int {
            if (viewportHeightPx <= 0) return 1
            val requested = requestedThresholdPx?.takeIf { it > 0 }
            if (requested != null && requested >= measuredHeightPx) return 1
            val minThreshold = ceil(viewportHeightPx * MinThresholdFraction).toInt()
            val threshold =
                (requested ?: defaultThresholdPx)
                    .coerceIn(minThreshold, viewportHeightPx.coerceAtLeast(minThreshold))
                    .coerceAtLeast(1)
            return max(1, (measuredHeightPx + threshold - 1) / threshold)
        }

        private const val MinThresholdFraction = 0.2f
    }
}

/**
 * Corner radius assumed for the default threshold: an approximation, as foundation does not know
 * item shapes. Matches the default large corner of Wear cards and buttons.
 */
private val DefaultItemCornerRadius = 26.dp

/** Remembers the [TransformingLazyColumnSnapPointsProvider] of [state]. */
@Composable
internal fun rememberTransformingLazyColumnSnapPoints(
    state: TransformingLazyColumnState,
    snapOffsetPx: Int,
): TransformingLazyColumnSnapPointsProvider {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenDiameterPx =
        if (isRoundDevice()) {
            with(density) { configuration.screenWidthDp.dp.toPx() }
        } else null
    val cornerRadiusPx = with(density) { DefaultItemCornerRadius.toPx() }
    return remember(state, snapOffsetPx, screenDiameterPx, cornerRadiusPx) {
        TransformingLazyColumnSnapPointsProvider(
            state,
            snapOffsetPx,
            screenDiameterPx,
            cornerRadiusPx,
        )
    }
}
