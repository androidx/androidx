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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope

@Composable
fun SceneHost(modifier: Modifier = Modifier, content: @Composable SceneScope.() -> Unit) {
    Box(modifier) {
        LookaheadScope {
            val sceneScope = remember { SceneScope(this) }
            sceneScope.content()
        }
    }
}

private const val debugSharedElement = true

class SceneScope internal constructor(lookaheadScope: LookaheadScope) :
    LookaheadScope by lookaheadScope {
    @OptIn(ExperimentalAnimatableApi::class)
    fun Modifier.sharedElement(): Modifier = composed {
        val offsetAnimation: DeferredTargetAnimation<IntOffset, AnimationVector2D> = remember {
            DeferredTargetAnimation(IntOffset.VectorConverter)
        }
        val sizeAnimation: DeferredTargetAnimation<IntSize, AnimationVector2D> = remember {
            DeferredTargetAnimation(IntSize.VectorConverter)
        }

        var placementOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
        val coroutineScope = rememberCoroutineScope()

        this.drawBehind {
                if (debugSharedElement) {
                    drawRect(
                        color = Color.Black,
                        style = Stroke(2f),
                        topLeft = (offsetAnimation.pendingTarget!! - placementOffset).toOffset(),
                        size = sizeAnimation.pendingTarget!!.toSize()
                    )
                }
            }
            .approachLayout(
                isMeasurementApproachInProgress = {
                    sizeAnimation.updateTarget(it, coroutineScope)
                    !sizeAnimation.isIdle
                },
                isPlacementApproachInProgress = {
                    val target = lookaheadScopeCoordinates.localLookaheadPositionOf(it)
                    offsetAnimation.updateTarget(target.round(), coroutineScope, spring())
                    !offsetAnimation.isIdle
                }
            ) { measurable, _ ->
                with(coroutineScope) {
                    val (width, height) =
                        sizeAnimation.updateTarget(
                            lookaheadSize,
                            coroutineScope,
                            spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    val animatedConstraints = Constraints.fixed(width, height)
                    val placeable = measurable.measure(animatedConstraints)
                    layout(placeable.width, placeable.height) {
                        val (x, y) =
                            offsetAnimation.updateTargetBasedOnCoordinates(
                                spring(stiffness = Spring.StiffnessMediumLow),
                            )
                        coordinates?.let {
                            placementOffset =
                                lookaheadScopeCoordinates.localPositionOf(it, Offset.Zero).round()
                        }
                        placeable.place(x, y)
                    }
                }
            }
    }
}

@OptIn(ExperimentalAnimatableApi::class)
fun Modifier.animateSizeAndSkipToFinalLayout() = composed {
    val sizeAnimation = remember { DeferredTargetAnimation(IntSize.VectorConverter) }
    var targetSize: IntSize? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    this.drawBehind {
            if (debugSharedElement) {
                drawRect(
                    color = Color.Black,
                    style = Stroke(2f),
                    topLeft = Offset.Zero,
                    size = targetSize!!.toSize()
                )
            }
        }
        .approachLayout(
            isMeasurementApproachInProgress = {
                sizeAnimation.updateTarget(it, scope)
                !sizeAnimation.isIdle
            }
        ) { measurable, constraints ->
            targetSize = lookaheadSize
            val (width, height) = sizeAnimation.updateTarget(lookaheadSize, scope)
            val placeable =
                measurable.measure(Constraints.fixed(lookaheadSize.width, lookaheadSize.height))
            // Make sure the content is aligned to topStart
            val wrapperWidth = width.coerceIn(constraints.minWidth, constraints.maxWidth)
            val wrapperHeight = height.coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                placeable.place(-(wrapperWidth - width) / 2, -(wrapperHeight - height) / 2)
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
            val animOffset =
                updateTarget(
                    targetOffset.round(),
                    this@CoroutineScope,
                    animationSpec,
                )
            val current =
                lookaheadScopeCoordinates.localPositionOf(coordinates, Offset.Zero).round()
            return (animOffset - current)
        }
    }

    return IntOffset.Zero
}
