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

import androidx.compose.foundation.assertPixels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.test.Ignore
import kotlin.test.Test
import kotlinx.test.IgnoreJsTarget
import kotlinx.test.IgnoreWasmTarget

@OptIn(ExperimentalTestApi::class)
class BasicTextFieldSelectionHandleTest {
    private val textPadding = 14.dp
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

    @Ignore // TODO: https://youtrack.jetbrains.com/issue/CMP-9990/Fix-basicTextFieldSelectionHandles-test
    @Test
    fun basicTextFieldSelectionHandles() = runSkikoComposeUiTest(size = Size(100f, 100f)) {
        val textState = TextFieldState(initialText = "Text")

        var selectionStart: Rect = Rect.Zero
        var selectionEnd: Rect = Rect.Zero

        setContent {
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
            selection = TextRange(start = 1, end = 3)
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
        val selection = TextRange(1, 3)
        var selectionStart: Rect = Rect.Zero
        var selectionEnd: Rect = Rect.Zero
        val textFieldValue = mutableStateOf(TextFieldValue(text = "Text", selection = selection))

        setContent {
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
        textFieldValue.value = TextFieldValue(text = "Text", selection = selection)

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

    private fun SkikoComposeUiTest.TestHandleShape(
        cursor: Rect,
        isStartHandler: Boolean,
        lineWidth: Dp = 2.dp,
        circleRadius: Dp = 6.dp,
    ) = density.run {
        val lineRect = cursor.copy(
            left = cursor.bottomCenter.x - lineWidth.toPx() / 2,
            right = cursor.bottomCenter.x + lineWidth.toPx() / 2,
        )
        val circleCenter = Offset(
            x = cursor.bottomCenter.x,
            y = if (isStartHandler) {
                cursor.top - circleRadius.toPx()
            } else {
                cursor.bottom + circleRadius.toPx()
            }
        )
        val circleRect = Rect(center = circleCenter, radius = circleRadius.toPx())

        SelectionHandleShape(lineRect, circleRect)
    }

    private data class SelectionHandleShape(
        val lineRect: Rect,
        val circleRect: Rect
    ) {
        fun containsInner(point: IntOffset): Boolean =
            lineRect.roundToIntRect().contains(point) ||
                circleRect.roundToIntRect().deflate(1).containsInOval(point)

        fun containsOuter(point: IntOffset): Boolean =
            lineRect.roundToIntRect().contains(point) ||
                circleRect.roundToIntRect().inflate(1).containsInOval(point)

        private fun IntRect.containsInOval(point: IntOffset): Boolean {
            val normX = (point.x + 0.5 - center.x) / (width / 2)
            val normY = (point.y + 0.5 - center.y) / (height / 2)
            return normX.pow(2) + normY.pow(2) <= 1.0
        }
    }

    private fun ImageBitmap.assertHandlers(
        left: SelectionHandleShape,
        right: SelectionHandleShape,
    ) {
        val shapes = listOf(left, right)
        assertPixels { offset ->
            if (shapes.any { it.containsInner(offset) }) {
                Color.Red
            } else if (shapes.any { it.containsOuter(offset) }) {
                null
            } else {
                Color.White
            }
        }
    }
}
