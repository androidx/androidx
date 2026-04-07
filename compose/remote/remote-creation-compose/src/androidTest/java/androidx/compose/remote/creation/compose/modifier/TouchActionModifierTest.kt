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
import androidx.compose.remote.creation.compose.action.CombinedAction
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class TouchActionModifierTest {

    @get:Rule
    val remoteComposeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    }

    @Test
    fun handlesTouchDown() {
        val action = HostAction(DOWN_ACTION.rs, ACTION_VALUE.rs)
        runActionTest(modifier = RemoteModifier.onTouchDown(action)) { element -> element.click() }
        assertThat(remoteComposeTestRule.clickEvents)
            .containsExactly(Pair(DOWN_ACTION, ACTION_VALUE))
    }

    @Test
    fun handlesTouchUp() {
        val dummyDownAction = HostAction("dummyDown".rs, "dummy".rs)
        val upAction = HostAction(UP_ACTION.rs, ACTION_VALUE.rs)
        // b/500282418: it should not require onTouchDown
        runActionTest(modifier = RemoteModifier.onTouchDown(dummyDownAction).onTouchUp(upAction)) {
            element ->
            element.click()
        }
        assertThat(remoteComposeTestRule.clickEvents)
            .containsExactly(Pair("dummyDown", "dummy"), Pair(UP_ACTION, ACTION_VALUE))
    }

    @Test
    fun handlesCombinedTouchDown() {
        val action1 = HostAction(DOWN_ACTION.rs, ACTION1_VALUE.rs)
        val action2 = HostAction(DOWN_ACTION.rs, ACTION2_VALUE.rs)
        runActionTest(modifier = RemoteModifier.onTouchDown(CombinedAction(action1, action2))) {
            element ->
            element.click()
        }
        assertThat(remoteComposeTestRule.clickEvents)
            .containsExactly(Pair(DOWN_ACTION, ACTION1_VALUE), Pair(DOWN_ACTION, ACTION2_VALUE))
    }

    @Test
    fun handlesCombinedTouchUp() {
        val dummyDownAction = HostAction("dummyDown".rs, "dummy".rs)
        val action1 = HostAction(UP_ACTION.rs, ACTION1_VALUE.rs)
        val action2 = HostAction(UP_ACTION.rs, ACTION2_VALUE.rs)
        // b/500282418: it should not require onTouchDown
        runActionTest(
            modifier =
                RemoteModifier.onTouchDown(dummyDownAction)
                    .onTouchUp(CombinedAction(action1, action2))
        ) { element ->
            element.click()
        }
        assertThat(remoteComposeTestRule.clickEvents)
            .containsExactly(
                Pair("dummyDown", "dummy"),
                Pair(UP_ACTION, ACTION1_VALUE),
                Pair(UP_ACTION, ACTION2_VALUE),
            )
    }

    private fun runActionTest(modifier: RemoteModifier, block: (element: UiObject2) -> Unit) {
        remoteComposeTestRule.setContent {
            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .semantics(mergeDescendants = true) {}
                        .then(modifier),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText(BUTTON_LABEL)
            }
        }

        uiAutomator {
            val element = onElement { text?.toString() == BUTTON_LABEL }
            block(element)
        }
    }

    companion object {
        private const val BUTTON_LABEL = "Touch me!"
        private const val DOWN_ACTION = "touchDownAction"
        private const val UP_ACTION = "touchUpAction"
        private const val ACTION_VALUE = "actionValue"
        private const val ACTION1_VALUE = "value1"
        private const val ACTION2_VALUE = "value2"
    }
}
