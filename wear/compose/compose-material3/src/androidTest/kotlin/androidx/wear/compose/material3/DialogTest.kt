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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class DialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            Dialog(show = true, modifier = Modifier.testTag(TEST_TAG), onDismissRequest = {}) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_content() {
        rule.setContentWithTheme {
            Dialog(show = true, onDismissRequest = {}) {
                Text("Text", modifier = Modifier.testTag(TEST_TAG))
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_swipeToDismiss() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var showDialog by remember { mutableStateOf(true) }
            Dialog(
                show = showDialog,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    showDialog = false
                    dismissCounter++
                }
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onDismissRequest_not_called_when_hidden() {
        val show = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            Dialog(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { dismissCounter++ },
                show = show.value
            ) {}
        }
        rule.waitForIdle()
        show.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(0, dismissCounter)
    }

    @Test
    fun hides_dialog_when_show_false() {
        rule.setContentWithTheme {
            Dialog(modifier = Modifier.testTag(TEST_TAG), onDismissRequest = {}, show = false) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }
}
