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
import androidx.ui.core.PxSize
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

class SpringBackScrolling : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                SpringBackExample()
            }
        }
    }

    @Composable
    fun SpringBackExample() {
        Column {
            Padding(40.dp) {
                Text("<== Scroll horizontally ==>", style = TextStyle(fontSize = 80f))
            }
            val animScroll = +animatedFloat(0f)
            val itemWidth = +state { 0f }
            var isFlinging = +state { false }
            DragGestureDetector(canDrag = { true }, dragObserver = object : DragObserver {
                override fun onDrag(dragDistance: PxPosition): PxPosition {
                    animScroll.snapTo(animScroll.targetValue + dragDistance.x.value)
                    return dragDistance
                }
                override fun onStop(velocity: PxPosition) {
                    isFlinging.value = true
                    animScroll.fling(velocity.x.value, onFinished = { isFlinging.value = false })
                }
            }) {
                val children = @Composable {
                    var paint = +memo { Paint() }
                    Draw { canvas, parentSize ->

                        itemWidth.value = parentSize.width.value / 2f
                        if (isFlinging.value) {
                            // Figure out what position to spring back to
                            val target = animScroll.targetValue
                            var rem = target % itemWidth.value
                            if (animScroll.velocity < 0) {
                                if (rem > 0) {
                                    rem -= itemWidth.value
                                }
                            } else {
                                if (rem < 0) {
                                    rem += itemWidth.value
                                }
                            }
                            val springBackTarget = target - rem

                            // Spring back as soon as the target position is crossed.
                            if ((animScroll.velocity > 0 && animScroll.value > springBackTarget) ||
                                (animScroll.velocity < 0 && animScroll.value < springBackTarget)) {
                                animScroll.animateTo(springBackTarget,
                                    PhysicsBuilder(dampingRatio = 0.8f, stiffness = 200f))
                            }
                        }
                        if (DEBUG) {
                            Log.w("Anim", "Spring back scrolling, redrawing with new" +
                                    " scroll value: ${animScroll.value}")
                        }
                        drawRects(canvas, parentSize, paint, animScroll.value)
                    }
                }
                Layout(children = children, layoutBlock = { _, constraints ->
                    layout(constraints.maxWidth, IntPx(1200)) {}
                })
            }
        }
    }

    private fun drawRects(canvas: Canvas, parentSize: PxSize, paint: Paint, animScroll: Float) {
        val width = parentSize.width.value / 2f
        val scroll = animScroll + width / 2
        var startingPos = scroll % width
        if (startingPos > 0) {
            startingPos -= width
        }
        var startingColorIndex = ((scroll - startingPos) / width).roundToInt().rem(colors.size)
        if (startingColorIndex < 0) {
            startingColorIndex += colors.size
        }
        paint.color = colors[startingColorIndex]
        canvas.drawRect(Rect(startingPos + 10, 0f, startingPos + width - 10,
            parentSize.height.value), paint)
        paint.color = colors[(startingColorIndex + colors.size - 1) % colors.size]
        canvas.drawRect(Rect(startingPos + width + 10, 0f, startingPos + width * 2 - 10,
            parentSize.height.value), paint)
        paint.color = colors[(startingColorIndex + colors.size - 2) % colors.size]
        canvas.drawRect(Rect(startingPos + width * 2 + 10, 0f, startingPos + width * 3 - 10,
            parentSize.height.value), paint)
    }

    private val colors = listOf(
            Color(0xFFdaf8e3.toInt()),
            Color(0xFF97ebdb.toInt()),
            Color(0xFF00c2c7.toInt()),
            Color(0xFF0086ad.toInt()),
            Color(0xFF005582.toInt()),
            Color(0xFF0086ad.toInt()),
            Color(0xFF00c2c7.toInt()),
            Color(0xFF97ebdb.toInt()))
}
