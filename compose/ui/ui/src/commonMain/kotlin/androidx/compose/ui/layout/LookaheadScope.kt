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

package androidx.compose.ui.layout

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize

/**
 * [LookaheadScope] creates a scope in which all layouts will first determine their destination
 * layout through a lookahead pass, followed by an _approach_ pass to run the measurement and
 * placement approach defined in [approachLayout] or [ApproachLayoutModifierNode], in order to
 * gradually reach the destination.
 *
 * Note: [LookaheadScope] does not introduce a new [Layout] to the [content] passed in. All the
 * [Layout]s in the [content] will have the same parent as they would without [LookaheadScope].
 *
 * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
 * @param content The child composable to be laid out.
 * @see ApproachLayoutModifierNode
 * @see approachLayout
 */
@UiComposable
@Composable
fun LookaheadScope(content: @Composable @UiComposable LookaheadScope.() -> Unit) {
    val scope = remember { LookaheadScopeImpl() }
    ReusableComposeNode<LayoutNode, Applier<Any>>(
        factory = { LayoutNode(isVirtual = true) },
        update = {
            init { isVirtualLookaheadRoot = true }
            set(scope) { scope ->
                // This internal lambda will be invoked during placement.
                scope.scopeCoordinates = { parent!!.innerCoordinator.coordinates }
            }
        },
        content = { scope.content() }
    )
}

/**
 * Creates an approach layout intended to help gradually approach the destination layout calculated
 * in the lookahead pass. This can be particularly helpful when the destination layout is
 * anticipated to change drastically and would consequently result in visual disruptions.
 *
 * In order to create a smooth approach, an interpolation (often through animations) can be used in
 * [approachMeasure] to interpolate the measurement or placement from a previously recorded size
 * and/or position to the destination/target size and/or position. The destination size is available
 * in [ApproachMeasureScope] as [ApproachMeasureScope.lookaheadSize]. And the target position can
 * also be acquired in [ApproachMeasureScope] during placement by using
 * [LookaheadScope.localLookaheadPositionOf] with the layout's
 * [Placeable.PlacementScope.coordinates]. The sample code below illustrates how that can be
 * achieved.
 *
 * [isMeasurementApproachInProgress] signals whether the measurement is in progress of approaching
 * destination size. It will be queried after the destination has been determined by the lookahead
 * pass, before [approachMeasure] is invoked. The lookahead size is provided to
 * [isMeasurementApproachInProgress] for convenience in deciding whether the destination size has
 * been reached.
 *
 * [isMeasurementApproachInProgress] indicates whether the position is currently approaching
 * destination defined by the lookahead, hence it's a signal to the system for whether additional
 * approach placements are necessary. [isPlacementApproachInProgress] will be invoked after the
 * destination position has been determined by lookahead pass, and before the placement phase in
 * [approachMeasure].
 *
 * Once both [isMeasurementApproachInProgress] and [isPlacementApproachInProgress] return false, the
 * system may skip approach pass until additional approach passes are necessary as indicated by
 * [isMeasurementApproachInProgress] and [isPlacementApproachInProgress].
 *
 * **IMPORTANT**: It is important to be accurate in [isPlacementApproachInProgress] and
 * [isMeasurementApproachInProgress]. A prolonged indication of incomplete approach will prevent the
 * system from potentially skipping approach pass when possible.
 *
 * @sample androidx.compose.ui.samples.approachLayoutSample
 * @see ApproachLayoutModifierNode
 */
fun Modifier.approachLayout(
    isMeasurementApproachInProgress: (lookaheadSize: IntSize) -> Boolean,
    isPlacementApproachInProgress:
        Placeable.PlacementScope.(lookaheadCoordinates: LayoutCoordinates) -> Boolean =
        defaultPlacementApproachInProgress,
    approachMeasure:
        ApproachMeasureScope.(
            measurable: Measurable,
            constraints: Constraints,
        ) -> MeasureResult,
): Modifier =
    this then
        ApproachLayoutElement(
            isMeasurementApproachInProgress = isMeasurementApproachInProgress,
            isPlacementApproachInProgress = isPlacementApproachInProgress,
            approachMeasure = approachMeasure
        )

private val defaultPlacementApproachInProgress:
    Placeable.PlacementScope.(lookaheadCoordinates: LayoutCoordinates) -> Boolean =
    {
        false
    }

private data class ApproachLayoutElement(
    val approachMeasure:
        ApproachMeasureScope.(
            measurable: Measurable,
            constraints: Constraints,
        ) -> MeasureResult,
    val isMeasurementApproachInProgress: (IntSize) -> Boolean,
    val isPlacementApproachInProgress:
        Placeable.PlacementScope.(lookaheadCoordinates: LayoutCoordinates) -> Boolean =
        defaultPlacementApproachInProgress,
) : ModifierNodeElement<ApproachLayoutModifierNodeImpl>() {
    override fun create() =
        ApproachLayoutModifierNodeImpl(
            approachMeasure,
            isMeasurementApproachInProgress,
            isPlacementApproachInProgress
        )

    override fun update(node: ApproachLayoutModifierNodeImpl) {
        node.measureBlock = approachMeasure
        node.isMeasurementApproachInProgress = isMeasurementApproachInProgress
        node.isPlacementApproachInProgress = isPlacementApproachInProgress
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "approachLayout"
        properties["approachMeasure"] = approachMeasure
        properties["isMeasurementApproachInProgress"] = isMeasurementApproachInProgress
        properties["isPlacementApproachInProgress"] = isPlacementApproachInProgress
    }
}

