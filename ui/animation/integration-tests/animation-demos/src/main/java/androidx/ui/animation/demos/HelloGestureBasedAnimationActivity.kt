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
import androidx.animation.ColorPropKey
import androidx.animation.FloatPropKey
import androidx.animation.transitionDefinition
import androidx.ui.animation.Transition
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Layout
import androidx.ui.core.Draw
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus

class HelloGestureBasedAnimationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HelloGesture() }
    }
}

@Composable
fun HelloGesture() {
    CraneWrapper {
        TransitionExample()
    }
}

private enum class ComponentState { Pressed, Released }

private val scale = FloatPropKey()
private val color = ColorPropKey()

private val definition = transitionDefinition {
    state(ComponentState.Released) {
        this[scale] = 1f
        this[color] = Color(alpha = 255, red = 0, green = 200, blue = 0)
    }
    state(ComponentState.Pressed) {
        this[scale] = 3f
        this[color] = Color(alpha = 255, red = 0, green = 100, blue = 0)
    }
}

@Composable
fun TransitionExample() {
    val toState = +state { ComponentState.Released }
    PressGestureDetector(
        onPress = { toState.value = ComponentState.Pressed },
        onRelease = { toState.value = ComponentState.Released },
        onCancel = { toState.value = ComponentState.Released }) {
        val children = @Composable {
            Transition(definition = definition, toState = toState.value) { state ->
                DrawScaledRect(scale = state[scale], color = state[color])
            }
        }
        Layout(children = children, layoutBlock = { _, constraints ->
            layout(constraints.maxWidth, constraints.maxHeight) {}
        })
    }
}

private val paint: Paint = Paint()
const val halfSize = 200f

@Composable
fun DrawScaledRect(scale: Float, color: Color) {
    Draw { canvas, parentSize ->
        val centerX = parentSize.width.value / 2
        val centerY = parentSize.height.value / 2
        paint.color = color
        canvas.drawRect(
            Rect(
                centerX - halfSize * scale, centerY - halfSize * scale,
                centerX + halfSize * scale, centerY + halfSize * scale
            ), paint
        )
    }
}
