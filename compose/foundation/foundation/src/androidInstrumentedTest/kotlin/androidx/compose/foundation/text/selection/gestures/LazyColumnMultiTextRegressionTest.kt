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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.text.selection.gestures

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.readText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.contextmenu.internal.ProvidePlatformTextContextMenuToolbar
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.SpyTextActionModeCallback
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionHandleInfoKey
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.assertSelectionHandlesShown
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ContextMenuFlagFlipperRunner::class)
class LazyColumnMultiTextRegressionTest {
    @get:Rule val rule = createComposeRule()
    private val stateRestorationTester = StateRestorationTester(rule)
    private val textCount = 20

    // regression - text going out of composition and then returning
    // resulted in selection not working
    @Test
    fun whenTextScrollsOutOfCompositionAndThenBackIn_creatingSelectionStillPossible() = runTest {
        scrollDown()
        scrollUp()
        createSelection(line = 0)
        assertSelection().isNotNull()
    }

    @Test
    fun whenSelectionScrollsOutOfCompositionAndThenBackIn_selectionRemains() = runTest {
        createSelection(line = 0)
        assertSelection().isNotNull()
        val initialSelection = selection
        scrollDown()
        assertSelection().isEqualTo(initialSelection)
        scrollUp()
        assertSelection().isEqualTo(initialSelection)
    }

    // Copy currently doesn't work when the text leaves the view of a lazy layout
    @Ignore("b/298067102")
    @Test
    fun whenTextScrollsOutOfLazyLayoutBounds_copyCorrectlySetsClipboard() = runTest {
        resetClipboard()
        createSelection(startLine = 0, endLine = 4)
        assertSelection().isNotNull()
        scrollDown()
        performCopy()
        assertClipboardTextEquals("01234")
    }

    @Test
    fun whenScrollingTextOutOfViewUpwards_handlesDisappear() = runTest {
        var prevStart: Offset? = null
        var prevEnd: Offset? = null

        fun updateHandlePositions() {
            prevStart = startHandlePosition
            prevEnd = endHandlePosition
        }

        rule.assertSelectionHandlesShown(startShown = false, endShown = false)

        createSelection(startLine = 1, endLine = 3)
        rule.assertSelectionHandlesShown(startShown = true, endShown = true)
        updateHandlePositions()

        scrollLines(fromLine = 3, toLine = 2)
        rule.assertSelectionHandlesShown(startShown = true, endShown = true)
        assertPositionMovedUp(prevStart, startHandlePosition)
        assertPositionMovedUp(prevEnd, endHandlePosition)
        updateHandlePositions()

        scrollLines(fromLine = 4, toLine = 2)
        rule.assertSelectionHandlesShown(startShown = false, endShown = true)
        assertPositionMovedUp(prevEnd, endHandlePosition)

        scrollLines(fromLine = 6, toLine = 4)
        rule.assertSelectionHandlesShown(startShown = false, endShown = false)
        updateHandlePositions()

        scrollLines(fromLine = 6, toLine = 8)
        rule.assertSelectionHandlesShown(startShown = false, endShown = true)
        updateHandlePositions()

        scrollLines(fromLine = 4, toLine = 6)
        rule.assertSelectionHandlesShown(startShown = true, endShown = true)
        assertHandleMovedDown(prevEnd, endHandlePosition)
        updateHandlePositions()

        scrollLines(fromLine = 2, toLine = 5)
        rule.assertSelectionHandlesShown(startShown = true, endShown = true)
        assertHandleMovedDown(prevStart, startHandlePosition)
        assertHandleMovedDown(prevEnd, endHandlePosition)
        updateHandlePositions()
    }

    @Test
    fun whenScrollingTextOutOfViewUpwards_textToolbarCoercedToTop() = runTest {
        assertThat(textToolbarShown).isFalse()

        createSelection(startLine = 1, endLine = 3)
        assertThat(textToolbarShown).isTrue()
        assertTextToolbarTopAt(boundingBoxForLineInRoot(1).top)

        scrollLines(fromLine = 3, toLine = 1)
        assertThat(textToolbarShown).isTrue()
        assertTextToolbarTopAt(pointerAreaRect.top)

        scrollLines(fromLine = 5, toLine = 3)
        assertTextToolbarTopAt(pointerAreaRect.top)

        scrollLines(fromLine = 5, toLine = 7)
        assertThat(textToolbarShown).isTrue()
        assertTextToolbarTopAt(pointerAreaRect.top)

        scrollLines(fromLine = 3, toLine = 5)
        assertThat(textToolbarShown).isTrue()
        assertTextToolbarTopAt(boundingBoxForLineInRoot(1).top)
    }

    // TODO(b/298067619)
    //  When we support saving selection, this test should instead check that
    //  the previous and current selection is the same.
    //  Change test name to reflect this when implemented.
    @Test
    fun whenTextIsSavedRestored_clearsSelection() = runTest {
        createSelection(line = 0)
        assertSelection().isNotNull()
        stateRestorationTester.emulateSavedInstanceStateRestore()
        assertSelection().isNull()
    }

