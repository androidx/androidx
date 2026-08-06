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

package androidx.compose.remote.creation.compose.modifier

import android.content.Context
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
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

/** Emulator-based screenshot test of [GraphicsLayerModifier]. */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class GraphicsLayerModifierTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    val size = Size(200f, 200f)
    private val creationDisplayInfo = createCreationDisplayInfo(context, size)

    @Test
    fun graphicsLayerConstantAlpha() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = creationDisplayInfo,
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            DemoBox("graphicsLayer { alpha = 0.5f }".rs) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize().background(Color.Red.rc).graphicsLayer {
                            alpha = 0.5f.rf
                        }
                )
            }
        }
    }

    @Test
    fun graphicsLayerConstantTransforms() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = creationDisplayInfo,
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            DemoBox("graphicsLayer { scale = 0.7f, rotationZ = 45f }".rs) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize().background(Color.Red.rc).graphicsLayer {
                            scaleX = 0.7f.rf
                            scaleY = 0.7f.rf
                            rotationZ = 45f.rf
                        }
                )
            }
        }
    }

    @Test
    fun graphicsLayerDynamicAlpha() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = creationDisplayInfo,
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            val dynamicAlpha = 0.3f.rf
            DemoBox("graphicsLayer { alpha(dynamic) }".rs) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize().background(Color.Red.rc).graphicsLayer {
                            alpha = dynamicAlpha
                        }
                )
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun DemoBox(title: RemoteString, content: @RemoteComposable @Composable () -> Unit) {
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize(),
            contentAlignment = RemoteAlignment.Center,
        ) {
            content()
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize(),
                contentAlignment = RemoteAlignment.BottomCenter,
            ) {
                RemoteText(title, color = RemoteColor(Color.White), fontSize = 8.rsp)
            }
        }
    }
}
