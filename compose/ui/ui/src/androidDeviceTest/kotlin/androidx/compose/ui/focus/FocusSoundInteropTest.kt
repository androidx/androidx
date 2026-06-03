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

import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
class FocusSoundInteropTest {
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
    fun tabNavigation_acrossComposeViews_doesNotPlayComposeSound_viewFocusFix() {
        ComposeUiFlags.isBypassUnfocusableComposeViewEnabled = false
        ComposeUiFlags.isViewFocusFixEnabled = true
        testTabNavigation_doesNotPlayComposeSounds_delegatesToViewRootImplSideEffect()
    }

    @Test
    fun tabNavigation_acrossComposeViews_doesNotPlayComposeSound_bypassUnfocusable() {
        ComposeUiFlags.isBypassUnfocusableComposeViewEnabled = true
        ComposeUiFlags.isViewFocusFixEnabled = false
        testTabNavigation_doesNotPlayComposeSounds_delegatesToViewRootImplSideEffect()
    }

    @Test
    fun tabNavigation_acrossComposeViews_doesNotPlayComposeSound_legacy() {
        ComposeUiFlags.isBypassUnfocusableComposeViewEnabled = false
        ComposeUiFlags.isViewFocusFixEnabled = false
        testTabNavigation_doesNotPlayComposeSounds_delegatesToViewRootImplSideEffect()
    }

    private fun testTabNavigation_doesNotPlayComposeSounds_delegatesToViewRootImplSideEffect() {
        var soundsPlayed = 0
        rule.setContent {
            AndroidView(
                modifier = Modifier.testTag("androidView"),
                factory = { context ->
                    LinearLayout(context).apply {
                        orientation = HORIZONTAL

                        addView(
                            ComposeView(context).apply {
                                setContent {
                                    val view =
                                        LocalView.current
                                            as androidx.compose.ui.platform.AndroidComposeView
                                    view.playNavigationSoundEffect = { direction, _ ->
                                        soundsPlayed++
                                    }
                                    Row {
                                        Box(Modifier.size(10.dp).focusable().testTag("box1"))
                                        Box(Modifier.size(10.dp).focusable().testTag("box2"))
                                    }
                                }
                            }
                        )

                        addView(
                            ComposeView(context).apply {
                                setContent {
                                    val view =
                                        LocalView.current
                                            as androidx.compose.ui.platform.AndroidComposeView
                                    view.playNavigationSoundEffect = { direction, _ ->
                                        soundsPlayed++
                                    }
                                    Row {
                                        Box(Modifier.size(10.dp).focusable().testTag("box3"))
                                        Box(Modifier.size(10.dp).focusable().testTag("box4"))
                                    }
                                }
                            }
                        )
                    }
                },
            )
        }

        rule.onNodeWithTag("box2").requestFocus()
        rule.waitForIdle()

        soundsPlayed = 0
        rule.onNodeWithTag("androidView").performKeyInput { pressKey(Key.Tab) }
        rule.waitForIdle()

        assertThat(soundsPlayed).isEqualTo(0)
    }
}
