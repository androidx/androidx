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

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

@Composable
fun CraneDemo() {
    val progressProvider = remember { ProgressProviderImpl(false) }
    val avatar = remember {
        movableContentWithReceiverOf<SceneScope> {
            Box(
                Modifier.sharedElementBasedOnProgress(
                        this@movableContentWithReceiverOf,
                        progressProvider
                    )
                    .background(Color(0xffff6f69), RoundedCornerShape(20))
                    .fillMaxSize()
            )
        }
    }

    val parent = remember {
        movableContentWithReceiverOf<SceneScope, @Composable () -> Unit> { child ->
            Surface(
                modifier =
                    Modifier.sharedElementBasedOnProgress(
                            this@movableContentWithReceiverOf,
                            progressProvider
                        )
                        .background(Color(0xfffdedac)),
                color = Color(0xfffdedac),
                shape = RoundedCornerShape(10.dp)
            ) {
                child()
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        SceneHost(Modifier.fillMaxSize()) {
            if (progressProvider.targetState) {
                Box(Modifier.offset(100.dp, 150.dp)) {
                    parent {
                        Box(Modifier.padding(10.dp).wrapContentSize(Alignment.Center).size(50.dp)) {
                            avatar()
                        }
                    }
                }
            } else {
                parent {
                    Column(Modifier.fillMaxSize()) {
                        val alpha =
                            produceState(0f) {
                                animate(0f, 1f, animationSpec = tween(2000)) { value, _ ->
                                    this.value = value
                                }
                            }
                        Box(
                            Modifier.fillMaxWidth()
                                .height(300.dp)
                                .graphicsLayer { this.alpha = alpha.value }
                                .background(Color.DarkGray)
                                .animateContentSize()
                        )
                        Box(Modifier.padding(10.dp).size(60.dp)) { avatar() }
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxHeight()
                .width(96.dp)
                .background(Color(0x88CCCCCC))
                .align(Alignment.CenterEnd)
                .draggable(
                    rememberDraggableState(
                        onDelta = {
                            progressProvider.progress =
                                (-it / 300f + progressProvider.progress).coerceIn(0f, 1f)
                        }
                    ),
                    onDragStarted = {
                        progressProvider.targetState = !progressProvider.targetState
                        progressProvider.progress = 0f
                    },
                    onDragStopped = {
                        with(progressProvider) {
                            if (progress < 0.5f) {
                                targetState = initialState
                            } else {
                                initialState = targetState
                            }
                            progressProvider.progress = 1f
                        }
                    },
                    orientation = Orientation.Horizontal
                )
        )
    }
}

class ProgressProviderImpl<T>(initialState: T) : ProgressProvider<T> {
    override var initialState: T by mutableStateOf(initialState)
    override var targetState: T by mutableStateOf(initialState)
    override var progress: Float by mutableFloatStateOf(0f)
}

interface ProgressProvider<T> {
    val initialState: T
    val targetState: T
    val progress: Float
}

@SuppressLint("PrimitiveInCollection")
fun <T> Modifier.sharedElementBasedOnProgress(
    lookaheadScope: LookaheadScope,
    provider: ProgressProvider<T>
) =
    with(lookaheadScope) {
        composed {
            val sizeMap = remember { mutableMapOf<T, IntSize>() }
            val offsetMap = remember { mutableMapOf<T, Offset>() }
            val calculateSize: (IntSize) -> IntSize = {
                sizeMap[provider.targetState] = it
                val (width, height) =
                    lerp(
                        sizeMap[provider.initialState]!!.toSize(),
                        sizeMap[provider.targetState]!!.toSize(),
                        provider.progress
                    )
                IntSize(width.roundToInt(), height.roundToInt())
            }

            val calculateOffset: Placeable.PlacementScope.(ApproachMeasureScope) -> IntOffset = {
                with(it) {
                    coordinates?.let {
                        offsetMap[provider.targetState] =
                            lookaheadScopeCoordinates.localLookaheadPositionOf(it)
                        val lerpedOffset =
                            lerp(
                                offsetMap[provider.initialState]!!,
                                offsetMap[provider.targetState]!!,
                                provider.progress
                            )
                        val currentOffset =
                            lookaheadScopeCoordinates.localPositionOf(
                                it,
                                androidx.compose.ui.geometry.Offset.Zero
                            )
                        (lerpedOffset - currentOffset).round()
                    } ?: IntOffset(0, 0)
                }
            }
            this.approachLayout({ provider.progress != 1f }) { measurable, _ ->
                val (width, height) = calculateSize(lookaheadSize)
                val animatedConstraints = androidx.compose.ui.unit.Constraints.fixed(width, height)
                val placeable = measurable.measure(animatedConstraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(calculateOffset(this@approachLayout))
                }
            }
        }
    }
