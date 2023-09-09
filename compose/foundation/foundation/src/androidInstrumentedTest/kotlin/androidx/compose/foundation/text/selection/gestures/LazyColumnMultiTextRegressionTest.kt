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

package androidx.compose.foundation.text.selection.gestures

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalViewConfiguration
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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class LazyColumnMultiTextRegressionTest {
    @get:Rule
    val rule = createComposeRule()
    private val stateRestorationTester = StateRestorationTester(rule)

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

    private inner class TestScope(
        private val pointerAreaTag: String,
        private val selectionState: MutableState<Selection?>,
        private val clipboardManager: ClipboardManager,
    ) {
        val initialText = "Initial text"
        val selection get() = Snapshot.withoutReadObservation { selectionState.value }

        fun createSelection(startLine: Int, endLine: Int) {
            performTouchInput {
                longPress(positionForLine(startLine))
                moveTo(positionForLine(endLine))
                up()
            }
        }

        fun createSelection(line: Int) {
            performTouchInput {
                longClick(positionForLine(line))
            }
        }

        private fun performTouchInput(block: TouchInjectionScope.() -> Unit) {
            rule.onNodeWithTag(pointerAreaTag).performTouchInput(block)
            rule.waitForIdle()
        }

        private fun positionForLine(lineNumber: Int): Offset {
            val containerPosition = rule.onNodeWithTag(pointerAreaTag).fetchSemanticsNode()
                .positionInRoot
                .also { println(it) }

            val textTag = lineNumber.toString()
            val textPosition = rule.onNodeWithTag(textTag).fetchSemanticsNode()
                .positionInRoot
                .also { println(it) }

            val textLayoutResult = rule.onNodeWithTag(textTag)
                .fetchTextLayoutResult()

            return textLayoutResult.getBoundingBox(0)
                .translate(textPosition - containerPosition)
                .center
                .also { println(it) }
        }

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
            resetClipboard()
        }

        fun assertSelection(): Subject = assertThat(selection)

        fun scrollDown() {
            performTouchInput {
                swipe(bottomCenter - Offset(0f, 1f), topCenter + Offset(0f, 1f))
            }
        }

        fun scrollUp() {
            performTouchInput {
                swipe(topCenter + Offset(0f, 1f), bottomCenter - Offset(0f, 1f))
            }
        }
    }

    private fun runTest(block: TestScope.() -> Unit) {
        val tag = "tag"
        val selection = mutableStateOf<Selection?>(null)
        val testViewConfiguration = TestViewConfiguration(
            minimumTouchTargetSize = DpSize.Zero
        )
        lateinit var clipboardManager: ClipboardManager
        stateRestorationTester.setContent {
            clipboardManager = LocalClipboardManager.current
            CompositionLocalProvider(LocalViewConfiguration provides testViewConfiguration) {
                SelectionContainer(
                    selection = selection.value,
                    onSelectionChange = { selection.value = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentHeight()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .height(100.dp)
                            .wrapContentHeight()
                            .testTag(tag)
                    ) {
                        items(count = 20) {
                            BasicText(
                                text = it.toString(),
                                style = TextStyle(fontSize = 15.sp, textAlign = TextAlign.Center),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(it.toString())
                            )
                        }
                    }
                }
            }
        }

        val scope = TestScope(tag, selection, clipboardManager)
        scope.resetClipboard()
        scope.block()
    }
}
