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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.createClipData
import androidx.compose.foundation.focusable
import androidx.compose.foundation.internal.readText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.contextmenu.internal.ProvidePlatformTextContextMenuToolbar
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagSuppress
import androidx.compose.foundation.text.contextmenu.test.SpyTextActionModeCallback
import androidx.compose.foundation.text.contextmenu.test.assertNotNull
import androidx.compose.foundation.text.contextmenu.test.assertShown
import androidx.compose.foundation.text.contextmenu.test.items
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.selectAll
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest as coroutineRunTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@LargeTest
@RunWith(ContextMenuFlagFlipperRunner::class)
@ContextMenuFlagSuppress(suppressedFlagValue = false)
class TextFieldTextContextMenuToolbarTest : FocusedWindowTest {
    @get:Rule val rule = createComposeRule()

    private val TAG = "BasicTextField"
    private val fontSize = 10.sp
    private val fontSizePx = with(rule.density) { fontSize.toPx() }

    @Test
    fun toolbarAppears_whenCursorHandleIsClicked() = runTest {
        clickOffset(2)
        assertTextToolbarNotShown()
        Handle.Cursor.click()
        assertTextToolbarShown()
    }

    @Test
    fun toolbarDisappears_whenCursorHandleIsClickedAgain() = runTest {
        clickOffset(2)
        assertTextToolbarNotShown()
        Handle.Cursor.click()
        assertTextToolbarShown()
        Handle.Cursor.click()
        assertTextToolbarNotShown()
    }

    @Test
    fun longClickOnEmptyTextField_showsToolbar_butNoHandle() =
        runTest(textFieldState = TextFieldState("")) {
            longClickOffset(1)
            Handle.Cursor.assertDoesNotExist()
            assertTextToolbarShown()
        }

    @Test
    fun toolbarDisappears_whenTextStateIsUpdated() = runTest {
        clickOffset(2)
        Handle.Cursor.click()
        assertTextToolbarShown()

        textFieldState.edit {
            append(" World!")
            placeCursorAtEnd()
        }

        assertTextToolbarNotShown()
    }

    @Test
    fun toolbarDoesNotAppear_ifSelectionIsInitiatedViaHardwareKeys() = runTest {
        performKeyInput {
            withKeyDown(Key.ShiftLeft) {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionLeft)
            }
        }

        assertSelection(5 to 2)
        assertTextToolbarNotShown()

        performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.A) } }

        assertSelection(0 to 5)
        assertTextToolbarNotShown()
    }

    @Test
    fun toolbarDoesNotAppear_ifSelectionIsInitiatedViaStateUpdate() = runTest {
        requestTextFieldFocus()
        textFieldState.edit { selectAll() }
        assertSelection(0 to 5)
        assertTextToolbarNotShown()
    }

    @Test
    fun toolbarDoesNotAppear_ifSelectionIsInitiatedViaSemantics() = runTest {
        requestTextFieldFocus()
        setSelectionViaSemantics(0 to 5)
        assertSelection(0 to 5)
        assertTextToolbarNotShown()
    }

    @Test
    fun toolbarAppears_ifSelectionIsInitiatedViaSemantics_inNoneTraversalMode() = runTest {
        requestTextFieldFocus()
        tagInteraction.performSemanticsAction(SemanticsActions.SetSelection) { it(0, 5, false) }
        assertSelection(0 to 5)
        assertTextToolbarShown()
    }

    @Test
    fun toolbarDisappears_whenTextIsEntered_throughIME() = runTest {
        clickOffset(2)
        Handle.Cursor.click()
        assertTextToolbarShown()
        tagInteraction.performTextInput(" World!")
        assertTextToolbarNotShown()
    }

    @Test
    fun cursorToolbarDisappears_whenTextField_getsDisabled_doesNotReappear() = runTest {
        clickOffset(2)
        Handle.Cursor.click()
        assertTextToolbarShown()

        enabled = false
        assertTextToolbarNotShown()

        enabled = true
        assertTextToolbarNotShown()
    }

    @Test
    fun selectionToolbarDisappears_whenTextField_getsDisabled_doesNotReappear() = runTest {
        requestTextFieldFocus()
        setSelectionViaSemanticsShowingToolbar(2 to 4)
        assertTextToolbarShown()

        enabled = false
        assertTextToolbarNotShown()

        enabled = true
        assertTextToolbarNotShown()
    }

    @Test
    fun toolbarDisappears_whenTextIsEntered_throughHardwareKeyboard() = runTest {
        clickOffset(2)
        Handle.Cursor.click()
        assertTextToolbarShown()

        performKeyInput { pressKey(Key.W) }
        assertTextToolbarNotShown()
    }

    @Test
    fun toolbarDoesNotShow_ifSelectionInitiatedByHardwareKeyboard() = runTest {
        longClickOffset(0)

        assertSelection(0 to 5)
        assertTextToolbarShown()

        // regular `performKeyInput` scope does not set source to InputDevice.SOURCE_KEYBOARD
        sendHardwareKeyEvent(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON)

        assertSelection(0 to 5)
        // even though there's selection, toolbar should not show when not in touch mode
        assertTextToolbarNotShown()

        // test the touch interaction again so we know that hardware keyboard changes are not
        // permanent
        longClickOffset(0)

        assertSelection(0 to 5)
        assertTextToolbarShown()
    }

    @Test
    fun toolbarTemporarilyHides_whenHandleIsBeingDragged() = runTest {
        clickOffset(0)
        Handle.Cursor.click()
        assertTextToolbarShown()

        Handle.Cursor.interaction.performTouchInput {
            down(center)
            moveBy(Offset(viewConfiguration.touchSlop, 0f))
            moveBy(Offset(fontSizePx, 0f))
        }
        assertTextToolbarNotShown()

        Handle.Cursor.interaction.performTouchInput { up() }
        assertTextToolbarShown()
    }

    @Test
    fun toolbarTemporarilyHides_whenCursor_goesOutOfBounds() =
        // make sure the field is scrollable by making it long
        runTest(textFieldState = TextFieldState("Hello ".repeat(20)), singleLine = true) {
            clickOffset(2)

            Handle.Cursor.click()
            assertTextToolbarShown()

            tagInteraction.performTouchInput {
                advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
                swipeLeft(startX = fontSizePx * 3, endX = 0f)
            }
            assertTextToolbarNotShown()

            tagInteraction.performTouchInput {
                advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
                swipeRight(startX = 0f, endX = fontSizePx * 3)
            }
            assertTextToolbarShown()
        }

    @Test
    fun toolbarFollowsTheCursor_whenTextFieldIsScrolled() =
        runTest(textFieldState = TextFieldState("Hello ".repeat(20)), singleLine = true) {
            clickCenter()

            Handle.Cursor.click()
            assertTextToolbarShown()
            val firstRectAnchor = assertNotNull(spyTextActionModeCallback.contentRect)

            tagInteraction.performTouchInput {
                advanceEventTime(1_000) // avoid this being interpreted as a multi-tap
                down(center)
                moveBy(Offset(-viewConfiguration.touchSlop - fontSizePx, 0f))
                up()
            }
            assertTextToolbarShown()
            val secondRectAnchor = assertNotNull(spyTextActionModeCallback.contentRect)

            val expectedRect = firstRectAnchor.translate(translateX = -fontSizePx, translateY = 0f)
            assertThatRect(secondRectAnchor).isEqualToWithTolerance(expectedRect)
        }

    @Test
    fun toolbarShowsSelectAll() =
        runTest(singleLine = true) {
            clickCenter()
            Handle.Cursor.click()
            assertTextToolbarHasItem(SELECT_ALL)
        }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun toolbarShowsAutofill_ifNotReadOnly() =
        runTest(readOnly = false) {
            clickOffset(2)
            Handle.Cursor.click()
            assertTextToolbarHasItem(AUTOFILL)
        }

    @Test
    fun toolbarDoesNotShowSelectAll_whenAllTextIsAlreadySelected() =
        runTest(singleLine = true) {
            clickCenter()
            Handle.Cursor.click()
            val selectAllItem = assertTextToolbarHasItem(SELECT_ALL)
            selectAllItem.invokeItem()
            assertSelection(0 to 5)
            assertTextToolbarDoesNotHaveItem(SELECT_ALL)
        }

    // Regression test for b/422754681
    @Test
    fun toolbarDoesNotAccessClipData_whenEvaluatingPaste() {
        val clipboard = FakeClipboard("hello, world")
        runTest(singleLine = true, clipboard = { clipboard }) {
            clickCenter()
            Handle.Cursor.click()

            verify(clipboard.nativeClipboard, never()).primaryClip
            assertThat(clipboard.getClipEntryCalled).isEqualTo(0)
        }
    }

    // Regression test for b/422754681
    @Test
    fun toolbarDoesNotAccessClipDescription_ifNoClipData_whenEvaluatingPaste() {
        val clipboard = FakeClipboard()
        runTest(singleLine = true, clipboard = { clipboard }) {
            clickCenter()
            Handle.Cursor.click()

            verify(clipboard.nativeClipboard, never()).primaryClip
            verify(clipboard.nativeClipboard, never()).primaryClipDescription
            assertThat(clipboard.getClipEntryCalled).isEqualTo(0)
        }
    }

    @Test
    fun toolbarDoesNotShowPaste_whenClipboardHasNoContent() =
        runTest(singleLine = true) {
            clickCenter()
            Handle.Cursor.click()
            assertTextToolbarDoesNotHaveItem(PASTE)
        }

    @Test
    fun toolbarShowsPaste_whenClipboardHasText() =
        runTest(singleLine = true, clipboard = { FakeClipboard("world") }) {
            clickCenter()
            Handle.Cursor.click()
            assertTextToolbarHasItem(PASTE)
        }

    @Test
    fun toolbarDoesNotShowPaste_whenClipboardHasContent_butNoReceiveContentConfigured() =
        runTest(
            singleLine = true,
            clipboard = {
                FakeClipboard().apply {
                    setClipEntry(createClipData(block = { addUri() }).toClipEntry())
                }
            },
        ) {
            clickCenter()
            Handle.Cursor.click()
            assertTextToolbarDoesNotHaveItem(PASTE)
        }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun toolbarShowsPaste_whenClipboardHasContent_andReceiveContentConfigured() =
        runTest(
            singleLine = true,
            clipboard = { FakeClipboard().apply { setClipEntry(createClipData().toClipEntry()) } },
            modifier = Modifier.contentReceiver { null },
        ) {
            clickCenter()
            Handle.Cursor.click()
            assertTextToolbarHasItem(PASTE)
        }

    @Test
    fun pasteInsertsContentAtCursor_placesCursorAfterInsertedContent() =
        runTest(singleLine = true, clipboard = { FakeClipboard("world") }) {
            clickOffset(2)
            Handle.Cursor.click()

            val pasteItem = assertTextToolbarHasItem(PASTE)
            pasteItem.invokeItem()

            assertText("Heworldllo")
            assertSelection(7 to 7)
        }

    @Test
    fun toolbarDoesNotShowCopyOrCut_whenSelectionIsCollapsed() =
        runTest(singleLine = true) {
            requestTextFieldFocus()
            setSelectionViaSemantics(2 to 2)
            assertTextToolbarNotShown()
        }

    @Test
    fun toolbarShowsCopyAndCut_whenSelectionIsExpanded() =
        runTest(singleLine = true) {
            requestTextFieldFocus()
            setSelectionViaSemanticsShowingToolbar(2 to 4)
            assertTextToolbarHasItem(COPY)
            assertTextToolbarHasItem(CUT)
        }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun toolbarShowsAutofill_whenSelectionIsCollapsed() =
        runTest(singleLine = true) {
            clickCenter()
            Handle.Cursor.click()
            assertTextToolbarHasItem(AUTOFILL)
        }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun toolbarDoesNotShowAutofill_whenSelectionIsExpanded() =
        runTest(singleLine = true) {
            requestTextFieldFocus()
            setSelectionViaSemanticsShowingToolbar(2 to 4)
            // Autofill should not display when text has been selected.
            assertTextToolbarDoesNotHaveItem(AUTOFILL)
        }

    @Test
    fun copyUpdatesClipboardManager_placesCursorAtTheEndOfSelectedRegion() =
        runTest(singleLine = true) {
            requestTextFieldFocus()
            setSelectionViaSemanticsShowingToolbar(0 to 5)

            assertSelection(0 to 5)
            val copyItem = assertTextToolbarHasItem(COPY)
            copyItem.invokeItem()

            assertClipboardText("Hello")
            assertSelection(5 to 5)
        }

    @Test
    fun cutUpdatesClipboardManager_placesCursorAtTheEndOfSelectedRegion_removesTheCutContent() =
        runTest(textFieldState = TextFieldState("Hello World!"), singleLine = true) {
            requestTextFieldFocus()
            setSelectionViaSemanticsShowingToolbar(1 to 5)

            val cutItem = assertTextToolbarHasItem(CUT)
            cutItem.invokeItem()

            assertClipboardText("ello")
            assertText("H World!")
            assertSelection(1 to 1)
        }

    @Test
    fun cutAppliesFilter() =
        runTest(
            textFieldState = TextFieldState("Hello World!"),
            singleLine = true,
            inputTransformation = {
                // only reject text changes, accept selection
                val initialSelection = selection
                replace(0, length, originalValue.toString())
                selection = initialSelection
            },
        ) {
            requestTextFieldFocus()
            setSelectionViaSemanticsShowingToolbar(1 to 5)

            val cutItem = assertTextToolbarHasItem(CUT)
            cutItem.invokeItem()

            assertClipboardText("ello")
            assertText("Hello World!")
            assertSelection(1 to 1)
        }

    @Test
    fun tappingTextField_hidesTheToolbar() = runTest {
        clickOffset(2)
        Handle.Cursor.click()
        assertTextToolbarShown()

        rule.mainClock.advanceTimeBy(1000) // to not cause double click
        clickOffset(2)
        assertTextToolbarNotShown()
    }

    @Test
    fun interactingWithTextFieldByMouse_doesNotShowTheToolbar() = runTest {
        clickOffset(2)
        Handle.Cursor.mouseClick()
        assertTextToolbarNotShown()
    }

    @Test
    fun toolbarDisappears_whenFocusIsLost() = runTest {
        clickOffset(2)
        Handle.Cursor.click()
        assertTextToolbarShown()

        rule.runOnUiThread { boxFocusRequester.requestFocus() }
        assertTextToolbarNotShown()
    }

    @Test
    fun toolbarDisappears_whenTextFieldIsDisposed() = runTest {
        clickOffset(2)
        Handle.Cursor.click()
        assertTextToolbarShown()

        showTextField = false
        assertTextToolbarNotShown()
    }

    @Test
    fun toolbarDisappears_whenLongPressIsInitiated() = runTest {
        clickOffset(2)
        Handle.Cursor.click()
        assertTextToolbarShown()

        tagInteraction.performTouchInput { longPress(positionForOffset(6)) }
        assertTextToolbarNotShown()
    }

    @Test
    fun toolbarCanReappear_whenTextFieldStateChanges() = runTest {
        clickOffset(2)
        Handle.Cursor.click()
        assertTextToolbarShown()

        // change the state
        textFieldState = TextFieldState("World")
        assertTextToolbarNotShown()

        // toolbar can now reappear if requested
        clickOffset(2)
        Handle.Cursor.click()
        assertTextToolbarShown()
    }

    private fun runTest(
        textFieldState: TextFieldState = TextFieldState("Hello"),
        singleLine: Boolean = false,
        readOnly: Boolean = false,
        clipboard: suspend () -> Clipboard = { FakeClipboard() },
        modifier: Modifier = Modifier,
        inputTransformation: InputTransformation? = null,
        block: suspend TestScope.() -> Unit,
    ) = coroutineRunTest {
        TestScope(
                initialTextFieldState = textFieldState,
                singleLine = singleLine,
                readOnly = readOnly,
                clipboard = clipboard(),
                modifier = modifier,
                filter = inputTransformation,
            )
            .block()
    }

    private inner class TestScope(
        initialTextFieldState: TextFieldState,
        private val singleLine: Boolean,
        private val readOnly: Boolean,
        private val clipboard: Clipboard,
        private val modifier: Modifier,
        private val filter: InputTransformation?,
    ) {
        var textFieldState by mutableStateOf(initialTextFieldState)
        var showTextField by mutableStateOf(true)
        var enabled by mutableStateOf(true)

        val boxFocusRequester = FocusRequester()

        private lateinit var view: View
        lateinit var spyTextActionModeCallback: SpyTextActionModeCallback

        init {
            rule.setTextFieldTestContent {
                view = LocalView.current
                spyTextActionModeCallback = SpyTextActionModeCallback()
                ProvidePlatformTextContextMenuToolbar(
                    callbackInjector = { spyTextActionModeCallback.apply { delegate = it } }
                ) {
                    CompositionLocalProvider(LocalClipboard provides clipboard) {
                        Column {
                            Box(
                                modifier =
                                    Modifier.focusRequester(boxFocusRequester)
                                        .focusable()
                                        .size(100.dp)
                            )
                            if (showTextField) {
                                BasicTextField(
                                    state = textFieldState,
                                    modifier = modifier.width(100.dp).testTag(TAG),
                                    textStyle =
                                        TextStyle(
                                            fontFamily = TEST_FONT_FAMILY,
                                            fontSize = fontSize,
                                        ),
                                    enabled = enabled,
                                    lineLimits =
                                        if (singleLine) {
                                            TextFieldLineLimits.SingleLine
                                        } else {
                                            TextFieldLineLimits.Default
                                        },
                                    inputTransformation = filter,
                                    readOnly = readOnly,
                                )
                            }
                        }
                    }
                }
            }
        }

        fun assertTextToolbarShown() {
            rule.waitForIdle()
            spyTextActionModeCallback.assertShown(true)
        }

        fun assertTextToolbarNotShown() {
            rule.waitForIdle()
            spyTextActionModeCallback.assertShown(false)
        }

        fun assertTextToolbarHasItem(title: String): MenuItem {
            assertTextToolbarShown()
            val items = spyTextActionModeCallback.menu!!.items()
            assertThat(items.map { it.title }).contains(title)
            return items.first { it.title == title }
        }

        fun MenuItem.invokeItem() {
            rule.runOnUiThread {
                val menu = spyTextActionModeCallback.menu!!
                menu.performIdentifierAction(itemId, Menu.FLAG_PERFORM_NO_CLOSE)
            }
        }

        suspend fun assertClipboardText(expectedClipboardText: String) {
            rule.waitForIdle()
            val clipEntry = assertNotNull(clipboard.getClipEntry()) { "No clip entry" }
            val clipboardText = assertNotNull(clipEntry.readText()) { "Clip entry is not text." }
            assertThat(clipboardText).isEqualTo(expectedClipboardText)
        }

        fun assertTextToolbarDoesNotHaveItem(title: String) {
            assertTextToolbarShown()
            val titles = spyTextActionModeCallback.menu!!.items().map { it.title }
            assertThat(titles).doesNotContain(title)
        }

        fun requestTextFieldFocus() {
            tagInteraction.requestFocus()
        }

        fun setSelectionViaSemantics(range: Pair<Int, Int>) {
            tagInteraction.performTextInputSelection(TextRange(range.first, range.second))
        }

        fun setSelectionViaSemanticsShowingToolbar(range: Pair<Int, Int>) {
            tagInteraction.performTextInputSelectionShowingToolbar(
                TextRange(range.first, range.second)
            )
        }

        fun assertText(expectedText: String) {
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
        }

        fun assertSelection(expectedRange: Pair<Int, Int>) {
            assertThat(textFieldState.selection)
                .isEqualTo(TextRange(expectedRange.first, expectedRange.second))
        }

        fun clickCenter() {
            tagInteraction.performTouchInput { click() }
        }

        fun clickOffset(offset: Int) {
            tagInteraction.performTouchInput { click(positionForOffset(offset)) }
        }

        fun longClickOffset(offset: Int) {
            tagInteraction.performTouchInput { longClick(positionForOffset(offset)) }
        }

        private val cursorInteraction: SemanticsNodeInteraction by lazy {
            rule.onNode(isSelectionHandle(Handle.Cursor))
        }

        val Handle.interaction: SemanticsNodeInteraction
            get() =
                if (this == Handle.Cursor) {
                    cursorInteraction
                } else {
                    rule.onNode(isSelectionHandle(this))
                }

        fun Handle.click() {
            assertShown()
            interaction.performClick()
        }

        fun Handle.mouseClick() {
            assertShown()
            interaction.performMouseInput { click() }
        }

        fun Handle.assertShown() {
            interaction.assertIsDisplayed()
        }

        fun Handle.assertDoesNotExist() {
            interaction.assertDoesNotExist()
        }

        fun positionForOffset(offset: Int): Offset = Offset(fontSizePx * offset, fontSizePx / 2)

        val tagInteraction: SemanticsNodeInteraction by lazy { rule.onNodeWithTag(TAG) }

        @OptIn(ExperimentalTestApi::class)
        fun performKeyInput(block: KeyInjectionScope.() -> Unit) {
            tagInteraction.requestFocus()
            tagInteraction.performKeyInput(block)
        }

        fun sendHardwareKeyEvent(key: Int, meta: Int) {
            view.dispatchKeyEvent(
                KeyEvent(
                    /* downTime = */ 0,
                    /* eventTime = */ 0,
                    /* action = */ ACTION_DOWN,
                    /* code = */ key,
                    /* repeat = */ 0,
                    /* metaState = */ meta,
                    /* deviceId = */ KeyCharacterMap.VIRTUAL_KEYBOARD,
                    /* scancode= */ 0,
                    /* flags= */ 0,
                    /* source= */ InputDevice.SOURCE_KEYBOARD,
                )
            )
        }
    }

    companion object {
        const val CUT = "Cut"
        const val COPY = "Copy"
        const val PASTE = "Paste"
        const val SELECT_ALL = "Select all"
        const val AUTOFILL = "Autofill"
    }
}
