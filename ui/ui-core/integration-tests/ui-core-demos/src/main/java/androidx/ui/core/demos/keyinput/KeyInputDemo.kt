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
import androidx.ui.focus.FocusModifier
import androidx.ui.focus.FocusState.Focused
import androidx.ui.focus.FocusState.NotFocusable
import androidx.ui.focus.FocusState.NotFocused
import androidx.ui.focus.focusState
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
            Text("WIP: Click on any item to bring it into focus:")
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
        modifier = focusModifier.tapGestureFilter { focusModifier.requestFocus() },
        text = text.value,
        color = when (focusModifier.focusState) {
            Focused -> Color.Green
            NotFocused -> Color.Black
            NotFocusable -> Color.Gray
        }
    )
}

@Composable
private fun CenteredRow(children: @Composable() RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        children = children
    )
}
