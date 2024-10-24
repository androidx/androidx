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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.handwriting.HandwritingBoundsVerticalOffset
import androidx.compose.foundation.text.handwriting.isStylusHandwritingSupported
import androidx.compose.foundation.text.performStylusHandwriting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextFieldHandwritingBoundsTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val immRule = ComposeInputMethodManagerTestRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val imm = FakeInputMethodManager()

    @Before
    fun setup() {
        // Test is only meaningful when stylus handwriting is supported.
        assumeTrue(isStylusHandwritingSupported)
    }

    @Test
    fun basicTextField_stylusPointerInEditorBounds_focusAndStartHandwriting() {
        immRule.setFactory { imm }

        val editorTag1 = "BasicTextField1"
        val editorTag2 = "BasicTextField2"

        inputMethodInterceptor.setTextFieldTestContent {
            Column(Modifier.safeContentPadding()) {
                EditLine(Modifier.testTag(editorTag1))
                EditLine(Modifier.testTag(editorTag2))
            }
        }

        rule.onNodeWithTag(editorTag1).performStylusHandwriting()

        rule.waitForIdle()

        rule.onNodeWithTag(editorTag1).assertIsFocused()
        imm.expectCall("startStylusHandwriting")
    }

    @Test
    fun basicTextField_stylusPointerInOverlappingArea_focusedEditorStartHandwriting() {
        immRule.setFactory { imm }

        val editorTag1 = "BasicTextField1"
        val editorTag2 = "BasicTextField2"
        val spacerTag = "Spacer"

        inputMethodInterceptor.setTextFieldTestContent {
            Column(Modifier.safeContentPadding()) {
                EditLine(Modifier.testTag(editorTag1))
                Spacer(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(HandwritingBoundsVerticalOffset)
                            .testTag(spacerTag)
                )
                EditLine(Modifier.testTag(editorTag2))
            }
        }

        rule.onNodeWithTag(editorTag2).requestFocus()
        rule.waitForIdle()

        // Spacer's height equals to HandwritingBoundsVerticalPadding, both editor will receive the
        // event.
        rule.onNodeWithTag(spacerTag).performStylusHandwriting()
        rule.waitForIdle()

        // Assert that focus didn't change, handwriting is started on the focused editor 2.
        rule.onNodeWithTag(editorTag2).assertIsFocused()
        imm.expectCall("startStylusHandwriting")

        rule.onNodeWithTag(editorTag1).requestFocus()
        rule.onNodeWithTag(spacerTag).performStylusHandwriting()
        rule.waitForIdle()

        // Now handwriting is performed on the focused editor 1.
        rule.onNodeWithTag(editorTag1).assertIsFocused()
        imm.expectCall("startStylusHandwriting")
    }

    @Composable
    fun EditLine(modifier: Modifier = Modifier) {
        val state = remember { TextFieldState() }
        BasicTextField(
            state = state,
            modifier =
                modifier
                    .fillMaxWidth()
                    // make the size of TextFields equal to padding, so that touch bounds of editors
                    // in the same column/row are overlapping.
                    .height(HandwritingBoundsVerticalOffset)
        )
    }
}
