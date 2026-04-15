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

package androidx.compose.remote.creation.compose.layout

import android.content.Context
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontVariation
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteCanvasScreenshotTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun remoteCanvas_drawText_fontVariations() {
        val width = 300
        val height = 200
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                ),
            backgroundColor = Color.Black,
        ) {
            val paintNull =
                RemotePaint().apply {
                    color = Color.White.rc
                    textSize = 30f.rf
                }
            val paintW100 =
                RemotePaint().apply {
                    color = Color.White.rc
                    textSize = 30f.rf
                    fontVariationSettings = FontVariation.Settings(FontVariation.weight(100))
                }
            val paintW900 =
                RemotePaint().apply {
                    color = Color.White.rc
                    textSize = 30f.rf
                    fontVariationSettings = FontVariation.Settings(FontVariation.weight(900))
                }

            val text = "Hello Font!".rs

            RemoteCanvas(modifier = RemoteModifier.size(width.rdp, height.rdp)) {
                drawText(text, 10f.rf, 40f.rf, paintNull)
                drawText(text, 10f.rf, 90f.rf, paintW100)
                drawText(text, 10f.rf, 140f.rf, paintW900)
            }
        }
    }
}
