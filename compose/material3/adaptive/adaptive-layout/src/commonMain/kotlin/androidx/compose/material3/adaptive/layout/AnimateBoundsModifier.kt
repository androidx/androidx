/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal fun Modifier.animateBounds(
    animateFraction: () -> Float,
    sizeAnimationSpec: FiniteAnimationSpec<IntSize>,
    positionAnimationSpec: FiniteAnimationSpec<IntOffset>,
    lookaheadScope: LookaheadScope,
    enabled: Boolean
) =
    this.then(
        AnimateBoundsElement(
            animateFraction,
            sizeAnimationSpec,
            positionAnimationSpec,
            lookaheadScope,
            enabled,
        )
    )

private data class AnimateBoundsElement(
    private val animateFraction: () -> Float,
    private val sizeAnimationSpec: FiniteAnimationSpec<IntSize>,
    private val positionAnimationSpec: FiniteAnimationSpec<IntOffset>,
    private val lookaheadScope: LookaheadScope,
    private val enabled: Boolean
) : ModifierNodeElement<AnimateBoundsNode>() {
    private val inspectorInfo = debugInspectorInfo {
        name = "animateBounds"
        properties["animateFraction"] = animateFraction
        properties["sizeAnimationSpec"] = sizeAnimationSpec
        properties["positionAnimationSpec"] = positionAnimationSpec
        properties["lookaheadScope"] = lookaheadScope
        properties["enabled"] = enabled
    }

    override fun create(): AnimateBoundsNode {
        return AnimateBoundsNode(
            animateFraction,
            sizeAnimationSpec,
            positionAnimationSpec,
            lookaheadScope,
            enabled,
        )
    }

    override fun update(node: AnimateBoundsNode) {
        node.animateFraction = animateFraction
        node.sizeAnimationSpec = sizeAnimationSpec
        node.positionAnimationSpec = positionAnimationSpec
        node.lookaheadScope = lookaheadScope
        node.enabled = enabled
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }
}

private class AnimateBoundsNode(
    var animateFraction: () -> Float,
    sizeAnimationSpec: FiniteAnimationSpec<IntSize>,
    positionAnimationSpec: FiniteAnimationSpec<IntOffset>,
    var lookaheadScope: LookaheadScope,
    var enabled: Boolean
) : ApproachLayoutModifierNode, Modifier.Node() {
    val sizeTracker = SizeTracker(sizeAnimationSpec)
    val positionTracker = PositionTracker(positionAnimationSpec)

    var sizeAnimationSpec
        set(value) {
            sizeTracker.animationSpec = value
        }
        get() = sizeTracker.animationSpec

    var positionAnimationSpec
        set(value) {
            positionTracker.animationSpec = value
        }
        get() = positionTracker.animationSpec

    // If animateBounds is not enabled, we need to do approach measure at least once so the size
    // tracker and the position tracker will be kept updated.
    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        sizeTracker.updateTargetSize(lookaheadSize)
        return enabled && animateFraction() != 1f
    }

    // If animateBounds is not enabled, we need to do approach measure at least once so the size
    // tracker and the position tracker will be kept updated.
    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        positionTracker.updateTargetOffset(lookaheadOffset(lookaheadScope))
        return enabled && animateFraction() != 1f
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // Use the current animating fraction to get the approach size and offset of the current
        // animating layut toward the target size and offset updated in measure().
        val (width, height) = sizeTracker.updateAndGetCurrentSize(animateFraction())
        val animatedConstraints = Constraints.fixed(width, height)
        val placeable = measurable.measure(animatedConstraints)
        return layout(placeable.width, placeable.height) {
            coordinates?.let {
                placeable.place(
                    convertOffsetToLookaheadCoordinates(
                        positionTracker.updateAndGetCurrentOffset(animateFraction()),
                        lookaheadScope
                    )
                )
            }
        }
    }
}

private class SizeTracker(var animationSpec: FiniteAnimationSpec<IntSize>) {
    private var originalSize: IntSize = InvalidIntSize
    private var targetSize: IntSize = InvalidIntSize
    private var currentSize = InvalidIntSize
    private lateinit var animation: TargetBasedAnimation<IntSize, AnimationVector2D>

    fun updateTargetSize(newSize: IntSize) {
        if (targetSize == newSize) {
            return
        }
        // TODO(conradchen): Handle the interruption better when the target size changes during
        //                   the animation
        originalSize =
            if (currentSize != InvalidIntSize) {
                currentSize
            } else {
                newSize
            }
        targetSize = newSize
        animation =
            TargetBasedAnimation(animationSpec, IntSize.VectorConverter, originalSize, targetSize)
    }

    fun updateAndGetCurrentSize(fraction: Float): IntSize {
        currentSize = animation.getValueFromNanos((animation.durationNanos * fraction).toLong())
        return currentSize
    }
}

private class PositionTracker(var animationSpec: FiniteAnimationSpec<IntOffset>) {
    private var originalOffset: IntOffset? = null
    private var targetOffset: IntOffset? = null
    private var currentOffset: IntOffset? = null
    private lateinit var animation: TargetBasedAnimation<IntOffset, AnimationVector2D>

    fun updateTargetOffset(newOffset: IntOffset) {
        if (targetOffset == newOffset) {
            return
        }
        // TODO(conradchen): Handle the interruption better when the target position changes during
        //                   the animation
        originalOffset =
            if (currentOffset != null) {
                currentOffset
            } else {
                newOffset
            }
        targetOffset = newOffset
        animation =
            TargetBasedAnimation(
                animationSpec,
                IntOffset.VectorConverter,
                originalOffset!!,
                targetOffset!!
            )
    }

    fun updateAndGetCurrentOffset(fraction: Float): IntOffset {
        currentOffset = animation.getValueFromNanos((animation.durationNanos * fraction).toLong())
        return currentOffset!!
    }
}
