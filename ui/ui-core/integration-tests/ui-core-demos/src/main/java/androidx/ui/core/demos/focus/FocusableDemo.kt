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
package androidx.ui.core.demos.focus

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.focus.ExperimentalFocus
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.core.focus.FocusRequester
import androidx.ui.core.focus.focus
import androidx.ui.core.focus.focusObserver
import androidx.ui.core.focus.focusRequester
import androidx.ui.core.focus.isFocused
import androidx.compose.foundation.Text
import androidx.ui.graphics.Color.Companion.Black
import androidx.ui.graphics.Color.Companion.Green
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.RowScope
import androidx.ui.layout.fillMaxWidth

@Composable
fun FocusableDemo() {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        CenteredRow {
            Text("Click on any focusable to bring it into focus:")
        }
        CenteredRow {
            FocusableText("Focusable 1")
        }
        CenteredRow {
            FocusableText("Focusable 2")
        }
        CenteredRow {
            FocusableText("Focusable 3")
        }
    }
}

@Composable
@OptIn(ExperimentalFocus::class)
private fun FocusableText(text: String) {
    var color by state { Black }
    val focusRequester = FocusRequester()
    Text(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusObserver { color = if (it.isFocused) Green else Black }
            .focus()
            .tapGestureFilter { focusRequester.requestFocus() },
        text = text,
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