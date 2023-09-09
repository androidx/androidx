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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldVisualTransformationCursorTest {
    @get:Rule
    val rule = createComposeRule()

    // small enough to fit in narrow screen in pre-submit,
    // big enough that pointer movement can target a single char on center
    private val fontSize = 15.sp
    private val fontFamily = TEST_FONT_FAMILY
    private val testTag = "testTag"
    private val defaultText = "text"

    private val zeroedOffsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = 0
        override fun transformedToOriginal(offset: Int): Int = 0
    }

    private fun runTest(
        text: String = defaultText,
        visualTransformation: VisualTransformation = VisualTransformation.None,
        block: (MutableState<TextFieldValue>) -> Unit
    ) {
        val textFieldValue = mutableStateOf(TextFieldValue(text))
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                visualTransformation = visualTransformation,
                textStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
            )
        }
        block(textFieldValue)
    }

    @Test
    fun longPressOnNonEmpty_showsCursor() = runTest {
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        assertCursorHandleShown(shown = true)
    }

    @Test
    fun longPressOnEmpty_doesNotShowCursor() = runTest(
        text = "",
    ) {
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        assertCursorHandleShown(shown = false)
    }

    @Test
    fun longPressOnEmptyAfterVisualTransformation_doesNotShowCursor() = runTest(
        visualTransformation = {
            TransformedText(AnnotatedString(text = ""), zeroedOffsetMapping)
        },
    ) {
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        assertCursorHandleShown(shown = false)
    }

    @Test
    fun longPressOnNonEmptyAfterVisualTransformation_showsCursor() = runTest(
        text = "",
        visualTransformation = {
            TransformedText(AnnotatedString(text = defaultText), zeroedOffsetMapping)
        },
    ) {
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        assertCursorHandleShown(shown = true)
    }

    private fun assertCursorHandleShown(shown: Boolean) {
        val cursorHandle = rule.onNode(isSelectionHandle(Handle.Cursor))
        if (shown) cursorHandle.assertExists() else cursorHandle.assertDoesNotExist()
    }
}
