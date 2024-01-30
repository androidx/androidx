/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.demos.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This demo enables easy, targeted testing of simple recomposition situations.
 * Each composable element below tests a specific hierarchy of objects. Tapping on the element
 * triggers a recomposition of that element, which can be tested to see what Compose is doing
 * to make that happen (allocations, etc).
 *
 * All of the demos use the same mechanism of a coroutine with an initial and per-frame delay
 * to force the recomposition to happen at times and frequencies that can be examined in tools.
 * The reason for the initial delay is to move the recomposition pieces away from any tough/ripple
 * behavior of a composable, to make the resulting results clearer.
 */
@Preview
@Composable
fun RecompositionDemo() {
    Column() {
        Row(Modifier.padding(8.dp)) {
            Text("Empty Box")
            BoxElement()
        }
        Row(Modifier.padding(8.dp)) {
            Text("No-Ripple Box")
            NoRippleBoxElement()
        }
        Row(Modifier.padding(8.dp)) {
            Text("Button")
            ButtonElement()
        }
    }
}

// How many times the test will run
val Iterations = 10

// How long to delay between each iteration
val PerFrameDelay = 100L

// How long to delay (after tap) before first recomposition
val InitialDelay = 1000L

/**
 * This hierarchy consists of just a single, empty Box composable.
 */
@Composable
fun BoxElement() {
    var iteration by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .background(Color.Blue)
        .clickable {
            scope.launch {
                // Note the initial delay to delineate the recomposition from other UI behavior
                // such as touch input and ripple
                delay(InitialDelay)
                while (iteration < Iterations) {
                    delay(PerFrameDelay)
                    iteration++
                }
            }
            iteration = 0
        })
}

/**
 * This hierarchy consists of just a single, empty Box composable. The default click
 * indication is disabled to ensure that there will be no ripple. This is useful for testing
 * the result of touch input when the element is tapped (separate from the ripple animation
 * behavior).
 */
@Composable
fun NoRippleBoxElement() {
    var iteration by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .background(Color.Blue)
        .clickable(interactionSource = interactionSource, indication = null) {
            scope.launch {
                delay(InitialDelay)
                while (iteration < Iterations) {
                    delay(PerFrameDelay)
                    iteration++
                }
            }
            iteration = 0
        })
}

/**
 * This hierarchy consists of just a single, empty Button composable, to see whether recomposition
 * of a Button is significantly different from other composables.
 */
@Composable
fun ButtonElement() {
    var iteration by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    Button(modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .background(Color.Blue),
        onClick = {
            scope.launch {
                delay(InitialDelay)
                while (iteration < Iterations) {
                    delay(PerFrameDelay)
                    iteration++
                }
                iteration = 0
            }
        }) {
        Text(text = "Button")
    }
}
