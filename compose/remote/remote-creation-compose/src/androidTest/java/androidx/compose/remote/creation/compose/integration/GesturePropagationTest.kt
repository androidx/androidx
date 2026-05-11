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

package androidx.compose.remote.creation.compose.integration

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.combinedClickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteInteractionTestRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class GesturePropagationTest {

    private val experimentalProfile =
        Profile(
            RcPlatformProfiles.ANDROIDX.apiLevel,
            RcPlatformProfiles.ANDROIDX.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
            RcPlatformProfiles.ANDROIDX.platform,
            RcPlatformProfiles.ANDROIDX.profileFactory,
        )

    @get:Rule
    val remoteComposeTestRule: RemoteInteractionTestRule =
        RemoteInteractionTestRule(context = ApplicationProvider.getApplicationContext())

    @Test
    fun clickable() {
        var composeClickCounter = 0
        val remoteComposeClick = "remote_compose_click"

        remoteComposeTestRule.setContent(
            profile = experimentalProfile,
            playComposableWrapper = { content ->
                Column(modifier = Modifier.fillMaxSize().clickable { composeClickCounter++ }) {
                    content()
                }
            },
        ) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize(),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(width = 200.rdp, height = 200.rdp)
                            .background(RemoteColor(Color.Red))
                            .clickable(HostAction(remoteComposeClick.rs)),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("RED_BOX".rs)
                }
            }
        }

        // 1. Click outside the red box (e.g. at middle coordinates)
        uiAutomator {
            val cx = device.displayWidth / 2
            val cy = device.displayHeight / 2
            device.click(cx, cy)
        }
        remoteComposeTestRule.composeTestRule.waitForIdle()

        // Verify outside click propagates to Compose click counter
        assertThat(composeClickCounter).isEqualTo(1)
        assertThat(remoteComposeTestRule.clickEvents.isEmpty()).isTrue()

        // 2. Click inside the red box using text-based element locating
        uiAutomator {
            val element = onElement { text?.toString() == "RED_BOX" }
            element.click()
        }
        remoteComposeTestRule.composeTestRule.waitForIdle()

        // Verify click inside red box triggers remote click event and does NOT increment Compose
        // click counter
        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(1)
        assertThat(remoteComposeTestRule.clickEvents.first().first).isEqualTo(remoteComposeClick)
        assertThat(composeClickCounter).isEqualTo(1)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun combinedClickable_click() {
        var composeClickCounter = 0

        val remoteClick = "remote_click"

        remoteComposeTestRule.setContent(
            profile = experimentalProfile,
            playComposableWrapper = { content ->
                Column(
                    modifier = Modifier.fillMaxSize().combinedClickable { composeClickCounter++ }
                ) {
                    content()
                }
            },
        ) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize(),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(width = 200.rdp, height = 200.rdp)
                            .background(RemoteColor(Color.Red))
                            .combinedClickable(onClick = HostAction(remoteClick.rs)),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("RED_BOX".rs)
                }
            }
        }

        // 1. Verify clicks OUTSIDE the red box bubble up to Compose counters
        uiAutomator {
            val cx = device.displayWidth / 2
            val cy = device.displayHeight / 2

            // Single Click Outside
            device.click(cx, cy)
        }
        remoteComposeTestRule.composeTestRule.waitForIdle()
        remoteComposeTestRule.composeTestRule.mainClock.advanceTimeBy(1000)
        assertThat(composeClickCounter).isEqualTo(1)

        assertThat(remoteComposeTestRule.clickEvents.isEmpty()).isTrue()

        // 2. Verify clicks INSIDE the red box trigger remote actions and do NOT increment Compose
        // counters
        // Click Inside
        uiAutomator {
            val element = onElement { text?.toString() == "RED_BOX" }
            element.click()
        }
        remoteComposeTestRule.composeTestRule.waitForIdle()
        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(1)
        assertThat(remoteComposeTestRule.clickEvents.last()).isEqualTo(Pair(remoteClick, null))

        // Confirm Compose counters did not increment during internal red box clicks
        assertThat(composeClickCounter).isEqualTo(1)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun combinedClickable_doubleClick() {
        var composeDoubleClickCounter = 0

        val remoteDoubleClick = "remote_double_click"

        remoteComposeTestRule.setContent(
            profile = experimentalProfile,
            playComposableWrapper = { content ->
                Column(
                    modifier =
                        Modifier.fillMaxSize().combinedClickable(
                            onDoubleClick = { composeDoubleClickCounter++ }
                        ) {}
                ) {
                    content()
                }
            },
        ) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize(),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(width = 200.rdp, height = 200.rdp)
                            .background(RemoteColor(Color.Red))
                            .combinedClickable(onDoubleClick = HostAction(remoteDoubleClick.rs)),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("RED_BOX".rs)
                }
            }
        }

        // 1. Verify double clicks OUTSIDE the red box bubble up to Compose counters
        uiAutomator {
            val cx = device.displayWidth / 2
            val cy = device.displayHeight / 2

            // Double Click Outside
            device.click(cx, cy)
            SystemClock.sleep(50)
            device.click(cx, cy)
        }
        remoteComposeTestRule.composeTestRule.waitForIdle()
        assertThat(composeDoubleClickCounter).isEqualTo(1)

        assertThat(remoteComposeTestRule.clickEvents.isEmpty()).isTrue()

        // 2. Verify double clicks INSIDE the red box trigger remote actions and do NOT increment
        // Compose counters
        // Double Click Inside
        uiAutomator {
            val element = onElement { text?.toString() == "RED_BOX" }
            element.doubleClickUsingMotionEvents()
        }
        remoteComposeTestRule.composeTestRule.waitForIdle()
        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(1)
        assertThat(remoteComposeTestRule.clickEvents.last())
            .isEqualTo(Pair(remoteDoubleClick, null))

        // Confirm Compose counters did not increment during internal red box clicks
        assertThat(composeDoubleClickCounter).isEqualTo(1)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun combinedClickable_longClick() {
        var composeLongClickCounter = 0

        val remoteLongClick = "remote_long_click"

        remoteComposeTestRule.setContent(
            profile = experimentalProfile,
            playComposableWrapper = { content ->
                Column(
                    modifier =
                        Modifier.fillMaxSize().combinedClickable(
                            onLongClick = { composeLongClickCounter++ }
                        ) {}
                ) {
                    content()
                }
            },
        ) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize(),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(width = 200.rdp, height = 200.rdp)
                            .background(RemoteColor(Color.Red))
                            .combinedClickable(onLongClick = HostAction(remoteLongClick.rs)),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("RED_BOX".rs)
                }
            }
        }

        // 1. Verify long clicks OUTSIDE the red box bubble up to Compose counters
        uiAutomator {
            val cx = device.displayWidth / 2f
            val cy = device.displayHeight / 2f

            // Long Click Outside
            longClickCoordinates(cx, cy)
        }
        remoteComposeTestRule.composeTestRule.waitForIdle()
        assertThat(composeLongClickCounter).isEqualTo(1)

        assertThat(remoteComposeTestRule.clickEvents.isEmpty()).isTrue()

        // 2. Verify long clicks INSIDE the red box trigger remote actions and do NOT increment
        // Compose counters
        // Long Click Inside
        uiAutomator {
            val element = onElement { text?.toString() == "RED_BOX" }
            element.longClick()
        }
        remoteComposeTestRule.composeTestRule.waitForIdle()
        assertThat(remoteComposeTestRule.clickEvents.size).isEqualTo(1)
        assertThat(remoteComposeTestRule.clickEvents.last()).isEqualTo(Pair(remoteLongClick, null))

        // Confirm Compose counters did not increment during internal red box clicks
        assertThat(composeLongClickCounter).isEqualTo(1)
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

    private fun longClickCoordinates(cx: Float, cy: Float) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()

        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, cx, cy, 0).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }
        uiAutomation.injectInputEvent(downEvent, true)
        downEvent.recycle()

        // Advance Compose's virtual clock so the long press coroutine can trigger!
        remoteComposeTestRule.composeTestRule.mainClock.advanceTimeBy(600)

        val upTime = downTime + 600
        val upEvent =
            MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, cx, cy, 0).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }
        uiAutomation.injectInputEvent(upEvent, true)
        upEvent.recycle()
    }
}
