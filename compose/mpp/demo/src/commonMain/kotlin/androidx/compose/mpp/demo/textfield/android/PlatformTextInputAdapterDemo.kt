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

package androidx.compose.mpp.demo.textfield.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusRequesterModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "WackyInput"

@Composable
fun PlatformTextInputAdapterDemo() {
    val textFieldState = remember { WackyTextState("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            var value by remember { mutableStateOf("") }
            Text("Standard text field: ")
            TextField(value = value, onValueChange = { value = it })
        }
        Divider()
        Row {
            Text("From-scratch text field: ")
            WackyTextField(textFieldState, Modifier.weight(1f))
        }

        // Cursor movement controls
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Move selection:")
            IconButton(onClick = {
                val newCursor = (textFieldState.selection.start - 1)
                    .coerceIn(0, textFieldState.buffer.length)
                textFieldState.selection = TextRange(newCursor)
            }) {
                Image(Icons.Default.KeyboardArrowLeft, contentDescription = "left")
            }
            IconButton(onClick = {
                val newCursor = (textFieldState.selection.end + 1)
                    .coerceIn(0, textFieldState.buffer.length)
                textFieldState.selection = TextRange(newCursor)
            }) {
                Image(Icons.Default.KeyboardArrowRight, contentDescription = "right")
            }
        }
    }
}

@Composable
fun WackyTextField(state: WackyTextState, modifier: Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var textLayoutResult: TextLayoutResult? by remember { mutableStateOf(null) }

    BasicText(
        text = state.toString(),
        onTextLayout = { textLayoutResult = it },
        modifier = modifier
            .border(if (isFocused) 2.dp else 1.dp, Color.Gray.copy(alpha = 0.5f))
            // The modifier element that produces the PlatformTextInputModifierNode must come before
            // or be the same as the focus target (i.e. focusable).
//            .then(WackyTextFieldModifierElement(state)) // TODO uncomment after merging with newer androidx-main branch
            .focusable(interactionSource = interactionSource)
            .drawWithContent {
                drawContent()

                if (isFocused) {
                    textLayoutResult?.let {
                        if (state.selection.collapsed) {
                            val cursorRect = it.getCursorRect(state.selection.start)
                            drawLine(
                                Color.Black,
                                start = cursorRect.topCenter,
                                end = cursorRect.bottomCenter,
                                strokeWidth = 1.dp.toPx()
                            )
                        } else {
                            val selectionPath =
                                it.getPathForRange(state.selection.start, state.selection.end)
                            drawPath(selectionPath, Color.Blue.copy(alpha = 0.5f))
                        }
                    }
                }
            }
    )
}

class WackyTextState(initialValue: String) {
    var refresh by mutableStateOf(Unit, neverEqualPolicy())
    val buffer = StringBuilder(initialValue)
    var selection by mutableStateOf(TextRange(0))

    override fun toString(): String {
        refresh
        return buffer.toString()
    }
}
