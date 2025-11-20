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
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent as NativeKeyEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.collections.removeLast as removeLastKt

private val modifierKeys =
    setOf(
        NativeKeyEvent.KEYCODE_SHIFT_LEFT,
        NativeKeyEvent.KEYCODE_SHIFT_RIGHT,
        NativeKeyEvent.KEYCODE_ALT_LEFT,
        NativeKeyEvent.KEYCODE_ALT_RIGHT,
        NativeKeyEvent.KEYCODE_CTRL_LEFT,
        NativeKeyEvent.KEYCODE_CTRL_RIGHT,
        NativeKeyEvent.KEYCODE_META_LEFT,
        NativeKeyEvent.KEYCODE_META_RIGHT,
    )

private val KeyEvent.keyCode
    get() = nativeKeyEvent.keyCode

private val demoInstructionText =
    """Navigate the below text fields using the (shift)-tab keys on a physical keyboard.
        | We expect the focus to move forward and backwards,
        | Arrow keys should move the cursor around the currently focused text field
        | (unless using a dpad device).
        | IME action is also set to next,
        | so the enter key ought to move the focus to the next focus element.
        | In multi-line, the tab and enter keys should add '\t' and '\n', respectively.
        |"""
        .trimMargin()
        .replace("\n", "")

private val keyIndicatorInstructionText =
    """The keys being pressed and their modifiers are shown below.
        | Keys that are currently being pressed are in red text."""
        .trimMargin()
        .replace("\n", "")

@Composable
fun TextFieldFocusDemo() {
    TextFieldFocusDemo(useBtf2 = false)
}

@Composable
fun BasicTextFieldFocusDemo() {
    TextFieldFocusDemo(useBtf2 = true)
}

@Composable
private fun TextFieldFocusDemo(useBtf2: Boolean) {
    val keys = remember { mutableStateListOf<KeyState>() }

    val onKeyDown: (KeyEvent) -> Unit = { event ->
        if (keys.none { it.keyEvent.keyCode == event.keyCode && !it.isUp }) {
            keys.add(0, KeyState(event))
            if (keys.size > 10) {
                keys.removeLastKt()
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
        modifier =
            Modifier.safeDrawingPadding()
                .verticalScroll(rememberScrollState())
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
        val (multiLine, setMultiLine) = rememberSaveable { mutableStateOf(false) }
        var isEditText by rememberSaveable { mutableStateOf(false) }
        Text(demoInstructionText)
        SingleLineToggle(checked = multiLine, onCheckedChange = setMultiLine)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TextField")
            Switch(isEditText, { isEditText = it })
            Text("EditText")
        }
        if (isEditText) {
            key(multiLine) { AndroidView({ createPlusLayout(it, multiLine) }) }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DemoTextField("Up", multiLine, useBtf2 = useBtf2)
                Row {
                    DemoTextField("Left", multiLine, useBtf2 = useBtf2)
                    DemoTextField("Center", multiLine, useBtf2 = useBtf2, startWithFocus = true)
                    DemoTextField("Right", multiLine, useBtf2 = useBtf2)
                }
                DemoTextField("Down", multiLine, useBtf2 = useBtf2)
            }
        }
        Text(keyIndicatorInstructionText)
        KeyPressList(keys)
    }
}

@Composable
private fun SingleLineToggle(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Single-line")
        Switch(checked, onCheckedChange)
        Text("Multi-line")
    }
}

@Composable
private fun DemoTextField(
    initText: String,
    multiLine: Boolean,
    useBtf2: Boolean,
    startWithFocus: Boolean = false,
) {
    var modifier =
        Modifier.padding(6.dp).border(1.dp, Color.LightGray, RoundedCornerShape(6.dp)).padding(6.dp)

    if (startWithFocus) {
        val focusRequester = remember { FocusRequester() }
        modifier = modifier.focusRequester(focusRequester)
        LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    }

    if (useBtf2) {
        val state =
            key(multiLine) {
                rememberTextFieldState(
                    if (multiLine) "$initText line 1\n$initText line 2" else initText
                )
            }

        BasicTextField(
            state = state,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            lineLimits =
                if (multiLine) TextFieldLineLimits.Default else TextFieldLineLimits.SingleLine,
            modifier = modifier,
        )
    } else {
        var text by
            remember(multiLine) {
                mutableStateOf(if (multiLine) "$initText line 1\n$initText line 2" else initText)
            }

        BasicTextField(
            value = text,
            onValueChange = { text = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = !multiLine,
            modifier = modifier,
        )
    }
}

private data class KeyState(
    val keyEvent: KeyEvent,
    val downTime: Long = System.nanoTime(),
    val isUp: Boolean = false,
)

@Composable
private fun KeyPressList(keys: List<KeyState>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
    ) {
        keys.forEach { keyState ->
            key(keyState.downTime) {
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
                        text =
                            ctrl +
                                alt +
                                shift +
                                meta +
                                NativeKeyEvent.keyCodeToString(event.keyCode)
                                    .replace("KEYCODE_", "")
                                    .replace("DPAD_", ""),
                        color = if (keyState.isUp) Color.Unspecified else Color.Red,
                    )
                }
            }
        }
    }
}

fun createPlusLayout(context: Context, isMultiLine: Boolean = false): ViewGroup {
    // Main vertical LinearLayout
    val mainLayout =
        LinearLayout(context).apply {
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

    // Common EditText configuration
    fun createEditText(text: String): EditText {
        return EditText(context).apply {
            setText(text)

            // Configure single vs multi-line
            if (isMultiLine) {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                setSingleLine(false)
                maxLines = 4
                minLines = 2
            } else {
                inputType = InputType.TYPE_CLASS_TEXT
                setSingleLine(true)
                maxLines = 1
            }

            // Styling
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(24, 16, 24, 16)
            background = context.getDrawable(android.R.drawable.edit_text)
        }
    }

    // Top EditText
    val topEditText =
        createEditText(if (isMultiLine) "Top line 1\nTop line 2" else "Top").apply {
            layoutParams =
                LinearLayout.LayoutParams(300, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = 24
                }
        }

    // Middle horizontal LinearLayout (Left, Center, Right)
    val middleLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams =
                LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    .apply { bottomMargin = 24 }
        }

    // Left EditText
    val leftEditText =
        createEditText(if (isMultiLine) "Left line 1\nLeft line 2" else "Left").apply {
            layoutParams =
                LinearLayout.LayoutParams(300, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = 24
                }
        }

    // Center EditText
    val centerEditText =
        createEditText(if (isMultiLine) "Center line 1\nCenter line 2" else "Center").apply {
            layoutParams = LinearLayout.LayoutParams(300, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

    // Right EditText
    val rightEditText =
        createEditText(if (isMultiLine) "Right line 1\nRight line 2" else "Right").apply {
            layoutParams =
                LinearLayout.LayoutParams(300, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = 24
                }
        }

    // Bottom EditText
    val bottomEditText =
        createEditText(if (isMultiLine) "Bottom line 1\nBottom line 2" else "Bottom").apply {
            layoutParams =
                LinearLayout.LayoutParams(300, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
        }

    // Add EditTexts to middle layout
    middleLayout.addView(leftEditText)
    middleLayout.addView(centerEditText)
    middleLayout.addView(rightEditText)

    // Add all components to main layout
    mainLayout.addView(topEditText)
    mainLayout.addView(middleLayout)
    mainLayout.addView(bottomEditText)

    return mainLayout
}
