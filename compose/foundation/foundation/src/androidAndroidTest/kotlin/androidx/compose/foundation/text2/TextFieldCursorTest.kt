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

package androidx.compose.foundation.text2

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.floor
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@LargeTest
class TextFieldCursorTest {

    private val motionDurationScale = object : MotionDurationScale {
        override var scaleFactor: Float by mutableStateOf(1f)
    }

    @OptIn(ExperimentalTestApi::class)
    @get:Rule
    val rule = createComposeRule(effectContext = motionDurationScale).also {
        it.mainClock.autoAdvance = false
    }

    private lateinit var state: TextFieldState

    private val boxPadding = 8.dp
    // Both TextField background and font color should be the same to make sure that only
    // cursor is visible
    private val contentColor = Color.White
    private val cursorColor = Color.Red
    private val textStyle = TextStyle(
        color = contentColor,
        background = contentColor,
        fontSize = 10.sp,
        fontFamily = TEST_FONT_FAMILY
    )

    private var isFocused = false
    private var textLayoutResult: TextLayoutResult? = null
    private val cursorRect: Rect
        // assume selection is collapsed
        get() = textLayoutResult?.getCursorRect(state.text.selectionInChars.start) ?: Rect.Zero

    private val backgroundModifier = Modifier.background(contentColor)
    private val focusModifier = Modifier.onFocusChanged { if (it.isFocused) isFocused = true }

    // default TextFieldModifier
    private val textFieldModifier = Modifier
        .then(backgroundModifier)
        .then(focusModifier)

