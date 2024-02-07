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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.layout

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

/**
 * [LookaheadScope] creates a scope in which all layouts will first determine their destination
 * layout through a lookahead pass, followed by an _approach_ pass to run the measurement
 * and placement approach defined in [approachLayout] or [ApproachLayoutModifierNode], in order to
 * gradually reach the destination.
 *
 * Note: [LookaheadScope] does not introduce a new [Layout] to the [content] passed in.
 * All the [Layout]s in the [content] will have the same parent as they would without
 * [LookaheadScope].
 *
 * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
 * @see ApproachLayoutModifierNode
 * @see approachLayout
 *
 * @param content The child composable to be laid out.
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
                scope.scopeCoordinates = {
                    parent!!.innerCoordinator.coordinates
                }
            }
        },
        content = {
            scope.content()
        }
    )
}

@ExperimentalComposeUiApi
@Deprecated(
    "IntermediateMeasureScope has been renamed to ApproachMeasureScope",
    replaceWith = ReplaceWith("ApproachMeasureScope")
)
interface IntermediateMeasureScope : ApproachMeasureScope, CoroutineScope, LookaheadScope

@ExperimentalComposeUiApi
@Deprecated(
    "intermediateLayout has been replaced with approachLayout, and requires an" +
        "additional parameter to signal if the approach is complete.",
    replaceWith = ReplaceWith(
        "approachLayout(isMeasurementApproachComplete = ," +
            "approachMeasure = measure)"
    )
)
fun Modifier.intermediateLayout(
    @Suppress("DEPRECATION")
    measure: IntermediateMeasureScope.(
        measurable: Measurable,
        constraints: Constraints,
    ) -> MeasureResult,
) = this then IntermediateLayoutElement(measure)

/**
 * Creates an approach layout intended to help gradually approach the destination layout calculated
 * in the lookahead pass. This can be particularly helpful when the destination layout is
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
 * Once both [isMeasurementApproachComplete] and [isPlacementApproachComplete] return true, the
 * system may skip approach pass until additional approach passes are necessary as indicated by
 * [isMeasurementApproachComplete] and [isPlacementApproachComplete].
 *
 * **IMPORTANT**:
 * It is important to be accurate in [isPlacementApproachComplete] and
 * [isMeasurementApproachComplete]. A prolonged indication of incomplete approach will prevent the
 * system from potentially skipping approach pass when possible.
 *
 * @see ApproachLayoutModifierNode
 * @sample androidx.compose.ui.samples.approachLayoutSample
 */
@ExperimentalComposeUiApi
fun Modifier.approachLayout(
    isMeasurementApproachComplete: (lookaheadSize: IntSize) -> Boolean,
    isPlacementApproachComplete: Placeable.PlacementScope.(
        lookaheadCoordinates: LayoutCoordinates
    ) -> Boolean = defaultPlacementApproachComplete,
    approachMeasure: ApproachMeasureScope.(
        measurable: Measurable,
        constraints: Constraints,
    ) -> MeasureResult,
): Modifier = this then ApproachLayoutElement(
    isMeasurementApproachComplete = isMeasurementApproachComplete,
    isPlacementApproachComplete = isPlacementApproachComplete,
    approachMeasure = approachMeasure
)

private val defaultPlacementApproachComplete: Placeable.PlacementScope.(
    lookaheadCoordinates: LayoutCoordinates
) -> Boolean = { true }

