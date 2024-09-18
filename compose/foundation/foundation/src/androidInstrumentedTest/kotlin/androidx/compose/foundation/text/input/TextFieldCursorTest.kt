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

package androidx.compose.foundation.text.input

import android.os.Build
import android.view.DragEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.DefaultCursorThickness
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.internal.DragAndDropTestUtils.makeTextDragEvent
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasPerformImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.floor
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
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

    private lateinit var state: TextFieldState

    private val boxPadding = 8.dp

    // Both TextField background and font color should be the same to make sure that only
    // cursor is visible
    private val contentColor = Color.White
    private val cursorColor = Color.Red
    private val fontSize = 10.sp
    private val textStyle =
        TextStyle(
            color = contentColor,
            background = contentColor,
            fontSize = fontSize,
            fontFamily = TEST_FONT_FAMILY
        )

    private var isFocused = false
    private var textLayoutResult: (() -> TextLayoutResult?)? = null
    private val cursorRect: Rect
        // assume selection is collapsed
        get() = textLayoutResult?.invoke()?.getCursorRect(state.selection.start) ?: Rect.Zero

    private val cursorSize: DpSize by lazy {
        with(rule.density) { DpSize(DefaultCursorThickness, fontSize.toDp()) }
    }

    private val cursorSizePx: Size by lazy { with(rule.density) { cursorSize.toSize() } }

    private val cursorTopCenterInLtr: Offset
        // assume selection is collapsed
        get() = cursorRect.topCenter + Offset(cursorSizePx.width / 2f, 0f)

    private val cursorTopCenterInRtl: Offset
        // assume selection is collapsed
        get() = cursorRect.topCenter - Offset(cursorSizePx.width / 2f, 0f)

    private val backgroundModifier = Modifier.background(contentColor)
    private val focusModifier = Modifier.onFocusChanged { if (it.isFocused) isFocused = true }

    // default TextFieldModifier
    private val textFieldModifier = Modifier.then(backgroundModifier).then(focusModifier)

    // default onTextLayout to capture cursor boundaries.
    private val onTextLayout: Density.(() -> TextLayoutResult?) -> Unit = { textLayoutResult = it }

    private fun ComposeContentTestRule.setTestContent(content: @Composable () -> Unit) {
        this.setTextFieldTestContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a
            // bit different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) { content() }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorRendered() {
        state = TextFieldState()
        rule.setTestContent {
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorRendered_rtlLayout() {
        state = TextFieldState()
        rule.setTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier.width(30.dp),
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        // an empty text layout will be placed on the right side of 30.dp-width area
        // cursor will be at the most right side
        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInRtl)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorRendered_rtlText_ltrLayout() {
        state = TextFieldState("\u05D0\u05D1\u05D2", TextRange(3))
        rule.setTestContent {
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorRendered_rtlTextLayout() {
        state = TextFieldState("\u05D0\u05D1\u05D2", TextRange(3))
        rule.setTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier.width(50.dp),
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            // 20 - 2(cursor)
            .assertCursor(cursorTopCenterInRtl)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldCursorAtTheEnd_coercedIntoView() {
        state = TextFieldState("hello", TextRange(5))
        rule.setTestContent {
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier.width(50.dp),
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertCursor(cursorTopCenterInLtr - Offset(cursorSizePx.width, 0f))
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldCursorAtTheEnd_coercedIntoView_rtl() {
        state = TextFieldState("\u05D0\u05D1\u05D2", TextRange(3))
        rule.setTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier.width(30.dp),
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertCursor(cursorTopCenterInRtl + Offset(cursorSizePx.width, 0f))
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorWithBrush() {
        state = TextFieldState()
        rule.setTestContent {
            BasicTextField(
                state = state,
                textStyle = textStyle.copy(fontSize = textStyle.fontSize * 2),
                modifier = Modifier.then(backgroundModifier).then(focusModifier),
                cursorBrush =
                    Brush.verticalGradient(
                        // make a brush double/triple color at the beginning and end so we have
                        // stable colors at the ends.
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
    fun cursorBlinkingAnimation() {
        state = TextFieldState()
        rule.setTestContent {
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(100)
        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)

        // cursor invisible during next 500 ms
        rule.mainClock.advanceTimeBy(700)
        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                antiAliasingGap = 0.0f
            )
    }

    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalTestApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorBlinkingAnimation_whenSystemDisablesAnimations() {
        motionDurationScale.scaleFactor = 0f
        state = TextFieldState()

        rule.setTestContent {
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(100)
        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)

        // cursor invisible during next 500 ms
        rule.mainClock.advanceTimeBy(700)
        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                antiAliasingGap = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorUnsetColor_noCursor() {
        state = TextFieldState("hello", initialSelection = TextRange(2))
        rule.setTestContent {
            BasicTextField(
                state = state,
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
                shapeColor = contentColor,
                backgroundColor = contentColor,
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
                shapeColor = contentColor,
                backgroundColor = contentColor,
                antiAliasingGap = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorNotBlinking_whileTyping() {
        state = TextFieldState("test", initialSelection = TextRange(4))
        rule.setTestContent {
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier.width(100.dp),
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(500)

        // change text field value
        rule.onNode(hasSetTextAction()).performTextInput("s")

        // cursor would have been invisible during next 500 ms if cursor blinks while typing.
        // To prevent blinking while typing we restart animation when new symbol is typed.
        rule.mainClock.advanceTimeBy(300)
        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun selectionChanges_cursorNotBlinking() {
        state = TextFieldState("test", initialSelection = TextRange(2))
        rule.setTestContent {
            BasicTextField(
                state = state,
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

        rule.onNode(hasSetTextAction()).performTextInputSelection(TextRange(0))

        // necessary for animation to start (shows cursor again)
        rule.mainClock.advanceTimeByFrame()

        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)
    }

    @Ignore // b/327235206
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun togglingInnerTextField_whileFocused_cursorContinuesToDraw() {
        state = TextFieldState("test", initialSelection = TextRange(2))
        var toggle by mutableStateOf(true)
        rule.setTestContent {
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout,
                decorator = {
                    if (toggle) {
                        Row { it() }
                    } else {
                        Column { it() }
                    }
                }
            )
        }

        focusAndWait()

        // hide the cursor
        rule.mainClock.advanceTimeBy(600)
        rule.mainClock.advanceTimeByFrame()

        // assert no cursor visible
        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                antiAliasingGap = 0.0f
            )

        toggle = !toggle
        // necessary for animation to start (shows cursor again)
        rule.mainClock.advanceTimeByFrame()

        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)

        toggle = !toggle

        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.advanceTimeByFrame()

        // assert no cursor visible
        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                antiAliasingGap = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun brushChanged_doesntResetTimer() {
        var cursorBrush by mutableStateOf(SolidColor(cursorColor))
        state = TextFieldState()
        rule.setTestContent {
            Box(Modifier.padding(boxPadding)) {
                BasicTextField(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = cursorBrush,
                    onTextLayout = onTextLayout
                )
            }
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
                shapeColor = contentColor,
                backgroundColor = contentColor,
                antiAliasingGap = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun selectionNotCollapsed_cursorNotDrawn() {
        state = TextFieldState("test", initialSelection = TextRange(2, 3))
        rule.setTestContent {
            // set selection highlight to a known color
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(Color.Blue, Color.Blue)
            ) {
                BasicTextField(
                    state = state,
                    // make sure that background is not obstructing selection
                    textStyle = textStyle.copy(background = Color.Unspecified),
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        // cursor should still be visible if there wasn't a selection
        rule.mainClock.advanceTimeBy(300)
        rule.mainClock.advanceTimeByFrame()

        rule.onNode(hasSetTextAction()).captureToImage().assertDoesNotContainColor(cursorColor)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun focusLost_cursorHidesImmediately() {
        state = TextFieldState("test")
        rule.setTestContent {
            Column {
                BasicTextField(
                    state = state,
                    // make sure that background is not obstructing selection
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
                Box(modifier = Modifier.focusable(true).testTag("box"))
            }
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)
        rule.mainClock.advanceTimeByFrame()

        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)

        rule.onNodeWithTag("box").requestFocus()
        rule.mainClock.advanceTimeByFrame()

        // cursor should hide immediately.
        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                antiAliasingGap = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun readOnly_cursorIsNotDrawn() {
        state = TextFieldState("test", initialSelection = TextRange(4))
        rule.setTestContent {
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier,
                readOnly = true,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)
        rule.mainClock.advanceTimeByFrame()

        // readonly fields do not have setText action
        rule.onNode(hasPerformImeAction()).captureToImage().assertDoesNotContainColor(cursorColor)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun toggling_readOnly_drawsCursorAgain() {
        var readOnly by mutableStateOf(true)
        state = TextFieldState("test", initialSelection = TextRange(4))
        rule.setTestContent {
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier,
                readOnly = readOnly,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)
        rule.mainClock.advanceTimeByFrame()

        // readonly fields do not have setText action
        rule.onNode(hasPerformImeAction()).captureToImage().assertDoesNotContainColor(cursorColor)

        readOnly = false
        rule.mainClock.advanceTimeByFrame()

        rule.onNode(hasPerformImeAction()).captureToImage().assertCursor(cursorTopCenterInLtr)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorNotBlinking_whenWindowLostFocus() {
        state = TextFieldState()
        val focusWindow = mutableStateOf(true)
        fun createWindowInfo(focused: Boolean) =
            object : WindowInfo {
                override val isWindowFocused: Boolean
                    get() = focused
            }

        rule.setTestContent {
            CompositionLocalProvider(LocalWindowInfo provides createWindowInfo(focusWindow.value)) {
                Box(Modifier.padding(boxPadding)) {
                    BasicTextField(
                        state = state,
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
        rule.mainClock.advanceTimeBy(300)
        rule.onNode(hasSetTextAction()).captureToImage().assertDoesNotContainColor(cursorColor)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun focusedTextField_resumeBlinking_whenWindowRegainsFocus() {
        state = TextFieldState()
        val focusWindow = mutableStateOf(true)
        fun createWindowInfo(focused: Boolean) =
            object : WindowInfo {
                override val isWindowFocused: Boolean
                    get() = focused
            }

        rule.setTestContent {
            CompositionLocalProvider(LocalWindowInfo provides createWindowInfo(focusWindow.value)) {
                Box(Modifier.padding(boxPadding)) {
                    BasicTextField(
                        state = state,
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
            .assertCursor(cursorTopCenterInLtr)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_keepsSelection_whenWindowLosesFocus() {
        state = TextFieldState("hello", initialSelection = TextRange(0, 5))
        val selectionColor = Color.Blue
        val focusWindow = mutableStateOf(true)
        val windowInfo =
            object : WindowInfo {
                override val isWindowFocused: Boolean
                    get() = focusWindow.value
            }

        rule.setTestContent {
            CompositionLocalProvider(
                LocalWindowInfo provides windowInfo,
                LocalTextSelectionColors provides
                    TextSelectionColors(selectionColor, selectionColor)
            ) {
                BasicTextField(
                    state = state,
                    // make sure that background is not obstructing selection
                    textStyle = textStyle.copy(background = Color.Unspecified),
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        rule.onNode(hasSetTextAction()).captureToImage().assertContainsColor(selectionColor)

        // window lost focus, make sure selection still drawn
        focusWindow.value = false
        rule.waitForIdle()

        rule.onNode(hasSetTextAction()).captureToImage().assertContainsColor(selectionColor)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textDragging_cursorRendered() {
        state = TextFieldState("Hello World")
        var view: View? = null
        rule.setTestContent {
            view = LocalView.current
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        rule.mainClock.advanceTimeBy(100)

        rule.runOnIdle {
            val startEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_STARTED)
            val enterEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_ENTERED)
            val moveEvent =
                makeTextDragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    offset = Offset(with(rule.density) { fontSize.toPx() * 3 }, 5f)
                )

            view?.dispatchDragEvent(startEvent)
            view?.dispatchDragEvent(enterEvent)
            view?.dispatchDragEvent(moveEvent)
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textDragging_cursorDisappearsAfterTimeout() {
        state = TextFieldState("Hello World")
        var view: View? = null
        rule.setTestContent {
            view = LocalView.current
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        rule.mainClock.advanceTimeBy(100)

        rule.runOnIdle {
            val startEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_STARTED)
            val enterEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_ENTERED)
            val moveEvent =
                makeTextDragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    offset = Offset(with(rule.density) { fontSize.toPx() * 3 }, 5f)
                )

            view?.dispatchDragEvent(startEvent)
            view?.dispatchDragEvent(enterEvent)
            view?.dispatchDragEvent(moveEvent)
        }

        rule.mainClock.advanceTimeBy(500)

        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                antiAliasingGap = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textDragging_cursorDoesNotDisappearWhileMoving() {
        state = TextFieldState("Hello World")
        var view: View? = null
        rule.setTestContent {
            view = LocalView.current
            BasicTextField(
                state = state,
                textStyle = textStyle,
                modifier = textFieldModifier,
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = onTextLayout
            )
        }

        rule.mainClock.advanceTimeBy(100)

        rule.runOnIdle {
            val startEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_STARTED)
            val enterEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_ENTERED)
            val moveEvent =
                makeTextDragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    offset = Offset(with(rule.density) { fontSize.toPx() * 3 }, 5f)
                )

            view?.dispatchDragEvent(startEvent)
            view?.dispatchDragEvent(enterEvent)
            view?.dispatchDragEvent(moveEvent)
        }

        rule.mainClock.advanceTimeBy(300)

        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)

        val moveEvent2 =
            makeTextDragEvent(
                action = DragEvent.ACTION_DRAG_LOCATION,
                offset = Offset(with(rule.density) { fontSize.toPx() * 4 }, 5f)
            )
        view?.dispatchDragEvent(moveEvent2)
        rule.mainClock.advanceTimeBy(400)

        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textDragging_noWindowFocus_cursorRendered() {
        state = TextFieldState("Hello World")
        var view: View? = null
        rule.setContent {
            Box(Modifier.padding(boxPadding)) {
                view = LocalView.current
                BasicTextField(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.runOnIdle {
            val startEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_STARTED)
            val enterEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_ENTERED)
            val moveEvent =
                makeTextDragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    offset = Offset(with(rule.density) { fontSize.toPx() * 3 }, 5f)
                )

            view?.dispatchDragEvent(startEvent)
            view?.dispatchDragEvent(enterEvent)
            view?.dispatchDragEvent(moveEvent)
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNode(hasSetTextAction()).captureToImage().assertCursor(cursorTopCenterInLtr)
    }

    private fun focusAndWait() {
        rule.onNode(hasPerformImeAction()).requestFocus()
        rule.mainClock.advanceTimeUntil { isFocused }
    }

    /** @param cursorPosition Top center of cursor rectangle */
    private fun ImageBitmap.assertCursor(cursorPosition: Offset) {
        assertThat(cursorPosition.x).isAtLeast(0f)
        assertThat(cursorPosition.y).isAtLeast(0f)

        // assert cursor width is greater than 2 since we will shrink the check area by 1 on each
        // side
        assertThat(cursorSizePx.width).isGreaterThan(2)

        // shrink the check are by 1px for left, top, right, bottom
        val checkRect =
            Rect(
                ceil(cursorPosition.x - cursorSizePx.width / 2) + 1,
                ceil(cursorPosition.y) + 1,
                floor(cursorPosition.x + cursorSizePx.width / 2) - 1,
                floor(cursorPosition.y + cursorSizePx.height) - 1
            )

        // skip an expanded rectangle that is 1px larger than cursor rectangle due to antialiasing
        val skipRect =
            Rect(
                floor(cursorPosition.x - cursorSizePx.width / 2) - 1,
                floor(cursorPosition.y) - 1,
                ceil(cursorPosition.x + cursorSizePx.width / 2) + 1,
                ceil(cursorPosition.y + cursorSizePx.height) + 1
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
                contentColor
            }
        }
    }

    @Test
    fun textFieldCursor_alwaysReadLatestState_duringDraw() {
        state = TextFieldState("hello world", TextRange(5))
        rule.setTestContent {
            Box(Modifier.padding(boxPadding)) {
                BasicTextField(
                    state = state,
                    textStyle = textStyle,
                    modifier =
                        textFieldModifier.layout { measurable, constraints ->
                            // change the state during layout so draw can read the new state
                            val currValue = state.text
                            if (currValue.isNotEmpty()) {
                                val newText = currValue.dropLast(1)
                                state.setTextAndPlaceCursorAtEnd(newText.toString())
                            }

                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) { p.place(0, 0) }
                        },
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        rule.waitForIdle()

        rule.onNode(hasSetTextAction()).assertTextEquals("")
        // this test just needs to finish without crashing. There is no other assertion
    }
}