    // default onTextLayout to capture cursor boundaries.
    private val onTextLayout: Density.(TextLayoutResult) -> Unit = { textLayoutResult = it }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorRendered() {
        state = TextFieldState()
        rule.setContent {
            Box(Modifier.padding(boxPadding)) {
                BasicTextField2(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertCursor(2.dp, cursorRect)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorWithBrush() {
        state = TextFieldState()
        rule.setContent {
            Box(Modifier.padding(boxPadding)) {
                BasicTextField2(
                    state = state,
                    textStyle = textStyle.copy(fontSize = textStyle.fontSize * 2),
                    modifier = Modifier
                        .then(backgroundModifier)
                        .then(focusModifier),
                    cursorBrush = Brush.verticalGradient(
                        // make a brush double/triple color at the beginning and end so we have
                        // stable colors at the ends.
                        // Without triple bottom, the bottom color never hits to the provided color.
                        listOf(
                            Color.Blue,
                            Color.Blue,
                            Color.Green,
                            Color.Green,
                            Color.Green
                        )
                    ),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        val bitmap = rule.onNode(hasSetTextAction())
            .captureToImage().toPixelMap()

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
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) {
                BasicTextField2(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(100)
        with(rule.density) {
            rule.onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, cursorRect)
        }

        // cursor invisible during next 500 ms
        rule.mainClock.advanceTimeBy(700)
        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalTestApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorBlinkingAnimation_whenSystemDisablesAnimations() {
        motionDurationScale.scaleFactor = 0f
        state = TextFieldState()

        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) {
                BasicTextField2(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(100)
        with(rule.density) {
            rule.onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, cursorRect)
        }

        // cursor invisible during next 500 ms
        rule.mainClock.advanceTimeBy(700)
        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorUnsetColor_noCursor() {
        state = TextFieldState("hello", initialSelectionInChars = TextRange(2))
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) {
                BasicTextField2(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(Color.Unspecified)
                )
            }
        }

        focusAndWait()

        // no cursor when usually shown
        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                shapeOverlapPixelCount = 0.0f
            )

        // no cursor when should be no cursor
        rule.mainClock.advanceTimeBy(700)
        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorNotBlinking_whileTyping() {
        state = TextFieldState("test", initialSelectionInChars = TextRange(4))
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) {
                BasicTextField2(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(500)
        // TODO(b/170298051) check here that cursor is visible when we have a way to control
        //  cursor position when sending a text

        // change text field value
        rule.onNode(hasSetTextAction())
            .performTextInput("s")

        // cursor would have been invisible during next 500 ms if cursor blinks while typing.
        // To prevent blinking while typing we restart animation when new symbol is typed.
        rule.mainClock.advanceTimeBy(300)
        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertCursor(2.dp, cursorRect)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun selectionChanges_cursorNotBlinking() {
        state = TextFieldState("test", initialSelectionInChars = TextRange(2))
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) {
                BasicTextField2(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        // hide the cursor
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.advanceTimeByFrame()

        // TODO(b/170298051) check here that cursor is visible when we have a way to control
        //  cursor position when sending a text

        rule.onNode(hasSetTextAction())
            .performTextInputSelection(TextRange(0))

        // necessary for animation to start (shows cursor again)
        rule.mainClock.advanceTimeByFrame()

        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertCursor(2.dp, cursorRect)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun brushChanged_doesntResetTimer() {
        var cursorBrush by mutableStateOf(SolidColor(cursorColor))
        state = TextFieldState()
        rule.setContent {
            Box(Modifier.padding(boxPadding)) {
                BasicTextField2(
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

        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun selectionNotCollapsed_cursorNotDrawn() {
        state = TextFieldState("test", initialSelectionInChars = TextRange(2, 3))
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) {
                // set selection highlight to a known color
                CompositionLocalProvider(
                    LocalTextSelectionColors provides TextSelectionColors(Color.Blue, Color.Blue)
                ) {
                    BasicTextField2(
                        state = state,
                        // make sure that background is not obstructing selection
                        textStyle = textStyle.copy(
                            background = Color.Unspecified
                        ),
                        modifier = textFieldModifier,
                        cursorBrush = SolidColor(cursorColor),
                        onTextLayout = onTextLayout
                    )
                }
            }
        }

        focusAndWait()

        // cursor should still be visible if there wasn't a selection
        rule.mainClock.advanceTimeBy(300)
        rule.mainClock.advanceTimeByFrame()

        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertDoesNotContainColor(cursorColor)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun focusLost_cursorHidesImmediately() {
        state = TextFieldState("test")
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Column(Modifier.padding(boxPadding)) {
                BasicTextField2(
                    state = state,
                    // make sure that background is not obstructing selection
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
                Box(modifier = Modifier
                    .focusable(true)
                    .testTag("box"))
            }
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)
        rule.mainClock.advanceTimeByFrame()

        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertCursor(2.dp, cursorRect)

        rule.onNodeWithTag("box").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.mainClock.advanceTimeByFrame()

        // cursor should hide immediately.
        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = contentColor,
                backgroundColor = contentColor,
                shapeOverlapPixelCount = 0.0f
            )
    }

    private fun focusAndWait() {
        rule.onNode(hasSetTextAction()).performClick()
        rule.mainClock.advanceTimeUntil { isFocused }
    }

    private fun ImageBitmap.assertCursor(cursorWidth: Dp, cursorRect: Rect) {
        assertThat(cursorRect.height).isNotEqualTo(0f)
        assertThat(cursorRect).isNotEqualTo(Rect.Zero)
        val cursorWidthPx = (with(rule.density) { cursorWidth.roundToPx() })

        // assert cursor width is greater than 2 since we will shrink the check area by 1 on each
        // side
        assertThat(cursorWidthPx).isGreaterThan(2)

        // shrink the check are by 1px for left, top, right, bottom
        val checkRect = Rect(
            ceil(cursorRect.left) + 1,
            ceil(cursorRect.top) + 1,
            floor(cursorRect.right) + cursorWidthPx - 1,
            floor(cursorRect.bottom) - 1
        )

        // skip an expanded rectangle that is 1px larger than cursor rectangle due to antialiasing
        val skipRect = Rect(
            floor(cursorRect.left) - 1,
            floor(cursorRect.top) - 1,
            ceil(cursorRect.right) + cursorWidthPx + 1,
            ceil(cursorRect.bottom) + 1
        )

        val width = width
        val height = height
        this.assertPixels(
            IntSize(width, height)
        ) { position ->
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
        rule.setContent {
            Box(Modifier.padding(boxPadding)) {
                BasicTextField2(
                    state = state,
                    textStyle = textStyle,
                    modifier = textFieldModifier.layout { measurable, constraints ->
                        // change the state during layout so draw can read the new state
                        val currValue = state.text
                        if (currValue.isNotEmpty()) {
                            val newText = currValue.dropLast(1)
                            val newValue =
                                TextFieldCharSequence(newText.toString(), TextRange(newText.length))
                            state.editProcessor.reset(newValue)
                        }

                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            p.place(0, 0)
                        }
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