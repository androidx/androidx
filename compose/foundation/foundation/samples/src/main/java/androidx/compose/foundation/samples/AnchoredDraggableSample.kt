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
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.samples.AnchoredDraggableSampleValue.Center
import androidx.compose.foundation.samples.AnchoredDraggableSampleValue.End
import androidx.compose.foundation.samples.AnchoredDraggableSampleValue.HalfEnd
import androidx.compose.foundation.samples.AnchoredDraggableSampleValue.HalfStart
import androidx.compose.foundation.samples.AnchoredDraggableSampleValue.Start
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

private enum class AnchoredDraggableSampleValue {
    Start, HalfStart, Center, HalfEnd, End
}

@Composable
@Preview
fun AnchoredDraggableAnchorsFromCompositionSample() {
    val density = LocalDensity.current
    val snapAnimationSpec = tween<Float>()
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val positionalThreshold = { distance: Float -> distance * 0.5f }
    val velocityThreshold = { with(density) { 125.dp.toPx() } }
    val state = rememberSaveable(
        density,
        saver = AnchoredDraggableState.Saver(
            snapAnimationSpec,
            decayAnimationSpec,
            positionalThreshold,
            velocityThreshold
        )
    ) {
        AnchoredDraggableState(
            initialValue = Center,
            positionalThreshold,
            velocityThreshold,
            snapAnimationSpec,
            decayAnimationSpec
        )
    }
    val draggableWidth = 70.dp
    val containerWidthPx = with(density) { draggableWidth.toPx() }
    // Our anchors depend on the density which is obtained from composition, so we update them using
    // updateAnchors whenever they are available
    SideEffect {
        state.updateAnchors(
            DraggableAnchors {
                Start at 0f
                Center at containerWidthPx / 2f
                End at containerWidthPx
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
    val snapAnimationSpec = tween<Float>()
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val positionalThreshold = { distance: Float -> distance * 0.5f }
    val velocityThreshold = { with(density) { 125.dp.toPx() } }
    val state = rememberSaveable(
        density,
        saver = AnchoredDraggableState.Saver(
            snapAnimationSpec,
            decayAnimationSpec,
            positionalThreshold,
            velocityThreshold
        )
    ) {
        AnchoredDraggableState(
            initialValue = Center,
            positionalThreshold,
            velocityThreshold,
            snapAnimationSpec,
            decayAnimationSpec
        )
    }
    val draggableSize = 60.dp
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
                        Start at 0f
                        HalfStart at dragEndPoint * .25f
                        Center at dragEndPoint * .5f
                        HalfEnd at dragEndPoint * .75f
                        End at dragEndPoint
                    }
                )
            }
            .visualizeDraggableAnchors(state, Orientation.Horizontal)
    ) {
        Box(
            Modifier
                .size(draggableSize)
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

@Preview
@Composable
fun AnchoredDraggableCatchAnimatingWidgetSample() {
    // Attempting to press the box while it is settling to one anchor won't stop the box from
    // animating to that anchor. If you want to catch it while it is animating, you need to press
    // the box and drag it past the touchSlop. This is because startDragImmediately is set to false.
    val density = LocalDensity.current
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    // Setting the duration of the snapAnimationSpec to 3000ms gives more time to attempt to press
    // or drag the settling box.
    val snapAnimationSpec = tween<Float>(durationMillis = 3000)
    val state = AnchoredDraggableState(
        initialValue = Start,
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { with(density) { 125.dp.toPx() } },
        snapAnimationSpec = snapAnimationSpec,
        decayAnimationSpec = decayAnimationSpec
    )

    val draggableSize = 100.dp
    val draggableSizePx = with(LocalDensity.current) { draggableSize.toPx() }
    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { layoutSize ->
                val dragEndPoint = layoutSize.width - draggableSizePx
                state.updateAnchors(
                    DraggableAnchors {
                        Start at 0f
                        End at dragEndPoint
                    }
                )
            }
    ) {
        Box(
            Modifier
                .size(draggableSize)
                .offset {
                    IntOffset(
                        x = state
                            .requireOffset()
                            .roundToInt(), y = 0
                    )
                }
                .anchoredDraggable(state, Orientation.Horizontal, startDragImmediately = false)
                .background(Color.Red)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun AnchoredDraggableWithOverscrollSample() {
    val density = LocalDensity.current
    val draggableSize = 80.dp
    val draggableSizePx = with(density) { draggableSize.toPx() }

    val animationSpec = tween<Float>()
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val positionalThreshold = { distance: Float -> distance * 0.5f }
    val velocityThreshold = { with(density) { 125.dp.toPx() } }
    val overscrollEffect = ScrollableDefaults.overscrollEffect()
    val state = rememberSaveable(
        density,
        saver = AnchoredDraggableState.Saver(
            animationSpec,
            decayAnimationSpec,
            positionalThreshold,
            velocityThreshold,
        )
    ) {
        AnchoredDraggableState(
            initialValue = Center,
            positionalThreshold,
            velocityThreshold,
            animationSpec,
            decayAnimationSpec,
        )
    }

    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { layoutSize ->
                val dragEndPoint = layoutSize.width - draggableSizePx
                state.updateAnchors(
                    DraggableAnchors {
                        Start at 0f
                        Center at dragEndPoint / 2f
                        End at dragEndPoint
                    }
                )
            }
    ) {
        Box(
            Modifier
                .size(draggableSize)
                .offset {
                    IntOffset(
                        x = state
                            .requireOffset()
                            .roundToInt(), y = 0
                    )
                }
                // pass the overscrollEffect to AnchoredDraggable
                .anchoredDraggable(
                    state,
                    Orientation.Horizontal,
                    overscrollEffect = overscrollEffect
                )
                .overscroll(overscrollEffect)
                .background(Color.Red)
        )
    }
}

@Composable
fun AnchoredDraggableProgressSample() {
    val density = LocalDensity.current
    val snapAnimationSpec = tween<Float>()
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val positionalThreshold = { distance: Float -> distance * 0.5f }
    val velocityThreshold = { with(density) { 125.dp.toPx() } }
    val state = rememberSaveable(
        density,
        saver = AnchoredDraggableState.Saver(
            snapAnimationSpec,
            decayAnimationSpec,
            positionalThreshold,
            velocityThreshold
        )
    ) {
        AnchoredDraggableState(
            initialValue = Center,
            positionalThreshold,
            velocityThreshold,
            snapAnimationSpec,
            decayAnimationSpec
        )
    }
    val draggableSize = 60.dp
    val draggableSizePx = with(LocalDensity.current) { draggableSize.toPx() }
    Column(
        Modifier
            .fillMaxWidth()
            // Our anchors depend on this box's size, so we obtain the size from onSizeChanged and
            // use updateAnchors to let the state know about the new anchors
            .onSizeChanged { layoutSize ->
                val dragEndPoint = layoutSize.width - draggableSizePx
                state.updateAnchors(
                    DraggableAnchors {
                        Start at 0f
                        Center at dragEndPoint * .5f
                        End at dragEndPoint
                    }
                )
            }
    ) {
        // Read progress in a snapshot-backed context to receive updates. This could be e.g. a
        //  derived state, snapshotFlow or other snapshot-aware context like the graphicsLayer
        //  block.
        val centerToStartProgress by derivedStateOf { state.progress(from = Center, to = Start) }
        val centerToEndProgress by derivedStateOf { state.progress(from = Center, to = End) }
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(draggableSize)
                    .graphicsLayer { alpha = max(centerToStartProgress, centerToEndProgress) }
                    .background(Color.Black)
            )
            Box(
                Modifier
                    .size(draggableSize)
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
}

/**
 * A [Modifier] that visualizes the anchors attached to an [AnchoredDraggableState] as lines along
 * the cross axis of the layout (start to end for [Orientation.Vertical], top to end for
 * [Orientation.Horizontal]).
 * This is useful to debug components with a complex set of anchors, or for AnchoredDraggable
 * development.
 *
 * @param state The state whose anchors to visualize
 * @param orientation The orientation of the [anchoredDraggable]
 * @param lineColor The color of the visualization lines
 * @param lineStrokeWidth The stroke width of the visualization lines
 * @param linePathEffect The path effect used to draw the visualization lines
 */
private fun Modifier.visualizeDraggableAnchors(
    state: AnchoredDraggableState<*>,
    orientation: Orientation,
    lineColor: Color = Color.Black,
    lineStrokeWidth: Float = 10f,
    linePathEffect: PathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 30f))
) = drawWithContent {
    drawContent()
    state.anchors.forEach { _, position ->
        val startOffset = Offset(
            x = if (orientation == Orientation.Horizontal) position else 0f,
            y = if (orientation == Orientation.Vertical) position else 0f
        )
        val endOffset = Offset(
            x = if (orientation == Orientation.Horizontal) startOffset.x else size.height,
            y = if (orientation == Orientation.Vertical) startOffset.y else size.width
        )
        drawLine(
            color = lineColor,
            start = startOffset,
            end = endOffset,
            strokeWidth = lineStrokeWidth,
            pathEffect = linePathEffect
        )
    }
}
