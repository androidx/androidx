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

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

private val modifierKeys = setOf(
    NativeKeyEvent.KEYCODE_SHIFT_LEFT,
    NativeKeyEvent.KEYCODE_SHIFT_RIGHT,
    NativeKeyEvent.KEYCODE_ALT_LEFT,
    NativeKeyEvent.KEYCODE_ALT_RIGHT,
    NativeKeyEvent.KEYCODE_CTRL_LEFT,
    NativeKeyEvent.KEYCODE_CTRL_RIGHT,
    NativeKeyEvent.KEYCODE_META_LEFT,
    NativeKeyEvent.KEYCODE_META_RIGHT,
)

private val KeyEvent.keyCode get() = nativeKeyEvent.keyCode

private val demoInstructionText =
    """Navigate the below text fields using the (shift)-tab keys on a physical keyboard.
        | We expect the focus to move forward and backwards,
        | Arrow keys should move the cursor around the currently focused text field
        | (unless using a dpad device).
        | IME action is also set to next,
        | so the enter key ought to move the focus to the next focus element.
        | In multi-line, the tab and enter keys should add '\t' and '\n', respectively.
        |"""
        .trimMargin().replace("\n", "")

private val keyIndicatorInstructionText =
    """The keys being pressed and their modifiers are shown below.
        | Keys that are currently being pressed are in red text."""
        .trimMargin().replace("\n", "")

@Composable
fun TextFieldFocusDemo() {
    val keys = remember { mutableStateListOf<KeyState>() }

    val onKeyDown: (KeyEvent) -> Unit = { event ->
        if (keys.none { it.keyEvent.keyCode == event.keyCode && !it.isUp }) {
            keys.add(0, KeyState(event))
            if (keys.size > 10) {
                keys.removeLast()
            }
        }
    }

    val onKeyUp: (KeyEvent) -> Unit = { event ->
        keys
            .indexOfFirst { it.keyEvent.keyCode == event.keyCode }
            .takeUnless { it == -1 }
            ?.let { keys[it] = keys[it].copy(isUp = true) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(10.dp)
            .onPreviewKeyEvent { event ->
                if (event.keyCode !in modifierKeys) {
                    when (event.type) {
                        KeyEventType.KeyDown -> onKeyDown(event)
                        KeyEventType.KeyUp -> onKeyUp(event)
                    }
                }
                false // don't consume the event, we just want to observe it
            },
    ) {
        val multiLine = rememberSaveable { mutableStateOf(false) }
        Text(demoInstructionText)
        SingleLineToggle(multiLine)
        val hideSoftKeyboardProvide = LocalTextInputService provides null
        CompositionLocalProvider(hideSoftKeyboardProvide) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DemoTextField("Up", multiLine)
                Row {
                    DemoTextField("Left", multiLine)
                    DemoTextField("Center", multiLine, startWithFocus = true)
                    DemoTextField("Right", multiLine)
                }
                DemoTextField("Down", multiLine)
            }
        }
        Text(keyIndicatorInstructionText)
        KeyPressList(keys)
    }
}

@Composable
private fun SingleLineToggle(multiLineState: MutableState<Boolean>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Single-line")
        Switch(
            checked = multiLineState.value,
            onCheckedChange = { multiLineState.value = it }
        )
        Text("Multi-line")
    }
}

@Composable
private fun DemoTextField(
    initText: String,
    multiLineState: MutableState<Boolean>,
    startWithFocus: Boolean = false
) {
    var modifier = Modifier
        .padding(6.dp)
        .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
        .padding(6.dp)

    if (startWithFocus) {
        val focusRequester = rememberSaveable { FocusRequester() }
        modifier = modifier.focusRequester(focusRequester)
        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }
    }

    var text by remember(multiLineState.value) {
        mutableStateOf(
            if (multiLineState.value) "$initText line 1\n$initText line 2" else initText
        )
    }

    BasicTextField(
        value = text,
        onValueChange = { text = it },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        singleLine = !multiLineState.value,
        modifier = modifier,
    )
}

private data class KeyState(
    val keyEvent: KeyEvent,
    val downTime: Long = System.nanoTime(),
    val isUp: Boolean = false
)

@Composable
private fun KeyPressList(keys: SnapshotStateList<KeyState>) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
    ) {
        items(keys, key = { it.downTime }) { keyState ->
            AnimatedVisibility(
                visible = !keyState.isUp,
                enter = fadeIn(tween(durationMillis = 100)),
                exit = fadeOut(tween(durationMillis = 1_000)),
            ) {
                val event = keyState.keyEvent
                val ctrl = if (event.isCtrlPressed) "CTRL + " else ""
                val alt = if (event.isAltPressed) "ALT + " else ""
                val shift = if (event.isShiftPressed) "SHIFT + " else ""
                val meta = if (event.isMetaPressed) "META + " else ""
                Text(
                    text = ctrl + alt + shift + meta +
                        NativeKeyEvent.keyCodeToString(event.keyCode)
                            .replace("KEYCODE_", "")
                            .replace("DPAD_", ""),
                    color = if (keyState.isUp) Color.Unspecified else Color.Red,
                )
            }
        }
    }
}
