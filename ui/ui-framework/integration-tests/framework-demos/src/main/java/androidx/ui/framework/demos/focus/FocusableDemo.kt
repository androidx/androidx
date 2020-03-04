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

package androidx.ui.framework.demos.focus

import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.focus.FocusState.Focused
import androidx.ui.focus.FocusState.NotFocusable
import androidx.ui.focus.FocusState.NotFocused
import androidx.ui.focus.Focusable
import androidx.ui.graphics.Color
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.RowScope
import androidx.ui.text.TextStyle

@Composable
fun FocusableDemo() {
    Focusable {
        Column(arrangement = Arrangement.SpaceEvenly) {
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
}

@Composable
private fun FocusableText(text: String) {
    Focusable { focus ->
        Text(
            modifier = PressIndicatorGestureDetector(onStart = { focus.requestFocus() }),
            text = text,
            style = TextStyle(
                color = when (focus.focusState) {
                    Focused -> Color.Green
                    NotFocused -> Color.Black
                    NotFocusable -> Color.Gray
                }
            )
        )
    }
}

@Composable
private fun CenteredRow(children: @Composable() RowScope.() -> Unit) {
    Row(modifier = LayoutWidth.Fill, arrangement = Arrangement.Center, children = children)
}