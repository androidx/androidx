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

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.animation.DEBUG
import androidx.animation.PhysicsBuilder
import androidx.animation.TargetAnimation
import androidx.animation.fling
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.animation.animatedFloat
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.PxPosition
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Padding
import androidx.ui.painting.Canvas
import androidx.ui.painting.Paint
import androidx.ui.painting.TextStyle
import kotlin.math.roundToInt

class FancyScrolling : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                FancyScrollingExample()
            }
        }
    }

    @Composable
    fun FancyScrollingExample() {
        Column {
            Padding(40.dp) {
                Text("<== Scroll horizontally ==>", style = TextStyle(fontSize = 80f))
            }
            val animScroll = +animatedFloat(0f)
            val itemWidth = +state { 0f }
            DragGestureDetector(canDrag = { true }, dragObserver = object : DragObserver {
                override fun onDrag(dragDistance: PxPosition): PxPosition {
                    // Snap to new drag position
                    animScroll.snapTo(animScroll.value + dragDistance.x.value)
                    return dragDistance
                }
                override fun onStop(velocity: PxPosition) {

                    // Uses default decay animation to calculate where the fling will settle,
                    // and adjust that position as needed. The target animation will be used for
                    // animating to the adjusted target.
                    animScroll.fling(velocity.x.value, adjustTarget = { target ->
                        // Adjust the target position to center align the item
                        val animation = PhysicsBuilder<Float>(dampingRatio = 2.0f, stiffness = 100f)
                        var rem = target % itemWidth.value
                        if (rem < 0) {
                            rem += itemWidth.value
                        }
                        TargetAnimation((target - rem), animation)
                    })
                }
            }) {
                val children = @Composable {
                    var paint = +memo { Paint() }
                    Draw { canvas, parentSize ->
                        val width = parentSize.width.value / 2f
                        val scroll = animScroll.value + width / 2
                        itemWidth.value = width
                        if (DEBUG) {
                            Log.w("Anim", "Drawing items with updated" +
                                    " AnimatedFloat: ${animScroll.value}")
                        }
                        drawItems(canvas, scroll, width, parentSize.height.value, paint)
                    }
                }
                Layout(children = children, layoutBlock = { _, constraints ->
                    layout(constraints.maxWidth, IntPx(1200)) {}
                })
            }
        }
    }

    private fun drawItems(
        canvas: Canvas,
        scrollPosition: Float,
        width: Float,
        height: Float,
        paint: Paint
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
        paint.color = colors[startingColorIndex]
        canvas.drawRect(Rect(startingPos + 10, 0f, startingPos + width - 10,
            height), paint)
        paint.color = colors[(startingColorIndex + colors.size - 1) % colors.size]
        canvas.drawRect(Rect(startingPos + width + 10, 0f, startingPos + width * 2 - 10,
            height), paint)
        paint.color = colors[(startingColorIndex + colors.size - 2) % colors.size]
        canvas.drawRect(Rect(startingPos + width * 2 + 10, 0f, startingPos + width * 3 - 10,
            height), paint)
    }

    private val colors = listOf(
        Color(0xFFffd9d9.toInt()),
        Color(0xFFffa3a3.toInt()),
        Color(0xFFff7373.toInt()),
        Color(0xFFff3b3b.toInt()),
        Color(0xFFce0000.toInt()),
        Color(0xFFff3b3b.toInt()),
        Color(0xFFff7373.toInt()),
        Color(0xFFffa3a3.toInt()))
}
