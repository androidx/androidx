/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.ui.desktop.example

import androidx.animation.LinearEasing
import androidx.animation.FloatPropKey
import androidx.animation.Infinite
import androidx.animation.transitionDefinition

import androidx.compose.Composable

import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.animation.Transition
import androidx.ui.graphics.Color
import androidx.ui.desktop.SkiaWindow
import androidx.ui.desktop.setContent
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.material.Button
import androidx.ui.material.CircularProgressIndicator
import androidx.ui.material.LinearProgressIndicator
import androidx.ui.unit.dp

import javax.swing.WindowConstants

fun main() {
    val width = 1024
    val height = 768

    val frame = SkiaWindow(width = width, height = height)

    frame.title = "Skija Demo"
    frame.setLocation(400, 400)
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

    frame.setContent {
        App()
    }

    frame.setVisible(true)
}

private val progress = FloatPropKey()
private fun progressTransition(time: Int) = transitionDefinition {
    state("start") {
        this[progress] = 0f
    }
    state("end") {
        this[progress] = 1.0f
    }
    transition("start" to "end") {
        progress using repeatable {
            animation = tween {
                easing = LinearEasing
                duration = time
            }
            iterations = Infinite
        }
    }
}

@Composable
fun App() {
    Box(Modifier.fillMaxSize(), backgroundColor = Color.Green)
    Box(Modifier.padding(40.dp) + Modifier.drawBackground(color = Color.Blue)) {
            Text(text = "Привет! 你好! Desktop Compose!",
                color = Color.Black,
                modifier = Modifier.preferredHeight(56.dp).wrapContentSize(Alignment.Center)
            )
        }
    var text = "Base"
    Button(onClick = {
        text = "Clicked"
    }) {
        Text(text)
    }
    Transition(
        definition = progressTransition(2000),
        initState = "start",
        toState = "end"
    ) {
            state -> LinearProgressIndicator(color = Color.Red, progress = state[progress])
    }
    Transition(
        definition = progressTransition(3000),
        initState = "start",
        toState = "end"
    ) {
            state ->
        CircularProgressIndicator(color = Color.Yellow, progress = 1.0f - state[progress])
    }
}
