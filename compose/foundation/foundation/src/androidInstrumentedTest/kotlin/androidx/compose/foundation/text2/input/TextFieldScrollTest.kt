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

package androidx.compose.foundation.text2.input

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text2.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class TextFieldScrollTest : FocusedWindowTest {

    private val TextfieldTag = "textField"

    private val longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
        "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
        " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
        "fugiat nulla pariatur."

    @get:Rule
    val rule = createComposeRule()

    private lateinit var testScope: CoroutineScope

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun textFieldScroll_horizontal_scrollable_withLongInput() {
        val scrollState = ScrollState(0)

        rule.setupHorizontallyScrollableContent(
            TextFieldState(longText), scrollState, Modifier.size(width = 300.dp, height = 50.dp)
        )

        rule.runOnIdle {
            assertThat(scrollState.maxValue).isLessThan(Int.MAX_VALUE)
            assertThat(scrollState.maxValue).isGreaterThan(0)
        }
    }

    @Test
    fun textFieldScroll_vertical_scrollable_withLongInput() {
        val scrollState = ScrollState(0)

        rule.setupVerticallyScrollableContent(
            state = TextFieldState(longText),
            scrollState = scrollState,
            modifier = Modifier.size(width = 300.dp, height = 50.dp)
        )

        rule.runOnIdle {
            assertThat(scrollState.maxValue).isLessThan(Int.MAX_VALUE)
            assertThat(scrollState.maxValue).isGreaterThan(0)
        }
    }

    @Test
    fun textFieldScroll_vertical_scrollable_withLongInput_whenMaxLinesProvided() {
        val scrollState = ScrollState(0)

        rule.setupVerticallyScrollableContent(
            state = TextFieldState(longText),
            modifier = Modifier.width(100.dp),
            scrollState = scrollState,
            maxLines = 3
        )

        rule.runOnIdle {
            assertThat(scrollState.maxValue).isLessThan(Int.MAX_VALUE)
            assertThat(scrollState.maxValue).isGreaterThan(0)
        }
    }

    @Test
    fun textFieldScroll_horizontal_notScrollable_withShortInput() {
        val scrollState = ScrollState(0)

        rule.setupHorizontallyScrollableContent(
            state = TextFieldState("text"),
            scrollState = scrollState,
            modifier = Modifier.size(width = 300.dp, height = 50.dp)
        )

        rule.runOnIdle {
            assertThat(scrollState.maxValue).isEqualTo(0)
        }
    }

    @Test
    fun textFieldScroll_vertical_notScrollable_withShortInput() {
        val scrollState = ScrollState(0)

        rule.setupVerticallyScrollableContent(
            state = TextFieldState("text"),
            scrollState = scrollState,
            modifier = Modifier.size(width = 300.dp, height = 100.dp)
        )

        rule.runOnIdle {
            assertThat(scrollState.maxValue).isEqualTo(0)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_singleLine_scrolledAndClipped() {
        val parentSize = 200
        val textFieldSize = 50
        val tag = "OuterBox"

        with(rule.density) {
            rule.setContent {
                Box(
                    Modifier
                        .size(parentSize.toDp())
                        .background(color = Color.White)
                        .testTag(tag)
                ) {
                    ScrollableContent(
                        state = TextFieldState(longText),
                        modifier = Modifier.size(textFieldSize.toDp()),
                        scrollState = rememberScrollState(),
                        lineLimits = SingleLine
                    )
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(tag)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(parentSize, parentSize)) { position ->
                if (position.x > textFieldSize || position.y > textFieldSize) Color.White else null
            }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_multiline_scrolledAndClipped() {
        val parentSize = 200
        val textFieldSize = 50
        val tag = "OuterBox"

        with(rule.density) {
            rule.setContent {
                Box(
                    Modifier
                        .size(parentSize.toDp())
                        .background(color = Color.White)
                        .testTag(tag)
                ) {
                    ScrollableContent(
                        state = TextFieldState(longText),
                        modifier = Modifier.size(textFieldSize.toDp()),
                        scrollState = rememberScrollState(),
                        lineLimits = MultiLine()
                    )
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(tag)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(parentSize, parentSize)) { position ->
                if (position.x > textFieldSize || position.y > textFieldSize) Color.White else null
            }
    }

    @Test
    fun textFieldScroll_horizontal_swipe_whenLongInput() {
        val scrollState = ScrollState(0)

        rule.setupHorizontallyScrollableContent(
            state = TextFieldState(longText),
            scrollState = scrollState,
            modifier = Modifier.size(width = 300.dp, height = 50.dp)
        )

        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(0)
        }

        rule.onNodeWithTag(TextfieldTag)
            .performTouchInput { swipeLeft() }

        val firstSwipePosition = rule.runOnIdle {
            scrollState.value
        }
        assertThat(firstSwipePosition).isGreaterThan(0)

        rule.onNodeWithTag(TextfieldTag)
            .performTouchInput { swipeRight() }
        rule.runOnIdle {
            assertThat(scrollState.value).isLessThan(firstSwipePosition)
        }
    }

    @Test
    fun textFieldScroll_vertical_swipe_whenLongInput() {
        val scrollState = ScrollState(0)

        rule.setupVerticallyScrollableContent(
            state = TextFieldState(longText),
            scrollState = scrollState,
            modifier = Modifier.size(width = 300.dp, height = 50.dp)
        )

        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(0)
        }

        rule.onNodeWithTag(TextfieldTag)
            .performTouchInput { swipeUp() }

        val firstSwipePosition = rule.runOnIdle {
            scrollState.value
        }
        assertThat(firstSwipePosition).isGreaterThan(0)

        rule.onNodeWithTag(TextfieldTag)
            .performTouchInput { swipeDown() }
        rule.runOnIdle {
            assertThat(scrollState.value).isLessThan(firstSwipePosition)
        }
    }

    @Test
    fun textFieldScroll_restoresScrollerPosition() {
        val restorationTester = StateRestorationTester(rule)
        var scrollState: ScrollState? = null

        restorationTester.setContent {
            scrollState = rememberScrollState()
            ScrollableContent(
                state = TextFieldState(longText),
                modifier = Modifier.size(width = 300.dp, height = 50.dp),
                scrollState = scrollState!!,
                lineLimits = SingleLine
            )
        }

        rule.onNodeWithTag(TextfieldTag)
            .performTouchInput { swipeLeft() }

        val swipePosition = rule.runOnIdle { scrollState!!.value }
        assertThat(swipePosition).isGreaterThan(0)

        rule.runOnIdle {
            scrollState = ScrollState(0)
            assertThat(scrollState!!.value).isEqualTo(0)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(scrollState!!.value).isEqualTo(swipePosition)
        }
    }

    @Test
    fun textFieldScrollStateChange_shouldResetTheScroll() {
        val scrollState1 = ScrollState(0)
        val scrollState2 = ScrollState(0)

        var stateToggle by mutableStateOf(true)

        rule.setContent {
            ScrollableContent(
                state = TextFieldState(longText),
                scrollState = if (stateToggle) scrollState1 else scrollState2,
                modifier = Modifier.size(width = 300.dp, height = 50.dp),
                lineLimits = SingleLine
            )
        }

        rule.runOnIdle {
            assertThat(scrollState1.maxValue).isLessThan(Int.MAX_VALUE)
            assertThat(scrollState1.maxValue).isGreaterThan(0)

            assertThat(scrollState2.maxValue).isEqualTo(Int.MAX_VALUE) // when it's not set
            assertThat(scrollState2.value).isEqualTo(0)
        }

        rule.onNodeWithTag(TextfieldTag).performTouchInput { swipeLeft() }

        rule.runOnIdle {
            assertThat(scrollState1.value).isGreaterThan(0)
        }

        stateToggle = false

        rule.runOnIdle {
            assertThat(scrollState2.maxValue).isLessThan(Int.MAX_VALUE)
            assertThat(scrollState2.maxValue).isGreaterThan(0)

            assertThat(scrollState2.value).isEqualTo(0)
        }
    }

    @Test
    fun textFieldDoesNotFollowCursor_whenNotFocused() {
        val state = TextFieldState(longText)
        val scrollState = ScrollState(0)
        rule.setContent {
            ScrollableContent(
                state = state,
                scrollState = scrollState,
                modifier = Modifier.size(width = 300.dp, height = 50.dp),
                lineLimits = SingleLine
            )
        }

        rule.onNodeWithTag(TextfieldTag).assertIsNotFocused()

        // move cursor to the end
        state.edit {
            placeCursorAtEnd()
        }

        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(0)
        }
    }

    @Test
    fun textFieldFollowsCursor_whenFocused() {
        val state = TextFieldState(longText, TextRange(0))
        val scrollState = ScrollState(0)
        rule.setTextFieldTestContent {
            ScrollableContent(
                state = state,
                scrollState = scrollState,
                modifier = Modifier.size(width = 300.dp, height = 50.dp),
                lineLimits = SingleLine
            )
        }

        rule.onNodeWithTag(TextfieldTag).requestFocus()

        rule.runOnIdle {
            // move cursor to the end
            state.edit { placeCursorAtEnd() }
        }

        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(scrollState.maxValue)
        }
    }

    @Test
    fun textFieldDoesNotFollowCursor_whenScrollStateChanges_butCursorRemainsTheSame() {
        val state = TextFieldState(longText, initialSelectionInChars = TextRange(5))
        val scrollState = ScrollState(0)
        rule.setContent {
            ScrollableContent(
                state = state,
                scrollState = scrollState,
                modifier = Modifier.size(width = 300.dp, height = 50.dp),
                lineLimits = SingleLine
            )
        }

        rule.onNodeWithTag(TextfieldTag).requestFocus()
        rule.waitForIdle()

        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(scrollState.maxValue)
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(5))
        }
    }

    @Test
    fun textFieldRtl_horizontalScroll_isReversed() {
        val scrollState = ScrollState(0)

        rule.setupHorizontallyScrollableContent(
            state = TextFieldState(longText),
            scrollState = scrollState,
            modifier = Modifier.size(width = 300.dp, height = 50.dp),
            isRtl = true
        )

        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(0)
        }

        rule.onNodeWithTag(TextfieldTag).performTouchInput { swipeLeft() }

        // swiping left at initial position should be no-op in RTL layout
        val firstSwipePosition = rule.runOnIdle { scrollState.value }
        assertThat(firstSwipePosition).isEqualTo(0)

        rule.onNodeWithTag(TextfieldTag).performTouchInput { swipeRight() }
        rule.runOnIdle {
            assertThat(scrollState.value).isGreaterThan(firstSwipePosition)
        }
    }

    @Test
    fun textFieldScroll_testNestedScrolling() = runBlocking {
        val size = 300.dp
        val text = """
            First Line
            Second Line
            Third Line
            Fourth Line
        """.trimIndent()

        val textFieldScrollState = ScrollState(0)
        val columnScrollState = ScrollState(0)
        var touchSlop = 0f
        val height = 60.dp

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Column(
                Modifier
                    .size(size)
                    .verticalScroll(columnScrollState)
            ) {
                ScrollableContent(
                    state = TextFieldState(text),
                    modifier = Modifier.size(size, height),
                    scrollState = textFieldScrollState,
                    lineLimits = MultiLine()
                )
                Box(Modifier.size(size))
                Box(Modifier.size(size))
            }
        }

        assertThat(textFieldScrollState.value).isEqualTo(0)
        assertThat(textFieldScrollState.maxValue).isGreaterThan(0)
        assertThat(columnScrollState.value).isEqualTo(0)

        with(rule.density) {
            val x = 10.dp.toPx()
            val desiredY = textFieldScrollState.maxValue + 10.dp.roundToPx()
            val nearEdge = (height - 1.dp)
            // not to exceed size
            val slopStartY = minOf(desiredY + touchSlop, nearEdge.toPx())
            val slopStart = Offset(x, slopStartY)
            val end = Offset(x, 0f)
            rule.onNodeWithTag(TextfieldTag)
                .performTouchInput {
                    swipe(slopStart, end)
                }
        }

        assertThat(textFieldScrollState.value).isGreaterThan(0)
        assertThat(textFieldScrollState.value).isEqualTo(textFieldScrollState.maxValue)
        assertThat(columnScrollState.value).isGreaterThan(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun cursorScrolledIntoViewWhenTyping_inHorizontallyScrollableField_whenAtStart() {
        val state = TextFieldState("baaaaaaaaaa")
        val scrollState = ScrollState(Int.MAX_VALUE)
        lateinit var coroutineScope: CoroutineScope
        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            BasicTextField2(
                state,
                scrollState = scrollState,
                lineLimits = SingleLine,
                modifier = Modifier
                    // Force the field to be scrollable.
                    // Must be at least as wide as the cursor rectangle for the assertions to work.
                    .requiredWidth(10.dp)
                    .testTag("field")
            )
        }
        rule.onNodeWithTag("field").requestFocus()
        rule.runOnIdle {
            // Start the cursor at index 1 then backspace to move it to zero. This makes the
            // assertion easier to write since we don't have to know the width of the glyph to
            // calculate the expected scroll offset. We have to do this after requesting focus since
            // the cursor will move change when focus is gained.
            state.edit {
                placeCursorBeforeCharAt(1)
            }
        }
        rule.runOnIdle {
            coroutineScope.launch {
                scrollState.scrollTo(scrollState.maxValue)
            }
        }
        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(scrollState.maxValue)
        }
        rule.onNodeWithTag("field").assertTextEquals("baaaaaaaaaa")

        rule.onNodeWithTag("field").performKeyInput {
            pressKey(Key.Backspace)
        }

        rule.onNodeWithTag("field").assertTextEquals("aaaaaaaaaa")
        rule.waitUntil(
            "scrollState.value (${scrollState.value}) == 0 && " +
                "state.text.selectionInChars (${state.text.selectionInChars}) == TextRange(0)"
        ) {
            scrollState.value == 0 && state.text.selectionInChars == TextRange(0)
        }
    }

    @Test
    fun cursorScrolledIntoViewWhenTyping_inHorizontallyScrollableField_whenAtEnd() {
        val state = TextFieldState("aaaaaaaaaa")
        val scrollState = ScrollState(0)
        rule.setContent {
            BasicTextField2(
                state,
                scrollState = scrollState,
                lineLimits = SingleLine,
                modifier = Modifier
                    // Force the field to be scrollable.
                    // Must be at least as wide as the cursor rectangle for the assertions to work.
                    .requiredWidth(10.dp)
                    .testTag("field")
            )
        }
        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(0)
        }

        rule.onNodeWithTag("field").performTextInput("b")

        rule.waitUntil(
            "scrollState.value (${scrollState.value}) == " +
                "scrollState.maxValue (${scrollState.maxValue})"
        ) {
            scrollState.value == scrollState.maxValue
        }
    }

    @Test
    fun cursorScrolledIntoViewWhenTyping_inVerticallyScrollableField_whenAtTop() {
        val state = TextFieldState("a\na\na\na\n", initialSelectionInChars = TextRange(0))
        val scrollState = ScrollState(Int.MAX_VALUE)
        rule.setContent {
            BasicTextField2(
                state,
                scrollState = scrollState,
                lineLimits = MultiLine(maxHeightInLines = 1),
                modifier = Modifier.testTag("field")
            )
        }
        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(scrollState.maxValue)
        }

        rule.onNodeWithTag("field").performTextInput("b")

        rule.waitUntil("scrollState.value (${scrollState.value}) == 0") {
            scrollState.value == 0
        }
    }

    @Test
    fun cursorScrolledIntoViewWhenTyping_inVerticallyScrollableField_whenAtBottom() {
        val state = TextFieldState("a\na\na\na\n")
        val scrollState = ScrollState(0)
        rule.setContent {
            BasicTextField2(
                state,
                scrollState = scrollState,
                lineLimits = MultiLine(maxHeightInLines = 1),
                modifier = Modifier.testTag("field")
            )
        }
        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(0)
        }

        rule.onNodeWithTag("field").performTextInput("b")

        rule.waitUntil(
            "scrollState.value (${scrollState.value}) == " +
                "scrollState.maxValue (${scrollState.maxValue})"
        ) {
            scrollState.value == scrollState.maxValue
        }
    }

    @Test
    fun cursorScrolledIntoViewWhenTyping_inVerticallyScrollableField_whenMovesBelowViewport() {
        val state = TextFieldState("a\na\na\na\n")
        val scrollState = ScrollState(Int.MAX_VALUE)
        rule.setContent {
            BasicTextField2(
                state,
                scrollState = scrollState,
                lineLimits = MultiLine(maxHeightInLines = 1),
                modifier = Modifier.testTag("field")
            )
        }
        rule.onNodeWithTag("field").requestFocus()
        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(scrollState.maxValue)
        }

        // At this point the field is scrolled all the way to the bottom, but then we enter a
        // newline, which will push the cursor below the bottom of the field. It should scroll up
        // to stay in view.
        rule.onNodeWithTag("field").performTextInput("\n")

        rule.waitUntil(
            "scrollState.value (${scrollState.value}) == " +
                "scrollState.maxValue (${scrollState.maxValue})"
        ) {
            scrollState.value == scrollState.maxValue
        }
    }

    @Test
    fun cursorScrolledIntoViewWhenTyping_inVerticallyScrollableContainer_whenFieldExpands() {
        // Start as a single line, then enter '\n' to grow to 2 lines.
        val state = TextFieldState("a")
        val scrollState = ScrollState(0)
        var containerHeight by mutableStateOf(0.dp)
        rule.setContent {
            Box(
                Modifier
                    .requiredHeight(containerHeight)
                    .fillMaxWidth()
                    .border(1.dp, Color.Red)
                    .verticalScroll(scrollState)
            ) {
                BasicTextField2(
                    state,
                    // The field should never scroll internally.
                    lineLimits = MultiLine(maxHeightInLines = Int.MAX_VALUE),
                    modifier = Modifier
                        .testTag("field")
                        .border(1.dp, Color.Blue)
                )
            }
        }
        rule.onNodeWithTag("field").requestFocus()
        rule.runOnIdle {
            assertThat(scrollState.value).isEqualTo(0)
        }

        // Make the container height equal to the size of the single-line text field.
        with(rule.density) {
            containerHeight = rule.onNodeWithTag("field").fetchSemanticsNode().size.height.toDp()
        }

        // Enter a newline, which will move the cursor to line 2 and grow the field to be 2 lines
        // tall. The second line will initially be hidden by the container, but should be scrolled
        // back into view.
        rule.onNodeWithTag("field").performTextInput("\n")

        rule.waitUntil(
            "maxValue (${scrollState.maxValue} > 0 && " +
                "scrollState.value (${scrollState.value}) == maxValue",
            timeoutMillis = 10_000
        ) {
            val maxValue = scrollState.maxValue
            maxValue > 0 && scrollState.value == maxValue
        }
    }

    private fun ComposeContentTestRule.setupHorizontallyScrollableContent(
        state: TextFieldState,
        scrollState: ScrollState,
        modifier: Modifier = Modifier,
        isRtl: Boolean = false
    ) {
        setContent {
            val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                ScrollableContent(
                    state = state,
                    scrollState = scrollState,
                    modifier = modifier,
                    lineLimits = SingleLine
                )
            }
        }
    }

    private fun ComposeContentTestRule.setupVerticallyScrollableContent(
        state: TextFieldState,
        scrollState: ScrollState,
        modifier: Modifier = Modifier,
        maxLines: Int = Int.MAX_VALUE,
        isRtl: Boolean = false
    ) {
        setContent {
            val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                ScrollableContent(
                    state = state,
                    scrollState = scrollState,
                    modifier = modifier,
                    lineLimits = MultiLine(maxHeightInLines = maxLines)
                )
            }
        }
    }

    @Composable
    private fun ScrollableContent(
        modifier: Modifier,
        state: TextFieldState,
        scrollState: ScrollState,
        lineLimits: TextFieldLineLimits
    ) {
        testScope = rememberCoroutineScope()
        BasicTextField2(
            state = state,
            scrollState = scrollState,
            lineLimits = lineLimits,
            modifier = modifier.testTag(TextfieldTag)
        )
    }

    private fun runBlockingAndAwaitIdle(block: suspend CoroutineScope.() -> Unit) {
        val job = testScope.launch(block = block)
        rule.waitForIdle()
        runBlocking {
            job.join()
        }
    }
}
