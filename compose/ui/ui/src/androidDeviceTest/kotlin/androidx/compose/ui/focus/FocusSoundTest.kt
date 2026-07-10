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

package androidx.compose.ui.focus

import android.graphics.Rect
import android.view.View
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusSoundTest {

    @get:Rule val rule = createComposeRule()

    private var oldBypass: Boolean = false
    private var oldFix: Boolean = false

    @Before
    fun setup() {
        oldBypass = ComposeUiFlags.isBypassUnfocusableComposeViewEnabled
        oldFix = ComposeUiFlags.isViewFocusFixEnabled
    }

    @After
    fun teardown() {
        ComposeUiFlags.isBypassUnfocusableComposeViewEnabled = oldBypass
        ComposeUiFlags.isViewFocusFixEnabled = oldFix
    }

    @Test
    fun dpadNavigation_playsSoundEffect() {
        val focusRequester = FocusRequester()
        var playedSoundDirection: FocusDirection? = null

        rule.setContent {
            val view = LocalView.current as androidx.compose.ui.platform.AndroidComposeView
            view.playNavigationSoundEffect = { direction, _ -> playedSoundDirection = direction }

            Row {
                Box(Modifier.size(10.dp).focusRequester(focusRequester).focusable().testTag("box1"))
                Box(Modifier.size(10.dp).focusable().testTag("box2"))
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.waitForIdle()
        rule.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        rule.waitForIdle()

        rule.onNodeWithTag("box2").assertIsFocused()
        assertThat(playedSoundDirection).isEqualTo(Right)
    }

    @Test
    fun dpadNavigation_playsSoundEffect_fastScrolling() {
        val focusRequester = FocusRequester()
        var playedSoundDirection: FocusDirection? = null
        var isFastScrollingCall = false
        var view: androidx.compose.ui.platform.AndroidComposeView? = null

        rule.setContent {
            view = LocalView.current as androidx.compose.ui.platform.AndroidComposeView
            view.playNavigationSoundEffect = { direction, fastScroll ->
                playedSoundDirection = direction
                isFastScrollingCall = fastScroll
            }

            Row {
                Box(Modifier.size(10.dp).focusRequester(focusRequester).focusable().testTag("box1"))
                Box(Modifier.size(10.dp).focusable().testTag("box2"))
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.waitForIdle()
        rule.runOnIdle {
            val event =
                android.view.KeyEvent(
                    0L,
                    0L,
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                    1, // repeat count > 0 means fast scrolling
                )
            view!!.dispatchKeyEvent(event)
        }
        rule.waitForIdle()

        rule.onNodeWithTag("box2").assertIsFocused()
        assertThat(playedSoundDirection).isEqualTo(Right)
        assertThat(isFastScrollingCall).isTrue()
    }

    @Test
    fun clusterNavigation_playsSoundEffect_withPrevRect() {
        var playedSoundDirection: FocusDirection? = null
        var view: androidx.compose.ui.platform.AndroidComposeView? = null

        rule.setContent {
            view = LocalView.current as androidx.compose.ui.platform.AndroidComposeView
            view.playNavigationSoundEffect = { direction, _ -> playedSoundDirection = direction }

            Row { Box(Modifier.size(10.dp).focusable().testTag("box1")) }
        }
        rule.waitForIdle()

        rule.runOnIdle { view!!.requestFocus(View.FOCUS_DOWN, Rect(0, 0, 1, 1)) }
        rule.waitForIdle()
        // requestFocus sounds are made by ViewRootImpl not compose
        assertThat(playedSoundDirection).isEqualTo(null)
    }

    @Test
    fun clusterNavigation_playsSoundEffect_withoutPrevRect() {
        var playedSoundDirection: FocusDirection? = null
        var view: androidx.compose.ui.platform.AndroidComposeView? = null

        rule.setContent {
            view = LocalView.current as androidx.compose.ui.platform.AndroidComposeView
            view.playNavigationSoundEffect = { direction, _ -> playedSoundDirection = direction }

            Row { Box(Modifier.size(10.dp).focusable().testTag("box1")) }
        }
        rule.waitForIdle()

        rule.runOnIdle { view!!.requestFocus(View.FOCUS_DOWN, null) }
        rule.waitForIdle()
        // requestFocus sounds are made by ViewRootImpl not compose
        assertThat(playedSoundDirection).isEqualTo(null)
    }

    @Test
    fun clusterNavigation_playsSoundEffect_viewFocusFix_withPrevRect() {
        var playedSoundDirection: FocusDirection? = null
        var view: androidx.compose.ui.platform.AndroidComposeView? = null
        val oldBypass = ComposeUiFlags.isBypassUnfocusableComposeViewEnabled
        ComposeUiFlags.isBypassUnfocusableComposeViewEnabled = false
        val oldFix = ComposeUiFlags.isViewFocusFixEnabled
        ComposeUiFlags.isViewFocusFixEnabled = true

        rule.setContent {
            view = LocalView.current as androidx.compose.ui.platform.AndroidComposeView
            view.playNavigationSoundEffect = { direction, _ -> playedSoundDirection = direction }

            Row { Box(Modifier.size(10.dp).focusable().testTag("box1")) }
        }
        rule.waitForIdle()

        rule.runOnIdle { view!!.requestFocus(View.FOCUS_DOWN, Rect(0, 0, 1, 1)) }
        rule.waitForIdle()
        // requestFocus sounds are made by ViewRootImpl not compose
        assertThat(playedSoundDirection).isEqualTo(null)

        ComposeUiFlags.isBypassUnfocusableComposeViewEnabled = oldBypass
        ComposeUiFlags.isViewFocusFixEnabled = oldFix
    }

    @Test
    fun clusterNavigation_playsSoundEffect_viewFocusFix_withoutPrevRect() {
        var playedSoundDirection: FocusDirection? = null
        var view: androidx.compose.ui.platform.AndroidComposeView? = null
        val oldBypass = ComposeUiFlags.isBypassUnfocusableComposeViewEnabled
        ComposeUiFlags.isBypassUnfocusableComposeViewEnabled = false
        val oldFix = ComposeUiFlags.isViewFocusFixEnabled
        ComposeUiFlags.isViewFocusFixEnabled = true

        rule.setContent {
            view = LocalView.current as androidx.compose.ui.platform.AndroidComposeView
            view.playNavigationSoundEffect = { direction, _ -> playedSoundDirection = direction }

            Row { Box(Modifier.size(10.dp).focusable().testTag("box1")) }
        }
        rule.waitForIdle()

        rule.runOnIdle { view!!.requestFocus(View.FOCUS_DOWN, null) }
        rule.waitForIdle()
        // requestFocus sounds are made by ViewRootImpl not compose
        assertThat(playedSoundDirection).isEqualTo(null)

        ComposeUiFlags.isBypassUnfocusableComposeViewEnabled = oldBypass
        ComposeUiFlags.isViewFocusFixEnabled = oldFix
    }

    @Test
    fun tabNavigation_playsSoundEffect() {
        var playedSoundDirection: FocusDirection? = null

        rule.setContent {
            val view = LocalView.current as androidx.compose.ui.platform.AndroidComposeView
            view.playNavigationSoundEffect = { direction, _ -> playedSoundDirection = direction }

            Row {
                Box(Modifier.size(10.dp).focusable().testTag("box1"))
                Box(Modifier.size(10.dp).focusable().testTag("box2"))
            }
        }

        rule.onNodeWithTag("box1").requestFocus()
        rule.onRoot().performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()

        rule.onNodeWithTag("box2").assertIsFocused()
        assertThat(playedSoundDirection).isEqualTo(FocusDirection.Next)
    }

    @Test
    fun shiftTabNavigation_playsSoundEffect() {
        var playedSoundDirection: FocusDirection? = null

        rule.setContent {
            val view = LocalView.current as androidx.compose.ui.platform.AndroidComposeView
            view.playNavigationSoundEffect = { direction, _ -> playedSoundDirection = direction }

            Row {
                Box(Modifier.size(10.dp).focusable().testTag("box1"))
                Box(Modifier.size(10.dp).focusable().testTag("box2"))
            }
        }

        rule.onNodeWithTag("box2").requestFocus()
        rule.onRoot().performKeyInput {
            // Simulate Shift + Tab
            withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("box1").assertIsFocused()
        assertThat(playedSoundDirection).isEqualTo(FocusDirection.Previous)
    }
}
