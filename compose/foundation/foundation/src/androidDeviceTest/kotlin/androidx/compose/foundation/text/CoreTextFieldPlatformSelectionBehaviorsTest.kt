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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 28)
class CoreTextFieldPlatformSelectionBehaviorsTest(override val testLongPress: Boolean) :
    PlatformSelectionBehaviorCommonTestCases() {
    private var textFieldValue = mutableStateOf(TextFieldValue())

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "testLongPress={0}")
        fun params() = arrayOf(true, false)
    }

    @Composable
    override fun Content(text: String, textStyle: TextStyle, modifier: Modifier) {
        var value by
            remember(text) {
                mutableStateOf(TextFieldValue(text)).also { this.textFieldValue = it }
            }
        CoreTextField(
            value = value,
            onValueChange = { value = it },
            modifier = modifier,
            textStyle = textStyle,
        )
    }

    override val selection: TextRange
        get() = textFieldValue.value.selection

    @Test
    fun longPress_onEmptyRegion_notCallSuggestSelectionForLongPressOrDoubleClick() {
        assumeTrue(testLongPress)
        rule.setTextFieldTestContent {
            Content(
                text = "abc def",
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(centerRight)
            up()
        }

        rule.waitForIdle()

        // Cursor at the end.
        assertThat(selection).isEqualTo(TextRange(7))
        expectOnShowContextMenu("abc def", TextRange(7))
        platformSelectionBehaviorsRule.assertNoMoreCalls()
    }

    @Test
    fun withVisualTransformation() {
        var value by mutableStateOf(TextFieldValue("xxx"))

        rule.setTextFieldTestContent {
            CoreTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(TAG).width(200.dp),
                textStyle = defaultTextStyle,
                visualTransformation =
                    object : VisualTransformation {
                        override fun filter(text: AnnotatedString): TransformedText {
                            return TransformedText(
                                text = AnnotatedString("abc $text def"),
                                offsetMapping =
                                    object : OffsetMapping {
                                        override fun originalToTransformed(offset: Int): Int {
                                            return offset + 4
                                        }

                                        override fun transformedToOriginal(offset: Int): Int {
                                            return (offset - 4).coerceIn(0, text.length)
                                        }
                                    },
                            )
                        }
                    },
            )
        }

        // The visual text is "abc xxx def"
        // select "xxx".
        performLongPressOrDoubleClick { Offset(x = fontSize.toPx() * 5, y = fontSize.toPx() / 2) }

        assertThat(value.selection).isEqualTo(TextRange(0, 3))
        platformSelectionBehaviorsRule.expectSuggestSelectionForLongPressOrDoubleClick(
            "abc xxx def",
            TextRange(4, 7),
        )
        expectOnShowContextMenu("abc xxx def", TextRange(4, 7))
    }

    @Test
    fun withVisualTransformation_codepointOnly() {
        var value by mutableStateOf(TextFieldValue("abc xxx ghi"))

        rule.setTextFieldTestContent {
            CoreTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(TAG).width(200.dp),
                textStyle = defaultTextStyle,
                visualTransformation =
                    object : VisualTransformation {
                        override fun filter(text: AnnotatedString): TransformedText {
                            return TransformedText(
                                text = AnnotatedString(text.replaceRange(4, 7, "def").toString()),
                                offsetMapping =
                                    object : OffsetMapping {
                                        override fun originalToTransformed(offset: Int): Int =
                                            offset

                                        override fun transformedToOriginal(offset: Int): Int =
                                            offset
                                    },
                            )
                        }
                    },
            )
        }

        // The visual text is "abc xxx def"
        // select "xxx".
        performLongPressOrDoubleClick { Offset(x = fontSize.toPx() * 5, y = fontSize.toPx() / 2) }

        assertThat(value.selection).isEqualTo(TextRange(4, 7))
        platformSelectionBehaviorsRule.expectSuggestSelectionForLongPressOrDoubleClick(
            "abc def ghi",
            TextRange(4, 7),
        )
    }
}
