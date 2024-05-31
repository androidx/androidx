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

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldSingleLineHeightTest : FocusedWindowTest {

    private val TextfieldTag = "textField"

    private val defaultText = "TEXT"

    // Arabic and Thai characters combined for super tall script
    private val tallText = "\u0627\u0644\u0646\u0635\u0E17\u0E35\u0E48"

    @get:Rule val rule = createComposeRule()

    @Test
    fun singleLineTextField_fromEmptyToTallText_updatesHeight() {
        val state = TextFieldState("")
        var reportedSize: IntSize = IntSize.Zero
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.onSizeChanged { reportedSize = it }
            )
        }

        rule.waitForIdle()
        val emptyHeight = reportedSize.height

        state.setTextAndPlaceCursorAtEnd(tallText)

        rule.waitForIdle()
        val tallHeight = reportedSize.height

        assertThat(emptyHeight).isLessThan(tallHeight)
    }

    @Test
    fun singleLineTextField_fromLatinToTallText_updatesHeight() {
        val state = TextFieldState(defaultText)
        var reportedSize: IntSize = IntSize.Zero
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.onSizeChanged { reportedSize = it }
            )
        }

        rule.waitForIdle()
        val latinHeight = reportedSize.height

        state.setTextAndPlaceCursorAtEnd(tallText)

        rule.waitForIdle()
        val tallHeight = reportedSize.height

        assertThat(latinHeight).isLessThan(tallHeight)
    }

    @Test
    fun singleLineTextField_withTallText_showsCursorHandle_whenClicked() {
        val state = TextFieldState(tallText)
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TextfieldTag)
            )
        }

        rule.onNodeWithTag(TextfieldTag).performClick()

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()
    }

    @Test
    fun multiLineTextField_withTallText_showsCursorHandle_whenClicked() {
        val state = TextFieldState(tallText)
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                lineLimits = TextFieldLineLimits.MultiLine(1, 1),
                modifier = Modifier.testTag(TextfieldTag)
            )
        }

        rule.onNodeWithTag(TextfieldTag).performClick()

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()
    }
}
