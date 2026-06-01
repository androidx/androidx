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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver as composeCompositeOver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 33, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemoteColorCompositeOverScreenshotTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    @Test
    fun compositeOverColors() =
        composeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = RemoteCreationDisplayInfo(300, 510, 240)
        ) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().background(Color.White).padding(8.rdp)
            ) {
                // Case 1: Constant Blue + 50% Red
                ComparisonRow(
                    title = "1. Constant: Blue + 50% Red",
                    bg = Color.Blue.rc,
                    fg = Color.Red.rc.copy(alpha = 0.5f.rf),
                    remoteResult = Color.Red.rc.copy(alpha = 0.5f.rf).compositeOver(Color.Blue.rc),
                    composeResult = Color.Red.copy(alpha = 0.5f).composeCompositeOver(Color.Blue).rc,
                )

                RemoteSpacer(modifier = RemoteModifier.height(6.rdp))

                // Case 2: Opaque Background: Blue + Dynamic 40% Magenta
                val dynamicAlpha40 = rememberNamedRemoteFloat("alpha_40") { 0.4f.rf }
                ComparisonRow(
                    title = "2. Opaque BG: Blue + Dynamic 40% Magenta",
                    bg = Color.Blue.rc,
                    fg = Color.Magenta.rc.copy(alpha = dynamicAlpha40),
                    remoteResult =
                        Color.Magenta.rc.copy(alpha = dynamicAlpha40).compositeOver(Color.Blue.rc),
                    composeResult =
                        Color.Magenta.copy(alpha = 0.4f).composeCompositeOver(Color.Blue).rc,
                )

                RemoteSpacer(modifier = RemoteModifier.height(6.rdp))

                // Case 3: Both Alphas Constant: Dynamic 50% Red + Dynamic 60% Blue
                val dynamicRedComponent = rememberNamedRemoteFloat("red_comp") { 0.8f.rf }
                val bgConstAlpha =
                    RemoteColor.rgb(
                        red = dynamicRedComponent,
                        green = 0.rf,
                        blue = 0.rf,
                        alpha = 0.5f.rf,
                    )
                val dynamicBlueComponent = rememberNamedRemoteFloat("blue_comp") { 0.9f.rf }
                val fgConstAlpha =
                    RemoteColor.rgb(
                        red = 0.rf,
                        green = 0.rf,
                        blue = dynamicBlueComponent,
                        alpha = 0.6f.rf,
                    )
                ComparisonRow(
                    title = "3. Const Alphas: Dynamic Red (50%) + Dynamic Blue (60%)",
                    bg = bgConstAlpha,
                    fg = fgConstAlpha,
                    remoteResult = fgConstAlpha.compositeOver(bgConstAlpha),
                    composeResult =
                        Color(red = 0f, green = 0f, blue = 0.9f, alpha = 0.6f)
                            .composeCompositeOver(
                                Color(red = 0.8f, green = 0f, blue = 0f, alpha = 0.5f)
                            )
                            .rc,
                )

                RemoteSpacer(modifier = RemoteModifier.height(6.rdp))

                // Case 4: Fully Dynamic: 80% Cyan + 30% Yellow
                val dynamicAlpha80 = rememberNamedRemoteFloat("alpha_80") { 0.8f.rf }
                val dynamicBgCyan = Color.Cyan.rc.copy(alpha = dynamicAlpha80)
                val dynamicAlpha30 = rememberNamedRemoteFloat("alpha_30") { 0.3f.rf }
                val dynamicFgYellow = Color.Yellow.rc.copy(alpha = dynamicAlpha30)
                ComparisonRow(
                    title = "4. Fully Dynamic: 80% Cyan + 30% Yellow",
                    bg = dynamicBgCyan,
                    fg = dynamicFgYellow,
                    remoteResult = dynamicFgYellow.compositeOver(dynamicBgCyan),
                    composeResult =
                        Color.Yellow.copy(alpha = 0.3f)
                            .composeCompositeOver(Color.Cyan.copy(alpha = 0.8f))
                            .rc,
                )

                RemoteSpacer(modifier = RemoteModifier.height(6.rdp))

                // Case 5: Black + 70% White
                ComparisonRow(
                    title = "5. Black + 70% White",
                    bg = Color.Black.rc,
                    fg = Color.White.rc.copy(alpha = 0.7f.rf),
                    remoteResult =
                        Color.White.rc.copy(alpha = 0.7f.rf).compositeOver(Color.Black.rc),
                    composeResult =
                        Color.White.copy(alpha = 0.7f).composeCompositeOver(Color.Black).rc,
                )
            }
        }

    @Composable
    @RemoteComposable
    private fun ComparisonRow(
        title: String,
        bg: RemoteColor,
        fg: RemoteColor,
        remoteResult: RemoteColor,
        composeResult: RemoteColor,
    ) {
        RemoteColumn {
            RemoteText(text = title, fontSize = 9.rsp, color = Color.Black.rc)
            RemoteSpacer(modifier = RemoteModifier.height(2.rdp))

            // Row 1: Remote Blend
            RemoteRow(verticalAlignment = RemoteAlignment.CenterVertically) {
                RemoteBox(modifier = RemoteModifier.size(20.rdp).background(bg))
                RemoteText(text = " + ", fontSize = 8.rsp, color = Color.Gray.rc)
                RemoteBox(modifier = RemoteModifier.size(20.rdp).background(fg))
                RemoteText(text = " = ", fontSize = 8.rsp, color = Color.Gray.rc)
                RemoteBox(modifier = RemoteModifier.size(20.rdp).background(remoteResult))
                RemoteText(text = " (Rem)", fontSize = 7.rsp, color = Color.Gray.rc)
            }

            RemoteSpacer(modifier = RemoteModifier.height(2.rdp))

            // Row 2: Compose Blend
            RemoteRow(verticalAlignment = RemoteAlignment.CenterVertically) {
                RemoteBox(modifier = RemoteModifier.size(20.rdp).background(bg))
                RemoteText(text = " + ", fontSize = 8.rsp, color = Color.Gray.rc)
                RemoteBox(modifier = RemoteModifier.size(20.rdp).background(fg))
                RemoteText(text = " = ", fontSize = 8.rsp, color = Color.Gray.rc)
                RemoteBox(modifier = RemoteModifier.size(20.rdp).background(composeResult))
                RemoteText(text = " (Comp)", fontSize = 7.rsp, color = Color.Gray.rc)
            }
        }
    }
}
