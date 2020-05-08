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

import android.os.Handler
import android.os.Looper
import androidx.animation.FloatPropKey
import androidx.animation.TransitionState
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.drawBackground
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxSize

@Composable
fun StateAnimationWithInterruptionsDemo() {
    Box(Modifier.fillMaxSize()) {
        ColorRect()
    }
}

private val background = ColorPropKey()
private val y = FloatPropKey()

private enum class OverlayState {
    Open,
    Closed
}

private val definition = transitionDefinition {
    state(OverlayState.Open) {
        this[background] = Color(red = 128, green = 128, blue = 128, alpha = 255)
        this[y] = 1f // percentage
    }
    state(OverlayState.Closed) {
        this[background] = Color(red = 188, green = 222, blue = 145, alpha = 255)
        this[y] = 0f // percentage
    }
    // Apply this transition to all state changes (i.e. Open -> Closed and Closed -> Open)
    transition {
        background using tween {
            duration = 800
        }
        y using physics {
            // Extremely low stiffness
            stiffness = 40f
        }
    }
}

private val handler = Handler(Looper.getMainLooper())

@Composable
private fun ColorRect() {
    var toState = OverlayState.Closed
    Recompose { recompose ->
        handler.postDelayed(object : Runnable {
            override fun run() {
                if ((0..1).random() == 0) {
                    toState = OverlayState.Open
                } else {
                    toState = OverlayState.Closed
                }
                recompose()
            }
        }, (200..800).random().toLong())
        Transition(definition = definition, toState = toState) { state ->
            ColorRectState(state = state)
        }
    }
}

@Composable
private fun ColorRectState(state: TransitionState) {
    val color = state[background]
    val scaleY = state[y]
    Canvas(Modifier.fillMaxSize().drawBackground(color = color)) {
        drawRect(
            Color(alpha = 255, red = 255, green = 255, blue = 255),
            topLeft = Offset(100f, 0f),
            size = Size(size.width - 200f, scaleY * size.height)
        )
    }
}
