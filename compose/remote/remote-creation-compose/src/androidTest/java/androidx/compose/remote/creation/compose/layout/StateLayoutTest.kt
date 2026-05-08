/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.action.ValueChange
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteEnum
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class StateLayoutTest {
    @get:Rule
    val testRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    @Test
    fun single() =
        testRule.runScreenshotTest {
            NestedStateLayout("one".rs) { one ->
                RemoteText("Innermost $one".rs, color = Color.Black.rc)
            }
        }

    @Test
    fun update() {
        testRule.setContent {
            val currentState = rememberMutableRemoteEnum(LayoutState.First)
            NestedStateLayout("one".rs, currentState = currentState) { state ->
                RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                    RemoteText("State $state".rs, color = Color.Black.rc)
                    RemoteText(
                        "Switch".rs,
                        color = Color.Black.rc,
                        modifier =
                            RemoteModifier.clickable(
                                ValueChange(
                                    currentState,
                                    RemoteEnum(
                                        if (state == LayoutState.First) LayoutState.Second
                                        else LayoutState.First
                                    ),
                                )
                            ),
                    )
                }
            }
        }

        uiAutomator {
            val currentText = onElement { text?.contains("State") == true }
            assertThat(currentText.text).isEqualTo("State First")

            onElement { this.isClickable }.click()

            val updatedText = onElement { text == "State Second" }
            assertThat(updatedText.text).isEqualTo("State Second")
        }

        testRule.verifyScreenshot()
    }

    @Test
    fun nested() =
        testRule.runScreenshotTest {
            NestedStateLayout("one".rs) { one ->
                NestedStateLayout(
                    "two".rs,
                    currentState = rememberMutableRemoteEnum(LayoutState.Second),
                ) { two ->
                    NestedStateLayout("three".rs) { three ->
                        RemoteText("Innermost $one / $two / $three".rs, color = Color.Black.rc)
                    }
                }
            }
        }

    @Composable
    @RemoteComposable
    private fun NestedStateLayout(
        label: RemoteString,
        currentState: RemoteEnum<LayoutState> = rememberMutableRemoteEnum(LayoutState.First),
        content: @Composable @RemoteComposable (LayoutState) -> Unit,
    ) {
        RemoteStateLayout(
            state = currentState,
            modifier = RemoteModifier.fillMaxSize().background(Color.LightGray.rc),
        ) { layoutState ->
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().border(1.rdp, Color.Blue.rc).padding(10.rdp)
            ) {
                RemoteText(label + " / " + layoutState.toString(), color = Color.Black.rc)
                when (layoutState) {
                    LayoutState.First -> {
                        content(layoutState)
                    }
                    LayoutState.Second -> {
                        content(layoutState)
                    }
                }
            }
        }
    }

    private enum class LayoutState {
        First,
        Second,
    }
}
