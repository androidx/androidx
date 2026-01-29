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
package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class LayoutDirectionTest {
    @get:Rule
    val remoteComposeTestRule by lazy {
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    }

    @Test
    fun ltr() {
        remoteComposeTestRule.runScreenshotTest {
            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth().background(Color.Black),
                horizontalArrangement = RemoteArrangement.Start,
                verticalAlignment = RemoteAlignment.CenterVertically,
            ) {
                RemoteBox(modifier = RemoteModifier.size(48.rdp)) { RemoteText("1".rs) }
                RemoteBox { RemoteText("Hello World".rs) }
                RemoteBox(modifier = RemoteModifier.size(48.rdp)) { RemoteText("3".rs) }
            }
        }
    }

    @Test
    fun rtl() {
        remoteComposeTestRule.runScreenshotTest(layoutDirection = LayoutDirection.Rtl) {
            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth().background(Color.Black),
                horizontalArrangement = RemoteArrangement.Start,
                verticalAlignment = RemoteAlignment.CenterVertically,
            ) {
                RemoteBox(modifier = RemoteModifier.size(48.rdp)) { RemoteText("1".rs) }
                RemoteBox { RemoteText("مرحبا بالعالم".rs) }
                RemoteBox(modifier = RemoteModifier.size(48.rdp)) { RemoteText("3".rs) }
            }
        }
    }

    @Test
    fun rtl_manual() {
        // Do the manual workarounds to display correctly
        remoteComposeTestRule.runScreenshotTest(layoutDirection = LayoutDirection.Rtl) {
            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth().background(Color.Black),
                horizontalArrangement = RemoteArrangement.End,
                verticalAlignment = RemoteAlignment.CenterVertically,
            ) {
                RemoteBox(modifier = RemoteModifier.size(48.rdp)) { RemoteText("3".rs) }
                RemoteBox { RemoteText("مرحبا بالعالم".rs) }
                RemoteBox(modifier = RemoteModifier.size(48.rdp)) { RemoteText("1".rs) }
            }
        }
    }
}
