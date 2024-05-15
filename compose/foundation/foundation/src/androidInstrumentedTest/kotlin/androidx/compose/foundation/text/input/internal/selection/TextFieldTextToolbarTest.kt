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

package androidx.compose.foundation.text.input.internal.selection

import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.createClipData
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.foundation.text.selection.FakeTextToolbar
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.withKeyDown
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@LargeTest
class TextFieldTextToolbarTest : FocusedWindowTest {

    @get:Rule
    val rule = createComposeRule()

    val fontSize = 10.sp

    private val fontSizePx = with(rule.density) { fontSize.toPx() }

    private val TAG = "BasicTextField"

    private var enabled by mutableStateOf(true)

    private lateinit var view: View

    @Test
    fun toolbarAppears_whenCursorHandleIsClicked() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }
    }

    @Test
    fun toolbarDisappears_whenCursorHandleIsClickedAgain() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun longClickOnEmptyTextField_showsToolbar_butNoHandle() {
        val state = TextFieldState("")
        val textToolbar = FakeTextToolbar({ _, _, _, _, _ -> }, {})
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSize.toPx(), fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
        assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
    }

    @Test
    fun toolbarDisappears_whenTextStateIsUpdated() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        state.edit {
            append(" World!")
            placeCursorAtEnd()
        }

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun toolbarDoesNotAppear_ifSelectionIsInitiatedViaHardwareKeys() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        with(rule.onNodeWithTag(TAG)) {
            requestFocus()
            performKeyInput {
                withKeyDown(Key.ShiftLeft) {
                    pressKey(Key.DirectionLeft)
                    pressKey(Key.DirectionLeft)
                    pressKey(Key.DirectionLeft)
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(5, 2))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }

        with(rule.onNodeWithTag(TAG)) {
            performKeyInput {
                withKeyDown(Key.CtrlLeft) {
                    pressKey(Key.A)
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(0, 5))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun toolbarDoesNotAppear_ifSelectionIsInitiatedViaStateUpdate() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        with(rule.onNodeWithTag(TAG)) {
            requestFocus()
        }

        rule.runOnIdle {
            state.edit { selectAll() }
        }

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(0, 5))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun toolbarDoesNotAppear_ifSelectionIsInitiatedViaSemantics() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        with(rule.onNodeWithTag(TAG)) {
            requestFocus()
            performTextInputSelection(TextRange(0, 5))
        }

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(0, 5))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun toolbarAppears_ifSelectionIsInitiatedViaSemantics_inNoneTraversalMode() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        with(rule.onNodeWithTag(TAG)) {
            requestFocus()
            performSemanticsAction(SemanticsActions.SetSelection) {
                it(0, 5, false)
            }
        }

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(0, 5))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }
    }

    @Test
    fun toolbarDisappears_whenTextIsEntered_throughIME() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        rule.onNodeWithTag(TAG).performTextInput(" World!")

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun cursorToolbarDisappears_whenTextField_getsDisabled_doesNotReappear() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        enabled = false

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }

        enabled = true

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun selectionToolbarDisappears_whenTextField_getsDisabled_doesNotReappear() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).requestFocus()
        rule.onNodeWithTag(TAG).performTextInputSelectionShowingToolbar(TextRange(2, 4))
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        enabled = false

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }

        enabled = true

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

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        rule.onNodeWithTag(TAG).performKeyInput {
            pressKey(Key.W)
        }

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun toolbarDoesNotShow_ifSelectionInitiatedByHardwareKeyboard() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSizePx, fontSizePx / 2))
        }

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(0, 5))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        // regular `performKeyInput` scope does not set source to InputDevice.SOURCE_KEYBOARD
        view.dispatchKeyEvent(
            KeyEvent(
                /* downTime = */ 0,
                /* eventTime = */ 0,
                /* action = */ ACTION_DOWN,
                /* code = */ KeyEvent.KEYCODE_A,
                /* repeat = */ 0,
                /* metaState = */ KeyEvent.META_CTRL_ON,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                /* scancode= */ 0,
                /* flags= */ 0,
                /* source= */ InputDevice.SOURCE_KEYBOARD
            )
        )

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(0, 5))
            // even though there's selection, toolbar should not show when not in touch mode
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }

        // test the touch interaction again so we know that hardware keyboard changes are not
        // permanent
        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSizePx, fontSizePx / 2))
        }

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(0, 5))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
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
            rule.runOnIdle {
                assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
            }
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

        rule.onNodeWithTag(TAG).performTouchInput {
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
            swipeLeft(startX = fontSizePx * 3, endX = 0f)
        }
        rule.runOnIdle { assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden) }

        rule.onNodeWithTag(TAG).performTouchInput {
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
            swipeRight(startX = 0f, endX = fontSizePx * 3)
        }
        rule.runOnIdle { assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown) }
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
            advanceEventTime(1_000) // avoid this being interpreted as a multi-tap
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

        assertThat(state.selection).isEqualTo(TextRange(0, 5))
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
    fun toolbarShowsPaste_whenClipboardHasText() {
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
    fun toolbarDoesNotShowPaste_whenClipboardHasContent_butNoReceiveContentConfigured() {
        var pasteOptionAvailable = false
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, _, onPasteRequested, _, _ ->
                pasteOptionAvailable = onPasteRequested != null
            },
            onHideMenu = {}
        )
        val clipboardManager = FakeClipboardManager(supportsClipEntry = true).apply {
            setClip(createClipData().toClipEntry())
        }
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar, true, clipboardManager)

        rule.onNodeWithTag(TAG).performTouchInput { click() }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()

        rule.runOnIdle {
            assertThat(pasteOptionAvailable).isFalse()
        }
    }

    @Test
    fun toolbarShowsPaste_whenClipboardHasContent_andReceiveContentConfigured() {
        var pasteOptionAvailable = false
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, _, onPasteRequested, _, _ ->
                pasteOptionAvailable = onPasteRequested != null
            },
            onHideMenu = {}
        )
        val clipboardManager = FakeClipboardManager(supportsClipEntry = true).apply {
            setClip(createClipData().toClipEntry())
        }
        val state = TextFieldState("Hello")
        setupContent(
            state = state,
            toolbar = textToolbar,
            singleLine = true,
            clipboardManager = clipboardManager,
            modifier = Modifier.contentReceiver { null }
        )

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
            assertThat(state.selection).isEqualTo(TextRange(7))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun toolbarDoesNotShowCopyOrCut_whenSelectionIsCollapsed() {
        var cutOptionAvailable = false
        var copyOptionAvailable = false
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, onCopyRequested, _, onCutRequested, _ ->
                copyOptionAvailable = onCopyRequested != null
                cutOptionAvailable = onCutRequested != null
            },
            onHideMenu = {}
        )
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar, true)

        rule.onNodeWithTag(TAG).requestFocus()
        rule.onNodeWithTag(TAG).performTextInputSelection(TextRange(2, 2))

        rule.runOnIdle {
            assertThat(copyOptionAvailable).isFalse()
            assertThat(cutOptionAvailable).isFalse()
        }
    }

    @Test
    fun toolbarShowsCopyAndCut_whenSelectionIsExpanded() {
        var cutOptionAvailable = false
        var copyOptionAvailable = false
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, onCopyRequested, _, onCutRequested, _ ->
                copyOptionAvailable = onCopyRequested != null
                cutOptionAvailable = onCutRequested != null
            },
            onHideMenu = {}
        )
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar, true)

        rule.onNodeWithTag(TAG).requestFocus()
        rule.onNodeWithTag(TAG).performTextInputSelectionShowingToolbar(TextRange(2, 4))

        rule.runOnIdle {
            assertThat(copyOptionAvailable).isTrue()
            assertThat(cutOptionAvailable).isTrue()
        }
    }

    @Test
    fun copyUpdatesClipboardManager_placesCursorAtTheEndOfSelectedRegion() {
        var copyOption: (() -> Unit)? = null
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, onCopyRequested, _, _, _ ->
                copyOption = onCopyRequested
            },
            onHideMenu = {}
        )
        val clipboardManager = FakeClipboardManager()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar, true, clipboardManager)

        rule.onNodeWithTag(TAG).requestFocus()
        rule.onNodeWithTag(TAG).performTextInputSelectionShowingToolbar(TextRange(0, 5))

        rule.runOnIdle {
            copyOption!!.invoke()
        }

        rule.runOnIdle {
            assertThat(clipboardManager.getText()?.toString()).isEqualTo("Hello")
            assertThat(state.selection).isEqualTo(TextRange(5))
        }
    }

    @Test
    fun cutUpdatesClipboardManager_placesCursorAtTheEndOfSelectedRegion_removesTheCutContent() {
        var cutOption: (() -> Unit)? = null
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, _, _, onCutRequested, _ ->
                cutOption = onCutRequested
            },
            onHideMenu = {}
        )
        val clipboardManager = FakeClipboardManager()
        val state = TextFieldState("Hello World!")
        setupContent(state, textToolbar, true, clipboardManager)

        rule.onNodeWithTag(TAG).requestFocus()
        rule.onNodeWithTag(TAG).performTextInputSelectionShowingToolbar(TextRange(1, 5))

        rule.runOnIdle {
            cutOption!!.invoke()
        }

        rule.runOnIdle {
            assertThat(clipboardManager.getText()?.toString()).isEqualTo("ello")
            assertThat(state.text.toString()).isEqualTo("H World!")
            assertThat(state.selection).isEqualTo(TextRange(1))
        }
    }

    @Test
    fun cutAppliesFilter() {
        var cutOption: (() -> Unit)? = null
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, _, _, onCutRequested, _ ->
                cutOption = onCutRequested
            },
            onHideMenu = {}
        )
        val clipboardManager = FakeClipboardManager()
        val state = TextFieldState("Hello World!")
        setupContent(state, textToolbar, true, clipboardManager) {
            // only reject text changes, accept selection
            val initialSelection = selection
            replace(0, length, originalValue.toString())
            selection = initialSelection
        }

        rule.onNodeWithTag(TAG).requestFocus()
        rule.onNodeWithTag(TAG).performTextInputSelectionShowingToolbar(TextRange(1, 5))

        rule.runOnIdle {
            cutOption!!.invoke()
        }

        rule.runOnIdle {
            assertThat(clipboardManager.getText()?.toString()).isEqualTo("ello")
            assertThat(state.text.toString()).isEqualTo("Hello World!")
            assertThat(state.selection).isEqualTo(TextRange(1))
        }
    }

    @Test
    fun tappingTextField_hidesTheToolbar() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        rule.mainClock.advanceTimeBy(1000) // to not cause double click
        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
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
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
                Column {
                    Box(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                            .size(100.dp)
                    )
                    BasicTextField(
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

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        focusRequester.requestFocus()

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun toolbarDisappears_whenTextFieldIsDisposed() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        val toggleState = mutableStateOf(true)
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
                Column {
                    if (toggleState.value) {
                        BasicTextField(
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

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        toggleState.value = false

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun toolbarDisappears_whenLongPressIsInitiated() {
        val textToolbar = FakeTextToolbar()
        val state = TextFieldState("Hello")
        setupContent(state, textToolbar)

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(fontSizePx * 2, fontSizePx / 2)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).performClick()
        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(3 * fontSizePx * 2, fontSizePx / 2))
        }

        rule.runOnIdle {
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    private fun setupContent(
        state: TextFieldState = TextFieldState(),
        toolbar: TextToolbar = FakeTextToolbar(),
        singleLine: Boolean = false,
        clipboardManager: ClipboardManager = FakeClipboardManager(),
        modifier: Modifier = Modifier,
        filter: InputTransformation? = null,
    ) {
        rule.setTextFieldTestContent {
            view = LocalView.current
            CompositionLocalProvider(
                LocalTextToolbar provides toolbar,
                LocalClipboardManager provides clipboardManager
            ) {
                BasicTextField(
                    state = state,
                    modifier = modifier
                        .width(100.dp)
                        .testTag(TAG),
                    textStyle = TextStyle(
                        fontFamily = TEST_FONT_FAMILY,
                        fontSize = fontSize
                    ),
                    enabled = enabled,
                    lineLimits = if (singleLine) {
                        TextFieldLineLimits.SingleLine
                    } else {
                        TextFieldLineLimits.Default
                    },
                    inputTransformation = filter
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
    initialText: String? = null,
    supportsClipEntry: Boolean = false,
) = object : ClipboardManager {
    private var currentText: AnnotatedString? = initialText?.let { AnnotatedString(it) }
    private var currentClipEntry: ClipEntry? = null

    override fun setText(annotatedString: AnnotatedString) {
        currentText = annotatedString
    }

    override fun getText(): AnnotatedString? {
        return currentText
    }

    override fun getClip(): ClipEntry? {
        if (supportsClipEntry) {
            return currentClipEntry
        } else {
            throw NotImplementedError("This clipboard does not support clip entries")
        }
    }

    override fun setClip(clipEntry: ClipEntry?) {
        if (supportsClipEntry) {
            currentClipEntry = clipEntry
        } else {
            throw NotImplementedError("This clipboard does not support clip entries")
        }
    }
}

/**
 * Toolbar does not show up when text is selected with traversal mode off (relative to original
 * text). This is an override of [SemanticsNodeInteraction.performTextInputSelection] that
 * makes sure the toolbar shows up after selection is initiated.
 */
fun SemanticsNodeInteraction.performTextInputSelectionShowingToolbar(selection: TextRange) {
    requestFocus()
    performSemanticsAction(SemanticsActions.SetSelection) {
        it(selection.min, selection.max, false)
    }
}
