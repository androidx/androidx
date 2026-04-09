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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.action.CombinedAction
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class ClickableModifierTest {

    @get:Rule
    val remoteComposeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    }

    @Test
    fun handlesClick() {
        runActionTest(
            action = HostAction(CLICK_ACTION.rs, CLICK_ACTION1_VALUE.rs),
            finder = { onElement { text?.toString() == BUTTON_LABEL } },
        ) { element ->
            element.click()
        }
        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(1)
        assertThat(remoteComposeTestRule.clickEvents.first())
            .isEqualTo(Pair(CLICK_ACTION, CLICK_ACTION1_VALUE))
    }

    @Test
    fun handlesMultipleActions() {
        val action1 = HostAction(CLICK_ACTION.rs, CLICK_ACTION1_VALUE.rs)
        val action2 = HostAction(CLICK_ACTION.rs, CLICK_ACTION2_VALUE.rs)
        runActionTest(
            action = CombinedAction(action1, action2),
            finder = { onElement { text?.toString() == BUTTON_LABEL } },
        ) { element ->
            element.click()
        }
        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(2)
        assertThat(remoteComposeTestRule.clickEvents[0])
            .isEqualTo(Pair(CLICK_ACTION, CLICK_ACTION1_VALUE))
        assertThat(remoteComposeTestRule.clickEvents[1])
            .isEqualTo(Pair(CLICK_ACTION, CLICK_ACTION2_VALUE))
    }

    @Test
    fun disabled_ignoresClick() {
        runActionTest(
            action = HostAction(CLICK_ACTION.rs, CLICK_ACTION1_VALUE.rs),
            enabled = false,
            finder = { onElement { text?.toString() == BUTTON_LABEL } },
        ) { element ->
            element.click()
        }
        assertThat(remoteComposeTestRule.clickEvents.isEmpty()).isTrue()
    }

    private fun runActionTest(
        action: Action,
        enabled: Boolean = true,
        finder: (UiAutomatorTestScope.() -> UiObject2)? = null,
        block: (element: UiObject2) -> Unit,
    ) {
        remoteComposeTestRule.setContent {
            val clickModifier = RemoteModifier.clickable(enabled = enabled, action = action)

            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .semantics(mergeDescendants = true) {}
                        .then(clickModifier),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText(BUTTON_LABEL)
            }
        }

        uiAutomator {
            val element = finder?.invoke(this) ?: onElement { isClickable }

            block(element)
        }
    }

    companion object {
        private const val BUTTON_LABEL = "Click me!"
        private const val CLICK_ACTION = "clickAction"
        private const val CLICK_ACTION1_VALUE = "action1"
        private const val CLICK_ACTION2_VALUE = "action2"
    }
}
