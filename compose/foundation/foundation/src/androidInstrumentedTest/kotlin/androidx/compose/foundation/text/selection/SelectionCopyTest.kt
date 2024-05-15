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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.gestures.util.SelectionSubject
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SelectionCopyTest {
    @get:Rule
    val rule = createComposeRule()

    private val fontFamily = TEST_FONT_FAMILY
    private val fontSize = 20.sp
    private val testTextStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize)

    private val textTag = "textTag"

    private val selection = mutableStateOf<Selection?>(null)
    private val startClipboardText = "Clipboard content at start of test."

    @Test
    fun whenSelect_thenCopy_clipboardContainsSelectedText() {
        lateinit var clipboardManager: ClipboardManager
        val textContent = "text"
        val selectionRange = 0 to 4
        rule.setContent {
            clipboardManager = LocalClipboardManager.current
            TestContent(textContent)
        }

        rule.waitForIdle()
        val onNode = rule.onNodeWithTag(textTag)
        clipboardManager.setText(AnnotatedString(startClipboardText))
        onNode.startSelection()

        rule.waitForIdle()
        assertSelection(textContent, selectionRange)
        clipboardManager.assertClipboardText(startClipboardText)
        onNode.performCopy()

        rule.waitForIdle()
        assertSelection(textContent, selectionRange)
        clipboardManager.assertClipboardText(textContent)
    }

    // Regression test for b/322066508 where shortening the selected text
    // then trying to copy it would crash
    @Test
    fun whenSelect_thenEditUnderlyingText_thenCopy_clipboardContainsSelectedText() {
        val textContent = mutableStateOf("text")
        val selectionRange = 0 to 4
        lateinit var clipboardManager: ClipboardManager
        rule.setContent {
            clipboardManager = LocalClipboardManager.current
            TestContent(textContent.value)
        }

        rule.waitForIdle()
        val onNode = rule.onNodeWithTag(textTag)
        clipboardManager.setText(AnnotatedString(startClipboardText))
        onNode.startSelection()

        rule.waitForIdle()
        assertSelection(textContent.value, selectionRange)
        clipboardManager.assertClipboardText(startClipboardText)

        // shorten the text, the selection should shorten as well
        textContent.value = "tex"

        rule.waitForIdle()
        onNode.performCopy()

        rule.waitForIdle()
        assertSelection(textContent.value, null)
        clipboardManager.assertClipboardText(startClipboardText)
        textContent.value
    }

    @Composable
    private fun TestContent(textContent: String) {
        SelectionContainer(
            selection = selection.value,
            onSelectionChange = {
                selection.value = it
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            BasicText(
                text = textContent,
                modifier = Modifier
                    .wrapContentSize()
                    .testTag(textTag),
                style = testTextStyle
            )
        }
    }

    private fun SemanticsNodeInteraction.startSelection(offset: Int = 0) {
        val textLayoutResult = fetchTextLayoutResult()
        val boundingBox = textLayoutResult.getBoundingBox(offset)
        performTouchInput { longClick(boundingBox.center) }
    }

    private fun SemanticsNodeInteraction.performCopy() {
        performKeyInput {
            keyDown(Key.CtrlLeft)
            keyDown(Key.C)
            keyUp(Key.C)
            keyUp(Key.CtrlLeft)
        }
    }

    private fun ClipboardManager.assertClipboardText(textContent: String) {
        assertThat(getText()?.text).isEqualTo(textContent)
    }

    private fun assertSelection(text: String, selectionRange: Pair<Int, Int>?) {
        Truth.assertAbout(SelectionSubject.withContent(text))
            .that(selection.value)
            .hasSelection(
                expected = selectionRange?.run { TextRange(first, second) },
                startTextDirection = ResolvedTextDirection.Ltr,
                endTextDirection = ResolvedTextDirection.Ltr,
            )
    }
}
