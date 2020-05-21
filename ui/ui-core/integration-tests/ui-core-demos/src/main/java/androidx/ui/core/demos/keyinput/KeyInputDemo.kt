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

package androidx.ui.core.demos.keyinput

import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.core.focus.FocusModifier
import androidx.ui.core.focus.FocusState.Focused
import androidx.ui.core.focus.FocusState.NotFocusable
import androidx.ui.core.focus.FocusState.NotFocused
import androidx.ui.core.focus.focusState
import androidx.ui.core.keyinput.Key
import androidx.ui.core.keyinput.KeyEvent
import androidx.ui.core.keyinput.KeyEventType
import androidx.ui.core.keyinput.keyInputFilter
import androidx.ui.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.RowScope
import androidx.ui.layout.fillMaxWidth

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
private fun FocusableText(text: MutableState<String>) {
    val focusModifier = FocusModifier()
    Text(
        modifier = focusModifier
            .tapGestureFilter { focusModifier.requestFocus() }
            .keyInputFilter { it.value?.let { text.value += it; true } ?: false },
        text = text.value,
        color = when (focusModifier.focusState) {
            Focused -> Color.Green
            NotFocused -> Color.Black
            NotFocusable -> Color.Gray
        }
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
