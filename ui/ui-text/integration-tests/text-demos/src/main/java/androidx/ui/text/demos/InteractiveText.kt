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

package androidx.ui.text.demos

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Text
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.Column
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextStyle

@Composable
fun InteractiveTextDemo() {
    TextOnClick()
}

@Composable
fun TextOnClick() {
    val layoutResult = state<TextLayoutResult?> { null }
    val clickedOffset = state { -1 }
    VerticalScroller {
        Column {
            Text("Clicked Offset: ${clickedOffset.value}")
            PressGestureDetector(
                onPress = { pos ->
                    layoutResult.value?.let { layout ->
                        clickedOffset.value = layout.getOffsetForPosition(pos)
                    }
                }
            ) {
                Text("ClickeMe",
                    style = TextStyle(fontSize = fontSize8),
                    onTextLayout = { layoutResult.value = it })
            }
        }
    }
}