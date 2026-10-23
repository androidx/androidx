/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.testutils

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class MacrobenchUiAutomatorExtensionsTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun waitOrThrow_whenConditionIsSatisfied_completesWithoutThrowing() {
        composeTestRule.setContent { BasicText(TARGET_TEXT) }

        device.waitOrThrow(condition = Until.hasObject(By.text(TARGET_TEXT)))
    }

    @Test
    fun waitOrThrow_whenConditionTimesOut_throwsIllegalStateExceptionWithMessage() {
        composeTestRule.setContent { BasicText(TARGET_TEXT) }

        val error =
            assertThrows(IllegalStateException::class.java) {
                device.waitOrThrow(
                    condition = Until.hasObject(By.text(MISSING_TEXT)),
                    timeoutMillis = SHORT_TIMEOUT_MS,
                    dumpWindowHierarchyOnFailure = false,
                ) {
                    "Expected condition to fail in test"
                }
            }

        assertThat(error).hasMessageThat().isEqualTo("Expected condition to fail in test")
    }

    @Test
    fun findObjectOrThrow_whenObjectExists_returnsMatchingUiObject() {
        composeTestRule.setContent { BasicText(TARGET_TEXT) }

        val found = device.findObjectOrThrow(selector = By.text(TARGET_TEXT))

        assertThat(found.text).isEqualTo(TARGET_TEXT)
    }

    @Test
    fun findObjectOrThrow_whenObjectIsMissing_throwsIllegalStateException() {
        composeTestRule.setContent { BasicText(TARGET_TEXT) }

        val error =
            assertThrows(IllegalStateException::class.java) {
                device.findObjectOrThrow(
                    selector = By.text(MISSING_TEXT),
                    timeoutMillis = SHORT_TIMEOUT_MS,
                    dumpWindowHierarchyOnFailure = false,
                )
            }

        assertThat(error).hasMessageThat().contains(MISSING_TEXT)
    }

    @Test
    fun waitUntilGoneOrThrow_whenObjectDisappears_completesWithoutThrowing() {
        var visible by mutableStateOf(true)
        composeTestRule.setContent { BasicText(if (visible) TARGET_TEXT else REPLACEMENT_TEXT) }
        device.findObjectOrThrow(selector = By.text(TARGET_TEXT))

        composeTestRule.runOnIdle { visible = false }
        composeTestRule.waitForIdle()

        device.waitUntilGoneOrThrow(selector = By.text(TARGET_TEXT))
    }

    @Test
    fun waitUntilGoneOrThrow_whenObjectRemainsVisible_throwsIllegalStateException() {
        composeTestRule.setContent { BasicText(TARGET_TEXT) }

        val error =
            assertThrows(IllegalStateException::class.java) {
                device.waitUntilGoneOrThrow(
                    selector = By.text(TARGET_TEXT),
                    timeoutMillis = SHORT_TIMEOUT_MS,
                    dumpWindowHierarchyOnFailure = false,
                )
            }

        assertThat(error).hasMessageThat().contains("remained visible")
    }

    @Test
    fun byResContains_matchesResourceIdSubstring() {
        composeTestRule.setContent {
            BasicText(
                text = TARGET_TEXT,
                modifier =
                    Modifier.semantics { testTagsAsResourceId = true }
                        .testTag("prefix_target_tag_suffix"),
            )
        }

        val found = device.findObjectOrThrow(selector = byResContains("target_tag"))

        assertThat(found.text).isEqualTo(TARGET_TEXT)
    }

    companion object {
        private const val SHORT_TIMEOUT_MS = 200L
        private const val TARGET_TEXT = "MacrobenchTargetItem"
        private const val REPLACEMENT_TEXT = "MacrobenchReplacementItem"
        private const val MISSING_TEXT = "NonexistentMacrobenchItem"
    }
}