    @Suppress("DEPRECATION")
    private inner class TestScope(
        private val pointerAreaTag: String,
        private val selectionState: MutableState<Selection?>,
        private val clipboardManager: ClipboardManager,
        private val clipboard: Clipboard,
        private val textToolbar: TextToolbarWrapper,
        private val coroutineScope: CoroutineScope,
        private val spyTextActionModeCallback: SpyTextActionModeCallback,
    ) {
        val initialText = "Initial text"
        val selection: Selection?
            get() = Snapshot.withoutReadObservation { selectionState.value }

        val textToolbarRect: Rect?
            get() = textToolbar.mostRecentRect

        @OptIn(ExperimentalFoundationApi::class)
        val textToolbarShown: Boolean
            get() =
                if (ComposeFoundationFlags.isNewContextMenuEnabled) {
                    spyTextActionModeCallback.menu != null
                } else {
                    textToolbar.shown
                }

        val startHandlePosition
            get() = handlePosition(Handle.SelectionStart)

        val endHandlePosition
            get() = handlePosition(Handle.SelectionEnd)

        fun createSelection(startLine: Int, endLine: Int) {
            performTouchInput {
                longPress(positionForLineInPointerArea(startLine))
                moveTo(positionForLineInPointerArea(endLine))
                up()
            }
        }

        fun createSelection(line: Int) {
            performTouchInput { longClick(positionForLineInPointerArea(line)) }
        }

        private fun performTouchInput(block: TouchInjectionScope.() -> Unit) {
            rule.onNodeWithTag(pointerAreaTag).performTouchInput(block)
            rule.waitForIdle()
        }

        fun boundingBoxForLineInPointerArea(lineNumber: Int): Rect {
            val containerPosition =
                rule.onNodeWithTag(pointerAreaTag).fetchSemanticsNode().positionInRoot
            return boundingBoxForLineInRoot(lineNumber).translate(-containerPosition)
        }

        fun boundingBoxForLineInRoot(lineNumber: Int): Rect {
            val textTag = lineNumber.toString()
            val textPosition = rule.onNodeWithTag(textTag).fetchSemanticsNode().positionInRoot
            val textLayoutResult = rule.onNodeWithTag(textTag).fetchTextLayoutResult()
            val lineStart = textLayoutResult.getLineStart(0)
            val lineEnd = textLayoutResult.getLineEnd(0)

            val rect =
                if (lineStart == lineEnd - 1) {
                    textLayoutResult.getBoundingBox(lineStart)
                } else {
                    val startRect = textLayoutResult.getBoundingBox(lineStart)
                    val endRect = textLayoutResult.getBoundingBox(lineEnd - 1)
                    Rect(
                        left = minOf(startRect.left, endRect.left),
                        top = minOf(startRect.top, endRect.top),
                        right = maxOf(startRect.right, endRect.right),
                        bottom = maxOf(startRect.bottom, endRect.bottom),
                    )
                }

            return rect.translate(textPosition)
        }

        fun positionForLineInPointerArea(lineNumber: Int): Offset =
            boundingBoxForLineInPointerArea(lineNumber).center

        @OptIn(ExperimentalTestApi::class)
        fun performCopy() {
            rule.onNodeWithTag(pointerAreaTag).performKeyInput {
                keyDown(Key.CtrlLeft)
                keyDown(Key.C)
                keyUp(Key.C)
                keyUp(Key.CtrlLeft)
            }
            rule.waitForIdle()
        }

        fun resetClipboard() {
            clipboardManager.setText(AnnotatedString(initialText))
        }

        fun assertClipboardTextEquals(text: String) {
            val actualClipboardText = clipboardManager.getText()?.text
            assertWithMessage("Clipboard contents was not changed.")
                .that(actualClipboardText)
                .isNotEqualTo(initialText)
            assertWithMessage("""Clipboard set to incorrect content: "$actualClipboardText".""")
                .that(actualClipboardText)
                .isEqualTo(text)

            var actualSuspendClipboardText: String? = null
            rule.waitUntil {
                coroutineScope
                    .launch(start = CoroutineStart.UNDISPATCHED) {
                        actualSuspendClipboardText = clipboard.getClipEntry()?.readText()
                    }
                    .isCompleted
            }

            assertWithMessage("Clipboard contents was not changed.")
                .that(actualSuspendClipboardText)
                .isNotEqualTo(initialText)
            assertWithMessage(
                    """Clipboard set to incorrect content: "$actualSuspendClipboardText"."""
                )
                .that(actualSuspendClipboardText)
                .isEqualTo(text)

            resetClipboard()
        }

        fun assertSelection(): Subject = assertThat(selection)

        fun scrollDown() {
            performTouchInput { swipe(bottomCenter - Offset(0f, 1f), topCenter + Offset(0f, 1f)) }
        }

        fun scrollUp() {
            performTouchInput { swipe(topCenter + Offset(0f, 1f), bottomCenter - Offset(0f, 1f)) }
        }

        fun scrollLines(fromLine: Int, toLine: Int) {
            performTouchInput {
                swipe(positionForLineInPointerArea(fromLine), positionForLineInPointerArea(toLine))
            }
        }

        fun assertPositionMovedUp(previous: Offset?, current: Offset?) {
            assertHandleMoved(previous, current, up = true)
        }

        fun assertHandleMovedDown(previous: Offset?, current: Offset?) {
            assertHandleMoved(previous, current, up = false)
        }

        private fun assertHandleMoved(previous: Offset?, current: Offset?, up: Boolean) {
            assertWithMessage("previous handle position should not be null")
                .that(previous)
                .isNotNull()

            assertWithMessage("current handle position should not be null")
                .that(current)
                .isNotNull()

            val (x, y) = current!!
            val (prevX, prevY) = previous!!

            assertWithMessage("x should not change").that(x).isWithin(0.1f).of(prevX)

            assertWithMessage("y should have moved ${if (up) "up" else "down"}").that(y).run {
                if (up) isLessThan(prevY) else isGreaterThan(prevY)
            }
        }

        private fun handlePosition(handle: Handle): Offset? =
            rule
                .onAllNodes(isSelectionHandle(handle))
                .fetchSemanticsNodes()
                .singleOrNull()
                ?.config
                ?.get(SelectionHandleInfoKey)
                ?.position

        @OptIn(ExperimentalFoundationApi::class)
        fun assertTextToolbarTopAt(y: Float) {
            val top =
                if (ComposeFoundationFlags.isNewContextMenuEnabled) {
                    spyTextActionModeCallback.contentRect?.top
                } else {
                    textToolbarRect?.top
                }
            assertThat(top).isWithin(0.1f).of(y)
        }

        val pointerAreaRect: Rect
            get() = rule.onNodeWithTag(pointerAreaTag).fetchSemanticsNode().boundsInRoot
    }

