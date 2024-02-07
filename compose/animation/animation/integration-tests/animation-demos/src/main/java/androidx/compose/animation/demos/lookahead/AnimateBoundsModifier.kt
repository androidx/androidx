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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import kotlinx.coroutines.CoroutineScope

context(LookaheadScope)
@OptIn(ExperimentalAnimatableApi::class)
fun Modifier.animateBounds(
    modifier: Modifier = Modifier,
    sizeAnimationSpec: FiniteAnimationSpec<IntSize> = spring(
        Spring.DampingRatioNoBouncy,
        Spring.StiffnessMediumLow
    ),
    positionAnimationSpec: FiniteAnimationSpec<IntOffset> = spring(
        Spring.DampingRatioNoBouncy,
        Spring.StiffnessMediumLow
    ),
    debug: Boolean = false,
) = composed {

    val outerOffsetAnimation = remember { DeferredTargetAnimation(IntOffset.VectorConverter) }
    val outerSizeAnimation = remember { DeferredTargetAnimation(IntSize.VectorConverter) }

    val offsetAnimation = remember { DeferredTargetAnimation(IntOffset.VectorConverter) }
    val sizeAnimation = remember { DeferredTargetAnimation(IntSize.VectorConverter) }

    val coroutineScope = rememberCoroutineScope()

    // The measure logic in `approachLayout` is skipped in the lookahead pass, as
    // approachLayout is expected to produce intermediate stages of a layout transform.
    // When the measure block is invoked after lookahead pass, the lookahead size of the
    // child will be accessible as a parameter to the measure block.
    this
        .drawWithContent {
            drawContent()
            if (debug) {
//                val offset = outerOffsetAnimation.pendingTarget!! - outerOffsetAnimation.value!!
//                translate(
//                    offset.x.toFloat(), offset.y.toFloat()
//                ) {
//                    drawRect(Color.Black.copy(alpha = 0.5f), style = Stroke(10f))
//                }
            }
        }
        .approachLayout(
            isMeasurementApproachComplete = {
                outerSizeAnimation.updateTarget(it, coroutineScope, sizeAnimationSpec)
                outerSizeAnimation.isIdle
            },
            isPlacementApproachComplete = {
                val target = lookaheadScopeCoordinates.localLookaheadPositionOf(it)
                outerOffsetAnimation.updateTarget(
                    target.round(),
                    coroutineScope,
                    positionAnimationSpec
                )
                outerOffsetAnimation.isIdle
            }
        ) { measurable, constraints ->
            val (w, h) = outerSizeAnimation.updateTarget(
                lookaheadSize,
                coroutineScope,
                sizeAnimationSpec,
            )
            measurable
                .measure(constraints)
                .run {
                    layout(w, h) {
                        with(coroutineScope) {
                            val (x, y) = outerOffsetAnimation.updateTargetBasedOnCoordinates(
                                positionAnimationSpec
                            )
                            place(x, y)
                        }
                    }
                }
        }
        .then(modifier)
        .drawWithContent {
            drawContent()
            if (debug) {
//                val offset = offsetAnimation.pendingTarget!! - offsetAnimation.value!!
//                translate(
//                    offset.x.toFloat(), offset.y.toFloat()
//                ) {
//                    drawRect(Color.Green.copy(alpha = 0.5f), style = Stroke(10f))
//                }
            }
        }
        .approachLayout(
            isMeasurementApproachComplete = {
                sizeAnimation.updateTarget(it, coroutineScope, sizeAnimationSpec)
                sizeAnimation.isIdle
            },
            isPlacementApproachComplete = {
                val target = lookaheadScopeCoordinates.localLookaheadPositionOf(it)
                offsetAnimation.updateTarget(
                    target.round(),
                    coroutineScope,
                    positionAnimationSpec
                )
                offsetAnimation.isIdle
            }
        ) { measurable, _ ->
            // When layout changes, the lookahead pass will calculate a new final size for the
            // child modifier. This lookahead size can be used to animate the size
            // change, such that the animation starts from the current size and gradually
            // change towards `lookaheadSize`.
            val (width, height) = sizeAnimation.updateTarget(
                lookaheadSize,
                coroutineScope,
                sizeAnimationSpec,
            )
            // Creates a fixed set of constraints using the animated size
            val animatedConstraints = Constraints.fixed(width, height)
            // Measure child/children with animated constraints.
            val placeable = measurable.measure(animatedConstraints)
            layout(placeable.width, placeable.height) {
                val (x, y) = with(coroutineScope) {
                    offsetAnimation.updateTargetBasedOnCoordinates(
                        positionAnimationSpec,
                    )
                }
                placeable.place(x, y)
            }
        }
}

context(LookaheadScope, Placeable.PlacementScope, CoroutineScope)
@OptIn(ExperimentalAnimatableApi::class)
internal fun DeferredTargetAnimation<IntOffset, AnimationVector2D>.updateTargetBasedOnCoordinates(
    animationSpec: FiniteAnimationSpec<IntOffset>,
): IntOffset {
    coordinates?.let { coordinates ->
        with(this@PlacementScope) {
            val targetOffset = lookaheadScopeCoordinates.localLookaheadPositionOf(coordinates)
            val animOffset = updateTarget(
                targetOffset.round(),
                this@CoroutineScope,
                animationSpec,
            )
            val current = lookaheadScopeCoordinates.localPositionOf(
                coordinates,
                Offset.Zero
            ).round()
            return (animOffset - current)
        }
    }

    return IntOffset.Zero
}
