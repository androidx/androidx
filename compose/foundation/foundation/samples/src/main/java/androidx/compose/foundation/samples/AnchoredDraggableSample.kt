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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.samples

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private enum class AnchoredDraggableSampleValue {
    Start, Center, End
}

@Composable
@Preview
fun AnchoredDraggableAnchorsFromCompositionSample() {
    val density = LocalDensity.current
    val animationSpec = tween<Float>()
    val positionalThreshold = { distance: Float -> distance * 0.5f }
    val velocityThreshold = { with(density) { 125.dp.toPx() } }
    val state = rememberSaveable(
        density,
        saver = AnchoredDraggableState.Saver(animationSpec, positionalThreshold, velocityThreshold)
    ) {
        AnchoredDraggableState(
            initialValue = AnchoredDraggableSampleValue.Center,
            positionalThreshold,
            velocityThreshold,
            animationSpec
        )
    }
    val draggableWidth = 70.dp
    val containerWidthPx = with(density) { draggableWidth.toPx() }
    // Our anchors depend on the density which is obtained from composition, so we update them using
    // updateAnchors whenever they are available
    SideEffect {
        state.updateAnchors(
            DraggableAnchors {
                AnchoredDraggableSampleValue.Start at 0f
                AnchoredDraggableSampleValue.Center at containerWidthPx / 2f
                AnchoredDraggableSampleValue.End at containerWidthPx
            }
        )
    }
    Box(Modifier.width(draggableWidth)) {
        Box(
            Modifier
                .size(100.dp)
                .offset {
                    IntOffset(
                        x = state
                            .requireOffset()
                            .roundToInt(), y = 0
                    )
                }
                .anchoredDraggable(state, Orientation.Horizontal)
                .background(Color.Red)
        )
    }
}

@Preview
@Composable
fun AnchoredDraggableLayoutDependentAnchorsSample() {
    val density = LocalDensity.current
    val animationSpec = tween<Float>()
    val positionalThreshold = { distance: Float -> distance * 0.5f }
    val velocityThreshold = { with(density) { 125.dp.toPx() } }
    val state = rememberSaveable(
        density,
        saver = AnchoredDraggableState.Saver(animationSpec, positionalThreshold, velocityThreshold)
    ) {
        AnchoredDraggableState(
            initialValue = AnchoredDraggableSampleValue.Center,
            positionalThreshold,
            velocityThreshold,
            animationSpec
        )
    }
    val draggableSize = 100.dp
    val draggableSizePx = with(LocalDensity.current) { draggableSize.toPx() }
    Box(
        Modifier
            .fillMaxWidth()
            // Our anchors depend on this box's size, so we obtain the size from onSizeChanged and
            // use updateAnchors to let the state know about the new anchors
            .onSizeChanged { layoutSize ->
                val dragEndPoint = layoutSize.width - draggableSizePx
                state.updateAnchors(
                    DraggableAnchors {
                        AnchoredDraggableSampleValue.Start at 0f
                        AnchoredDraggableSampleValue.Center at dragEndPoint / 2f
                        AnchoredDraggableSampleValue.End at dragEndPoint
                    }
                )
            }
    ) {
        Box(
            Modifier
                .size(100.dp)
                .offset {
                    IntOffset(
                        x = state
                            .requireOffset()
                            .roundToInt(), y = 0
                    )
                }
                .anchoredDraggable(state, Orientation.Horizontal)
                .background(Color.Red)
        )
    }
}

@Preview
@Composable
fun AnchoredDraggableCustomAnchoredSample() {
    @Suppress("unused")
    // Using AnchoredDraggableState's anchoredDrag APIs, we can build a custom animation
    suspend fun <T> AnchoredDraggableState<T>.customAnimation(
        target: T,
        snapAnimationSpec: AnimationSpec<Float>,
        velocity: Float = lastVelocity,
    ) {
        anchoredDrag(target) { latestAnchors, latestTarget ->
            // If the anchors change while this block is suspending, it will get cancelled and
            // restarted with the latest anchors and latest target
            val targetOffset = latestAnchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                animate(
                    initialValue = offset,
                    initialVelocity = velocity,
                    targetValue = targetOffset,
                    animationSpec = snapAnimationSpec
                ) { value, velocity ->
                    dragTo(value, velocity)
                }
            }
        }
    }
}
