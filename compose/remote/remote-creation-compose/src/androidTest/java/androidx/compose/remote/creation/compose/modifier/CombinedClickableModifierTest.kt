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

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.compose.action.CombinedAction
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.RemoteInteractionTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class CombinedClickableModifierTest {

    private val experimentalProfile =
        Profile(
            RcPlatformProfiles.ANDROIDX.apiLevel,
            RcPlatformProfiles.ANDROIDX.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
            RcPlatformProfiles.ANDROIDX.platform,
            RcPlatformProfiles.ANDROIDX.profileFactory,
        )

    @get:Rule
    val remoteComposeTestRule =
        RemoteInteractionTestRule(ApplicationProvider.getApplicationContext())

    @Test
    fun handlesSingleClick() {
        runActionTest(block = { element -> element.click() })
        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(1)
        assertThat(remoteComposeTestRule.clickEvents.first())
            .isEqualTo(Pair(CLICK_ACTION, CLICK_ACTION_SINGLE))
    }

    @Test
    fun handlesLongClick() {
        runActionTest(block = { element -> element.longClick() })
        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(1)
        assertThat(remoteComposeTestRule.clickEvents.first())
            .isEqualTo(Pair(CLICK_ACTION, CLICK_ACTION_LONG))
    }

    @Test
    fun handlesDoubleClick() {
        runActionTest(block = { element -> element.doubleClickUsingMotionEvents() })
        // b/498149887: it should not trigger click
        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(2)
        assertThat(remoteComposeTestRule.clickEvents.first())
            .isEqualTo(Pair(CLICK_ACTION, CLICK_ACTION_SINGLE))
        assertThat(remoteComposeTestRule.clickEvents.last())
            .isEqualTo(Pair(CLICK_ACTION, CLICK_ACTION_DOUBLE))
    }

    @Test
    fun disabled_ignoresClick() {
        runActionTest(
            enabled = false,
            finder = { onElement { text?.toString() == BUTTON_LABEL } },
        ) { element ->
            element.click()
        }
        assertThat(remoteComposeTestRule.clickEvents.isEmpty()).isTrue()
    }

    @Test
    fun disabled_ignoresLongClick() {
        runActionTest(
            enabled = false,
            finder = { onElement { text?.toString() == BUTTON_LABEL } },
        ) { element ->
            element.longClick()
        }
        assertThat(remoteComposeTestRule.clickEvents.isEmpty()).isTrue()
    }

    @Test
    fun disabled_ignoresDoubleClick() {
        runActionTest(
            enabled = false,
            finder = { onElement { text?.toString() == BUTTON_LABEL } },
        ) { element ->
            element.doubleClickUsingMotionEvents()
        }
        assertThat(remoteComposeTestRule.clickEvents.isEmpty()).isTrue()
    }

    @Test
    fun handlesMultipleClickActions() {
        remoteComposeTestRule.setContent(profile = experimentalProfile) {
            val onClickAction1 = HostAction(CLICK_ACTION.rs, "action1".rs)
            val onClickAction2 = HostAction(CLICK_ACTION.rs, "action2".rs)

            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .semantics(mergeDescendants = true) {}
                        .combinedClickable(
                            onClick = CombinedAction(onClickAction1, onClickAction2)
                        ),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText(BUTTON_LABEL)
            }
        }

        uiAutomator {
            val element = onElement { isClickable }
            element.click()
        }

        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(2)
        assertThat(remoteComposeTestRule.clickEvents[0]).isEqualTo(Pair(CLICK_ACTION, "action1"))
        assertThat(remoteComposeTestRule.clickEvents[1]).isEqualTo(Pair(CLICK_ACTION, "action2"))
    }

    @Test
    fun handlesMultipleLongClickActions() {
        remoteComposeTestRule.setContent(profile = experimentalProfile) {
            val onLongClickAction1 = HostAction(CLICK_ACTION.rs, "long1".rs)
            val onLongClickAction2 = HostAction(CLICK_ACTION.rs, "long2".rs)

            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .semantics(mergeDescendants = true) {}
                        .combinedClickable(
                            onLongClick = CombinedAction(onLongClickAction1, onLongClickAction2)
                        ),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText(BUTTON_LABEL)
            }
        }

        uiAutomator {
            val element = onElement { isClickable }
            element.longClick()
        }

        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(2)
        assertThat(remoteComposeTestRule.clickEvents[0]).isEqualTo(Pair(CLICK_ACTION, "long1"))
        assertThat(remoteComposeTestRule.clickEvents[1]).isEqualTo(Pair(CLICK_ACTION, "long2"))
    }

    @Test
    fun handlesMultipleDoubleClickActions() {
        remoteComposeTestRule.setContent(profile = experimentalProfile) {
            val onDoubleClickAction1 = HostAction(CLICK_ACTION.rs, "double1".rs)
            val onDoubleClickAction2 = HostAction(CLICK_ACTION.rs, "double2".rs)

            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .semantics(mergeDescendants = true) {}
                        .combinedClickable(
                            onDoubleClick =
                                CombinedAction(onDoubleClickAction1, onDoubleClickAction2)
                        ),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText(BUTTON_LABEL)
            }
        }

        uiAutomator {
            val element = onElement { isClickable }
            element.doubleClickUsingMotionEvents()
        }

        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(2)
        assertThat(remoteComposeTestRule.clickEvents[0]).isEqualTo(Pair(CLICK_ACTION, "double1"))
        assertThat(remoteComposeTestRule.clickEvents[1]).isEqualTo(Pair(CLICK_ACTION, "double2"))
    }

    private fun runActionTest(
        enabled: Boolean = true,
        finder: (UiAutomatorTestScope.() -> UiObject2)? = null,
        block: (element: UiObject2) -> Unit,
    ) {
        remoteComposeTestRule.setContent(profile = experimentalProfile) {
            val onClickAction = HostAction(CLICK_ACTION.rs, CLICK_ACTION_SINGLE.rs)
            val onLongClickAction = HostAction(CLICK_ACTION.rs, CLICK_ACTION_LONG.rs)
            val onDoubleClickAction = HostAction(CLICK_ACTION.rs, CLICK_ACTION_DOUBLE.rs)

            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .semantics(mergeDescendants = true) {}
                        .combinedClickable(
                            enabled = enabled,
                            onClick = onClickAction,
                            onLongClick = onLongClickAction,
                            onDoubleClick = onDoubleClickAction,
                        ),
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

    private fun UiObject2.doubleClickUsingMotionEvents() {
        fun injectEvent(action: Int, downTime: Long, eventTime: Long, sync: Boolean) {
            val bounds = visibleBounds
            val cx = bounds.centerX().toFloat()
            val cy = bounds.centerY().toFloat()
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

            val event =
                MotionEvent.obtain(downTime, eventTime, action, cx, cy, 0).apply {
                    source = InputDevice.SOURCE_TOUCHSCREEN
                }
            uiAutomation.injectInputEvent(event, sync)
            event.recycle()
        }

        val now = SystemClock.uptimeMillis()
        val t1Down = now - 200
        val t1Up = now - 190
        val t2Down = now - 150
        val t2Up = now - 140

        injectEvent(MotionEvent.ACTION_DOWN, t1Down, t1Down, false)
        injectEvent(MotionEvent.ACTION_UP, t1Down, t1Up, false)
        injectEvent(MotionEvent.ACTION_DOWN, t2Down, t2Down, false)
        injectEvent(MotionEvent.ACTION_UP, t2Down, t2Up, true)
    }

    companion object {
        private const val BUTTON_LABEL = "Click me!"
        private const val CLICK_ACTION = "clickAction"
        private const val CLICK_ACTION_SINGLE = "single"
        private const val CLICK_ACTION_LONG = "long"
        private const val CLICK_ACTION_DOUBLE = "double"
    }
}
