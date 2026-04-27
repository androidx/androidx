/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.forEachPixel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.test.IgnoreJsTarget
import kotlinx.test.IgnoreWasmTarget

@Suppress("DEPRECATION")
@OptIn(ExperimentalTestApi::class)
class BasicTextFieldSelectionHandleTest {
    private val textPadding = 30.dp
    private val selectionColor = TextSelectionColors(
        handleColor = Color.Red,
        backgroundColor = Color.White
    )
    private val textStyle = TextStyle(
        color = Color.White,
        background = Color.White,
        fontSize = 10.sp
    )

    private val textModifier = Modifier
        .background(Color.White)
        .padding(textPadding)
        .fillMaxSize()

    @Test
    fun basicTextFieldSelectionHandles() = runSkikoComposeUiTest(size = Size(100f, 100f)) {
        val textState = TextFieldState(initialText = "TextText")

        var selectionStart: Rect = Rect.Zero
        var selectionEnd: Rect = Rect.Zero

        setContent {
            SetInitialTouchInputMode()
            CompositionLocalProvider(
                value = LocalTextSelectionColors provides selectionColor,
            ) {
                BasicTextField(
                    state = textState,
                    modifier = textModifier,
                    textStyle = textStyle,
                    onTextLayout = {
                        selectionStart = it()?.getCursorRect(textState.selection.start) ?: Rect.Zero
                        selectionEnd = it()?.getCursorRect(textState.selection.end) ?: Rect.Zero
                    }
                )
            }
        }

        onNode(hasSetTextAction()).performTouchInput {
            // Simulate gesture to focus and select text on text field
            down(center)
            up()
        }
        textState.edit {
            selection = TextRange(start = 1, end = 6)
        }
        waitForIdle()

        // Check that both selection handlers exist
        onAllNodes(SemanticsMatcher.keyIsDefined(SelectionHandleInfoKey)).assertCountEquals(2)

        val offset = with(density) {
            Offset(textPadding.toPx(), textPadding.toPx())
        }

        captureToImage().assertHandlers(
            left = TestHandleShape(
                cursor = selectionStart.translate(offset),
                isStartHandler = true
            ),
            right = TestHandleShape(
                cursor = selectionEnd.translate(offset),
                isStartHandler = false
            )
        )
    }

    @Test
    // FIXME https://youtrack.jetbrains.com/issue/CMP-8803
    @IgnoreJsTarget
    @IgnoreWasmTarget
    fun coreTextFieldSelectionHandles() = runSkikoComposeUiTest(size = Size(100f, 100f)) {
        val selection = TextRange(1, 6)
        var selectionStart: Rect = Rect.Zero
        var selectionEnd: Rect = Rect.Zero
        val textFieldValue = mutableStateOf(TextFieldValue(text = "Text Text", selection = selection))

        setContent {
            SetInitialTouchInputMode()
            CompositionLocalProvider(
                value = LocalTextSelectionColors provides selectionColor,
            ) {
                CoreTextField(
                    value = textFieldValue.value,
                    onValueChange = { textFieldValue.value = it },
                    modifier = textModifier,
                    textStyle = textStyle,
                    onTextLayout = {
                        selectionStart = it.getCursorRect(textFieldValue.value.selection.start)
                        selectionEnd = it.getCursorRect(textFieldValue.value.selection.end)
                    }
                )
            }
        }

        onNode(hasSetTextAction()).performTouchInput {
            // Simulate gesture to focus and select text on text field
            down(Offset.Zero)
            up()

            down(Offset.Zero)
            move(1000)
            up()
        }
        textFieldValue.value = TextFieldValue(text = "TextText", selection = selection)

        waitForIdle()

        // Check that both selection handlers exist
        onAllNodes(SemanticsMatcher.keyIsDefined(SelectionHandleInfoKey)).assertCountEquals(2)

        val offset = with(density) {
            Offset(textPadding.toPx(), textPadding.toPx())
        }

        captureToImage().assertHandlers(
            left = TestHandleShape(
                cursor = selectionStart.translate(offset),
                isStartHandler = true
            ),
            right = TestHandleShape(
                cursor = selectionEnd.translate(offset),
                isStartHandler = false
            )
        )
    }

    @Composable
    fun SetInitialTouchInputMode() {
        val inputModeManager = LocalInputModeManager.current
        LaunchedEffect(inputModeManager) {
            inputModeManager.requestInputMode(InputMode.Touch)
        }
    }

    private fun SkikoComposeUiTest.TestHandleShape(
        cursor: Rect,
        isStartHandler: Boolean,
    ) = PlatformSelectionHandleShape(density, cursor, isStartHandler)

    private fun ImageBitmap.assertHandlers(
        left: SelectionHandleShape,
        right: SelectionHandleShape,
    ) {
        val shapes = listOf(left, right)
        forEachPixel { color, offset ->
            if (shapes.any { it.isInside(offset) }) {
                assertEquals(Color.Red, color, "Expected $offset to be red, but was $color")
            } else if (shapes.all { it.isOutside(offset) }) {
                assertNotEquals(Color.Red, color, "Expected $offset to be not red, but was $color")
            }
        }
    }
}