    @Suppress("DEPRECATION")
    private fun runTest(block: TestScope.() -> Unit) {
        val tag = "tag"
        val selection = mutableStateOf<Selection?>(null)
        val testViewConfiguration = TestViewConfiguration(minimumTouchTargetSize = DpSize.Zero)
        val spyTextActionModeCallback = SpyTextActionModeCallback()
        lateinit var clipboardManager: ClipboardManager
        lateinit var clipboard: Clipboard
        lateinit var textToolbar: TextToolbarWrapper
        lateinit var coroutineScope: CoroutineScope
        stateRestorationTester.setContent {
            clipboardManager = LocalClipboardManager.current
            clipboard = LocalClipboard.current
            val originalTextToolbar = LocalTextToolbar.current
            textToolbar = remember(originalTextToolbar) { TextToolbarWrapper(originalTextToolbar) }
            coroutineScope = rememberCoroutineScope()
            ProvidePlatformTextContextMenuToolbar(
                callbackInjector = { spyTextActionModeCallback.apply { delegate = it } }
            ) {
                CompositionLocalProvider(
                    LocalTextToolbar provides textToolbar,
                    LocalViewConfiguration provides testViewConfiguration,
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SelectionContainer(
                            modifier = Modifier.height(100.dp),
                            selection = selection.value,
                            onSelectionChange = { selection.value = it },
                        ) {
                            LazyColumn(modifier = Modifier.testTag(tag)) {
                                items(count = textCount) {
                                    BasicText(
                                        text = it.toString(),
                                        style =
                                            TextStyle(
                                                fontSize = 15.sp,
                                                textAlign = TextAlign.Center,
                                            ),
                                        modifier = Modifier.fillMaxWidth().testTag(it.toString()),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val scope =
            TestScope(
                tag,
                selection,
                clipboardManager,
                clipboard,
                textToolbar,
                coroutineScope,
                spyTextActionModeCallback,
            )
        scope.resetClipboard()
        scope.block()
    }
}

private class TextToolbarWrapper(private val delegate: TextToolbar) : TextToolbar {
    private var _shown: Boolean = false
    val shown: Boolean
        get() = _shown

    private var _mostRecentRect: Rect? = null
    val mostRecentRect: Rect?
        get() = _mostRecentRect

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
        onAutofillRequested: (() -> Unit)?,
    ) {
        _shown = true
        _mostRecentRect = rect
        delegate.showMenu(
            rect,
            onCopyRequested,
            onPasteRequested,
            onCutRequested,
            onSelectAllRequested,
            onAutofillRequested,
        )
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        _shown = true
        _mostRecentRect = rect
        delegate.showMenu(
            rect,
            onCopyRequested,
            onPasteRequested,
            onCutRequested,
            onSelectAllRequested,
        )
    }

    override fun hide() {
        _shown = false
        delegate.hide()
    }

    override val status: TextToolbarStatus
        get() = delegate.status
}
