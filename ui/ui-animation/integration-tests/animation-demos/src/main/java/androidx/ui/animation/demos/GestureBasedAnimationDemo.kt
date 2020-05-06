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

import androidx.animation.FloatPropKey
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.foundation.Canvas
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxSize

private const val halfSize = 200f

private enum class ComponentState { Pressed, Released }

private val scale = FloatPropKey()
private val color = ColorPropKey()

private val definition = transitionDefinition {
    state(ComponentState.Released) {
        this[scale] = 1f
        this[color] = Color(red = 0, green = 200, blue = 0, alpha = 255)
    }
    state(ComponentState.Pressed) {
        this[scale] = 3f
        this[color] = Color(red = 0, green = 100, blue = 0, alpha = 255)
    }
    transition {
        scale using physics {
            stiffness = 50f
        }
        color using physics {
            stiffness = 50f
        }
    }
}

@Composable
fun GestureBasedAnimationDemo() {
    val toState = state { ComponentState.Released }
    val pressIndicator =
        Modifier.pressIndicatorGestureFilter(
            onStart = { toState.value = ComponentState.Pressed },
            onStop = { toState.value = ComponentState.Released },
            onCancel = { toState.value = ComponentState.Released })

    Transition(definition = definition, toState = toState.value) { state ->
        ScaledColorRect(pressIndicator, scale = state[scale], color = state[color])
    }
}

@Composable
private fun ScaledColorRect(modifier: Modifier = Modifier, scale: Float, color: Color) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            color,
            topLeft = Offset(center.dx - halfSize * scale, center.dy - halfSize * scale),
            size = Size(halfSize * 2 * scale, halfSize * 2 * scale)
        )
    }
}
