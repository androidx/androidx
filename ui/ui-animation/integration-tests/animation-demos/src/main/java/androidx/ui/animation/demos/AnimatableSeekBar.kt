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
import androidx.animation.PhysicsBuilder
import androidx.compose.Composable
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.animation.animatedFloat
import androidx.ui.core.Draw
import androidx.ui.core.PxPosition
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.gesture.RawDragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.core.setContent
import androidx.ui.core.sp
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Padding
import androidx.ui.graphics.Paint
import androidx.ui.text.TextStyle

class AnimatableSeekBar : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(mainAxisSize = LayoutSize.Expand) {
                Padding(40.dp) {
                    Text("Drag or tap on the seek bar", style = TextStyle(fontSize = 8.sp))
                }

                Padding(left = 10.dp, right = 10.dp, bottom = 30.dp) {
                    MovingTargetExample()
                }
            }
        }
    }

    @Composable
    fun MovingTargetExample() {
            val animValue = +animatedFloat(0f)
            RawDragGestureDetector(dragObserver = object : DragObserver {
                override fun onDrag(dragDistance: PxPosition): PxPosition {
                    animValue.snapTo(animValue.targetValue + dragDistance.x.value)
                    return dragDistance
                }
            }) {
                PressGestureDetector(
                    onPress = { position ->
                        animValue.animateTo(position.x.value,
                            PhysicsBuilder(dampingRatio = 1.0f, stiffness = 1500f))
                    }) {

                    Container(height = 60.dp, expanded = true) {
                        DrawSeekBar(animValue.value)
                    }
                }
            }
        }

    @Composable
    fun DrawSeekBar(x: Float) {
        val paint = +memo { Paint() }
        Draw { canvas, parentSize ->
            val centerY = parentSize.height.value / 2
            val xConstraint = x.coerceIn(0f, parentSize.width.value)
            // draw bar
            paint.color = Color.Gray
            canvas.drawRect(
                Rect(0f, centerY - 5, parentSize.width.value, centerY + 5),
                paint
            )
            paint.color = Color.Magenta
            canvas.drawRect(
                Rect(0f, centerY - 5, xConstraint, centerY + 5),
                paint
            )

            // draw ticker
            canvas.drawCircle(
                Offset(xConstraint, centerY), 40f, paint
            )
        }
    }
}
