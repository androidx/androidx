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

package androidx.compose.material3.internal

/** Material-specific anchor layout logic which considers lookahead. */
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material3.ComposeMaterial3Flags.isAnchoredDraggableComponentsAnchorRecoveryEnabled
import androidx.compose.material3.ComposeMaterial3Flags.isAnchoredDraggableComponentsInvalidationFixEnabled
import androidx.compose.material3.ComposeMaterial3Flags.isAnchoredDraggableComponentsStrictOffsetCheckEnabled
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * This Modifier allows configuring an [AnchoredDraggableState]'s anchors based on this layout
 * node's size and offsetting it. It considers lookahead and reports the appropriate size and
 * measurement for the appropriate phase.
 *
 * @param state The state the anchors should be attached to
 * @param orientation The orientation the component should be offset in
 * @param anchors Lambda to calculate the anchors based on this layout's size and the incoming
 *   constraints. These can be useful to avoid subcomposition.
 */
@Stable
internal fun <T> Modifier.draggableAnchors(
    state: AnchoredDraggableState<T>,
    orientation: Orientation,
    anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
) = this then DraggableAnchorsElement(state, anchors, orientation)

private class DraggableAnchorsElement<T>(
    private val state: AnchoredDraggableState<T>,
    private val anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
    private val orientation: Orientation,
) : ModifierNodeElement<DraggableAnchorsNode<T>>() {

    override fun create() = DraggableAnchorsNode(state, anchors, orientation)

    override fun update(node: DraggableAnchorsNode<T>) {
        node.update(state, anchors, orientation)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is DraggableAnchorsElement<*>) return false

        if (state != other.state) return false
        if (anchors !== other.anchors) return false
        if (orientation != other.orientation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + anchors.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        debugInspectorInfo {
            properties["state"] = state
            properties["anchors"] = anchors
            properties["orientation"] = orientation
        }
    }
}

private class DraggableAnchorsNode<T>(
    var state: AnchoredDraggableState<T>,
    var anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
    var orientation: Orientation,
) : Modifier.Node(), LayoutModifierNode {
    private var didInitializeAnchors = false

    override fun onDetach() {
        didInitializeAnchors = false
    }

    private val isReverseDirection: Boolean
        get() =
            requireLayoutDirection() == LayoutDirection.Rtl && orientation == Orientation.Horizontal

    @OptIn(ExperimentalMaterial3Api::class)
    fun update(
        state: AnchoredDraggableState<T>,
        anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
        orientation: Orientation,
    ) {
        val shouldInvalidateMeasure =
            isAnchoredDraggableComponentsInvalidationFixEnabled && this.state != state
        this.state = state
        this.anchors = anchors
        this.orientation = orientation
        if (shouldInvalidateMeasure) {
            didInitializeAnchors = false
            invalidateMeasurement()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        // If we are in a lookahead pass, we only want to update the anchors here and not in
        // post-lookahead. If there is no lookahead happening (!isLookingAhead && !didLookahead),
        // update the anchors in the main pass.
        if (!isLookingAhead || !didInitializeAnchors) {
            val size = IntSize(placeable.width, placeable.height)
            val (newAnchors, suggestedTarget) = anchors(size, constraints)

            if (isAnchoredDraggableComponentsAnchorRecoveryEnabled) {
                // Edge case where AnchoredDraggable target value is removed from set of available
                // anchors before placement.
                val validatedTarget =
                    if (newAnchors.hasPositionFor(suggestedTarget)) {
                        suggestedTarget
                    } else {
                        newAnchors.anchorAt(0) ?: suggestedTarget
                    }
                state.updateAnchors(newAnchors, validatedTarget)
            } else {
                // Previous behavior which places provided target naively.
                state.updateAnchors(newAnchors, suggestedTarget)
            }
            didInitializeAnchors = true
        }

        didInitializeAnchors = isLookingAhead || didInitializeAnchors
        return layout(placeable.width, placeable.height) {
            // In a lookahead pass, we use the position of the current target as this is where any
            // ongoing animations would move. If the component is in a settled state, lookahead
            // and post-lookahead will converge.
            val offset =
                if (isLookingAhead) {
                    state.anchors.positionOf(state.targetValue)
                } else {
                    state.offset
                }

            // By default, we want to be strict about cases with uninitialized offsets and throw an
            // exception.
            if (isAnchoredDraggableComponentsStrictOffsetCheckEnabled) {
                checkOffsetIsValid(offset, isLookingAhead)
            } else {
                // For debugging purposes, we allow the offset to be uninitialized by disabling the
                // flag. In that case, we don't place anything.
                if (offset.isNaN()) return@layout
            }

            val rtlModifier = if (isReverseDirection) -1f else 1f
            val xOffset = if (orientation == Orientation.Horizontal) offset * rtlModifier else 0f
            val yOffset = if (orientation == Orientation.Vertical) offset else 0f
            // Tagging as motion frame of reference placement, meaning the placement
            // contains scrolling. This allows the consumer of this placement offset to
            // differentiate this offset vs. offsets from structural changes. Generally
            // speaking, this signals a preference to directly apply changes rather than
            // animating, to avoid a chasing effect to scrolling.
            withMotionFrameOfReferencePlacement {
                placeable.place(xOffset.roundToInt(), yOffset.roundToInt())
            }
        }
    }

    /**
     * Require the [AnchoredDraggableState.offset] to be a valid float, or throw an exception with
     * more information otherwise.
     */
    private fun checkOffsetIsValid(offset: Float, isLookingAhead: Boolean) {
        if (offset.isNaN()) {
            throw AnchoredDraggableUninitializedException(
                isLookingAhead = isLookingAhead,
                didLookahead = didInitializeAnchors,
                anchors = state.anchors,
                targetValue = state.targetValue,
            )
        }
    }
}

internal class AnchoredDraggableUninitializedException(
    isLookingAhead: Boolean,
    didLookahead: Boolean,
    anchors: DraggableAnchors<*>,
    targetValue: Any?,
) : Throwable() {
    override val message: String =
        "AnchoredDraggableState was not initialized correctly. " +
            "isLookingAhead=$isLookingAhead,didLookahead=$didLookahead,anchors=$anchors,targetValue=$targetValue"
}

internal const val ConfirmValueChangeDeprecated =
    "confirmValueChange is deprecated without replacement. Rather than relying on a callback to " +
        "veto state changes, the anchor set should not include disallowed anchors. See " +
        "androidx.compose.foundation.samples.AnchoredDraggableDynamicAnchorsSample for an " +
        "example of using dynamic anchors over confirmValueChange."
