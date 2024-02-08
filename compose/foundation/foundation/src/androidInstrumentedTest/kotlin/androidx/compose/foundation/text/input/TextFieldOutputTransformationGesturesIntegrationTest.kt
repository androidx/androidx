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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicTextField2
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldOutputTransformationGesturesIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField2"

    @Test
    fun clickingAroundReplacement_movesCursorToEdgesOfReplacement() {
        val text = TextFieldState("zaz", initialSelectionInChars = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField2(
                state = text,
                modifier = Modifier.testTag(Tag),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontFamily = TEST_FONT_FAMILY,
                    fontSize = 10.sp
                ),
                outputTransformation = {
                    replace(1, 2, "bbbb") // "zbbbbz"
                }
            )
        }
        rule.onNodeWithTag(Tag).assertTextEquals("zbbbbz")

        rule.onNodeWithTag(Tag).performTouchInput {
            // Click 1 pixel to the right of center.
            click(center + Offset(1f, 0f))
        }
        rule.runOnIdle {
            assertThat(text.text.selectionInChars).isEqualTo(TextRange(2))
        }

        rule.onNodeWithTag(Tag).performTouchInput {
            // Add a delay to avoid triggering double-click.
            advanceEventTime(1000)
            // Click 1 pixel to the left of center.
            click(center + Offset(-1f, 0f))
        }
        rule.runOnIdle {
            assertThat(text.text.selectionInChars).isEqualTo(TextRange(1))
        }
    }

    @Test
    fun clickingAroundReplacement_movesCursorToEdgesOfReplacement_withLineBreak() {
        // The top right is geometrically closer to the start of the replacement, even though the
        // index is closer to the end.
        // The bottom left is closer to the end.
        // +-------->---------+
        // | zzzzzzzzbbbb     |
        // | bbz              |
        // +---<--------------+

        val text = TextFieldState("zzzzzzzzaz", initialSelectionInChars = TextRange(0))
        val replacement = "bbbb\nbb"
        val indexOfA = text.text.indexOf('a')
        inputMethodInterceptor.setContent {
            BasicTextField2(
                state = text,
                modifier = Modifier.testTag(Tag),
                textStyle = TextStyle(
                    textAlign = TextAlign.Left,
                    fontFamily = TEST_FONT_FAMILY,
                    fontSize = 10.sp
                ),
                outputTransformation = {
                    replace(indexOfA, indexOfA + 1, replacement)
                }
            )
        }
        rule.onNodeWithTag(Tag).assertTextEquals("zzzzzzzz${replacement}z")

        rule.onNodeWithTag(Tag).performTouchInput {
            click(topRight)
        }
        rule.runOnIdle {
            assertThat(text.text.selectionInChars).isEqualTo(TextRange(indexOfA))
        }
        assertCursor(indexOfA)

        rule.onNodeWithTag(Tag).performTouchInput {
            // Add a delay to avoid triggering double-click.
            advanceEventTime(1000)
            click(bottomLeft)
        }
        rule.runOnIdle {
            assertThat(text.text.selectionInChars)
                .isEqualTo(TextRange(indexOfA + 1))
        }
        assertCursor(indexOfA + replacement.length)
    }

    @Test
    fun clickingAroundReplacement_movesCursorToEdgesOfInsertion() {
        val text = TextFieldState("zz", initialSelectionInChars = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField2(
                state = text,
                modifier = Modifier.testTag(Tag),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontFamily = TEST_FONT_FAMILY,
                    fontSize = 10.sp
                ),
                outputTransformation = {
                    insert(1, "bbbb") // "zbbbbz"
                }
            )
        }
        rule.onNodeWithTag(Tag).assertTextEquals("zbbbbz")

        rule.onNodeWithTag(Tag).performTouchInput {
            // Click 1 pixel to the right of center.
            click(center + Offset(1f, 0f))
        }
        rule.runOnIdle {
            assertThat(text.text.selectionInChars).isEqualTo(TextRange(1))
        }
        assertCursor(5)

        rule.onNodeWithTag(Tag).performTouchInput {
            // Add a delay to avoid triggering double-click.
            advanceEventTime(1000)
            // Click 1 pixel to the left of center.
            click(center + Offset(-1f, 0f))
        }
        rule.runOnIdle {
            assertThat(text.text.selectionInChars).isEqualTo(TextRange(1))
        }
        assertCursor(1)
    }

    private fun assertCursor(offset: Int) {
        val node = rule.onNodeWithTag(Tag).fetchSemanticsNode()
        Truth.assertWithMessage("Selection via semantics")
            .that(node.config[SemanticsProperties.TextSelectionRange])
            .isEqualTo(TextRange(offset))
    }
}
