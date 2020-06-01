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

import android.util.Log
import androidx.animation.DEBUG
import androidx.animation.PhysicsBuilder
import androidx.animation.TargetAnimation
import androidx.animation.fling
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.animation.animatedFloat
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.rawDragGestureFilter
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
import kotlin.math.roundToInt

@Composable
fun FancyScrollingDemo() {
    Column {
        Text(
            "<== Scroll horizontally ==>",
            fontSize = 20.sp,
            modifier = Modifier.padding(40.dp)
        )
        val animScroll = animatedFloat(0f)
        val itemWidth = state { 0f }
        val gesture = Modifier.rawDragGestureFilter(dragObserver = object : DragObserver {
            override fun onDrag(dragDistance: PxPosition): PxPosition {
                // Snap to new drag position
                animScroll.snapTo(animScroll.value + dragDistance.x)
                return dragDistance
            }

            override fun onStop(velocity: PxPosition) {

                // Uses default decay animation to calculate where the fling will settle,
                // and adjust that position as needed. The target animation will be used for
                // animating to the adjusted target.
                animScroll.fling(velocity.x, adjustTarget = { target ->
                    // Adjust the target position to center align the item
                    val animation = PhysicsBuilder<Float>(dampingRatio = 2.0f, stiffness = 100f)
                    var rem = target % itemWidth.value
                    if (rem < 0) {
                        rem += itemWidth.value
                    }
                    TargetAnimation((target - rem), animation)
                })
            }
        })

        Canvas(gesture.fillMaxWidth().preferredHeight(400.dp)) {
            val width = size.width / 2f
            val scroll = animScroll.value + width / 2
            itemWidth.value = width
            if (DEBUG) {
                Log.w(
                    "Anim", "Drawing items with updated" +
                            " AnimatedFloat: ${animScroll.value}"
                )
            }
            drawItems(scroll, width, size.height)
        }
    }
}

private fun DrawScope.drawItems(
    scrollPosition: Float,
    width: Float,
    height: Float
) {
    var startingPos = scrollPosition % width
    if (startingPos > 0) {
        startingPos -= width
    }
    var startingColorIndex =
        ((scrollPosition - startingPos) / width).roundToInt().rem(colors.size)
    if (startingColorIndex < 0) {
        startingColorIndex += colors.size
    }

    val size = Size(width - 20, height)
    drawRect(
        colors[startingColorIndex],
        topLeft = Offset(startingPos + 10, 0f),
        size = size
    )

    drawRect(
        colors[(startingColorIndex + colors.size - 1) % colors.size],
        topLeft = Offset(startingPos + width + 10, 0.0f),
        size = size
    )

    drawRect(
        colors[(startingColorIndex + colors.size - 2) % colors.size],
        topLeft = Offset(startingPos + width * 2 + 10, 0.0f),
        size = size
    )
}

private val colors = listOf(
    Color(0xFFffd9d9),
    Color(0xFFffa3a3),
    Color(0xFFff7373),
    Color(0xFFff3b3b),
    Color(0xFFce0000),
    Color(0xFFff3b3b),
    Color(0xFFff7373),
    Color(0xFFffa3a3)
)
