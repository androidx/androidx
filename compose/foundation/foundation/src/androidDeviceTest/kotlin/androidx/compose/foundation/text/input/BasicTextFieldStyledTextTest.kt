/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextFieldStyledTextTest {

    @get:Rule val rule = createComposeRule()

    private val tag = "BasicTextField"

    private val style = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp)

    companion object {
        var oldIsBasicTextFieldStyledTextEnabled: Boolean = true

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            oldIsBasicTextFieldStyledTextEnabled =
                ComposeFoundationFlags.isBasicTextFieldStyledTextEnabled
            ComposeFoundationFlags.isBasicTextFieldStyledTextEnabled = true
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            ComposeFoundationFlags.isBasicTextFieldStyledTextEnabled =
                oldIsBasicTextFieldStyledTextEnabled
        }
    }

    @Test
    fun styledText_isAppliedOnTextLayout() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val state = TextFieldState("Hello")
        state.edit {
            addStyle(boldStyle, 0, 1)
            addStyle(boldStyle, 4, 5)
        }

        rule.setContent {
            BasicTextField(state = state, modifier = Modifier.testTag(tag), textStyle = style)
        }

        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        assertThat(textLayoutResult.layoutInput.text.spanStyles.size).isEqualTo(2)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].start).isEqualTo(0)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].end).isEqualTo(1)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].item).isEqualTo(boldStyle)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[1].start).isEqualTo(4)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[1].end).isEqualTo(5)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[1].item).isEqualTo(boldStyle)
    }

    @Test
    fun styledText_paragraphStyle_isAppliedOnTextLayout() {
        val alignStyle =
            androidx.compose.ui.text.ParagraphStyle(
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        val state = TextFieldState("Hello")
        state.edit {
            addStyle(alignStyle, 0, 1)
            addStyle(alignStyle, 4, 5)
        }

        rule.setContent {
            BasicTextField(state = state, modifier = Modifier.testTag(tag), textStyle = style)
        }

        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        assertThat(textLayoutResult.layoutInput.text.paragraphStyles.size).isEqualTo(2)
        assertThat(textLayoutResult.layoutInput.text.paragraphStyles[0].start).isEqualTo(0)
        assertThat(textLayoutResult.layoutInput.text.paragraphStyles[0].end).isEqualTo(1)
        assertThat(textLayoutResult.layoutInput.text.paragraphStyles[0].item).isEqualTo(alignStyle)
        assertThat(textLayoutResult.layoutInput.text.paragraphStyles[1].start).isEqualTo(4)
        assertThat(textLayoutResult.layoutInput.text.paragraphStyles[1].end).isEqualTo(5)
        assertThat(textLayoutResult.layoutInput.text.paragraphStyles[1].item).isEqualTo(alignStyle)
    }

    @Test
    fun styledText_withTextChanges_isAppliedOnTextLayout() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val state = TextFieldState("Hello")

        rule.setContent {
            BasicTextField(state = state, modifier = Modifier.testTag(tag), textStyle = style)
        }

        state.edit {
            append(" World!")
            addStyle(boldStyle, 0, length)
        }

        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        assertThat(textLayoutResult.layoutInput.text.toString()).isEqualTo("Hello World!")
        assertThat(textLayoutResult.layoutInput.text.spanStyles.size).isEqualTo(1)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].start).isEqualTo(0)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].end).isEqualTo(12)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].item).isEqualTo(boldStyle)
    }

    @Test
    fun styledText_rangeUpdatedWhenTextInserted() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val state = TextFieldState("Hello")
        state.edit { addStyle(boldStyle, 0, 5) }

        rule.setContent {
            BasicTextField(state = state, modifier = Modifier.testTag(tag), textStyle = style)
        }

        state.edit {
            insert(2, "xx") // insert inside the styled range
        }

        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        assertThat(textLayoutResult.layoutInput.text.toString()).isEqualTo("Hexxllo")
        assertThat(textLayoutResult.layoutInput.text.spanStyles.size).isEqualTo(1)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].start).isEqualTo(0)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].end).isEqualTo(7)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].item).isEqualTo(boldStyle)
    }

    @Test
    fun styledText_inputTransformation_isAppliedOnTextLayout() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val state = TextFieldState("Hello")

        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(tag),
                textStyle = style,
                inputTransformation = { addStyle(boldStyle, 0, length) },
            )
        }

        rule.onNodeWithTag(tag).performTextInput(" World!")

        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        assertThat(textLayoutResult.layoutInput.text.toString()).isEqualTo("Hello World!")
        assertThat(textLayoutResult.layoutInput.text.spanStyles.size).isEqualTo(1)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].start).isEqualTo(0)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].end).isEqualTo(12)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].item).isEqualTo(boldStyle)
    }

    @Test
    fun styledText_outputTransformation_isAppliedOnTextLayout() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val state = TextFieldState("Hello")

        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(tag),
                textStyle = style,
                outputTransformation = {
                    append(" World!")
                    addStyle(boldStyle, 0, length)
                },
            )
        }

        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        assertThat(textLayoutResult.layoutInput.text.toString()).isEqualTo("Hello World!")
        assertThat(textLayoutResult.layoutInput.text.spanStyles.size).isEqualTo(1)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].start).isEqualTo(0)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].end).isEqualTo(12)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].item).isEqualTo(boldStyle)
    }

    @Test
    fun styledText_stateAndOutputTransformation_isAppliedOnTextLayout() {
        val redStyle = SpanStyle(color = androidx.compose.ui.graphics.Color.Red)
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val state = TextFieldState("Hello")
        state.edit { addStyle(redStyle, 0, 5) }

        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(tag),
                textStyle = style,
                outputTransformation = { addStyle(boldStyle, 0, length) },
            )
        }

        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        assertThat(textLayoutResult.layoutInput.text.toString()).isEqualTo("Hello")
        assertThat(textLayoutResult.layoutInput.text.spanStyles.size).isEqualTo(2)
        val styles = textLayoutResult.layoutInput.text.spanStyles.map { it.item }
        assertThat(styles).containsExactly(redStyle, boldStyle)
    }

    @Test
    fun styledText_inputTransformation_doesChangeState() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val state = TextFieldState("Hello")

        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(tag),
                textStyle = style,
                inputTransformation = { addStyle(boldStyle, 0, length) },
            )
        }

        rule.onNodeWithTag(tag).performTextInput(" World!")

        assertThat(state.text.toString()).isEqualTo("Hello World!")
        assertThat(state.value.textStyleBuffer?.getAllStyles()?.size).isEqualTo(1)
        assertThat(state.value.textStyleBuffer?.getAllStyles()?.get(0)?.item).isEqualTo(boldStyle)
        assertThat(state.value.textStyleBuffer?.getAllStyles()?.get(0)?.start).isEqualTo(0)
        assertThat(state.value.textStyleBuffer?.getAllStyles()?.get(0)?.end).isEqualTo(12)
    }

    @Test
    fun styledText_outputTransformation_doesNotChangeState() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val state = TextFieldState("Hello")

        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(tag),
                textStyle = style,
                outputTransformation = {
                    append(" World!")
                    addStyle(boldStyle, 0, length)
                },
            )
        }

        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        assertThat(textLayoutResult.layoutInput.text.toString()).isEqualTo("Hello World!")

        assertThat(state.text.toString()).isEqualTo("Hello")
        assertThat(state.value.textStyleBuffer?.getStyles<SpanStyle>(0, 5)).isNull()
    }
}
