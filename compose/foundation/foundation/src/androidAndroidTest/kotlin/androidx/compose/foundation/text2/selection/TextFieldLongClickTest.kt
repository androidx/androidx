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

package androidx.compose.foundation.text2.selection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

/**
 * Tests for long click interactions on BasicTextField2.
 */
@OptIn(ExperimentalFoundationApi::class)
@LargeTest
class TextFieldLongClickTest {

    @get:Rule
    val rule = createComposeRule()

    private val TAG = "BasicTextField2"

    private val fontSize = 10.sp

    private val defaultTextStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize)

    @Test
    fun emptyTextField_longClickDoesNotShowCursor() {
        rule.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { longClick() }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun longClickOnEmptyRegion_showsCursorAtTheEnd() {
        val state = TextFieldState("abc")
        rule.setContent {
            BasicTextField2(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier
                    .testTag(TAG)
                    .width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSize.toPx() * 5, fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(3))
    }

    @Test
    fun longClickOnWord_selectsWord() {
        val state = TextFieldState("abc def ghi")
        rule.setContent {
            BasicTextField2(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSize.toPx() * 5, fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(4, 7))
    }

    @Test
    fun longClickOnWhitespace_doesNotSelectWhitespace() {
        val state = TextFieldState("abc def ghi")
        rule.setContent {
            BasicTextField2(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSize.toPx() * 7.5f, fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.text.selectionInChars).isNotEqualTo(TextRange(7, 8))
        assertThat(state.text.selectionInChars.collapsed).isFalse()
    }

    @Test
    fun longClickOnScrolledTextField_selectsWord() {
        val state = TextFieldState("abc def ghi abc def ghi")
        val scrollState = ScrollState(0)
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField2(
                state = state,
                textStyle = defaultTextStyle,
                scrollState = scrollState,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier
                    .testTag(TAG)
                    .width(30.dp)
            )
        }

        assertThat(scrollState.maxValue).isGreaterThan(0)
        scope.launch { scrollState.scrollTo(scrollState.maxValue) }

        rule.onNodeWithTag(TAG).performTouchInput { longClick(centerRight) }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(20, 23))
    }

    @Test
    fun longClickOnDecoratedTextField_selectsWord() {
        val state = TextFieldState("abc def ghi")
        rule.setContent {
            BasicTextField2(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG),
                decorationBox = {
                    Box(modifier = Modifier.padding(32.dp)) {
                        it()
                    }
                }
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(
                x = 32.dp.toPx() + fontSize.toPx() * 5f,
                y = 32.dp.toPx() + fontSize.toPx() / 2
            ))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(4, 7))
    }
}