@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
private data class IntermediateLayoutElement(
    val measure: IntermediateMeasureScope.(
        measurable: Measurable,
        constraints: Constraints,
    ) -> MeasureResult,
) : ModifierNodeElement<IntermediateLayoutModifierNodeImpl>() {
    override fun create() =
        IntermediateLayoutModifierNodeImpl(
            measure,
        )

    override fun update(node: IntermediateLayoutModifierNodeImpl) {
        node.measureBlock = measure
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "intermediateLayout"
        properties["measure"] = measure
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
private class IntermediateLayoutModifierNodeImpl(
    var measureBlock: IntermediateMeasureScope.(
        measurable: Measurable,
        constraints: Constraints,
    ) -> MeasureResult,
) : ApproachLayoutModifierNode, Modifier.Node() {
    private var intermediateMeasureScope: IntermediateMeasureScopeImpl? = null

    private inner class IntermediateMeasureScopeImpl(
        val approachScope: ApproachMeasureScopeImpl
    ) : IntermediateMeasureScope, LookaheadScope by approachScope,
        ApproachMeasureScope by approachScope, CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = this@IntermediateLayoutModifierNodeImpl.coroutineScope.coroutineContext
    }

    override fun isMeasurementApproachComplete(lookaheadSize: IntSize): Boolean {
        // Important: Returning false here is strongly discouraged as it'll prevent layout
        // performance optimization. This ModifierNodeImpl is only intended to help devs transition
        // over to the new ApproachLayoutNodeModifier, and it'll be removed after a couple of
        // releases.
        return false
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val scope = intermediateMeasureScope
        val newScope = if (scope?.approachScope != this) {
            IntermediateMeasureScopeImpl(this as ApproachMeasureScopeImpl)
        } else {
            scope
        }
        intermediateMeasureScope = newScope
        return with(newScope) {
            measureBlock(measurable, constraints)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private data class ApproachLayoutElement(
    val approachMeasure: ApproachMeasureScope.(
        measurable: Measurable,
        constraints: Constraints,
    ) -> MeasureResult,
    val isMeasurementApproachComplete: (IntSize) -> Boolean,
    val isPlacementApproachComplete: Placeable.PlacementScope.(
        lookaheadCoordinates: LayoutCoordinates
    ) -> Boolean = defaultPlacementApproachComplete,
) : ModifierNodeElement<ApproachLayoutModifierNodeImpl>() {
    override fun create() =
        ApproachLayoutModifierNodeImpl(
            approachMeasure,
            isMeasurementApproachComplete,
            isPlacementApproachComplete
        )

    override fun update(node: ApproachLayoutModifierNodeImpl) {
        node.measureBlock = approachMeasure
        node.isMeasurementApproachComplete = isMeasurementApproachComplete
        node.isPlacementApproachComplete = isPlacementApproachComplete
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "approachLayout"
        properties["approachMeasure"] = approachMeasure
        properties["isMeasurementApproachComplete"] = isMeasurementApproachComplete
        properties["isPlacementApproachComplete"] = isPlacementApproachComplete
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class ApproachLayoutModifierNodeImpl(
    var measureBlock: ApproachMeasureScope.(
        measurable: Measurable,
        constraints: Constraints,
    ) -> MeasureResult,
    var isMeasurementApproachComplete: (IntSize) -> Boolean,
    var isPlacementApproachComplete:
    Placeable.PlacementScope.(LayoutCoordinates) -> Boolean,
) : ApproachLayoutModifierNode, Modifier.Node() {
    override fun isMeasurementApproachComplete(lookaheadSize: IntSize): Boolean {
        return isMeasurementApproachComplete.invoke(lookaheadSize)
    }

    override fun Placeable.PlacementScope.isPlacementApproachComplete(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        return isPlacementApproachComplete.invoke(this, lookaheadCoordinates)
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
 * [LookaheadScope]. This receiver scope allows access to [lookaheadScopeCoordinates] from
 * any child's [Placeable.PlacementScope]. It also allows any child to convert
 * [LayoutCoordinates] (which can be retrieved in [Placeable.PlacementScope]) to
 * [LayoutCoordinates] in lookahead coordinate space using [toLookaheadCoordinates].
 *
 * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
 */
interface LookaheadScope {
    /**
     * Converts a [LayoutCoordinates] into a [LayoutCoordinates] in the Lookahead coordinates space.
     * This is only applicable to child layouts within [LookaheadScope].
     */
    @ExperimentalComposeUiApi
    fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates

    /**
     * Returns the [LayoutCoordinates] of the [LookaheadScope]. This is
     * only accessible from [Placeable.PlacementScope] (i.e. during placement time).
     */
    @ExperimentalComposeUiApi
    val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates

    /**
     * Calculates the localPosition in the Lookahead coordinate space. This is a convenient
     * method for 1) converting the given [LayoutCoordinates] to lookahead coordinates using
     * [toLookaheadCoordinates], and 2) invoking [LayoutCoordinates.localPositionOf] with the
     * converted coordinates.
     */
    @ExperimentalComposeUiApi
    fun LayoutCoordinates.localLookaheadPositionOf(coordinates: LayoutCoordinates) =
        this.toLookaheadCoordinates().localPositionOf(
            coordinates.toLookaheadCoordinates(),
            Offset.Zero
        )
}

@OptIn(ExperimentalComposeUiApi::class)
internal class LookaheadScopeImpl(
    var scopeCoordinates: (() -> LayoutCoordinates)? = null
) : LookaheadScope {
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
