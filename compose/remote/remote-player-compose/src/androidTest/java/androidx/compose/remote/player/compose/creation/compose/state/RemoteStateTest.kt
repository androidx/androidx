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

package androidx.compose.remote.player.compose.creation.compose.state

import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberRemoteString
import androidx.compose.remote.player.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteStateTest {
    @get:Rule
    val composeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun testNamedFloatIdDiffers() {
        var widthId = 0
        var configurableWidthId = 0
        var configurableWidth2Id = 0

        composeTestRule.runTest {
            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                val creationState = LocalRemoteComposeCreationState.current

                val width = rememberRemoteFloat { componentWidth() }

                val configurableWidth = rememberRemoteFloat(name = "configurableWidth") { width }

                val configurableWidth2 = rememberRemoteFloat(name = "configurableWidth2") { width }

                RemoteText(RemoteString("Width: ") + width.toRemoteString(3, 0))
                RemoteText(
                    RemoteString("Configurable Width: ") + configurableWidth.toRemoteString(3, 0)
                )
                RemoteText(
                    RemoteString("Configurable Width2: ") + configurableWidth2.toRemoteString(3, 0)
                )

                with(creationState) {
                    widthId = width.id
                    configurableWidthId = configurableWidth.id
                    configurableWidth2Id = configurableWidth2.id
                }
            }
        }

        assertThat(widthId).isNotEqualTo(configurableWidthId)
        assertThat(configurableWidthId).isNotEqualTo(configurableWidth2Id)
    }

    @Test
    fun testNamedStringIdDiffers() {
        var valId = 0
        var namedId1 = 0
        var namedId2 = 0

        composeTestRule.runTest {
            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                val valString = rememberRemoteString { "Hello" }

                val namedString1 = rememberRemoteString(name = "named1") { "Hello" }

                val namedString2 = rememberRemoteString(name = "named2") { "Hello" }

                RemoteText(RemoteString("val: ") + valString)
                RemoteText(RemoteString("named1: ") + namedString1)
                RemoteText(RemoteString("named2: ") + namedString2)

                val state = LocalRemoteComposeCreationState.current

                valId = valString.getIdForCreationState(state)
                namedId1 = namedString1.getIdForCreationState(state)
                namedId2 = namedString2.getIdForCreationState(state)
            }
        }

        // If ids match that updates will be seen across values
        assertThat(valId).isNotEqualTo(namedId1)
        assertThat(valId).isNotEqualTo(namedId2)
        assertThat(namedId1).isNotEqualTo(namedId2)
    }
}
