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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.intermediateLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

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

class SceneScope internal constructor(
    lookaheadScope: LookaheadScope
) : LookaheadScope by lookaheadScope {
    fun Modifier.sharedElement(): Modifier = composed {
        val offsetAnimation: DeferredAnimation<IntOffset, AnimationVector2D> =
            remember {
                DeferredAnimation(IntOffset.VectorConverter)
            }
        val sizeAnimation: DeferredAnimation<IntSize, AnimationVector2D> =
            remember { DeferredAnimation(IntSize.VectorConverter) }

        var placementOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }

        this
            .drawBehind {
                if (debugSharedElement) {
                    drawRect(
                        color = Color.Black,
                        style = Stroke(2f),
                        topLeft = (offsetAnimation.target!! - placementOffset).toOffset(),
                        size = sizeAnimation.target!!.toSize()
                    )
                }
            }
            .intermediateLayout { measurable, _ ->
                val (width, height) = sizeAnimation.updateTarget(
                    lookaheadSize, spring(stiffness = Spring.StiffnessMediumLow)
                )
                val animatedConstraints = Constraints.fixed(width, height)
                val placeable = measurable.measure(animatedConstraints)
                layout(placeable.width, placeable.height) {
                    val (x, y) = offsetAnimation.updateTargetBasedOnCoordinates(
                        spring(stiffness = Spring.StiffnessMediumLow),
                    )
                    coordinates?.let {
                        placementOffset = lookaheadScopeCoordinates.localPositionOf(
                            it, Offset.Zero
                        ).round()
                    }
                    placeable.place(x, y)
                }
            }
    }
}

fun Modifier.animateSizeAndSkipToFinalLayout() = composed {
    var sizeAnimation: Animatable<IntSize, AnimationVector2D>? by remember {
        mutableStateOf(null)
    }
    var targetSize: IntSize? by remember { mutableStateOf(null) }
    this
        .drawBehind {
            if (debugSharedElement) {
                drawRect(
                    color = Color.Black,
                    style = Stroke(2f),
                    topLeft = Offset.Zero,
                    size = targetSize!!.toSize()
                )
            }
        }
        .intermediateLayout { measurable, constraints ->
            targetSize = lookaheadSize
            if (lookaheadSize != sizeAnimation?.targetValue) {
                sizeAnimation?.run {
                    launch { animateTo(lookaheadSize) }
                } ?: Animatable(lookaheadSize, IntSize.VectorConverter).let {
                    sizeAnimation = it
                }
            }
            val (width, height) = sizeAnimation?.value ?: lookaheadSize
            val placeable = measurable.measure(
                Constraints.fixed(lookaheadSize.width, lookaheadSize.height)
            )
            // Make sure the content is aligned to topStart
            val wrapperWidth = width.coerceIn(constraints.minWidth, constraints.maxWidth)
            val wrapperHeight =
                height.coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                placeable.place(-(wrapperWidth - width) / 2, -(wrapperHeight - height) / 2)
            }
        }
}
