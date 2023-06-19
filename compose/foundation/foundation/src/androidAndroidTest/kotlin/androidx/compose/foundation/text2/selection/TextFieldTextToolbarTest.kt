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

package androidx.compose.foundation.text2.selection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.FakeTextToolbar
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.placeCursorAtEnd
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
class TextFieldTextToolbarTest {

    @get:Rule
    val rule = createComposeRule()

    val fontSize = 10.sp

    val fontSizePx = with(rule.density) { fontSize.toPx() }

    val TAG = "BasicTextField2"

    @Test
    fun toolbarAppears_whenCursorHandleIsClicked() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
    }

    @Test
    fun toolbarDisappears_whenCursorHandleIsClickedAgain() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
    }

    @Test
    fun toolbarDisappears_whenTextStateIsUpdated() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)

        state.edit {
            append(" World!")
            placeCursorAtEnd()
        }

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun toolbarDisappears_whenTextIsEntered_throughIME() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)

        rule.onNodeWithTag(TAG).performTextInput(" World!")

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun toolbarDisappears_whenTextIsEntered_throughHardwareKeyboard() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)

        rule.onNodeWithTag(TAG).performKeyInput {
            pressKey(Key.W)
        }

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun toolbarTemporarilyHides_whenHandleIsBeingDragged() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(0f, fontSizePx / 2)) }

        with(rule.onNode(isSelectionHandle(Handle.Cursor))) {
            performClick()
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
            performTouchInput {
                down(center)
                moveBy(Offset(viewConfiguration.touchSlop, 0f))
                moveBy(Offset(fontSizePx, 0f))
            }
        }
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performTouchInput {
            up()
        }
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }
    }

    @Test
    fun toolbarTemporarilyHides_whenCursor_goesOutOfBounds() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello ".repeat(20)) // make sure the field is scrollable
        setupContent(state, textToolbar, true)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }

        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        rule.onNodeWithTag(TAG).performTouchInput { swipeLeft(startX = fontSizePx * 3, endX = 0f) }
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }

        rule.onNodeWithTag(TAG).performTouchInput { swipeRight(startX = 0f, endX = fontSizePx * 3) }
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }
    }

    @Test
    fun toolbarFollowsTheCursor_whenTextFieldIsScrolled() {
        var shownRect: Rect? = null
        val textToolbar = FakeTextToolbar(
            onShowMenu = { rect, _, _, _, _ ->
                shownRect = rect
            },
            onHideMenu = {}
        )
        val state = TextFieldState("Hello ".repeat(20)) // make sure the field is scrollable
        setupContent(state, textToolbar, true)

        rule.onNodeWithTag(TAG).performTouchInput { click() }

        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        lateinit var firstRectAnchor: Rect
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
            firstRectAnchor = shownRect!!
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            down(center)
            moveBy(Offset(-viewConfiguration.touchSlop - fontSizePx, 0f))
            up()
        }
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
            val secondRectAnchor = shownRect!!
            Truth.assertAbout(RectSubject.SUBJECT_FACTORY)
                .that(secondRectAnchor)!!
                .isEqualToWithTolerance(
                    firstRectAnchor.translate(
                        translateX = -fontSizePx,
                        translateY = 0f
                    )
                )
        }
    }

    @Test
    fun toolbarShowsSelectAll() {
        var selectAllOptionAvailable = false
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, _, _, _, onSelectAllRequested ->
                selectAllOptionAvailable = onSelectAllRequested != null
            },
            onHideMenu = {}
        )
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar, true)

        rule.onNodeWithTag(TAG).performTouchInput { click() }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()

        rule.runOnIdle {
            assertThat(selectAllOptionAvailable).isTrue()
        }
    }

    @Test
    fun toolbarDoesNotShowSelectAll_whenAllTextIsAlreadySelected() {
        var selectAllOption: (() -> Unit)? = null
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, _, _, _, onSelectAllRequested ->
                selectAllOption = onSelectAllRequested
            },
            onHideMenu = {}
        )
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar, true)

        rule.onNodeWithTag(TAG).performTouchInput { click() }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()

        rule.runOnIdle {
            assertThat(selectAllOption).isNotNull()
        }

        selectAllOption?.invoke()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 5))
        rule.runOnIdle {
            assertThat(selectAllOption).isNull()
        }
    }

    @Test
    fun toolbarDoesNotShowPaste_whenClipboardHasNoContent() {
        var pasteOptionAvailable = false
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, _, onPasteRequested, _, _ ->
                pasteOptionAvailable = onPasteRequested != null
            },
            onHideMenu = {}
        )
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar, true)

        rule.onNodeWithTag(TAG).performTouchInput { click() }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()

        rule.runOnIdle {
            assertThat(pasteOptionAvailable).isFalse()
        }
    }

    @Test
    fun toolbarShowsPaste_whenClipboardHasContent() {
        var pasteOptionAvailable = false
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, _, onPasteRequested, _, _ ->
                pasteOptionAvailable = onPasteRequested != null
            },
            onHideMenu = {}
        )
        val clipboardManager = FakeClipboardManager("world")
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar, true, clipboardManager)

        rule.onNodeWithTag(TAG).performTouchInput { click() }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()

        rule.runOnIdle {
            assertThat(pasteOptionAvailable).isTrue()
        }
    }

    @Test
    fun pasteInsertsContentAtCursor_placesCursorAfterInsertedContent() {
        var pasteOption: (() -> Unit)? = null
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, _, onPasteRequested, _, _ ->
                pasteOption = onPasteRequested
            },
            onHideMenu = {}
        )
        val clipboardManager = FakeClipboardManager("world")
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar, true, clipboardManager)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, 0f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()

        rule.runOnIdle {
            pasteOption!!.invoke()
        }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("Heworldllo")
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(7))
        }
    }

    @Test
    fun tappingTextField_hidesTheToolbar() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun interactingWithTextFieldByMouse_doeNotShowTheToolbar() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performMouseInput {
            click()
        }
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
    }

    @Test
    fun toolbarDisappears_whenFocusIsLost() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        val focusRequester = FocusRequester()
        rule.setContent {
            CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
                Column {
                    Box(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                            .size(100.dp)
                    )
                    BasicTextField2(
                        state = state,
                        modifier = Modifier
                            .width(100.dp)
                            .testTag(TAG),
                        textStyle = TextStyle(
                            fontFamily = TEST_FONT_FAMILY,
                            fontSize = fontSize
                        )
                    )
                }
            }
        }

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun toolbarDisappears_whenTextFieldIsDisposed() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        val toggleState = mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
                Column {
                    if (toggleState.value) {
                        BasicTextField2(
                            state = state,
                            modifier = Modifier
                                .width(100.dp)
                                .testTag(TAG),
                            textStyle = TextStyle(
                                fontFamily = TEST_FONT_FAMILY,
                                fontSize = fontSize
                            )
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)

        toggleState.value = false

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    private fun setupContent(
        state: TextFieldState = TextFieldState(),
        toolbar: TextToolbar = FakeTextToolbar(),
        singleLine: Boolean = false,
        clipboardManager: ClipboardManager = FakeClipboardManager(),
    ) {
        rule.setContent {
            CompositionLocalProvider(
                LocalTextToolbar provides toolbar,
                LocalClipboardManager provides clipboardManager
            ) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier
                        .width(100.dp)
                        .testTag(TAG),
                    textStyle = TextStyle(
                        fontFamily = TEST_FONT_FAMILY,
                        fontSize = fontSize
                    ),
                    lineLimits = if (singleLine) {
                        TextFieldLineLimits.SingleLine
                    } else {
                        TextFieldLineLimits.Default
                    }
                )
            }
        }
    }

    private fun FakeTextToolbar() = FakeTextToolbar(
        onShowMenu = { _, _, _, _, _ -> },
        onHideMenu = {
            println("hide")
        }
    )
}

internal class RectSubject private constructor(
    failureMetadata: FailureMetadata?,
    private val subject: Rect?
) : Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<RectSubject?, Rect?> =
            Factory { failureMetadata, subject -> RectSubject(failureMetadata, subject) }
    }

    fun isEqualToWithTolerance(expected: Rect, tolerance: Float = 1f) {
        if (subject == null) failWithoutActual(Fact.simpleFact("is null"))
        check("instanceOf()").that(subject).isInstanceOf(Rect::class.java)
        assertThat(subject!!.left).isWithin(tolerance).of(expected.left)
        assertThat(subject.top).isWithin(tolerance).of(expected.top)
        assertThat(subject.right).isWithin(tolerance).of(expected.right)
        assertThat(subject.bottom).isWithin(tolerance).of(expected.bottom)
    }
}

internal fun FakeClipboardManager(
    initialText: String? = null
) = object : ClipboardManager {
    private var currentText: AnnotatedString? = initialText?.let { AnnotatedString(it) }
    override fun setText(annotatedString: AnnotatedString) {
        currentText = annotatedString
    }

    override fun getText(): AnnotatedString? {
        return currentText
    }
}