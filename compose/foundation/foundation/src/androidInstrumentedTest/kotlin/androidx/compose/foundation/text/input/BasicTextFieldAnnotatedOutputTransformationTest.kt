/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextFieldAnnotatedOutputTransformationTest {

    @get:Rule val rule = createComposeRule()

    private val tag = "BasicTextField"

    private val state = TextFieldState("Hello")

    private val style = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp)

    @Test
    fun annotatedOutputTransformation_isAppliedOnTextLayout() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(tag),
                outputTransformation =
                    OutputTransformation {
                        addStyle(boldStyle, 0, 1)
                        addStyle(boldStyle, 4, 5)
                    },
                textStyle = style,
            )
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
    fun annotatedOutputTransformation_withTextChanges_isAppliedOnTextLayout() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(tag),
                outputTransformation =
                    OutputTransformation {
                        append(" World!")
                        addStyle(boldStyle, 0, length)
                    },
                textStyle = style,
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
    fun annotatedOutputTransformation_changesByState() {
        var spanStyle by mutableStateOf(SpanStyle(fontSize = 12.sp))
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(tag),
                outputTransformation = OutputTransformation { addStyle(spanStyle, 0, length) },
                textStyle = style,
            )
        }

        var textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        assertThat(textLayoutResult.layoutInput.text.spanStyles.size).isEqualTo(1)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].start).isEqualTo(0)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].end).isEqualTo(5)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].item.fontSize).isEqualTo(12.sp)

        spanStyle = SpanStyle(fontSize = 14.sp)

        textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        assertThat(textLayoutResult.layoutInput.text.spanStyles.size).isEqualTo(1)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].start).isEqualTo(0)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].end).isEqualTo(5)
        assertThat(textLayoutResult.layoutInput.text.spanStyles[0].item.fontSize).isEqualTo(14.sp)
    }
}
