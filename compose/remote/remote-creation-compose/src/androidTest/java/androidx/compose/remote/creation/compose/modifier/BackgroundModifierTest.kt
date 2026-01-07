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

package androidx.compose.remote.creation.compose.modifier

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.capture.shaders.horizontalGradient
import androidx.compose.remote.creation.compose.capture.shaders.radialGradient
import androidx.compose.remote.creation.compose.capture.shaders.sweepGradient
import androidx.compose.remote.creation.compose.capture.shaders.verticalGradient
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rememberRemoteIntValue
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based screenshot test of [BackgroundModifier]. */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(TestParameterInjector::class)
class BackgroundModifierTest {
    @TestParameter private lateinit var targetPlayer: TargetPlayer

    @get:Rule
    val remoteComposeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = targetPlayer,
            matcher = MSSIMMatcher(threshold = 0.999),
        )
    }
    private val context: Context = ApplicationProvider.getApplicationContext()

    val size = Size(200f, 200f)
    private val creationDisplayInfo =
        CreationDisplayInfo(
            size.width.toInt(),
            size.height.toInt(),
            context.resources.displayMetrics.densityDpi,
        )

    @SuppressLint("UnrememberedMutableState")
    @Test
    fun backgroundSolidColor() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            DemoBox("background(Color.Blue)") {
                RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Blue))
            }
        }
    }

    @Test
    fun backgroundVerticalGradient() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            DemoBox("verticalGradient(listOf(Color.Blue, Color.Red))") {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(RemoteBrush.verticalGradient(listOf(Color.Blue, Color.Red)))
                )
            }
        }
    }

    @Test
    fun backgroundHorizontalGradient() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            DemoBox("horizontalGradient(listOf(Color.Blue, Color.Red))") {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(
                                RemoteBrush.horizontalGradient(listOf(Color.Blue, Color.Red))
                            )
                )
            }
        }
    }

    @Test
    fun backgroundRadialGradient() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            DemoBox("radialGradient(listOf(Color.Blue, Color.Red))") {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(RemoteBrush.radialGradient(listOf(Color.Blue, Color.Red)))
                )
            }
        }
    }

    @Test
    fun backgroundSweepGradient() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            DemoBox("sweepGradient(listOf(Color.Blue, Color.Red))") {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(RemoteBrush.sweepGradient(listOf(Color.Blue, Color.Red)))
                )
            }
        }
    }

    @Test
    fun backgroundRemoteColor() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            // workaround issue with createNamedRemoteFloat
            val alpha = rememberRemoteIntValue("a1") { 1 }.toRemoteFloat()
            val red = rememberRemoteIntValue("r1") { 1 }.toRemoteFloat()
            val green = rememberRemoteIntValue("g1") { 0 }.toRemoteFloat()
            val blue = rememberRemoteIntValue("b1") { 0 }.toRemoteFloat()

            val color = RemoteColor.fromARGB(alpha, red, green, blue)

            DemoBox("background(RemoteColor.fromARGB(alpha, red, green, blue))") {
                RemoteBox(modifier = RemoteModifier.fillMaxSize().background(color))
            }
        }
    }

    @SuppressLint("UnrememberedMutableState")
    @RemoteComposable
    @Composable
    private fun DemoBox(title: String, content: @RemoteComposable @Composable () -> Unit) {
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = RemoteArrangement.Center,
        ) {
            content()
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize(),
                verticalArrangement = RemoteArrangement.Bottom,
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                RemoteText(title, color = RemoteColor(Color.White), fontSize = 8.sp)
            }
        }
    }
}
