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

package androidx.compose.remote.a11y

import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.action.ValueChange
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.text
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based screenshot test of [RecordingCanvas]. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class BasicA11yTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun textSemantics() {
        remoteComposeTestRule.runTest {
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize(),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("Hello World")
            }
        }

        uiAutomator { onElement { text == "Hello World" } }
    }

    @Test
    fun textSemanticHierarchy() {
        remoteComposeTestRule.runTest {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().semantics { text = "Item 1".rs },
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText("Item 1.1")
                RemoteColumn(
                    modifier = RemoteModifier.fillMaxWidth().semantics { text = "Item 1.2".rs },
                    horizontalAlignment = RemoteAlignment.CenterHorizontally,
                    verticalArrangement = RemoteArrangement.Center,
                ) {
                    RemoteText("Item 1.2.1", modifier = RemoteModifier.padding(10.rf))
                    RemoteText("Item 1.2.2", modifier = RemoteModifier.padding(10.rf))
                }
                RemoteText("Item 1.3")
            }
        }

        uiAutomator {
            val item1 = onElement { text == "Item 1" }

            assertThat(item1.childCount).isEqualTo(3)
            assertThat(item1.children.size).isEqualTo(3)

            val item1_1 = item1.children[0]
            assertThat(item1_1.text).isEqualTo("Item 1.1")

            val item1_2 = item1.children[1]
            assertThat(item1_2.text).isEqualTo("Item 1.2")
            assertThat(item1_2.childCount).isEqualTo(2)

            val item1_2_1 = item1_2.children[0]
            assertThat(item1_2_1.text).isEqualTo("Item 1.2.1")

            val item1_2_2 = item1_2.children[1]
            assertThat(item1_2_2.text).isEqualTo("Item 1.2.2")

            val item1_3 = item1.children[2]
            assertThat(item1_3.text).isEqualTo("Item 1.3")
        }
    }

    @Test
    fun textValueChange() {
        remoteComposeTestRule.runTest {
            val text = rememberMutableRemoteString("Initial")
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .clickable(ValueChange(text, "Updated".rs))
                        .background(Color.White),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text)
            }
        }
        uiAutomator {
            val item = onElement { text == "Initial" }
            assertThat(item).isNotNull()

            item.click()
            assertThat(item.text).isEqualTo("Updated")
        }
    }

    @Test
    @Ignore("This test will be failing until b/492161842 is resolved")
    fun intValueChange() {
        remoteComposeTestRule.runTest {
            val remoteInt = rememberMutableRemoteInt(0)
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .clickable(ValueChange(remoteInt, remoteInt + 1))
                        .background(Color.White),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("".rs + remoteInt.toRemoteString(3, Rc.TextFromFloat.PAD_PRE_NONE))
            }
        }
        uiAutomator {
            val item = onElement { text == "0" }

            item.click()
            assertThat(item.text).isEqualTo("1")

            item.click()
            assertThat(item.text).isEqualTo("2")
        }
    }
}
