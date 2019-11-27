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

package androidx.ui.animation.samples

import androidx.animation.transitionDefinition
import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.Transition
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color

private enum class State {
    First,
    Second
}

@Sampled
fun TransitionSample() {
    val ColorKey = ColorPropKey()
    val definition = transitionDefinition {
        state(State.First) {
            this[ColorKey] = Color.Red
        }
        state(State.Second) {
            this[ColorKey] = Color.Green
        }
    }

    @Composable
    fun TransitionBasedColoredRect() {
        // This puts the transition in State.First. Any subsequent state change will trigger a
        // transition animation, as defined in the transition definition.
        Transition(definition = definition, toState = State.First) { state ->
            ColoredRect(color = state[ColorKey])
        }
    }

    @Composable
    fun ColorRectWithInitState() {
        // This starts the transition going from State.First to State.Second when this composable
        // gets composed for the first time.
        Transition(definition = definition, initState = State.First, toState = State.Second) {
                state ->
            ColoredRect(color = state[ColorKey])
        }
    }
}
