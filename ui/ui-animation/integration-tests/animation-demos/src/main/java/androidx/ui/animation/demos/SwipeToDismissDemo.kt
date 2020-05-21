/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.animation.demos

import androidx.animation.AnimationEndReason
import androidx.animation.ExponentialDecay
import androidx.animation.FastOutSlowInEasing
import androidx.animation.PhysicsBuilder
import androidx.animation.TargetAnimation
import androidx.animation.fling
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.animation.animatedFloat
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.rawDragGestureFilter
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.Text
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.graphics.drawscope.DrawScope
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import kotlin.math.sign

@Composable
fun SwipeToDismissDemo() {
    Column {
        SwipeToDismiss()
        Text(
            "Swipe up to dismiss",
            fontSize = 30.sp,
            modifier = Modifier.padding(40.dp)
        )
    }
}

private val height = 1600f
private val itemHeight = 1600f * 2 / 3f
private val padding = 10f

@Composable
private fun SwipeToDismiss() {
    val itemBottom = animatedFloat(height)
    val index = state { 0 }
    val itemWidth = state { 0f }
    val isFlinging = state { false }
    val modifier = Modifier.rawDragGestureFilter(dragObserver = object : DragObserver {
        override fun onStart(downPosition: PxPosition) {
            itemBottom.setBounds(0f, height)
            if (isFlinging.value && itemBottom.targetValue < 100f) {
                reset()
            }
        }

        private fun reset() {
            itemBottom.snapTo(height)
            index.value--
            if (index.value < 0) {
                index.value += colors.size
            }
        }

        override fun onDrag(dragDistance: PxPosition): PxPosition {
            itemBottom.snapTo(itemBottom.targetValue + dragDistance.y)
            return dragDistance
        }

        fun adjustTarget(velocity: Float): (Float) -> TargetAnimation? {
            return { target: Float ->
                // The velocity is fast enough to fly off screen
                if (target <= 0) {
                    null
                } else {
                    val animation = PhysicsBuilder<Float>(dampingRatio = 0.8f, stiffness = 300f)
                    val projectedTarget = target + sign(velocity) * 0.2f * height
                    if (projectedTarget < 0.6 * height) {
                        TargetAnimation(0f, animation)
                    } else {
                        TargetAnimation(height, animation)
                    }
                }
            }
        }

        override fun onStop(velocity: PxPosition) {
            isFlinging.value = true
            itemBottom.fling(velocity.y,
                ExponentialDecay(3.0f),
                adjustTarget(velocity.y),
                onEnd = { endReason, final, _ ->
                    isFlinging.value = false
                    if (endReason != AnimationEndReason.Interrupted && final == 0f) {
                        reset()
                    }
                })
        }
    })

    val heightDp = with(DensityAmbient.current) { height.toDp() }

    Canvas(
        modifier.fillMaxWidth()
            .preferredHeight(heightDp)
            .onPositioned { coordinates ->
                itemWidth.value = coordinates.size.width.value * 2 / 3f
            }
    ) {
        val progress = 1 - itemBottom.value / height
        // TODO: this progress can be used to drive state transitions
        val alpha = 1f - FastOutSlowInEasing(progress)
        val horizontalOffset = progress * itemWidth.value
        drawLeftItems(horizontalOffset, itemWidth.value, itemHeight, index.value)
        drawDismissingItem(itemBottom.value, itemWidth.value, itemHeight, index.value + 1, alpha)
    }
}

private fun DrawScope.drawLeftItems(
    horizontalOffset: Float,
    width: Float,
    height: Float,
    index: Int
) {
    val offset = Offset(center.dx - width * 1.5f + horizontalOffset + padding, size.height - height)
    val rectSize = Size(width - (2 * padding), height)
    drawRect(colors[index % colors.size], offset, rectSize)

    if (offset.dx >= 0) {
        // draw another item
        drawRect(
            colors[(index - 1 + colors.size) % colors.size],
            offset - Offset(width, 0.0f),
            rectSize
        )
    }
}

private fun DrawScope.drawDismissingItem(
    bottom: Float,
    width: Float,
    height: Float,
    index: Int,
    alpha: Float
) = drawRect(
        colors[index % colors.size],
        topLeft = Offset(center.dx - width / 2 + padding, bottom - height),
        size = Size(width - (2 * padding), height),
        alpha = alpha
    )

private val colors = listOf(
    Color(0xFFffd7d7),
    Color(0xFFffe9d6),
    Color(0xFFfffbd0),
    Color(0xFFe3ffd9),
    Color(0xFFd0fff8)
)
