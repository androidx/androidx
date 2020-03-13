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
import androidx.animation.LinearEasing
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.animation.Transition
import androidx.ui.core.Text
import androidx.ui.core.gesture.TapGestureDetector
import androidx.ui.foundation.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutSize
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import androidx.ui.unit.toRect

@Composable
fun RepeatedRotationDemo() {
    Center {
        val state = state { RotationStates.Original }
        Column(arrangement = Arrangement.SpaceEvenly) {
            val textStyle = TextStyle(fontSize = 18.sp)
            Text(
                modifier = TapGestureDetector(onTap = { state.value = RotationStates.Rotated }),
                text = "Rotate 10 times",
                style = textStyle
            )
            Text(
                modifier = TapGestureDetector(onTap = { state.value = RotationStates.Original }),
                text = "Reset",
                style = textStyle
            )
            Transition(
                definition = definition,
                toState = state.value
            ) { state ->
                Canvas(modifier = LayoutSize(100.dp)) {
                    // TODO (njawad) replace with save lambda when multi children DrawNodes are supported
                    save()
                    rotate(state[rotation])
                    drawRect(size.toRect(), Paint().apply { color = Color(0xFF00FF00) })
                    // TODO (njawad) replace with save lambda when multi children DrawNodes are supported
                    restore()
                }
            }
        }
    }
}

private enum class RotationStates {
    Original,
    Rotated
}

private val rotation = FloatPropKey()

private val definition = transitionDefinition {
    state(RotationStates.Original) {
        this[rotation] = 0f
    }
    state(RotationStates.Rotated) {
        this[rotation] = 360f
    }
    transition(RotationStates.Original to RotationStates.Rotated) {
        rotation using repeatable {
            iterations = 10
            animation = tween {
                easing = LinearEasing
                duration = 1000
            }
        }
    }
    transition(RotationStates.Rotated to RotationStates.Original) {
        rotation using tween {
            duration = 300
        }
    }
}
