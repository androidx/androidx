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
package androidx.compose.ui.demos.keyinput

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.state
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.foundation.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.focus
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focusObserver
import androidx.compose.ui.focusRequester
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.ui.input.key.keyInputFilter

@Composable
fun KeyInputDemo() {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        CenteredRow {
            Text(
                "Click on any item to bring it into focus. \nThen type using a hardware keyboard."
            )
        }
        CenteredRow {
            FocusableText(state { "Enter Text Here" })
        }
        CenteredRow {
            FocusableText(state { "Enter Text Here" })
        }
        CenteredRow {
            FocusableText(state { "Enter Text Here" })
        }
    }
}

@Composable
@OptIn(
    ExperimentalFocus::class,
    ExperimentalKeyInput::class
)
private fun FocusableText(text: MutableState<String>) {
    var color by state { Color.Black }
    val focusRequester = FocusRequester()
    Text(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusObserver { color = if (it.isFocused) Color.Green else Color.Black }
            .focus()
            .tapGestureFilter { focusRequester.requestFocus() }
            .keyInputFilter { it.value?.let { text.value += it; true } ?: false },
        text = text.value,
        color = color
    )
}

@Composable
private fun CenteredRow(children: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        children = children
    )
}

@OptIn(ExperimentalKeyInput::class)
private val KeyEvent.value: String?
    get() {
        if (type != KeyEventType.KeyUp) return null
        return when (key) {
            Key.A -> "a"
            Key.B -> "b"
            Key.C -> "c"
            Key.D -> "d"
            Key.E -> "e"
            Key.F -> "f"
            Key.G -> "g"
            Key.H -> "h"
            Key.I -> "i"
            Key.J -> "j"
            Key.K -> "k"
            Key.L -> "l"
            Key.M -> "m"
            Key.N -> "n"
            Key.O -> "o"
            Key.P -> "p"
            Key.Q -> "q"
            Key.R -> "r"
            Key.S -> "s"
            Key.T -> "t"
            Key.U -> "u"
            Key.V -> "v"
            Key.W -> "w"
            Key.X -> "x"
            Key.Y -> "y"
            Key.Z -> "z"
            Key.Spacebar -> " "
            else -> null
        }
    }
