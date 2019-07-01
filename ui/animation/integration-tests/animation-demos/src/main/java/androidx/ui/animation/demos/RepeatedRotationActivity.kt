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
import androidx.animation.FloatPropKey
import androidx.animation.LinearEasing
import androidx.animation.transitionDefinition
import androidx.ui.animation.Transition
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.ui.core.toRect
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.TextStyle
import androidx.ui.vectormath64.radians
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.sp

class RepeatedRotationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RepeatedRotation() }
    }
}

@Composable
fun RepeatedRotation() {
    CraneWrapper {
        Center {
            val state = +state { RotationStates.Original }
            Column(mainAxisAlignment = MainAxisAlignment.SpaceEvenly) {
                val textStyle = TextStyle(fontSize = 18.sp)
                PressReleasedGestureDetector(onRelease = {
                    state.value = RotationStates.Rotated
                }) {
                    Text(text = "Rotate 10 times", style = textStyle)
                }
                PressReleasedGestureDetector(onRelease = {
                    state.value = RotationStates.Original
                }) {
                    Text(text = "Reset", style = textStyle)
                }
                Container(width = 100.dp, height = 100.dp) {
                    Transition(
                        definition = definition,
                        toState = state.value
                    ) { state ->
                        Draw { canvas, parentSize ->
                            // TODO (njawad) replace with save lambda when multi children DrawNodes are supported
                            canvas.nativeCanvas.save()
                            canvas.rotate(state[rotation])
                            canvas.drawRect(parentSize.toRect(),
                                Paint().apply { color = Color(0xFF00FF00.toInt()) })
                            // TODO (njawad) replace with save lambda when multi children DrawNodes are supported
                            canvas.nativeCanvas.restore()
                        }
                    }
                }
            }
        }
    }
}

enum class RotationStates {
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
