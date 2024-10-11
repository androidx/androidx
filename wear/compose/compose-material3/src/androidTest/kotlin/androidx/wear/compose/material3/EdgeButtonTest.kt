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

package androidx.wear.compose.material3

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EdgeButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            EdgeButton(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_click_action_when_enabled() {
        rule.setContentWithTheme {
            EdgeButton(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_click_action_when_disabled() {
        rule.setContentWithTheme {
            EdgeButton(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            EdgeButton(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            EdgeButton(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun responds_to_click_when_enabled() {
        var clicked = false

        rule.setContentWithTheme {
            EdgeButton(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle { assertEquals(true, clicked) }
    }
}