private class ApproachLayoutModifierNodeImpl(
    var measureBlock:
        ApproachMeasureScope.(
            measurable: Measurable,
            constraints: Constraints,
        ) -> MeasureResult,
    var isMeasurementApproachInProgress: (IntSize) -> Boolean,
    var isPlacementApproachInProgress: Placeable.PlacementScope.(LayoutCoordinates) -> Boolean,
) : ApproachLayoutModifierNode, Modifier.Node() {
    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        return isMeasurementApproachInProgress.invoke(lookaheadSize)
    }

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        return isPlacementApproachInProgress.invoke(this, lookaheadCoordinates)
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        return measureBlock(measurable, constraints)
    }
}

/**
 * [LookaheadScope] provides a receiver scope for all (direct and indirect) child layouts in
 * [LookaheadScope]. This receiver scope allows access to [lookaheadScopeCoordinates] from any
 * child's [Placeable.PlacementScope]. It also allows any child to convert [LayoutCoordinates]
 * (which can be retrieved in [Placeable.PlacementScope]) to [LayoutCoordinates] in lookahead
 * coordinate space using [toLookaheadCoordinates].
 *
 * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
 */
interface LookaheadScope {
    /**
     * Converts a [LayoutCoordinates] into a [LayoutCoordinates] in the Lookahead coordinate space.
     * This can be used for layouts within [LookaheadScope].
     */
    fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates

    /**
     * Returns the [LayoutCoordinates] of the [LookaheadScope]. This is only accessible from
     * [Placeable.PlacementScope] (i.e. during placement time).
     *
     * Note: The returned coordinates is **not** coordinates in the lookahead coordinate space. If
     * the lookahead coordinates of the lookaheadScope is needed, suggest converting the returned
     * coordinates using [toLookaheadCoordinates].
     */
    val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates

    /**
     * Converts [relativeToSource] in [sourceCoordinates]'s lookahead coordinate space into local
     * lookahead coordinates. This is a convenient method for 1) converting both [this] coordinates
     * and [sourceCoordinates] into lookahead space coordinates using [toLookaheadCoordinates],
     * and 2) invoking [LayoutCoordinates.localPositionOf] with the converted coordinates.
     *
     * For layouts where [LayoutCoordinates.introducesMotionFrameOfReference] returns `true` (placed
     * under [Placeable.PlacementScope.withMotionFrameOfReferencePlacement]) you may pass
     * [includeMotionFrameOfReference] as `false` to get their position while excluding the
     * additional Offset.
     */
    fun LayoutCoordinates.localLookaheadPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset = Offset.Zero,
        includeMotionFrameOfReference: Boolean = true,
    ): Offset =
        localLookaheadPositionOf(
            coordinates = this,
            sourceCoordinates = sourceCoordinates,
            relativeToSource = relativeToSource,
            includeMotionFrameOfReference = includeMotionFrameOfReference
        )
}

/** Internal implementation to handle [LookaheadScope.localLookaheadPositionOf]. */
internal fun LookaheadScope.localLookaheadPositionOf(
    coordinates: LayoutCoordinates,
    sourceCoordinates: LayoutCoordinates,
    relativeToSource: Offset,
    includeMotionFrameOfReference: Boolean
): Offset {
    val lookaheadCoords = coordinates.toLookaheadCoordinates()
    val source = sourceCoordinates.toLookaheadCoordinates()

    return if (lookaheadCoords is LookaheadLayoutCoordinates) {
        lookaheadCoords.localPositionOf(
            sourceCoordinates = source,
            relativeToSource = relativeToSource,
            includeMotionFrameOfReference = includeMotionFrameOfReference
        )
    } else if (source is LookaheadLayoutCoordinates) {
        // Relative from source, so we take its negative position
        -source.localPositionOf(
            sourceCoordinates = lookaheadCoords,
            relativeToSource = relativeToSource,
            includeMotionFrameOfReference = includeMotionFrameOfReference
        )
    } else {
        lookaheadCoords.localPositionOf(
            sourceCoordinates = lookaheadCoords,
            relativeToSource = relativeToSource,
            includeMotionFrameOfReference = includeMotionFrameOfReference
        )
    }
}

internal class LookaheadScopeImpl(var scopeCoordinates: (() -> LayoutCoordinates)? = null) :
    LookaheadScope {
    override fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates {
        return this as? LookaheadLayoutCoordinates
            ?: (this as NodeCoordinator).let {
                // If the coordinator has no lookahead delegate. Its
                // lookahead coords is the same as its coords
                it.lookaheadDelegate?.lookaheadLayoutCoordinates ?: it
            }
    }

    override val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates
        get() = scopeCoordinates!!()
}
