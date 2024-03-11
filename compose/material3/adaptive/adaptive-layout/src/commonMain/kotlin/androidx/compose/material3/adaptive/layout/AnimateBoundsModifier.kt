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
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.animateBounds(
    animateFraction: Float,
    sizeAnimationSpec: FiniteAnimationSpec<IntSize>,
    positionAnimationSpec: FiniteAnimationSpec<IntOffset>,
    lookaheadScope: LookaheadScope,
    enabled: Boolean
) = composed {
    val sizeTracker = remember { SizeTracker(sizeAnimationSpec) }.apply {
        this.animationSpec = sizeAnimationSpec
    }
    val positionTracker = remember { PositionTracker(positionAnimationSpec) }.apply {
        this.animationSpec = positionAnimationSpec
    }
    if (!enabled) {
        return@composed this
    }
    this.approachLayout(
        isMeasurementApproachComplete = {
            animateFraction == 1f
        },
        isPlacementApproachComplete = {
            animateFraction == 1f
        },
    ) { measurable, _ ->
        // When layout changes, the lookahead pass will calculate a new final size for the
        // child modifier. This lookahead size can be used to animate the size
        // change, such that the animation starts from the current size and gradually
        // change towards `lookaheadSize`.
        sizeTracker.updateTargetSize(lookaheadSize)
        val (width, height) = sizeTracker.updateAndGetCurrentSize(animateFraction)
        // Creates a fixed set of constraints using the animated size
        val animatedConstraints = Constraints.fixed(width, height)
        // Measure child/children with animated constraints.
        val placeable = measurable.measure(animatedConstraints)
        layout(placeable.width, placeable.height) {
            coordinates?.let {
                positionTracker.updateTargetOffset(
                    with(lookaheadScope) {
                        lookaheadScopeCoordinates.localLookaheadPositionOf(it).toIntOffset()
                    }
                )
                placeable.place(
                    with(lookaheadScope) {
                        positionTracker.updateAndGetCurrentOffset(animateFraction) -
                            lookaheadScopeCoordinates.localPositionOf(it, Offset.Zero).toIntOffset()
                    }
                )
            }
        }
    }
}

private class SizeTracker(var animationSpec: FiniteAnimationSpec<IntSize>) {
    private var originalSize: IntSize = IntSize.Zero
    private var targetSize: IntSize = IntSize.Zero
    private var currentSize = IntSize.Zero
    private lateinit var animation: TargetBasedAnimation<IntSize, AnimationVector2D>

    fun updateTargetSize(newSize: IntSize) {
        if (targetSize == newSize) {
            return
        }
        // TODO(conradchen): Handle the interruption better when the target size changes during
        //                   the animation
        originalSize = currentSize
        targetSize = newSize
        animation = TargetBasedAnimation(
            animationSpec,
            IntSize.VectorConverter,
            originalSize,
            targetSize
        )
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
        originalOffset = if (currentOffset != null) {
            currentOffset
        } else {
            newOffset
        }
        targetOffset = newOffset
        animation = TargetBasedAnimation(
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

private fun Offset.toIntOffset() = IntOffset(x.roundToInt(), y.roundToInt())
