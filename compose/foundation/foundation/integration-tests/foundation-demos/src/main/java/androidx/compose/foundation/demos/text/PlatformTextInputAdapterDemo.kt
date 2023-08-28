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

package androidx.compose.foundation.demos.text

import android.content.Context
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.textInputSession
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.inputmethod.EditorInfoCompat
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
                Image(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "backward")
            }
            IconButton(onClick = {
                val newCursor = (textFieldState.selection.end + 1)
                    .coerceIn(0, textFieldState.buffer.length)
                textFieldState.selection = TextRange(newCursor)
            }) {
                Image(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "forward")
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
            .then(WackyTextFieldModifierElement(state))
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

@Suppress("ModifierNodeInspectableProperties")
private data class WackyTextFieldModifierElement(val state: WackyTextState) :
    ModifierNodeElement<WackyTextFieldModifierNode>() {
    override fun create() = WackyTextFieldModifierNode(state)
    override fun update(node: WackyTextFieldModifierNode) {}
}

private class WackyTextFieldModifierNode(private val state: WackyTextState) : Modifier.Node(),
    PlatformTextInputModifierNode,
    FocusEventModifierNode,
    FocusRequesterModifierNode,
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {

    private var isFocused = false
    private var job: Job? = null

    override fun onFocusEvent(focusState: FocusState) {
        if (isFocused == focusState.isFocused) return
        isFocused = focusState.isFocused
        if (isFocused) {
            job = coroutineScope.launch {

                // In a real app, creating this session would be platform-specific code.
                // This will cancel any previous request.
                textInputSession {
                    val imm = view.context
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                    launch {
                        snapshotFlow { state.selection }.collectLatest { selection ->
                            imm.updateSelection(view, selection.start, selection.end, 0, 0)
                        }
                    }

                    startInputMethod { outAttrs ->
                        Log.d(TAG, "creating input connection for $state")

                        outAttrs.initialSelStart = state.buffer.length
                        outAttrs.initialSelEnd = state.buffer.length
                        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
                        EditorInfoCompat.setInitialSurroundingText(outAttrs, state.toString())
                        outAttrs.imeOptions =
                            EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_FULLSCREEN
                        state.refresh = Unit
                        WackyInputConnection(state, view)
                    }
                }
            }
        } else {
            job?.cancel()
            job = null
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass == PointerEventPass.Main && pointerEvent.changes.any { it.changedToUp() }) {
            if (isFocused) {
                currentValueOf(LocalSoftwareKeyboardController)?.show()
            } else {
                requestFocus()
            }
        }
    }

    override fun onCancelPointerInput() {
        // Noop, would handle in a real text field.
    }
}

/**
 * This class can mostly be ignored for the sake of this demo.
 *
 * This is where most of the actual communication with the Android IME system APIs is. It is
 * an implementation of the Android interface [InputConnection], which is a very large and
 * complex interface to implement. Here we use the [BaseInputConnection] class to avoid
 * implementing the whole thing from scratch, and then only make very weak attempts at handling
 * all the edge cases a real-world text editor would need to handle.
 */
private class WackyInputConnection(
    private val state: WackyTextState,
    view: View
) : BaseInputConnection(view, false) {
    private var composition: TextRange? = null

    private var batchLevel = 0
    private val batch = mutableVectorOf<() -> Unit>()

    // region InputConnection

    override fun beginBatchEdit(): Boolean {
        batchLevel++
        return true
    }

    override fun endBatchEdit(): Boolean {
        batchLevel--
        if (batchLevel == 0) {
            Log.d(TAG, "ending batch edit")
            batch.forEach { it() }
            batch.clear()
            state.refresh = Unit
        }
        return true
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        Log.d(TAG, "committing text: text=\"$text\", newCursorPosition=$newCursorPosition")
        @Suppress("NAME_SHADOWING")
        val text = text.toString()
        withBatch {
            state.selection = if (composition != null) {
                state.buffer.replace(composition!!.start, composition!!.end, text)
                TextRange(composition!!.end)
            } else {
                state.buffer.replace(state.selection.start, state.selection.end, text)
                TextRange(state.selection.start + text.length)
            }
        }
        return true
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        Log.d(TAG, "setting composing region: start=$start, end=$end")
        withBatch {
            composition =
                TextRange(
                    start.coerceIn(0, state.buffer.length),
                    end.coerceIn(0, state.buffer.length)
                )
        }
        return true
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        Log.d(
            TAG,
            "setting composing text: text=\"$text\", newCursorPosition=$newCursorPosition"
        )
        @Suppress("NAME_SHADOWING")
        val text = text.toString()
        withBatch {
            if (composition != null) {
                state.buffer.replace(composition!!.start, composition!!.end, text)
                if (text.isNotEmpty()) {
                    composition =
                        TextRange(composition!!.start, composition!!.start + text.length)
                }
                state.selection = TextRange(composition!!.end)
            } else {
                state.buffer.replace(state.selection.start, state.selection.end, text)
                if (text.isNotEmpty()) {
                    composition =
                        TextRange(state.selection.start, state.selection.start + text.length)
                }
                state.selection = TextRange(state.selection.start + text.length)
            }
        }
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        Log.d(
            TAG,
            "deleting surrounding text: beforeLength=$beforeLength, afterLength=$afterLength"
        )
        withBatch {
            state.buffer.delete(
                state.selection.end.coerceIn(0, state.buffer.length),
                (state.selection.end + afterLength).coerceIn(0, state.buffer.length)
            )
            state.buffer.delete(
                (state.selection.start - beforeLength).coerceIn(0, state.buffer.length),
                state.selection.start.coerceIn(0, state.buffer.length)
            )
        }
        return false
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        Log.d(TAG, "setting selection: start=$start, end=$end")
        withBatch {
            state.selection = TextRange(
                start.coerceIn(0, state.buffer.length),
                end.coerceIn(0, state.buffer.length)
            )
        }
        return true
    }

    override fun finishComposingText(): Boolean {
        Log.d(TAG, "finishing composing text")
        withBatch {
            composition = null
        }
        return true
    }

    override fun closeConnection() {
        Log.d(TAG, "closing input connection")
        // This calls finishComposingText, so don't clear the batch until after.
        super.closeConnection()
        batch.clear()
    }

    // endregion

    private inline fun withBatch(crossinline block: () -> Unit) {
        beginBatchEdit()
        block()
        endBatchEdit()
    }
}
