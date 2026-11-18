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

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteLayoutDirection
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.remote.player.compose.test.utils.plus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

/**
 * End-to-end Scuba screenshot tests validating LTR and RTL document creation
 * (`remote-creation-compose`) and playback with the embedded Compose player (`RcPlayer`) for both
 * layout and text.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class LayoutDirectionEmbeddedScreenshotTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val density = context.resources.displayMetrics.density

    @get:Rule
    val screenshotRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            remoteCreationDisplayInfo =
                createRemoteCreationDisplayInfo(context, Size(240f * density, 240f * density)),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    @Test
    fun creationLtr_playLtr() {
        screenshotRule.runScreenshotTest(
            playComposableWrapper = ComposableWrappers.blackBackground
        ) {
            LayoutAndTextSample()
        }
    }

    @Test
    fun creationLtr_playRtl() {
        screenshotRule.runScreenshotTest(
            playComposableWrapper = ComposableWrappers.blackBackground + ComposableWrappers.rtl
        ) {
            LayoutAndTextSample()
        }
    }

    @Test
    fun creationRtl_playLtr() {
        screenshotRule.runScreenshotTest(
            creationComposableWrapper = ComposableWrappers.rtl,
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            LayoutAndTextSample()
        }
    }

    @Test
    fun creationRtl_playRtl() {
        screenshotRule.runScreenshotTest(
            creationComposableWrapper = ComposableWrappers.rtl,
            playComposableWrapper = ComposableWrappers.blackBackground + ComposableWrappers.rtl,
        ) {
            LayoutAndTextSample()
        }
    }

    @Test
    fun nestedOverrides_creationLtr_playRtl() {
        screenshotRule.runScreenshotTest(
            playComposableWrapper = ComposableWrappers.blackBackground + ComposableWrappers.rtl
        ) {
            NestedLayoutAndTextSample(innerDirection = RemoteLayoutDirection.Rtl)
        }
    }

    @Test
    fun nestedOverrides_creationRtl_playLtr() {
        screenshotRule.runScreenshotTest(
            creationComposableWrapper = ComposableWrappers.rtl,
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            NestedLayoutAndTextSample(innerDirection = RemoteLayoutDirection.Ltr)
        }
    }

    @Composable
    @RemoteComposable
    private fun LayoutAndTextSample() {
        RemoteColumn(
            modifier =
                RemoteModifier.fillMaxSize()
                    .background(Color(0xFF1A1C1E).rc)
                    .padding(start = 16.rdp, top = 8.rdp, end = 4.rdp, bottom = 8.rdp)
        ) {
            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth().background(Color(0xFF2E3238).rc),
                horizontalArrangement = RemoteArrangement.Start,
            ) {
                RemoteBox(modifier = RemoteModifier.size(36.rdp, 22.rdp).background(Color.Red.rc))
                RemoteBox(modifier = RemoteModifier.size(24.rdp, 22.rdp).background(Color.Blue.rc))
                RemoteBox(modifier = RemoteModifier.size(14.rdp, 22.rdp).background(Color.Green.rc))
            }
            RemoteBox(modifier = RemoteModifier.height(6.rdp))
            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth().background(Color(0xFF2E3238).rc),
                horizontalArrangement = RemoteArrangement.spacedBy(8.rdp, RemoteAlignment.End),
            ) {
                RemoteBox(
                    modifier = RemoteModifier.size(20.rdp, 18.rdp).background(Color.Yellow.rc)
                )
                RemoteBox(
                    modifier = RemoteModifier.size(12.rdp, 18.rdp).background(Color.Magenta.rc)
                )
            }
            RemoteBox(modifier = RemoteModifier.height(6.rdp))
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxWidth().height(26.rdp).background(Color(0xFF262A30).rc),
                contentAlignment = RemoteAlignment.CenterStart,
            ) {
                RemoteBox(modifier = RemoteModifier.size(48.rdp, 14.rdp).background(Color.Cyan.rc))
            }
            RemoteBox(modifier = RemoteModifier.height(6.rdp))
            RemoteText(
                text = "Start aligned".rs,
                modifier = RemoteModifier.fillMaxWidth().background(Color(0xFF1E3A5F).rc),
                color = Color.White.rc,
                fontSize = 14.rsp,
                textAlign = RemoteTextAlign.Start,
            )
            RemoteText(
                text = "End aligned".rs,
                modifier = RemoteModifier.fillMaxWidth().background(Color(0xFF1F4E46).rc),
                color = Color.White.rc,
                fontSize = 14.rsp,
                textAlign = RemoteTextAlign.End,
            )
            RemoteText(
                text = "Left aligned".rs,
                modifier = RemoteModifier.fillMaxWidth(),
                color = Color.Yellow.rc,
                fontSize = 12.rsp,
                textAlign = RemoteTextAlign.Left,
            )
            RemoteText(
                text = "Right aligned".rs,
                modifier = RemoteModifier.fillMaxWidth(),
                color = Color.Cyan.rc,
                fontSize = 12.rsp,
                textAlign = RemoteTextAlign.Right,
            )
            RemoteText(
                text = "שלום World!".rs,
                modifier = RemoteModifier.fillMaxWidth().background(Color(0xFF3D2C4E).rc),
                color = Color.White.rc,
                fontSize = 14.rsp,
                textAlign = RemoteTextAlign.Start,
            )
        }
    }

    @Composable
    @RemoteComposable
    private fun NestedLayoutAndTextSample(innerDirection: RemoteLayoutDirection) {
        RemoteColumn(
            modifier = RemoteModifier.fillMaxSize().background(Color(0xFF1A1C1E).rc).padding(12.rdp)
        ) {
            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth().background(Color(0xFF2E3238).rc),
                horizontalArrangement = RemoteArrangement.Start,
            ) {
                RemoteBox(modifier = RemoteModifier.size(32.rdp, 20.rdp).background(Color.Red.rc))
                RemoteBox(modifier = RemoteModifier.size(18.rdp, 20.rdp).background(Color.Blue.rc))
            }
            RemoteText(
                text = "Outer Start".rs,
                modifier = RemoteModifier.fillMaxWidth().background(Color(0xFF1E3A5F).rc),
                color = Color.White.rc,
                fontSize = 14.rsp,
                textAlign = RemoteTextAlign.Start,
            )
            RemoteBox(modifier = RemoteModifier.height(12.rdp))
            CompositionLocalProvider(LocalRemoteLayoutDirection provides innerDirection) {
                RemoteColumn(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .background(Color(0xFF262A30).rc)
                            .padding(start = 16.rdp, top = 8.rdp, end = 4.rdp, bottom = 8.rdp)
                ) {
                    RemoteRow(
                        modifier = RemoteModifier.fillMaxWidth(),
                        horizontalArrangement = RemoteArrangement.Start,
                    ) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(32.rdp, 20.rdp).background(Color.Green.rc)
                        )
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(18.rdp, 20.rdp).background(Color.Yellow.rc)
                        )
                    }
                    RemoteText(
                        text = "Inner Override".rs,
                        modifier = RemoteModifier.fillMaxWidth().background(Color(0xFF1F4E46).rc),
                        color = Color.White.rc,
                        fontSize = 14.rsp,
                        textAlign = RemoteTextAlign.Start,
                    )
                }
            }
        }
    }
}
