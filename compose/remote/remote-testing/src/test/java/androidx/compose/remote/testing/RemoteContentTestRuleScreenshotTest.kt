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

package androidx.compose.remote.testing

import androidx.compose.remote.creation.compose.action.ValueChange
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteString
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.testing.util.assertAgainstGolden
import androidx.compose.remote.testing.util.captureToImage
import androidx.compose.remote.testing.util.saveToFile
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RemoteContentTestRuleScreenshotTest {
    @get:Rule val remoteContentTestRule = RemoteContentTestRule()

    private val saveScreenshots = false

    @Test
    fun textValueChange() {
        remoteContentTestRule.setContent(
            RemoteCreationDisplayInfo(width = 500, height = 500, densityDpi = 1)
        ) {
            val text = rememberMutableRemoteString("Initial")
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .clickable(ValueChange(text, "Updated".rs))
                        .background(Color.White),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text, color = Color.Black.rc)
            }
        }

        val screenshot = captureToImage(remoteContentTestRule.composeTestRule)
        if (saveScreenshots) {
            screenshot
                .asAndroidBitmap()
                .saveToFile("RemoteContentTestRuleScreenshotTest_textValueChange")
        }
        screenshot.assertAgainstGolden("RemoteContentTestRuleScreenshotTest_textValueChange")
    }
}
