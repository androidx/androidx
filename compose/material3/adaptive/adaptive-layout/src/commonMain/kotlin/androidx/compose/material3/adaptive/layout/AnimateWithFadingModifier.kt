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

package androidx.compose.material3.adaptive.layout

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
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
import kotlin.math.abs

internal fun Modifier.animateWithFading(
    enabled: Boolean,
    animateFraction: () -> Float,
    lookaheadScope: LookaheadScope,
    fadingAnimationSpec: FiniteAnimationSpec<Float> = tween()
) =
    this.then(
        AnimateWithFadingElement(animateFraction, lookaheadScope, enabled, fadingAnimationSpec)
    )

private data class AnimateWithFadingElement(
    val animateFraction: () -> Float,
    val lookaheadScope: LookaheadScope,
    val enabled: Boolean,
    val fadingAnimationSpec: FiniteAnimationSpec<Float>
) : ModifierNodeElement<AnimateWithFadingNode>() {
    private val inspectorInfo = debugInspectorInfo {
        name = "animateWithFading"
        properties["animateFraction"] = animateFraction
        properties["lookaheadScope"] = lookaheadScope
        properties["enabled"] = enabled
        properties["fadingAnimationSpec"] = fadingAnimationSpec
    }

    override fun create(): AnimateWithFadingNode {
        return AnimateWithFadingNode(animateFraction, lookaheadScope, enabled, fadingAnimationSpec)
    }

    override fun update(node: AnimateWithFadingNode) {
        node.animateFraction = animateFraction
        node.lookaheadScope = lookaheadScope
        node.enabled = enabled
        node.fadingAnimationSpec = fadingAnimationSpec
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }
}

private class AnimateWithFadingNode(
    var animateFraction: () -> Float,
    var lookaheadScope: LookaheadScope,
    var enabled: Boolean,
    fadingAnimationSpec: FiniteAnimationSpec<Float>
) : ApproachLayoutModifierNode, Modifier.Node() {
    private var originalOffset: IntOffset = InvalidOffset
    private var targetOffset: IntOffset = InvalidOffset

    var fadingAnimationSpec = fadingAnimationSpec
        set(value) {
            animation = value.createAnimation()
            field = value
        }

    private var animation = fadingAnimationSpec.createAnimation()

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean = false

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        updateTargetOffset(lookaheadOffset(lookaheadScope))
        return enabled &&
            originalOffset != InvalidOffset &&
            originalOffset != targetOffset &&
            animateFraction() != 1f
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val currentAnimatedValue = animation.getValue(animateFraction())
        return measurable.measure(constraints).run {
            layout(width, height) {
                coordinates?.let {
                    placeWithLayer(
                        if (currentAnimatedValue > 0f) {
                            IntOffset.Zero
                        } else {
                            originalOffset - targetOffset
                        },
                        layerBlock = { alpha = abs(currentAnimatedValue) }
                    )
                }
            }
        }
    }

    fun updateTargetOffset(newOffset: IntOffset) {
        if (targetOffset == newOffset) {
            return
        }
        originalOffset = targetOffset
        targetOffset = newOffset
    }

    private fun FiniteAnimationSpec<Float>.createAnimation() =
        TargetBasedAnimation(this, Float.VectorConverter, -1f, 1f)
}

private fun <T, V : AnimationVector> TargetBasedAnimation<T, V>.getValue(progress: Float) =
    getValueFromNanos((durationNanos * progress).toLong())
