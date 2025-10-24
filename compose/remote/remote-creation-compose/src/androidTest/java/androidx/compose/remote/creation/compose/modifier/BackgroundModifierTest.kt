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
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.capture.shaders.horizontalGradient
import androidx.compose.remote.creation.compose.capture.shaders.radialGradient
import androidx.compose.remote.creation.compose.capture.shaders.sweepGradient
import androidx.compose.remote.creation.compose.capture.shaders.verticalGradient
import androidx.compose.remote.creation.compose.layout.Arrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rememberRemoteIntValue
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based screenshot test of [BackgroundModifier]. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class BackgroundModifierTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )

    val size = Size(200f, 200f)

    @SuppressLint("UnrememberedMutableState")
    @Test
    fun backgroundSolidColor() {
        remoteComposeTestRule.runScreenshotTest(size = size, backgroundColor = Color.Black) {
            DemoBox("background(Color.Blue)") {
                RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Blue)) {}
            }
        }
    }

    @Test
    fun backgroundVerticalGradient() {
        remoteComposeTestRule.runScreenshotTest(size = size, backgroundColor = Color.Black) {
            DemoBox("verticalGradient(listOf(Color.Blue, Color.Red))") {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(
                                RemoteBrush.verticalGradient(
                                    listOf(Color.Blue, Color.Red),
                                    startY = 0f,
                                    endY = 200f,
                                )
                            )
                ) {}
            }
        }
    }

    @Test
    fun backgroundHorizontalGradient() {
        remoteComposeTestRule.runScreenshotTest(size = size, backgroundColor = Color.Black) {
            DemoBox("horizontalGradient(listOf(Color.Blue, Color.Red))") {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(
                                RemoteBrush.horizontalGradient(
                                    listOf(Color.Blue, Color.Red),
                                    startX = 0f,
                                    endX = 200f,
                                )
                            )
                ) {}
            }
        }
    }

    @Test
    fun backgroundRadialGradient() {
        remoteComposeTestRule.runScreenshotTest(size = size, backgroundColor = Color.Black) {
            DemoBox("radialGradient(listOf(Color.Blue, Color.Red))") {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(
                                RemoteBrush.radialGradient(
                                    listOf(Color.Blue, Color.Red),
                                    radius = 100f,
                                    center = Offset(100f, 100f),
                                )
                            )
                ) {}
            }
        }
    }

    @Test
    fun backgroundSweepGradient() {
        remoteComposeTestRule.runScreenshotTest(size = size, backgroundColor = Color.Black) {
            DemoBox("sweepGradient(listOf(Color.Blue, Color.Red))") {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(
                                RemoteBrush.sweepGradient(
                                    listOf(Color.Blue, Color.Red),
                                    center = Offset(100f, 100f),
                                )
                            )
                ) {}
            }
        }
    }

    @Test
    fun backgroundRemoteColor() {
        remoteComposeTestRule.runScreenshotTest(size = size, backgroundColor = Color.Black) {
            // workaround issue with createNamedRemoteFloat
            val alpha = rememberRemoteIntValue("a1") { 1 }.toRemoteFloat()
            val red = rememberRemoteIntValue("r1") { 1 }.toRemoteFloat()
            val green = rememberRemoteIntValue("g1") { 0 }.toRemoteFloat()
            val blue = rememberRemoteIntValue("b1") { 0 }.toRemoteFloat()

            val color = RemoteColor.fromARGB(alpha, red, green, blue)

            DemoBox("background(RemoteColor.fromARGB(alpha, red, green, blue))") {
                RemoteBox(modifier = RemoteModifier.fillMaxSize().background(color)) {}
            }
        }
    }

    @SuppressLint("UnrememberedMutableState")
    @RemoteComposable
    @Composable
    private fun DemoBox(title: String, content: @RemoteComposable @Composable () -> Unit) {
        RemoteBox(modifier = RemoteModifier.fillMaxSize()) {
            content()
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                RemoteText(title, color = RemoteColor(Color.White), fontSize = 8.sp)
            }
        }
    }
}
