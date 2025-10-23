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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.PlatformSelectionBehaviorCommonTestCases
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.internal.CodepointTransformation
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
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
class TextFieldPlatformSelectionBehaviorsTest(override val testLongPress: Boolean) :
    PlatformSelectionBehaviorCommonTestCases() {

    private var textFieldState: TextFieldState = TextFieldState()

    override val selection: TextRange
        get() = textFieldState.selection

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "testLongPress={0}")
        fun params() = arrayOf(true, false)
    }

    @Composable
    override fun Content(text: String, textStyle: TextStyle, modifier: Modifier) {
        val state = remember(text) { TextFieldState(text).also { this.textFieldState = it } }
        BasicTextField(state = state, textStyle = textStyle, modifier = modifier)
    }

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
    fun withOutputTransformation() {
        val state = TextFieldState("xxx")

        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(200.dp),
                outputTransformation =
                    object : OutputTransformation {
                        override fun TextFieldBuffer.transformOutput() {
                            insert(0, "abc ")
                            insert(length, " def")
                        }
                    },
            )
        }

        // The visual text is "abc xxx def"
        // select "xxx".
        performLongPressOrDoubleClick { Offset(x = fontSize.toPx() * 5, y = fontSize.toPx() / 2) }

        assertThat(state.selection).isEqualTo(TextRange(0, 3))
        // The prefix "abc " is also considered selected by OutputTransformation.
        platformSelectionBehaviorsRule.expectSuggestSelectionForLongPressOrDoubleClick(
            "abc xxx def",
            TextRange(0, 7),
        )
        expectOnShowContextMenu("abc xxx def", TextRange(0, 7))
    }

    @Test
    fun withCodePointTransformation() {
        val state = TextFieldState("abc xxx def")

        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(200.dp),
                codepointTransformation =
                    object : CodepointTransformation {
                        override fun transform(codepointIndex: Int, codepoint: Int): Int =
                            if (codepoint == 'x'.code) '*'.code else codepoint
                    },
            )
        }

        // The visual text is "abc xxx def"
        //  select "xxx".
        performLongPressOrDoubleClick { Offset(x = fontSize.toPx() * 5, y = fontSize.toPx() / 2) }

        assertThat(state.selection).isEqualTo(TextRange(4, 7))

        platformSelectionBehaviorsRule.expectSuggestSelectionForLongPressOrDoubleClick(
            "abc *** def",
            TextRange(4, 7),
        )
        expectOnShowContextMenu("abc *** def", TextRange(4, 7))
    }
}
