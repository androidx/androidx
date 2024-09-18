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

package androidx.compose.foundation.textfield

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.floor
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@LargeTest
class TextFieldCursorTest : FocusedWindowTest {

    private val motionDurationScale =
        object : MotionDurationScale {
            override var scaleFactor: Float by mutableStateOf(1f)
        }

    @OptIn(ExperimentalTestApi::class)
    @get:Rule
    val rule =
        createComposeRule(effectContext = motionDurationScale).also {
            it.mainClock.autoAdvance = false
        }

    private val boxPadding = 10.dp
    private val cursorColor = Color.Red
    private val textStyle =
        TextStyle(color = Color.White, background = Color.White, fontSize = 10.sp)

    private val textFieldWidth = 40.dp
    private val textFieldHeight = 20.dp
    private val textFieldBgColor = Color.White
    private var isFocused = false
    private var cursorRect = Rect.Zero

    private val bgModifier = Modifier.background(textFieldBgColor)
    private val focusModifier = Modifier.onFocusChanged { if (it.isFocused) isFocused = true }
    private val sizeModifier = Modifier.size(textFieldWidth, textFieldHeight)

    // default TextFieldModifier
    private val textFieldModifier = sizeModifier.then(bgModifier).then(focusModifier)

    // default onTextLayout to capture cursor boundaries.
    private val onTextLayout: (TextLayoutResult) -> Unit = { cursorRect = it.getCursorRect(0) }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorRendered() {
        rule.setTextFieldTestContent {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        with(rule.density) {
            rule.onNode(hasSetTextAction()).captureToImage().assertCursor(2.dp, this, cursorRect)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorWithBrush() {
        rule.setTextFieldTestContent {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = textStyle.copy(fontSize = textStyle.fontSize * 2),
                modifier =
                    Modifier.size(textFieldWidth, textFieldHeight * 2)
                        .then(bgModifier)
                        .then(focusModifier),
                cursorBrush =
                    Brush.verticalGradient(
                        // make a brush double/triple color at the beginning and end so we have
                        // stable
                        // colors at the ends.
                        // Without triple bottom, the bottom color never hits to the provided color.
                        listOf(Color.Blue, Color.Blue, Color.Green, Color.Green, Color.Green)
                    ),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        val bitmap = rule.onNode(hasSetTextAction()).captureToImage().toPixelMap()

        val cursorLeft = ceil(cursorRect.left).toInt() + 1
        val cursorTop = ceil(cursorRect.top).toInt() + 1
        val cursorBottom = floor(cursorRect.bottom).toInt() - 1
        bitmap.assertPixelColor(Color.Blue, x = cursorLeft, y = cursorTop)
        bitmap.assertPixelColor(Color.Green, x = cursorLeft, y = cursorBottom)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorPosition() =
        with(rule.density) {
            val cursorOffset = 4
            val textValue =
                mutableStateOf(TextFieldValue("test", selection = TextRange(cursorOffset)))
            rule.setTextFieldTestContent {
                Box(Modifier.padding(boxPadding)) {
                    BasicTextField(
                        value = textValue.value,
                        onValueChange = {},
                        textStyle = textStyle,
                        modifier = textFieldModifier,
                        cursorBrush = SolidColor(cursorColor),
                        onTextLayout = { cursorRect = it.getCursorRect(cursorOffset) }
                    )
                }
            }

            focusAndWait()

            rule.mainClock.advanceTimeBy(100)

            assertThat(cursorRect.left).isGreaterThan(0f)
            assertThat(cursorRect.left).isLessThan(textFieldWidth.toPx())
            rule.onNode(hasSetTextAction()).captureToImage().assertCursor(2.dp, this, cursorRect)
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorPosition_rtl() =
        with(rule.density) {
            val cursorOffset = 2
            val textValue =
                mutableStateOf(
                    TextFieldValue("\u05D0\u05D1\u05D2", selection = TextRange(cursorOffset))
                )
            rule.setTextFieldTestContent {
                Box(Modifier.padding(boxPadding)) {
                    BasicTextField(
                        value = textValue.value,
                        onValueChange = {},
                        textStyle = textStyle.copy(textDirection = TextDirection.Rtl),
                        modifier = textFieldModifier,
                        cursorBrush = SolidColor(cursorColor),
                        onTextLayout = { cursorRect = it.getCursorRect(cursorOffset) }
                    )
                }
            }

            focusAndWait()

            rule.mainClock.advanceTimeBy(100)

            assertThat(cursorRect.left).isGreaterThan(0f)
            assertThat(cursorRect.left).isLessThan(textFieldWidth.toPx())
            rule.onNode(hasSetTextAction()).captureToImage().assertCursor(2.dp, this, cursorRect)
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorPosition_clamped() =
        with(rule.density) {
            val cursorOffset = 20
            val textValue =
                mutableStateOf(
                    TextFieldValue(
                        "test                      ",
                        selection = TextRange(cursorOffset)
                    )
                )
            rule.setTextFieldTestContent {
                Box(Modifier.padding(boxPadding)) {
                    BasicTextField(
                        value = textValue.value,
                        onValueChange = {},
                        textStyle = textStyle,
                        modifier = textFieldModifier,
                        cursorBrush = SolidColor(cursorColor),
                        onTextLayout = { cursorRect = it.getCursorRect(cursorOffset) }
                    )
                }
            }

            focusAndWait()

            rule.mainClock.advanceTimeBy(100)

            // Cursor is beyond the right edge of the text field
            assertThat(cursorRect.left).isGreaterThan(textFieldWidth.toPx())
            val cursorWidth = 2.dp
            val clampedCursorHorizontal = (textFieldWidth - cursorWidth).toPx()
            val clampedCursorRect =
                Rect(
                    clampedCursorHorizontal,
                    cursorRect.top,
                    clampedCursorHorizontal,
                    cursorRect.bottom
                )
            rule
                .onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(cursorWidth, this, clampedCursorRect)
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorPosition_rtl_clamped() =
        with(rule.density) {
            val cursorOffset = 20
            val textValue =
                mutableStateOf(
                    TextFieldValue(
                        "\u05D0\u05D1\u05D2                      ",
                        selection = TextRange(cursorOffset)
                    )
                )
            rule.setTextFieldTestContent {
                Box(Modifier.padding(boxPadding)) {
                    BasicTextField(
                        value = textValue.value,
                        onValueChange = {},
                        textStyle = textStyle.copy(textDirection = TextDirection.Rtl),
                        modifier = textFieldModifier,
                        cursorBrush = SolidColor(cursorColor),
                        onTextLayout = { cursorRect = it.getCursorRect(cursorOffset) }
                    )
                }
            }

            focusAndWait()

            rule.mainClock.advanceTimeBy(100)

            // Cursor is beyond the left edge of the text field
            assertThat(cursorRect.left).isLessThan(0f)
            val clampedCursorRect = Rect(0f, cursorRect.top, 0f, cursorRect.bottom)
            rule
                .onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this, clampedCursorRect)
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorPosition_textFieldThinnerThanCursor() =
        with(rule.density) {
            val cursorWidth = 2.dp
            val thinSizeModifier = Modifier.size(cursorWidth - 0.5.dp, textFieldHeight)
            val thinTextFieldModifier = thinSizeModifier.then(bgModifier).then(focusModifier)
            rule.setTextFieldTestContent {
                Box(Modifier.padding(boxPadding)) {
                    BasicTextField(
                        value = "",
                        onValueChange = {},
                        textStyle = textStyle,
                        modifier = thinTextFieldModifier,
                        cursorBrush = SolidColor(cursorColor),
                        onTextLayout = onTextLayout
                    )
                }
            }

            focusAndWait()

            rule.mainClock.advanceTimeBy(100)

            assertThat(cursorRect.left).isEqualTo(0f)
            rule.onNode(hasSetTextAction()).captureToImage().assertCursor(2.dp, this, cursorRect)
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorBlinkingAnimation() {
        rule.setTextFieldTestContent {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(100)
        with(rule.density) {
            rule.onNode(hasSetTextAction()).captureToImage().assertCursor(2.dp, this, cursorRect)
        }

        // cursor invisible during next 500 ms
        rule.mainClock.advanceTimeBy(700)
        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                antiAliasingGap = 0.0f
            )
    }

    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalTestApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorBlinkingAnimation_whenSystemDisablesAnimations() {
        motionDurationScale.scaleFactor = 0f

        rule.setTextFieldTestContent {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(100)
        with(rule.density) {
            rule.onNode(hasSetTextAction()).captureToImage().assertCursor(2.dp, this, cursorRect)
        }

        // cursor invisible during next 500 ms
        rule.mainClock.advanceTimeBy(700)
        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                antiAliasingGap = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorUnsetColor_noCursor() {
        rule.setTextFieldTestContent {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(Color.Unspecified)
            )
        }

        focusAndWait()

        // no cursor when usually shown
        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                antiAliasingGap = 0.0f
            )

        // no cursor when should be no cursor
        rule.mainClock.advanceTimeBy(700)
        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                antiAliasingGap = 0.0f
            )
    }

    @Ignore // b/271927667
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorNotBlinking_whileTyping() {
        rule.setTextFieldTestContent {
            val text = remember { mutableStateOf("test") }
            BasicTextField(
                value = text.value,
                onValueChange = { text.value = it },
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(500)
        // TODO(b/170298051) check here that cursor is visible when we have a way to control
        //  cursor position when sending a text

        // change text field value
        rule.onNode(hasSetTextAction()).performTextReplacement("")

        // cursor would have been invisible during next 500 ms if cursor blinks while typing.
        // To prevent blinking while typing we restart animation when new symbol is typed.
        rule.mainClock.advanceTimeBy(400)
        with(rule.density) {
            rule.onNode(hasSetTextAction()).captureToImage().assertCursor(2.dp, this, cursorRect)
        }
    }

    @Test
    @FlakyTest(bugId = 283292820)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun selectionChanges_cursorNotBlinking() {
        rule.mainClock.autoAdvance = false
        val textValue = mutableStateOf(TextFieldValue("test", selection = TextRange(2)))
        rule.setTextFieldTestContent {
            BasicTextField(
                value = textValue.value,
                onValueChange = { textValue.value = it },
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        // hide the cursor
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.advanceTimeByFrame()

        // TODO(b/170298051) check here that cursor is visible when we have a way to control
        //  cursor position when sending a text

        rule.runOnIdle { textValue.value = textValue.value.copy(selection = TextRange(0)) }

        // necessary for animation to start (shows cursor again)
        rule.mainClock.advanceTimeByFrame()

        with(rule.density) {
            rule.onNode(hasSetTextAction()).captureToImage().assertCursor(2.dp, this, cursorRect)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun brushChanged_doesNotResetTimer() {
        var cursorBrush by mutableStateOf(SolidColor(cursorColor))
        rule.setTextFieldTestContent {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = cursorBrush,
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(800)
        cursorBrush = SolidColor(Color.Green)
        rule.mainClock.advanceTimeByFrame()

        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                antiAliasingGap = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorNotBlinking_whenWindowLostFocus() {
        val focusWindow = mutableStateOf(true)
        fun createWindowInfo(focused: Boolean) =
            object : WindowInfo {
                override val isWindowFocused: Boolean
                    get() = focused
            }
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalWindowInfo provides createWindowInfo(focusWindow.value)) {
                Box(Modifier.padding(boxPadding)) {
                    BasicTextField(
                        value = "",
                        onValueChange = {},
                        textStyle = textStyle,
                        modifier = textFieldModifier,
                        cursorBrush = SolidColor(cursorColor),
                        onTextLayout = onTextLayout
                    )
                }
            }
        }

        focusAndWait()

        // cursor visible first 500ms
        rule.mainClock.advanceTimeBy(100)
        rule.onNode(hasSetTextAction()).captureToImage().assertContainsColor(cursorColor)

        // window loses focus
        focusWindow.value = false
        rule.waitForIdle()

        // check that text field cursor disappeared even within visible 500ms
        rule.mainClock.advanceTimeBy(100)
        rule.onNode(hasSetTextAction()).captureToImage().assertDoesNotContainColor(cursorColor)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun focusedTextField_resumeBlinking_whenWindowRegainsFocus() {
        val focusWindow = mutableStateOf(true)
        fun createWindowInfo(focused: Boolean) =
            object : WindowInfo {
                override val isWindowFocused: Boolean
                    get() = focused
            }
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalWindowInfo provides createWindowInfo(focusWindow.value)) {
                Box(Modifier.padding(boxPadding)) {
                    BasicTextField(
                        value = "",
                        onValueChange = {},
                        textStyle = textStyle,
                        modifier = textFieldModifier,
                        cursorBrush = SolidColor(cursorColor),
                        onTextLayout = onTextLayout
                    )
                }
            }
        }

        focusAndWait()

        // window loses focus
        focusWindow.value = false
        rule.waitForIdle()

        // check that text field cursor disappeared even within visible 500ms
        rule.mainClock.advanceTimeBy(100)
        rule.onNode(hasSetTextAction()).captureToImage().assertDoesNotContainColor(cursorColor)

        // window regains focus within 500ms
        focusWindow.value = true
        rule.waitForIdle()

        rule.mainClock.advanceTimeBy(100)
        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertContainsColor(cursorColor)
            .assertCursor(2.dp, rule.density, cursorRect)
    }

    private fun focusAndWait() {
        rule.onNode(hasSetTextAction()).performClick()
        rule.mainClock.advanceTimeUntil { isFocused }
    }

    private fun ImageBitmap.assertCursor(cursorWidth: Dp, density: Density, cursorRect: Rect) {
        assertThat(cursorRect.height).isNotEqualTo(0f)
        assertThat(cursorRect).isNotEqualTo(Rect.Zero)
        val cursorWidthPx = (with(density) { cursorWidth.roundToPx() })

        // assert cursor width is greater than 2 since we will shrink the check area by 1 on each
        // side
        assertThat(cursorWidthPx).isGreaterThan(2)

        // shrink the check are by 1px for left, top, right, bottom
        val checkRect =
            Rect(
                ceil(cursorRect.left) + 1,
                ceil(cursorRect.top) + 1,
                floor(cursorRect.right) + cursorWidthPx - 1,
                floor(cursorRect.bottom) - 1
            )

        // skip an expanded rectangle that is 1px larger than cursor rectangle
        val skipRect =
            Rect(
                floor(cursorRect.left) - 1,
                floor(cursorRect.top) - 1,
                ceil(cursorRect.right) + cursorWidthPx + 1,
                ceil(cursorRect.bottom) + 1
            )

        val width = width
        val height = height
        this.assertPixels(IntSize(width, height)) { position ->
            if (checkRect.contains(position.toOffset())) {
                // cursor
                cursorColor
            } else if (skipRect.contains(position.toOffset())) {
                // skip some pixels around cursor
                null
            } else {
                // text field background
                textFieldBgColor
            }
        }
    }
}
