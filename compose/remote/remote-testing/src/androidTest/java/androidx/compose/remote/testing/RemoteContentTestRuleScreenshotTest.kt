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
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
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
import androidx.compose.remote.testing.util.GoldenScreenshotNameTestRule
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.graphics.Color
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteContentTestRuleScreenshotTest {
    @get:Rule val remoteContentTestRule = RemoteContentTestRule()
    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @get:Rule val goldenScreenshotNameTestRule = GoldenScreenshotNameTestRule()

    @Test
    fun textValueChange() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        remoteContentTestRule.setContent(createCreationDisplayInfo(context)) {
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

        val screenshotBefore = remoteContentTestRule.captureRootToImage()
        screenshotBefore.assertAgainstGolden(
            screenshotRule,
            goldenScreenshotNameTestRule.getName("before"),
        )

        uiAutomator {
            val item = onElement { isClickable }
            assertThat(item).isNotNull()

            item.click()
            assertThat(item.text).isEqualTo("Updated")
        }

        val screenshotAfter = remoteContentTestRule.captureRootToImage()
        screenshotAfter.assertAgainstGolden(
            screenshotRule,
            goldenScreenshotNameTestRule.getName("after"),
        )
    }
}
