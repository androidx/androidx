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

package androidx.compose.foundation.text

import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.ExtractedText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.setFocusableContent
import androidx.compose.foundation.text.handwriting.HandwritingBoundsVerticalOffset
import androidx.compose.foundation.text.handwriting.isStylusHandwritingSupported
import androidx.compose.foundation.text.input.InputMethodInterceptor
import androidx.compose.foundation.text.input.internal.InputMethodManager
import androidx.compose.foundation.text.input.internal.inputMethodManagerFactory
import androidx.compose.foundation.text.matchers.isZero
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldHandwritingBoundsTest {
    @get:Rule val rule = createComposeRule()
    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val fakeImm =
        object : InputMethodManager {
            private var stylusHandwritingStartCount = 0

            fun expectStylusHandwriting(started: Boolean) {
                if (started) {
                    assertThat(stylusHandwritingStartCount).isEqualTo(1)
                    stylusHandwritingStartCount = 0
                } else {
                    assertThat(stylusHandwritingStartCount).isZero()
                }
            }

            override fun isActive(): Boolean = true

            override fun restartInput() {}

            override fun showSoftInput() {}

            override fun hideSoftInput() {}

            override fun updateExtractedText(token: Int, extractedText: ExtractedText) {}

            override fun updateSelection(
                selectionStart: Int,
                selectionEnd: Int,
                compositionStart: Int,
                compositionEnd: Int
            ) {}

            override fun updateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {}

            override fun startStylusHandwriting() {
                ++stylusHandwritingStartCount
            }
        }

    @Before
    fun setup() {
        // Test is only meaningful when stylusHandwriting is supported.
        assumeTrue(isStylusHandwritingSupported)
    }

    @Test
    fun coreTextField_stylusPointerInEditorBounds_focusAndStartHandwriting() {
        inputMethodManagerFactory = { fakeImm }

        val editorTag1 = "CoreTextField1"
        val editorTag2 = "CoreTextField2"

        setContent {
            Column(Modifier.safeContentPadding()) {
                EditLine(Modifier.testTag(editorTag1))
                EditLine(Modifier.testTag(editorTag2))
            }
        }

        rule.onNodeWithTag(editorTag1).performStylusHandwriting()

        rule.waitForIdle()

        rule.onNodeWithTag(editorTag1).assertIsFocused()
        fakeImm.expectStylusHandwriting(true)
    }

    @Test
    fun coreTextField_stylusPointerInOverlappingArea_focusedEditorStartHandwriting() {
        inputMethodManagerFactory = { fakeImm }

        val editorTag1 = "CoreTextField1"
        val editorTag2 = "CoreTextField2"
        val spacerTag = "Spacer"

        setContent {
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
        fakeImm.expectStylusHandwriting(true)

        rule.onNodeWithTag(editorTag1).requestFocus()
        rule.onNodeWithTag(spacerTag).performStylusHandwriting()
        rule.waitForIdle()

        // Now handwriting is performed on the focused editor 1.
        rule.onNodeWithTag(editorTag1).assertIsFocused()
        fakeImm.expectStylusHandwriting(true)
    }

    @Composable
    fun EditLine(modifier: Modifier = Modifier) {
        var value by remember { mutableStateOf(TextFieldValue()) }
        CoreTextField(
            value = value,
            onValueChange = { value = it },
            modifier =
                modifier
                    .fillMaxWidth()
                    // make the size of TextFields equal to padding, so that touch bounds of editors
                    // in the same column/row are overlapping.
                    .height(HandwritingBoundsVerticalOffset)
        )
    }

    private fun setContent(
        extraItemForInitialFocus: Boolean = true,
        content: @Composable () -> Unit
    ) {
        rule.setFocusableContent(extraItemForInitialFocus) {
            inputMethodInterceptor.Content { content() }
        }
    }
}
