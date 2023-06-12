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

package androidx.compose.ui.input

import android.graphics.Matrix
import android.view.Choreographer
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.InputMethodManager
import androidx.compose.ui.text.input.RecordingInputConnection
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputServiceAndroid
import androidx.compose.ui.text.input.asExecutor
import androidx.compose.ui.text.input.build
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.fonts.R
import kotlin.math.ceil
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@SmallTest
@RunWith(AndroidJUnit4::class)
class TextInputServiceAndroidCursorAnchorInfoTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val fontFamilyMeasureFont =
        Font(resId = R.font.sample_font, weight = FontWeight.Normal, style = FontStyle.Normal)
            .toFontFamily()

    private lateinit var textInputService: TextInputServiceAndroid
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var inputConnection: RecordingInputConnection

    private val builder = CursorAnchorInfo.Builder()
    private val matrix = Matrix()

    @Before
    fun setup() {
        val view = View(InstrumentationRegistry.getInstrumentation().context)
        inputMethodManager = mock() { on { isActive() } doReturn true }
        // Choreographer must be retrieved on main thread.
        val choreographer = Espresso.onIdle { Choreographer.getInstance() }
        textInputService =
            TextInputServiceAndroid(
                view,
                inputMethodManager,
                inputCommandProcessorExecutor = choreographer.asExecutor()
            )
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )
        inputConnection =
            textInputService.createInputConnection(EditorInfo()) as RecordingInputConnection
    }

    @Test
    fun requestCursorUpdates_immediate() {
        val textFieldValue =
            TextFieldValue("abc", selection = TextRange(2), composition = TextRange(1, 2))
        textInputService.updateState(oldValue = textFieldValue, newValue = textFieldValue)

        val textLayoutResult = getTextLayoutResult(textFieldValue.text)
        var textLayoutPositionInWindow = Offset(1f, 1f)
        val innerTextFieldBounds = Rect.Zero
        val decorationBoxBounds = Rect.Zero
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        inputConnection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE)

        // Immediate update
        matrix.reset()
        matrix.postTranslate(textLayoutPositionInWindow.x, textLayoutPositionInWindow.y)
        val expected =
            builder.build(
                textFieldValue,
                textLayoutResult,
                matrix,
                innerTextFieldBounds,
                decorationBoxBounds
            )
        verify(inputMethodManager).updateCursorAnchorInfo(expected)

        clearInvocations(inputMethodManager)
        textLayoutPositionInWindow = Offset(2f, 2f)
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        // No further updates since monitoring is off
        verify(inputMethodManager, never()).updateCursorAnchorInfo(any())
    }

    @Test
    fun requestCursorUpdates_immediate_beforeUpdateTextLayoutResult() {
        val textFieldValue =
            TextFieldValue("abc", selection = TextRange(2), composition = TextRange(1, 2))
        textInputService.updateState(oldValue = textFieldValue, newValue = textFieldValue)

        inputConnection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE)

        // No immediate update until updateTextLayoutResult call
        verify(inputMethodManager, never()).updateCursorAnchorInfo(any())

        val textLayoutResult = getTextLayoutResult(textFieldValue.text)
        var textLayoutPositionInWindow = Offset(1f, 1f)
        val innerTextFieldBounds = Rect.Zero
        val decorationBoxBounds = Rect.Zero
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        // Immediate update
        matrix.reset()
        matrix.postTranslate(textLayoutPositionInWindow.x, textLayoutPositionInWindow.y)
        val expected =
            builder.build(
                textFieldValue,
                textLayoutResult,
                matrix,
                innerTextFieldBounds,
                decorationBoxBounds
            )
        verify(inputMethodManager).updateCursorAnchorInfo(expected)

        clearInvocations(inputMethodManager)
        textLayoutPositionInWindow = Offset(2f, 2f)
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        // No further updates since monitoring is off
        verify(inputMethodManager, never()).updateCursorAnchorInfo(any())
    }

    @Test
    fun requestCursorUpdates_monitor() {
        var textFieldValue =
            TextFieldValue("abc", selection = TextRange(2), composition = TextRange(1, 2))
        textInputService.updateState(oldValue = textFieldValue, newValue = textFieldValue)

        val textLayoutResult = getTextLayoutResult(textFieldValue.text)
        var textLayoutPositionInWindow = Offset(1f, 1f)
        val innerTextFieldBounds = Rect.Zero
        val decorationBoxBounds = Rect.Zero
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        inputConnection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)

        // No immediate update
        verify(inputMethodManager, never()).updateCursorAnchorInfo(any())

        clearInvocations(inputMethodManager)
        textLayoutPositionInWindow = Offset(2f, 2f)
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        // Monitoring update
        matrix.reset()
        matrix.postTranslate(textLayoutPositionInWindow.x, textLayoutPositionInWindow.y)
        val expected =
            builder.build(
                textFieldValue,
                textLayoutResult,
                matrix,
                innerTextFieldBounds,
                decorationBoxBounds
            )
        verify(inputMethodManager).updateCursorAnchorInfo(expected)

        clearInvocations(inputMethodManager)
        textFieldValue = TextFieldValue("ac")
        textInputService.updateState(oldValue = textFieldValue, newValue = textFieldValue)

        // No monitoring update until updateTextLayoutResult call
        verify(inputMethodManager, never()).updateCursorAnchorInfo(any())

        clearInvocations(inputMethodManager)
        textLayoutPositionInWindow = Offset(3f, 3f)
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        // Monitoring update
        matrix.reset()
        matrix.postTranslate(textLayoutPositionInWindow.x, textLayoutPositionInWindow.y)
        val expected2 =
            builder.build(
                textFieldValue,
                textLayoutResult,
                matrix,
                innerTextFieldBounds,
                decorationBoxBounds
            )
        verify(inputMethodManager).updateCursorAnchorInfo(expected2)
    }

    @Test
    fun requestCursorUpdates_immediateAndMonitor() {
        val textFieldValue =
            TextFieldValue("abc", selection = TextRange(2), composition = TextRange(1, 2))
        textInputService.updateState(oldValue = textFieldValue, newValue = textFieldValue)

        val textLayoutResult = getTextLayoutResult(textFieldValue.text)
        var textLayoutPositionInWindow = Offset(1f, 1f)
        val innerTextFieldBounds = Rect.Zero
        val decorationBoxBounds = Rect.Zero
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        inputConnection.requestCursorUpdates(
            InputConnection.CURSOR_UPDATE_IMMEDIATE or InputConnection.CURSOR_UPDATE_MONITOR
        )

        // Immediate update
        matrix.reset()
        matrix.postTranslate(textLayoutPositionInWindow.x, textLayoutPositionInWindow.y)
        val expected =
            builder.build(
                textFieldValue,
                textLayoutResult,
                matrix,
                innerTextFieldBounds,
                decorationBoxBounds
            )
        verify(inputMethodManager).updateCursorAnchorInfo(expected)

        clearInvocations(inputMethodManager)
        textLayoutPositionInWindow = Offset(2f, 2f)
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        // Monitoring update
        matrix.reset()
        matrix.postTranslate(textLayoutPositionInWindow.x, textLayoutPositionInWindow.y)
        val expected2 =
            builder.build(
                textFieldValue,
                textLayoutResult,
                matrix,
                innerTextFieldBounds,
                decorationBoxBounds
            )
        verify(inputMethodManager).updateCursorAnchorInfo(expected2)
    }

    @Test
    fun requestCursorUpdates_cancel() {
        val textFieldValue =
            TextFieldValue("abc", selection = TextRange(2), composition = TextRange(1, 2))
        textInputService.updateState(oldValue = textFieldValue, newValue = textFieldValue)

        val textLayoutResult = getTextLayoutResult(textFieldValue.text)
        var textLayoutPositionInWindow = Offset(1f, 1f)
        val innerTextFieldBounds = Rect.Zero
        val decorationBoxBounds = Rect.Zero
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        inputConnection.requestCursorUpdates(
            InputConnection.CURSOR_UPDATE_IMMEDIATE or InputConnection.CURSOR_UPDATE_MONITOR
        )

        // Immediate update
        verify(inputMethodManager).updateCursorAnchorInfo(any())

        clearInvocations(inputMethodManager)
        inputConnection.requestCursorUpdates(0) // cancel updates

        // No immediate update
        verify(inputMethodManager, never()).updateCursorAnchorInfo(any())

        textLayoutPositionInWindow = Offset(2f, 2f)
        textInputService.updateTextLayoutResult(
            textFieldValue = textFieldValue,
            textLayoutResult = textLayoutResult,
            textLayoutPositionInWindow = textLayoutPositionInWindow,
            innerTextFieldBounds = innerTextFieldBounds,
            decorationBoxBounds = decorationBoxBounds
        )

        // No monitoring update
        verify(inputMethodManager, never()).updateCursorAnchorInfo(any())
    }

    private fun getTextLayoutResult(text: String): TextLayoutResult {
        val width = 1000
        val fontFamilyResolver = createFontFamilyResolver(context)

        val input =
            TextLayoutInput(
                text = AnnotatedString(text),
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                placeholders = listOf(),
                maxLines = Int.MAX_VALUE,
                softWrap = true,
                overflow = TextOverflow.Visible,
                density = defaultDensity,
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = Constraints(maxWidth = width)
            )

        val paragraph =
            MultiParagraph(
                annotatedString = input.text,
                style = input.style,
                constraints = Constraints(maxWidth = width),
                density = input.density,
                fontFamilyResolver = fontFamilyResolver
            )

        return TextLayoutResult(input, paragraph, IntSize(width, ceil(paragraph.height).toInt()))
    }
}
