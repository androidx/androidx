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

package androidx.compose.foundation.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test

class VisibleTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    val TEST_TAG = "visibility"

    @Test
    fun visible_true_isDisplayed() {
        rule.setContent { Box(Modifier.size(50.dp).visible(true).testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun visible_false_isNotDisplayed() {
        rule.setContent { Box(Modifier.size(50.dp).visible(false).testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun visible_false_occupiesSpace() {
        var parentSize by mutableStateOf(IntSize.Zero)
        rule.setContent {
            Box(Modifier.onGloballyPositioned { parentSize = it.size }) {
                Box(Modifier.size(50.dp).visible(false).testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotDisplayed()

        rule.waitForIdle()

        val expectedSize = with(rule.density) { 50.dp.roundToPx() }
        assertThat(parentSize.width).isEqualTo(expectedSize)
        assertThat(parentSize.height).isEqualTo(expectedSize)
    }

    @Test
    fun visible_toggle_changesDisplay() {
        var isVisible by mutableStateOf(true)
        rule.setContent { Box(Modifier.size(50.dp).visible(isVisible).testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertIsDisplayed()

        rule.runOnIdle { isVisible = false }

        rule.onNodeWithTag(TEST_TAG).assertIsNotDisplayed()

        rule.runOnIdle { isVisible = true }

        rule.onNodeWithTag(TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun visible_false_isNotClickable() {
        var clicked = false
        rule.setContent {
            Box(Modifier.size(50.dp).visible(false).clickable { clicked = true }.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).performClick()
        rule.runOnIdle { assertThat(clicked).isFalse() }
    }

    @Test
    fun visible_false_childIsNotClickable() {
        var clicked = false
        rule.setContent {
            Box(Modifier.size(50.dp).visible(false).testTag(TEST_TAG)) {
                Box(Modifier.size(20.dp).clickable { clicked = true }.testTag("child"))
            }
        }

        rule.onNodeWithTag("child").performClick()
        rule.runOnIdle { assertThat(clicked).isFalse() }
    }

    @Test
    fun visible_false_isNotFocusable() {
        val focusRequester = FocusRequester()
        rule.setContent {
            Box(
                Modifier.size(50.dp)
                    .visible(false)
                    .focusRequester(focusRequester)
                    .focusable()
                    .testTag(TEST_TAG)
            )
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(TEST_TAG).assertIsNotFocused()
    }

    @Test
    fun visible_toggle_losesFocus() {
        var isVisible by mutableStateOf(true)
        val focusRequester = FocusRequester()
        rule.setContent {
            Box(
                Modifier.size(50.dp)
                    .visible(isVisible)
                    .focusRequester(focusRequester)
                    .focusable()
                    .testTag(TEST_TAG)
            )
        }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.onNodeWithTag(TEST_TAG).assertIsFocused()

        rule.runOnIdle { isVisible = false }
        rule.onNodeWithTag(TEST_TAG).assertIsNotFocused()
    }
}
