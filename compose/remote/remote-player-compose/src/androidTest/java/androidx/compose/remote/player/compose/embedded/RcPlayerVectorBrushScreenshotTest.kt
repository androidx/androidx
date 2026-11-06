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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.horizontalGradient
import androidx.compose.remote.creation.compose.shaders.verticalGradient
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.painterRemoteVector
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerVectorBrushScreenshotTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val remoteComposeTestRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = context,
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val whiteBackground: @Composable (composable: @Composable () -> Unit) -> Unit = {
        Box(modifier = Modifier.background(Color.White)) { it() }
    }

    @Test
    fun gradientFillAndStrokeWithAlpha() {
        val vector =
            RemoteImageVector.Builder(
                    viewportWidth = 24f.rf,
                    viewportHeight = 24f.rf,
                    tintColor = Color.Unspecified.rc,
                )
                .path(
                    fill = RemoteBrush.horizontalGradient(listOf(Color.Red.rc, Color.Blue.rc)),
                    fillAlpha = 0.5f.rf,
                    stroke = RemoteBrush.verticalGradient(listOf(Color.Green.rc, Color.Magenta.rc)),
                    strokeAlpha = 0.7f.rf,
                    strokeLineWidth = 3f.rf,
                ) {
                    moveTo(3f.rf, 3f.rf)
                    lineTo(21f.rf, 3f.rf)
                    lineTo(21f.rf, 21f.rf)
                    lineTo(3f.rf, 21f.rf)
                    close()
                }
                .build()

        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = createCreationDisplayInfo(context, Size(96f, 96f)),
            playComposableWrapper = whiteBackground,
        ) {
            RemoteBox(RemoteModifier.fillMaxSize()) {
                val painter = painterRemoteVector(vector, Color.Unspecified.rc)
                RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) { with(painter) { onDraw() } }
            }
        }
    }
}
