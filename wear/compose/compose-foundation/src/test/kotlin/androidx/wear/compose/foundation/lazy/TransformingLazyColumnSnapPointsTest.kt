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

import androidx.wear.compose.foundation.lazy.TransformingLazyColumnSnapPointsProvider.Companion.clampedToScrollRange
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnSnapPointsProvider.Companion.defaultSnapSegmentThresholdPx
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnSnapPointsProvider.Companion.snapSegmentCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TransformingLazyColumnSnapPointsTest {
    @Test
    fun defaultThreshold_squareCorners_isChordAtItemWidth() {
        // sqrt(500^2 - 300^2) = 400
        assertEquals(400, defaultThreshold(measuredWidthPx = 300, cornerRadiusPx = 0f))
    }

    @Test
    fun defaultThreshold_roundedCorners_isLarger() {
        // 2 * 52 + 2 * sqrt((228 - 52)^2 - (204 - 52)^2) = 104 + 2 * sqrt(7872) ~= 281.45
        assertEquals(
            281,
            defaultSnapSegmentThresholdPx(
                measuredWidthPx = 408,
                screenDiameterPx = 456f,
                cornerRadiusPx = 52f,
            ),
        )
    }

    @Test
    fun defaultThreshold_cornerRadiusOverHalfWidth_isClamped() {
        // r = 100: 2 * 100 + 2 * sqrt((250 - 100)^2 - 0) = 500
        assertEquals(500, defaultThreshold(measuredWidthPx = 200, cornerRadiusPx = 1000f))
    }

    @Test
    fun defaultThreshold_wideItem_capsWidthAtReference() {
        // 0.896 * 500 = 448: sqrt(500^2 - 448^2) ~= 222.03
        assertEquals(222, defaultThreshold(measuredWidthPx = 500, cornerRadiusPx = 0f))
        assertEquals(222, defaultThreshold(measuredWidthPx = 448, cornerRadiusPx = 0f))
    }

    @Test
    fun defaultThreshold_unknownWidth_usesReferenceWidth() {
        assertEquals(222, defaultThreshold(measuredWidthPx = null, cornerRadiusPx = 0f))
    }

    @Test
    fun defaultThreshold_narrowItem_isTallerThanFullWidth() {
        assertTrue(
            defaultThreshold(measuredWidthPx = 300, cornerRadiusPx = 0f) >
                defaultThreshold(measuredWidthPx = 448, cornerRadiusPx = 0f)
        )
    }

    @Test
    fun defaultThreshold_snapOffset_reducedByTwiceTheOffset() {
        // The band of 400 px is centered on the screen; a snap line 30 px off the center leaves
        // 400 - 2 * 30 px on its further side.
        assertEquals(
            340,
            defaultSnapSegmentThresholdPx(
                measuredWidthPx = 300,
                screenDiameterPx = 500f,
                cornerRadiusPx = 0f,
                snapOffsetPx = -30,
            ),
        )
    }

    @Test
    fun segmentCount_itemNoTallerThanThreshold_isOne() {
        assertEquals(1, count(measuredHeightPx = 100))
        assertEquals(1, count(measuredHeightPx = 300))
    }

    @Test
    fun segmentCount_itemSlightlyTallerThanThreshold_isTwo() {
        assertEquals(2, count(measuredHeightPx = 301))
    }

    @Test
    fun segmentCount_tallItem_isFewestSegmentsWithinThreshold() {
        assertEquals(4, count(measuredHeightPx = 1000))
    }

    @Test
    fun segmentCount_requestedAtLeastItemHeight_keepsItemWhole() {
        assertEquals(1, count(measuredHeightPx = 1000, requestedThresholdPx = 1000))
        assertEquals(1, count(measuredHeightPx = 1000, requestedThresholdPx = Int.MAX_VALUE))
    }

    @Test
    fun segmentCount_requestedZeroOrLess_usesDefault() {
        assertEquals(4, count(measuredHeightPx = 1000, requestedThresholdPx = 0))
        assertEquals(4, count(measuredHeightPx = 1000, requestedThresholdPx = -5))
    }

    @Test
    fun segmentCount_requestedTooSmall_coercedToMinimum() {
        // Coerced to 100 px.
        assertEquals(10, count(measuredHeightPx = 1000, requestedThresholdPx = 10))
    }

    @Test
    fun segmentCount_requestedAboveViewport_coercedToViewport() {
        // Coerced to 500 px.
        assertEquals(2, count(measuredHeightPx = 1000, requestedThresholdPx = 700))
    }

    @Test
    fun segmentCount_emptyViewport_isOne() {
        assertEquals(
            1,
            snapSegmentCount(
                measuredHeightPx = 5000,
                requestedThresholdPx = null,
                defaultThresholdPx = 300,
                viewportHeightPx = 0,
            ),
        )
    }

    @Test
    fun segmentCount_defaultAboveViewport_coercedToViewport() {
        assertEquals(2, count(measuredHeightPx = 600, defaultThresholdPx = 800))
    }

    @Test
    fun clampedToScrollRange_pointsInRange_keptWithoutEdgePoints() {
        val points = clampedToScrollRange(points(150f, 250f), start = 100f, end = 300f)
        assertEquals(listOf(150f, 250f), points.map { it.position })
    }

    @Test
    fun clampedToScrollRange_pointsBeyondEdges_replacedByOnePointAtEdge() {
        val points =
            clampedToScrollRange(points(50f, 150f, 250f, 350f, 450f), start = 100f, end = 300f)
        assertEquals(listOf(100f, 150f, 250f, 300f), points.map { it.position })
        assertEquals(key(0), points.first().key)
        assertEquals(key(3), points.last().key)
    }

    @Test
    fun clampedToScrollRange_pointAlreadyOnEdge_dropsPointsBeyond() {
        val points =
            clampedToScrollRange(points(50f, 100.5f, 200f, 299.5f, 350f), start = 100f, end = 300f)
        assertEquals(listOf(100.5f, 200f, 299.5f), points.map { it.position })
    }

    @Test
    fun clampedToScrollRange_contentFitsViewport_noSnapPoints() {
        // The start of the list is after its end: the content fits the viewport.
        val points = clampedToScrollRange(points(50f, 150f, 250f), start = 200f, end = 100f)
        assertTrue(points.isEmpty())
    }

    @Test
    fun clampedToScrollRange_edgesNotLaidOut_ignored() {
        val points = clampedToScrollRange(points(50f, 350f), start = null, end = null)
        assertEquals(listOf(50f, 350f), points.map { it.position })
    }

    @Test
    fun nearest_isClosestToSnapLine() {
        assertEquals(key(2), snapPoints(0f, 100f, 190f, 300f).nearest?.key)
    }

    @Test
    fun stepSize_twoNeighbours_isAverageDistance() {
        assertEquals(150f, snapPoints(0f, 100f, 200f, 400f).stepSize())
    }

    @Test
    fun stepSize_noNeighbours_isFallback() {
        val points =
            TransformingLazyColumnSnapPoints(
                points = listOf(TransformingLazyColumnSnapPoint(key(0), 200f)),
                snapLine = 200f,
                hasItemsAfter = true,
                fallbackStepSize = 404f,
            )
        assertEquals(404f, points.stepSize())
    }

    @Test
    fun stepSize_noNeighbours_isZero() {
        assertEquals(0f, snapPoints(200f).stepSize())
    }

    @Test
    fun flingTarget_noDirection_isNearestToDecay() {
        // The decay of 100 px is nearer to staying (200) than to the next point (500).
        assertEquals(200f, snapPoints(200f, 500f).flingTarget(100f, direction = 0)?.position)
    }

    @Test
    fun flingTarget_shortFlingForward_isNextPointAhead() {
        assertEquals(500f, snapPoints(200f, 500f).flingTarget(100f, direction = 1)?.position)
    }

    @Test
    fun flingTarget_shortFlingBackward_isPreviousPoint() {
        assertEquals(-100f, snapPoints(-100f, 200f).flingTarget(-100f, direction = -1)?.position)
    }

    @Test
    fun flingTarget_longFling_isPointAheadNearestToDecay() {
        assertEquals(
            600f,
            snapPoints(200f, 300f, 400f, 600f, 800f).flingTarget(450f, direction = 1)?.position,
        )
    }

    @Test
    fun flingTarget_noPointAhead_isNearest() {
        assertEquals(200f, snapPoints(0f, 200f).flingTarget(100f, direction = 1)?.position)
    }

    @Test
    fun key_ordering_followsItemThenPointIndex() {
        assertTrue(
            TransformingLazyColumnSnapPointKey(1, pointIndex = 2) <
                TransformingLazyColumnSnapPointKey(2, pointIndex = 0)
        )
        assertTrue(
            TransformingLazyColumnSnapPointKey(1, pointIndex = 0) <
                TransformingLazyColumnSnapPointKey(1, pointIndex = 1)
        )
    }

    @Test
    fun pointFor_laidOutKey_isItsPoint() {
        assertEquals(100f, snapPoints(0f, 100f, 200f).pointFor(key(1))?.position)
    }

    @Test
    fun pointFor_keyBeyondLaidOutPoints_isFirstOrLastPoint() {
        // For example an edge snap point whose key changed as the list scrolled.
        val laidOut =
            TransformingLazyColumnSnapPoints(
                listOf(
                    TransformingLazyColumnSnapPoint(key(3), 0f),
                    TransformingLazyColumnSnapPoint(key(4), 100f),
                ),
                snapLine = 0f,
            )

        assertEquals(key(3), laidOut.pointFor(key(1))?.key)
        assertEquals(key(4), laidOut.pointFor(key(9))?.key)
        assertEquals(100f, laidOut.distanceTo(key(9)))
    }

    @Test
    fun pointFor_keyBeyondLaidOutPointsWithMoreItems_isNull() {
        // The item is only out of view, rather than an edge snap point that changed.
        val laidOut =
            TransformingLazyColumnSnapPoints(
                listOf(
                    TransformingLazyColumnSnapPoint(key(3), 0f),
                    TransformingLazyColumnSnapPoint(key(4), 100f),
                ),
                snapLine = 0f,
                hasItemsBefore = true,
                hasItemsAfter = true,
            )

        assertNull(laidOut.pointFor(key(1)))
        assertNull(laidOut.pointFor(key(9)))
    }

    @Test
    fun pointFor_keyBetweenLaidOutPoints_isNull() {
        val laidOut =
            TransformingLazyColumnSnapPoints(
                listOf(
                    TransformingLazyColumnSnapPoint(key(0), 0f),
                    TransformingLazyColumnSnapPoint(key(2), 100f),
                ),
                snapLine = 0f,
            )

        assertNull(laidOut.pointFor(key(1)))
        assertNull(laidOut.distanceTo(key(1)))
    }

    @Test
    fun resolve_keyAfterLaidOutPoints_movesToLastPoint() {
        val resolved = TransformingLazyColumnSnapTarget(key(9)).resolve(snapPoints(0f, 100f))

        assertEquals(TransformingLazyColumnSnapTarget(key(1)), resolved)
    }

    @Test
    fun resolve_pendingStepsForward_moveOntoLaidOutPoints() {
        val target = TransformingLazyColumnSnapTarget(key(2), pendingSteps = 2)

        val resolved = target.resolve(snapPoints(0f, 100f, 200f, 300f, 400f))

        assertEquals(TransformingLazyColumnSnapTarget(key(4), stepPx = 100f), resolved)
    }

    @Test
    fun resolve_pendingStepsBackward_moveOntoLaidOutPoints() {
        val target = TransformingLazyColumnSnapTarget(key(2), pendingSteps = -2)

        val resolved = target.resolve(snapPoints(0f, 100f, 200f, 300f, 400f))

        assertEquals(TransformingLazyColumnSnapTarget(key(0), stepPx = 100f), resolved)
    }

    @Test
    fun resolve_stepsBeyondLaidOutPoints_stayPending() {
        val laidOut = snapPoints(0f, 100f, 200f, 300f, 400f, hasItemsAfter = true)

        val resolved = TransformingLazyColumnSnapTarget(key(3), pendingSteps = 3).resolve(laidOut)

        assertEquals(TransformingLazyColumnSnapTarget(key(4), 2, 100f), resolved)
        // Snap point 4 is 200 px below the snap line, plus 2 steps of 100 px.
        assertEquals(400f, resolved.distance(laidOut))
    }

    @Test
    fun resolve_stepsPastEndOfList_dropped() {
        val resolved =
            TransformingLazyColumnSnapTarget(key(3), pendingSteps = 3)
                .resolve(snapPoints(0f, 100f, 200f, 300f, 400f))

        assertEquals(TransformingLazyColumnSnapTarget(key(4), stepPx = 100f), resolved)
    }

    @Test
    fun distance_targetNotLaidOut_isNull() {
        assertNull(TransformingLazyColumnSnapTarget(key(9)).distance(snapPoints(0f, 100f)))
    }

    @Test
    fun canStep_atEndOfList_isFalse() {
        val laidOut = snapPoints(0f, 100f, 200f)

        assertTrue(TransformingLazyColumnSnapTarget(key(1)).canStep(1, laidOut))
        assertFalse(TransformingLazyColumnSnapTarget(key(1), pendingSteps = 1).canStep(1, laidOut))
        assertFalse(TransformingLazyColumnSnapTarget(key(0)).canStep(-1, laidOut))
    }

    @Test
    fun canStep_moreItemsAfter_isTrue() {
        val laidOut = snapPoints(0f, 100f, 200f, hasItemsAfter = true)

        assertTrue(TransformingLazyColumnSnapTarget(key(2), pendingSteps = 5).canStep(1, laidOut))
    }

    @Test
    fun collapsedPointsBeyond_scrollableVsAtEdges_countsOnlyWhileScrollable() {
        val unclamped = points(-50f, 50f, 150f, 250f, 350f, 450f)
        val clamped = clampedToScrollRange(unclamped, start = 100f, end = 300f)

        val scrollable =
            TransformingLazyColumnSnapPoints(
                points = clamped,
                snapLine = 200f,
                unclampedPoints = unclamped,
                canScrollBefore = true,
                canScrollAfter = true,
            )
        assertEquals(1, scrollable.collapsedPointsBeyond(-1))
        assertEquals(1, scrollable.collapsedPointsBeyond(1))

        val atEdges =
            TransformingLazyColumnSnapPoints(
                points = clamped,
                snapLine = 200f,
                unclampedPoints = unclamped,
                canScrollBefore = false,
                canScrollAfter = false,
            )
        assertEquals(0, atEdges.collapsedPointsBeyond(-1))
        assertEquals(0, atEdges.collapsedPointsBeyond(1))

        // No synthetic edge point added when an existing point is already within tolerance.
        val onEdges = points(50f, 100.5f, 200f, 299.5f, 350f)
        val clampedOnEdges =
            TransformingLazyColumnSnapPoints(
                points = clampedToScrollRange(onEdges, start = 100f, end = 300f),
                snapLine = 200f,
                unclampedPoints = onEdges,
                canScrollBefore = true,
                canScrollAfter = true,
            )
        assertEquals(1, clampedOnEdges.collapsedPointsBeyond(-1))
        assertEquals(1, clampedOnEdges.collapsedPointsBeyond(1))
    }

    @Test
    fun flingTarget_collapsedAfter_repicksAmongCollapsedPointsOnly() {
        val unclamped = points(200f, 350f, 450f, 550f)
        val clamped = clampedToScrollRange(unclamped, start = null, end = 300f)
        val laidOut =
            TransformingLazyColumnSnapPoints(
                points = clamped,
                snapLine = 200f,
                unclampedPoints = unclamped,
                canScrollAfter = true,
            )

        // Short fling keeps the borrowed key on the edge.
        val shortFling = laidOut.flingTarget(50f, direction = 1)!!
        assertEquals(key(1), shortFling.key)
        assertEquals(300f, shortFling.position)

        // Medium and fast flings pick further collapsed keys while keeping the edge position.
        val mediumFling = laidOut.flingTarget(240f, direction = 1)!!
        assertEquals(key(2), mediumFling.key)
        assertEquals(300f, mediumFling.position)

        val fastFling = laidOut.flingTarget(400f, direction = 1)!!
        assertEquals(key(3), fastFling.key)
        assertEquals(300f, fastFling.position)

        // Settling near the edge never pulls back to an interior point.
        assertEquals(key(1), laidOut.flingTarget(60f, direction = 0)?.key)
    }

    @Test
    fun flingTarget_collapsedBefore_repicksAmongCollapsedPointsOnly() {
        val unclamped = points(-150f, -50f, 50f, 200f)
        val clamped = clampedToScrollRange(unclamped, start = 100f, end = null)
        val laidOut =
            TransformingLazyColumnSnapPoints(
                points = clamped,
                snapLine = 200f,
                unclampedPoints = unclamped,
                canScrollBefore = true,
            )

        val shortFling = laidOut.flingTarget(-50f, direction = -1)!!
        assertEquals(key(2), shortFling.key)
        assertEquals(100f, shortFling.position)

        val mediumFling = laidOut.flingTarget(-240f, direction = -1)!!
        assertEquals(key(1), mediumFling.key)
        assertEquals(100f, mediumFling.position)

        val fastFling = laidOut.flingTarget(-400f, direction = -1)!!
        assertEquals(key(0), fastFling.key)
        assertEquals(100f, fastFling.position)
    }

    @Test
    fun resolveAndCanStep_collapsedPoints_cappedAtCollapsedCount() {
        val unclamped = points(100f, 200f, 350f, 450f, 550f)
        val clamped = clampedToScrollRange(unclamped, start = null, end = 300f)
        val laidOut =
            TransformingLazyColumnSnapPoints(
                points = clamped,
                snapLine = 200f,
                unclampedPoints = unclamped,
                canScrollAfter = true,
            )

        assertEquals(
            TransformingLazyColumnSnapTarget(key(2), pendingSteps = 1, stepPx = 150f),
            TransformingLazyColumnSnapTarget(key(1), pendingSteps = 2).resolve(laidOut),
        )
        assertEquals(
            TransformingLazyColumnSnapTarget(key(2), pendingSteps = 2, stepPx = 150f),
            TransformingLazyColumnSnapTarget(key(1), pendingSteps = 5).resolve(laidOut),
        )

        assertTrue(TransformingLazyColumnSnapTarget(key(2), pendingSteps = 0).canStep(1, laidOut))
        assertTrue(TransformingLazyColumnSnapTarget(key(2), pendingSteps = 1).canStep(1, laidOut))
        assertFalse(TransformingLazyColumnSnapTarget(key(2), pendingSteps = 2).canStep(1, laidOut))
    }

    @Test
    fun stepSizeAndResolve_clampedPointNearEdge_usesUnclampedSpacing() {
        val unclamped = points(95f, 195f, 295f, 395f, 495f)
        // Clamped end at 300f sits only 5px past the interior point at 295f.
        val clamped = clampedToScrollRange(unclamped, start = 90f, end = 300f)
        assertEquals(listOf(95f, 195f, 295f, 300f), clamped.map { it.position })

        val atEdge =
            TransformingLazyColumnSnapPoints(
                points = clamped,
                snapLine = 300f,
                unclampedPoints = unclamped,
                canScrollAfter = true,
            )
        assertEquals(100f, atEdge.stepSize())

        val beforeEdge =
            TransformingLazyColumnSnapPoints(
                points = clamped,
                snapLine = 295f,
                unclampedPoints = unclamped,
                canScrollAfter = true,
            )
        assertEquals(100f, beforeEdge.stepSize())

        val resolved =
            TransformingLazyColumnSnapTarget(key(2), pendingSteps = 2).resolve(beforeEdge)
        assertEquals(
            TransformingLazyColumnSnapTarget(key(3), pendingSteps = 1, stepPx = 100f),
            resolved,
        )
        assertEquals(105f, resolved.distance(beforeEdge))

        // Symmetrically at the start edge when clamped at 0f, 5px before an interior point at 5f.
        val unclampedStart = points(-95f, 5f, 105f, 205f)
        val atStartEdge =
            TransformingLazyColumnSnapPoints(
                points = clampedToScrollRange(unclampedStart, start = 0f, end = null),
                snapLine = 0f,
                unclampedPoints = unclampedStart,
                canScrollBefore = true,
            )
        assertEquals(100f, atStartEdge.stepSize())
    }

    @Test
    fun clampedToScrollRange_allPointsBeyondStartOrEnd_leavesSingleEdgePoint() {
        val beforeStart = clampedToScrollRange(points(10f, 50f), start = 100f, end = 300f)
        assertEquals(listOf(100f), beforeStart.map { it.position })
        assertEquals(key(1), beforeStart.single().key)

        val afterEnd = clampedToScrollRange(points(350f, 450f), start = 100f, end = 300f)
        assertEquals(listOf(300f), afterEnd.map { it.position })
        assertEquals(key(0), afterEnd.single().key)
    }

    @Test
    fun stepSize_atFirstOrLastPoint_usesSingleNeighbour() {
        // Nearest is index 0 (at snapLine = 200f): only the next neighbour exists.
        assertEquals(140f, snapPoints(200f, 340f, 500f).stepSize())
        // Nearest is lastIndex (at snapLine = 200f): only the previous neighbour exists.
        assertEquals(110f, snapPoints(0f, 90f, 200f).stepSize())
    }

    @Test
    fun flingTarget_emptyPoints_isNull() {
        val empty = TransformingLazyColumnSnapPoints(emptyList(), snapLine = 200f)
        assertNull(empty.nearest)
        assertEquals(0f, empty.stepSize())
        assertNull(empty.flingTarget(100f, direction = 1))
        assertNull(empty.flingTarget(0f, direction = 0))
    }

    private fun key(itemIndex: Int) = TransformingLazyColumnSnapPointKey(itemIndex, pointIndex = 0)

    /** Snap points of items at [positions]. */
    private fun points(vararg positions: Float) = positions.mapIndexed { i, p ->
        TransformingLazyColumnSnapPoint(key(i), p)
    }

    /** Snap points of items at [positions], with the snap line at 200 px. */
    private fun snapPoints(vararg positions: Float, hasItemsAfter: Boolean = false) =
        TransformingLazyColumnSnapPoints(
            points = positions.mapIndexed { i, p -> TransformingLazyColumnSnapPoint(key(i), p) },
            snapLine = 200f,
            hasItemsAfter = hasItemsAfter,
        )

    /** Default threshold on a round screen of 500 px. */
    private fun defaultThreshold(measuredWidthPx: Int?, cornerRadiusPx: Float) =
        defaultSnapSegmentThresholdPx(
            measuredWidthPx = measuredWidthPx,
            screenDiameterPx = 500f,
            cornerRadiusPx = cornerRadiusPx,
        )

    /** Segment count in a viewport of 500 px, with a default threshold of 300 px. */
    private fun count(
        measuredHeightPx: Int,
        requestedThresholdPx: Int? = null,
        defaultThresholdPx: Int = 300,
    ) =
        snapSegmentCount(
            measuredHeightPx = measuredHeightPx,
            requestedThresholdPx = requestedThresholdPx,
            defaultThresholdPx = defaultThresholdPx,
            viewportHeightPx = 500,
        )
}
