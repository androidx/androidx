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

package androidx.wear.compose.foundation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class CurvedTestTagTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun curvedBox_supports_testTag() {
        rule.setContent {
            CurvedLayout {
                curvedBox(modifier = CurvedModifier.testTag(TEST_TAG)) { curvedComposable {} }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun curvedRow_supports_testTag() {
        rule.setContent {
            CurvedLayout {
                curvedRow(modifier = CurvedModifier.testTag(TEST_TAG)) { curvedComposable {} }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun curvedColumn_supports_testTag() {
        rule.setContent {
            CurvedLayout {
                curvedColumn(modifier = CurvedModifier.testTag(TEST_TAG)) { curvedComposable {} }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun curvedComposable_supports_testTag() {
        rule.setContent {
            CurvedLayout { curvedComposable(modifier = CurvedModifier.testTag(TEST_TAG)) {} }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }
}
