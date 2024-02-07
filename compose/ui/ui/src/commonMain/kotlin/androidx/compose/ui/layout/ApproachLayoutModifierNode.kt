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

package androidx.compose.ui.layout

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.NodeMeasuringIntrinsics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize

/**
 * [ApproachLayoutModifierNode] is designed to support gradually approaching the destination layout
 * calculated in the lookahead pass. This can be particularly helpful when the destination layout is
 * anticipated to change drastically and would consequently result in visual disruptions.
 *
 * In order to create a smooth approach, an interpolation (often through animations) can be used
 * in [approachMeasure] to interpolate the measurement or placement from a previously recorded size
 * and/or position to the destination/target size and/or position. The destination size is
 * available in [ApproachMeasureScope] as [ApproachMeasureScope.lookaheadSize]. And the target
 * position can also be acquired in [ApproachMeasureScope] during placement by using
 * [LookaheadScope.localLookaheadPositionOf] with the layout's
 * [Placeable.PlacementScope.coordinates]. The sample code below illustrates how that can be
 * achieved.
 *
 * During the lookahead pass, [measure] will be invoked. By default [measure] simply passes the
 * incoming constraints to its child, and returns the child measure result to parent without any
 * modification. The default behavior for [measure] is simply a pass through of constraints and
 * measure results without modification. This can be overridden as needed. [approachMeasure]
 * will be invoked during the approach pass after lookahead.
 *
 * [isMeasurementApproachComplete] signals whether the measurement has already reached the
 * destination size. It will be queried after the destination has been determined by the lookahead
 * pass, before [approachMeasure] is invoked. The lookahead size is provided to
 * [isMeasurementApproachComplete] for convenience in deciding whether the destination size has
 * been reached.
 *
 * [isPlacementApproachComplete] indicates whether the position has approached
 * destination defined by the lookahead, hence it's a signal to the system for whether additional
 * approach placements are necessary. [isPlacementApproachComplete] will be invoked after the
 * destination position has been determined by lookahead pass, and before the placement phase in
 * [approachMeasure].
 *
 * **IMPORTANT**:
 * When both [isMeasurementApproachComplete] and [isPlacementApproachComplete] become true, the
 * approach is considered complete. Approach pass will subsequently snap the measurement and
 * placement to lookahead measurement and placement. Once approach is complete, [approachMeasure]
 * may never be invoked until either [isMeasurementApproachComplete] or
 * [isPlacementApproachComplete] becomes false again. Therefore it is important to ensure
 * [approachMeasure] and [measure] result in the same measurement and placement when the approach is
 * complete. Otherwise, there may be visual discontinuity when we snap the measurement and placement
 * to lookahead.
 *
 * It is important to be accurate in [isPlacementApproachComplete] and
 * [isMeasurementApproachComplete]. A prolonged indication of incomplete approach will prevent the
 * system from potentially skipping approach pass when possible.
 *
 * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
 */
interface ApproachLayoutModifierNode : LayoutModifierNode {
    /**
     * [isMeasurementApproachComplete] signals whether the measurement has already reached the
     * destination size. It will be queried after the destination has been determined by the
     * lookahead pass, before [approachMeasure] is invoked. The lookahead size is provided to
     * [isMeasurementApproachComplete] for convenience in deciding whether the destination size has
     * been reached.
     *
     * Note: It is important to be accurate in [isPlacementApproachComplete] and
     * [isMeasurementApproachComplete]. A prolonged indication of incomplete approach will prevent
     * the system from potentially skipping approach pass when possible.
     */
    fun isMeasurementApproachComplete(lookaheadSize: IntSize): Boolean

    /**
     * [isPlacementApproachComplete] indicates whether the position has approached destination
     * defined by the lookahead, hence it's a signal to the system for whether additional
     * approach placements are necessary. [isPlacementApproachComplete] will be invoked after the
     * destination position has been determined by lookahead pass, and before the placement phase in
     * [approachMeasure].
     *
     * Note: It is important to be accurate in [isPlacementApproachComplete] and
     * [isMeasurementApproachComplete]. A prolonged indication of incomplete approach will prevent
     * the system from potentially skipping approach pass when possible.
     *
     * By default, [isPlacementApproachComplete] returns true.
     */
    fun Placeable.PlacementScope.isPlacementApproachComplete(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        return true
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult = measurable.measure(constraints).run {
        layout(width, height) {
            place(0, 0)
        }
    }

    /**
     * [approachMeasure] defines how the measurement and placement of the layout approach the
     * destination size and position. In order to achieve a smooth approach from the current size
     * and position to the destination, an interpolation (often through animations) can be used
     * in [approachMeasure] to interpolate the measurement or placement from a previously recorded
     * size and position to the destination/target size and position. The destination size is
     * available in [ApproachMeasureScope] as [ApproachMeasureScope.lookaheadSize]. And the target
     * position can also be acquired in [ApproachMeasureScope] during placement by using
     * [LookaheadScope.localLookaheadPositionOf] with the layout's
     * [Placeable.PlacementScope.coordinates]. Please see sample code below for how that can be
     * achieved.
     *
     * Note: [approachMeasure] is only guaranteed to be invoked when either
     * [isPlacementApproachComplete] or [isMeasurementApproachComplete] is false. Otherwise, the
     * system will consider the approach complete (i.e. destination reached) and may skip the
     * approach pass when possible.
     *
     * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
     */
    @ExperimentalComposeUiApi
    fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult

    /**
     * The function used to calculate minIntrinsicWidth for the approach pass changes.
     */
    @ExperimentalComposeUiApi
    fun ApproachIntrinsicMeasureScope.minApproachIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = NodeMeasuringIntrinsics.minWidth(
        NodeMeasuringIntrinsics.ApproachMeasureBlock { intrinsicMeasurable, constraints ->
            approachMeasure(intrinsicMeasurable, constraints)
        },
        this,
        measurable,
        height
    )

    /**
     * The function used to calculate minIntrinsicHeight for the approach pass changes.
     */
    @ExperimentalComposeUiApi
    fun ApproachIntrinsicMeasureScope.minApproachIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = NodeMeasuringIntrinsics.minHeight(
        NodeMeasuringIntrinsics.ApproachMeasureBlock { intrinsicMeasurable, constraints ->
            approachMeasure(intrinsicMeasurable, constraints)
        },
        this,
        measurable,
        width
    )

    /**
     * The function used to calculate maxIntrinsicWidth for the approach pass changes.
     */
    @ExperimentalComposeUiApi
    fun ApproachIntrinsicMeasureScope.maxApproachIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = NodeMeasuringIntrinsics.maxWidth(
        NodeMeasuringIntrinsics.ApproachMeasureBlock { intrinsicMeasurable, constraints ->
            approachMeasure(intrinsicMeasurable, constraints)
        },
        this,
        measurable,
        height
    )

    /**
     * The function used to calculate maxIntrinsicHeight for the approach pass changes.
     */
    @ExperimentalComposeUiApi
    fun ApproachIntrinsicMeasureScope.maxApproachIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = NodeMeasuringIntrinsics.maxHeight(
        NodeMeasuringIntrinsics.ApproachMeasureBlock { intrinsicMeasurable, constraints ->
            approachMeasure(intrinsicMeasurable, constraints)
        },
        this,
        measurable,
        width
    )
}
