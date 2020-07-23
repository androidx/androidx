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

package androidx.ui.tooling

import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.transition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.pressIndicatorGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.ui.tooling.preview.Preview

private enum class PressState { Pressed, Released }

private val color = ColorPropKey()

private val definition = transitionDefinition<PressState> {
    state(PressState.Released) {
        this[color] = Color.Red
    }
    state(PressState.Pressed) {
        this[color] = Color.Blue
    }
    transition {
        color using spring(
            stiffness = 50f
        )
    }
}

@Preview
@Composable
fun PressStateAnimation() {
    val toState = remember { mutableStateOf(PressState.Released) }
    val pressIndicator =
        Modifier.pressIndicatorGestureFilter(
            onStart = { toState.value = PressState.Pressed },
            onStop = { toState.value = PressState.Released },
            onCancel = { toState.value = PressState.Released })

    val state = transition(label = "colorAnim", definition = definition, toState = toState.value)
    ColorRect(pressIndicator, color = state[color])
}

@Composable
private fun ColorRect(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(color)
    }
}