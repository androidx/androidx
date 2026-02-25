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

import android.content.Context
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteButtonGroupThreeButtons
import androidx.wear.compose.remote.material3.previews.RemoteButtonGroupTwoButtons
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteButtonGroupTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo =
        CreationDisplayInfo(500, 500, context.resources.displayMetrics.densityDpi)

    @Test
    fun buttonGroup_threeButton() {
        remoteComposeTestRule.runScreenshotTest(
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            Center { RemoteButtonGroupThreeButtons() }
        }
    }

    @Test
    fun buttonGroup_twoButton() {
        remoteComposeTestRule.runScreenshotTest(
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            Center { RemoteButtonGroupTwoButtons() }
        }
    }

    @Composable
    @RemoteComposable
    private fun Center(
        modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        RemoteBox(
            modifier,
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = RemoteArrangement.Center,
            content = content,
        )
    }
}
