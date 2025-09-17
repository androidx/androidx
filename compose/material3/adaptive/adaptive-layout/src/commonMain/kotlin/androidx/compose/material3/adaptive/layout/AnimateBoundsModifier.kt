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

import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

internal fun Modifier.animateBounds(
    animateFraction: () -> Float,
    animationSpec: FiniteAnimationSpec<IntRect>,
    scaleConversion: (IntOffset) -> IntOffset,
    lookaheadScope: LookaheadScope,
    enabled: Boolean,
) =
    this.then(
        AnimateBoundsElement(
            animateFraction,
            animationSpec,
            scaleConversion,
            lookaheadScope,
            enabled,
        )
    )

private class AnimateBoundsElement(
    private val animateFraction: () -> Float,
    private val animationSpec: FiniteAnimationSpec<IntRect>,
    private val scaleConversion: (IntOffset) -> IntOffset,
    private val lookaheadScope: LookaheadScope,
    private val enabled: Boolean,
) : ModifierNodeElement<AnimateBoundsNode>() {
    private val inspectorInfo = debugInspectorInfo {
        name = "animateBounds"
        properties["animateFraction"] = animateFraction
        properties["animationSpec"] = animationSpec
        properties["scaleConversion"] = scaleConversion
        properties["lookaheadScope"] = lookaheadScope
        properties["enabled"] = enabled
    }

    override fun create(): AnimateBoundsNode {
        return AnimateBoundsNode(
            animateFraction,
            animationSpec,
            scaleConversion,
            lookaheadScope,
            enabled,
        )
    }

    override fun update(node: AnimateBoundsNode) {
        node.animateFraction = animateFraction
        node.animationSpec = animationSpec
        node.scaleConversion = scaleConversion
        node.lookaheadScope = lookaheadScope
        node.enabled = enabled
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimateBoundsElement) return false

        if (enabled != other.enabled) return false
        if (animateFraction !== other.animateFraction) return false
        if (animationSpec != other.animationSpec) return false
        if (scaleConversion !== other.scaleConversion) return false
        if (lookaheadScope != other.lookaheadScope) return false
        if (inspectorInfo !== other.inspectorInfo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + animateFraction.hashCode()
        result = 31 * result + animationSpec.hashCode()
        result = 31 * result + scaleConversion.hashCode()
        result = 31 * result + lookaheadScope.hashCode()
        result = 31 * result + inspectorInfo.hashCode()
        return result
    }
}

private class AnimateBoundsNode(
    var animateFraction: () -> Float,
    animationSpec: FiniteAnimationSpec<IntRect>,
    var scaleConversion: (IntOffset) -> IntOffset,
    var lookaheadScope: LookaheadScope,
    var enabled: Boolean,
) : ApproachLayoutModifierNode, Modifier.Node() {
    val boundsTracker = BoundsTracker(animationSpec)

    var animationSpec
        set(value) {
            boundsTracker.animationSpec = value
        }
        get() = boundsTracker.animationSpec

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean =
        enabled && animateFraction() != 1f

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates
    ) = enabled && animateFraction() != 1f

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult =
        // MeasureScope.measure() will only be called during lookahead. Perform a "no-op" measuring
        // here and update target size and offset.
        measurable.measure(constraints).run {
            boundsTracker.updateTargetSize(IntSize(width, height))
            layout(width, height) {
                if (coordinates != null) {
                    boundsTracker.updateTargetOffset(lookaheadOffset(lookaheadScope))
                    if (!enabled) {
                        boundsTracker.updateAndGetCurrentBounds(1f)
                    }
                    place(0, 0)
                }
            }
        }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Workaround (b/435756530): Avoid approaching measure if the animation is not enabled.
        if (!enabled) {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        // Use the current animating fraction to get the approach size and offset of the current
        // animating layout toward the target size and offset updated in measure().
        val currentBounds = boundsTracker.updateAndGetCurrentBounds(animateFraction())
        val animatedConstraints = Constraints.fixed(currentBounds.width, currentBounds.height)
        val placeable = measurable.measure(animatedConstraints)
        return layout(placeable.width, placeable.height) {
            if (coordinates != null) {
                // Workaround(b/413692430): Convert the current offset according to the parent
                // scaling.
                // This workaround the issue that the coordinates of ApproachMeasureScope are
                // transformed, which enforces us to manually scale the offset here to account for
                // the
                // transformation. Note that this is not a real solution - if other transformations
                // other than predictive back scaling are being applied to the scaffold, the
                // animation
                // will still be broken.
                val scaledOffset = scaleConversion(currentBounds.topLeft)
                placeable.place(convertOffsetToCurrentCoordinates(scaledOffset, lookaheadScope))
            }
        }
    }
}

private class BoundsTracker(var animationSpec: FiniteAnimationSpec<IntRect>) {
    private val origin = Bounds()
    private val target = Bounds()
    private var current = InvalidIntRect

    private var boundsAnimation: TargetBasedAnimation<IntRect, AnimationVector4D>? = null

    private fun TargetBasedAnimation<IntRect, AnimationVector4D>.valueAtProgress(
        animateFraction: Float
    ) = getValueFromNanos((this.durationNanos * animateFraction).toLong())

    fun updateTargetSize(newSize: IntSize) {
        if (target.size == newSize) {
            return
        }
        // TODO(conradchen): Handle the interruption better when the target size changes during
        //                   the animation
        origin.size =
            if (current.isValid) {
                current.size
            } else {
                newSize
            }
        target.size = newSize
    }

    fun updateTargetOffset(newOffset: IntOffset) {
        if (target.topLeft == newOffset) {
            return
        }
        // TODO(conradchen): Handle the interruption better when the target position changes during
        //                   the animation
        origin.topLeft =
            if (current.isValid) {
                current.topLeft
            } else {
                newOffset
            }
        target.topLeft = newOffset
    }

    fun updateAndGetCurrentBounds(fraction: Float): IntRect {
        current =
            when (fraction) {
                0f -> origin.rect
                1f -> target.rect
                else -> {
                    updateBoundsAnimationIfNeeded()
                    boundsAnimation?.valueAtProgress(fraction) ?: InvalidIntRect
                }
            }

        return current
    }

    private fun updateBoundsAnimationIfNeeded() {
        if (!origin.isValid || !target.isValid) {
            return
        }
        if (
            boundsAnimation == null ||
                boundsAnimation!!.initialValue != origin.rect ||
                boundsAnimation!!.targetValue != target.rect
        ) {
            boundsAnimation =
                TargetBasedAnimation(animationSpec, IntRectToVector, origin.rect, target.rect)
        }
    }
}
