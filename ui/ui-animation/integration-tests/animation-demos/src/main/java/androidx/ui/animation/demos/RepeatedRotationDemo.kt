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
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.graphics.drawscope.rotate
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp

@Composable
fun RepeatedRotationDemo() {
    val state = state { RotationStates.Original }
    Column(
        Modifier.fillMaxSize()
            .wrapContentSize(Alignment.Center),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        val textStyle = TextStyle(fontSize = 18.sp)
        Text(
            modifier = Modifier.tapGestureFilter(onTap = { state.value = RotationStates.Rotated }),
            text = "Rotate 10 times",
            style = textStyle
        )
        Text(
            modifier = Modifier.tapGestureFilter(onTap = { state.value = RotationStates.Original }),
            text = "Reset",
            style = textStyle
        )
        Transition(
            definition = definition,
            toState = state.value
        ) { state ->
            Canvas(Modifier.preferredSize(100.dp)) {
                rotate(state[rotation], 0.0f, 0.0f) {
                    drawRect(Color(0xFF00FF00))
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
