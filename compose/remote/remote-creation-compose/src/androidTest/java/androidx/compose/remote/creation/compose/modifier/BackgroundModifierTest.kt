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

import android.content.Context
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.painter.painterRemoteColor
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.horizontalGradient
import androidx.compose.remote.creation.compose.shaders.radialGradient
import androidx.compose.remote.creation.compose.shaders.sweepGradient
import androidx.compose.remote.creation.compose.shaders.verticalGradient
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberRemoteColor
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based screenshot test of [BackgroundModifier]. */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class BackgroundModifierTest {
    @get:Rule
    val remoteComposeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
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

    fun RemoteInt.toHexDigit(): RemoteString {
        return eq(15.ri)
            .select(
                "F".rs,
                eq(14.ri)
                    .select(
                        "E".rs,
                        eq(13.ri)
                            .select(
                                "D".rs,
                                eq(12.ri)
                                    .select(
                                        "C".rs,
                                        eq(11.ri)
                                            .select(
                                                "B".rs,
                                                eq(10.ri)
                                                    .select("A".rs, absoluteValue.toRemoteString(1)),
                                            ),
                                    ),
                            ),
                    ),
            )
    }

    fun RemoteFloat.toHexString(): RemoteString {
        return (toRemoteInt() / 16).toHexDigit() + (toRemoteInt() % 16).toHexDigit()
    }

    fun RemoteColor.toHexString(): RemoteString {
        return "0x".rs +
            (alpha * 255f).toHexString() +
            (red * 255f).toHexString() +
            (green * 255f).toHexString() +
            (blue * 255f).toHexString()
    }

    @Test
    fun backgroundRemoteColor() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            val blue = Color.Blue.rc
            DemoBox("background(".rs + blue.toHexString() + ".rc)") {
                RemoteBox(modifier = RemoteModifier.fillMaxSize().background(blue))
            }
        }
    }

    @Test
    fun backgroundSolidColorNamedRemote() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            val blue = rememberRemoteColor("ABC") { Color.Blue }
            val title = ("background(".rs + blue.toHexString() + ".rc named)")
            DemoBox(title) { RemoteBox(modifier = RemoteModifier.fillMaxSize().background(blue)) }
        }
    }

    @Test
    fun backgroundSolidColor() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            val blue = Color.Blue
            DemoBox("background(0x".rs + Integer.toHexString(blue.toArgb()) + ")") {
                RemoteBox(modifier = RemoteModifier.fillMaxSize().background(blue))
            }
        }
    }

    @Test
    fun backgroundVerticalGradient() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            DemoBox("verticalGradient(listOf(Color.Blue, Color.Red))".rs) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(
                                RemoteBrush.verticalGradient(listOf(Color.Blue.rc, Color.Red.rc))
                            )
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
            DemoBox("horizontalGradient(listOf(Color.Blue, Color.Red))".rs) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(
                                RemoteBrush.horizontalGradient(listOf(Color.Blue.rc, Color.Red.rc))
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
            DemoBox("radialGradient(listOf(Color.Blue, Color.Red))".rs) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(
                                RemoteBrush.radialGradient(listOf(Color.Blue.rc, Color.Red.rc))
                            )
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
            DemoBox("sweepGradient(listOf(Color.Blue, Color.Red))".rs) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(
                                RemoteBrush.sweepGradient(listOf(Color.Blue.rc, Color.Red.rc))
                            )
                )
            }
        }
    }

    @Test
    fun backgroundRemotePainter() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            val blue = Color.Blue.rc
            DemoBox("background(painterRemoteColor(Color.Blue))".rs) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(remotePainter = painterRemoteColor(blue))
                )
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun DemoBox(title: RemoteString, content: @RemoteComposable @Composable () -> Unit) {
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
