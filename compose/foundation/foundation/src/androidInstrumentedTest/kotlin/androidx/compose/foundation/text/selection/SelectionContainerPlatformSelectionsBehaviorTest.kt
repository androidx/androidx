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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.PlatformSelectionBehaviorCommonTestCases
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 28)
class SelectionContainerPlatformSelectionsBehaviorTest(override val testLongPress: Boolean) :
    PlatformSelectionBehaviorCommonTestCases() {
    private var _selection: MutableState<Selection?>? = null

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "testLongPress={0}")
        fun params() = arrayOf(true, false)
    }

    @Composable
    override fun Content(text: String, textStyle: TextStyle, modifier: Modifier) {
        var selection by remember { mutableStateOf<Selection?>(null).also { _selection = it } }
        SelectionContainer(selection = selection, onSelectionChange = { selection = it }) {
            BasicText(text = text, modifier = modifier, style = textStyle)
        }
    }

    override val selection: TextRange
        get() {
            val currentSelection = _selection?.value ?: return TextRange.Zero
            // Only one BasicText is in SelectionContainer for relevant tests.
            return TextRange(currentSelection.start.offset, currentSelection.end.offset)
        }

    @Test
    fun multipleBasicText_callSuggestSelectionForLongPressOrDoubleClick() {
        var selection by mutableStateOf<Selection?>(null)

        rule.setTextFieldTestContent {
            SelectionContainer(selection = selection, onSelectionChange = { selection = it }) {
                Column {
                    BasicText(text = "abc def", style = defaultTextStyle)
                    BasicText(
                        text = "ghi",
                        modifier = Modifier.testTag(TAG),
                        style = defaultTextStyle,
                    )
                    BasicText(text = "jkl mno", style = defaultTextStyle)
                }
            }
        }

        performLongPressOrDoubleClick { center }

        rule.waitForIdle()

        assertThat(selection!!.start.offset).isEqualTo(0)
        assertThat(selection!!.end.offset).isEqualTo(3)
        platformSelectionBehaviorsRule.expectSuggestSelectionForLongPressOrDoubleClick(
            "ghi",
            TextRange(0, 3),
        )
        expectOnShowContextMenu("ghi", TextRange(0, 3))
    }

    @Test
    fun multipleBasicText_doesApplySuggestedRange() {
        var selection by mutableStateOf<Selection?>(null)

        rule.setTextFieldTestContent {
            SelectionContainer(selection = selection, onSelectionChange = { selection = it }) {
                Column {
                    BasicText(
                        text = "abc def",
                        modifier = Modifier.testTag(TAG),
                        style = defaultTextStyle,
                    )
                    BasicText(text = "ghi", style = defaultTextStyle)
                    BasicText(text = "jkl mno", style = defaultTextStyle)
                }
            }
        }

        platformSelectionBehaviorsRule.suggestedSelection = TextRange(0, 7)

        performLongPressOrDoubleClick { Offset(x = fontSize.toPx() * 5, y = fontSize.toPx() / 2) }

        rule.waitForIdle()

        assertThat(selection!!.start.offset).isEqualTo(0)
        assertThat(selection!!.end.offset).isEqualTo(7)
        platformSelectionBehaviorsRule.expectSuggestSelectionForLongPressOrDoubleClick(
            "abc def",
            TextRange(4, 7),
        )
        expectOnShowContextMenu("abc def", TextRange(4, 7))
    }
}
