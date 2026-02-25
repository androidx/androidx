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
package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemoteButtonA11yTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun button_isFocusable() {
        remoteComposeTestRule.runTest {
            RemoteButton(onClick = HostAction("click".rs)) { RemoteText("text".rs) }
        }

        uiAutomator {
            val button = onElement { text == "text" }
            assertThat(button.isFocusable).isTrue()
            assertThat(button.isClickable).isTrue()
        }
    }

    @Test
    fun compactButton_isFocusable() {
        remoteComposeTestRule.runTest {
            RemoteCompactButton(onClick = HostAction("click".rs)) { RemoteText("text".rs) }
        }

        uiAutomator {
            val button = onElement { text == "text" }
            assertThat(button.isFocusable).isTrue()
            assertThat(button.isClickable).isTrue()
        }
    }

    @Test
    fun buttonWithSecondaryLabelAndIcon_isFocusable() {
        remoteComposeTestRule.runTest {
            RemoteButton(
                onClick = HostAction("click".rs),
                secondaryLabel = { RemoteText("text".rs) },
                icon = { RemoteIcon(TestImageVectors.VolumeUp, contentDescription = "VolumeUp".rs) },
            ) {}
        }

        uiAutomator {
            val button = onElement { text == "text" }
            assertThat(button.isFocusable).isTrue()
            assertThat(button.isClickable).isTrue()
        }
    }

    @Test
    fun iconButton_isFocusable() {
        remoteComposeTestRule.runTest {
            RemoteIconButton(onClick = HostAction("click".rs)) {
                RemoteIcon(TestImageVectors.VolumeUp, contentDescription = "Add".rs)
            }
        }

        uiAutomator {
            val button = onElement { contentDescription == "Add" }
            assertThat(button.isFocusable).isTrue()
            assertThat(button.isClickable).isTrue()
        }
    }

    @Test
    fun textButton_isFocusable() {
        remoteComposeTestRule.runTest {
            RemoteTextButton(onClick = HostAction("click".rs)) { RemoteText("text".rs) }
        }

        uiAutomator {
            val button = onElement { text == "text" }
            assertThat(button.isFocusable).isTrue()
            assertThat(button.isClickable).isTrue()
        }
    }
}
