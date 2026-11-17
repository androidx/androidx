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
import android.content.res.Resources
import androidx.compose.remote.creation.compose.capture.createRemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteContentDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.remote.player.compose.test.utils.plus
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

/**
 * Screenshot tests for [RcPlayer] modifier ordering (`graphicsLayer`, `padding`, `drawWithContent`)
 * and host RTL `LocalLayoutDirection` isolation.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerModifierAndLayoutDirectionScreenshotTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val density = Resources.getSystem().displayMetrics.density

    @get:Rule
    val screenshotRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = context,
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun paddingAndGraphicsLayerWithDrawWithContent_grid() =
        screenshotRule.runScreenshotTest(
            playComposableWrapper = ComposableWrappers.blackBackground
        ) {
            val blueFill: RemoteContentDrawScope.() -> Unit = {
                val paint = RemotePaint().apply { color = RemoteColor(Color.Blue) }
                drawRect(paint = paint)
                drawContent()
            }
            val tests =
                listOf<Pair<String, @RemoteComposable @Composable () -> Unit>>(
                    "padding then draw" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(70.rdp)
                                        .background(Color.White.rc)
                                        .padding(15.rdp)
                                        .drawWithContent(blueFill)
                            ) {
                                RemoteBox(
                                    modifier = RemoteModifier.size(20.rdp).background(Color.Red.rc)
                                )
                            }
                        },
                    "asymmetric padding draw" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(70.rdp)
                                        .background(Color.White.rc)
                                        .padding(
                                            start = 20.rdp,
                                            top = 10.rdp,
                                            end = 5.rdp,
                                            bottom = 5.rdp,
                                        )
                                        .drawWithContent(blueFill)
                            ) {
                                RemoteBox(
                                    modifier = RemoteModifier.size(20.rdp).background(Color.Red.rc)
                                )
                            }
                        },
                    "graphicsLayer then padding" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(70.rdp)
                                        .graphicsLayer {
                                            rotationZ = 45f.rf
                                            scaleX = 0.75f.rf
                                            scaleY = 0.75f.rf
                                        }
                                        .background(Color.White.rc)
                                        .padding(
                                            start = 24.rdp,
                                            top = 8.rdp,
                                            end = 4.rdp,
                                            bottom = 4.rdp,
                                        )
                                        .drawWithContent(blueFill)
                            ) {
                                RemoteBox(
                                    modifier = RemoteModifier.size(20.rdp).background(Color.Red.rc)
                                )
                            }
                        },
                    "padding then graphicsLayer" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(70.rdp)
                                        .background(Color.White.rc)
                                        .padding(
                                            start = 24.rdp,
                                            top = 8.rdp,
                                            end = 4.rdp,
                                            bottom = 4.rdp,
                                        )
                                        .graphicsLayer {
                                            rotationZ = 45f.rf
                                            scaleX = 0.75f.rf
                                            scaleY = 0.75f.rf
                                        }
                                        .drawWithContent(blueFill)
                            ) {
                                RemoteBox(
                                    modifier = RemoteModifier.size(20.rdp).background(Color.Red.rc)
                                )
                            }
                        },
                )
            gridScreenshotUI.GridContent(tests)
        }

    @Test
    fun hostRtlLayoutDirection_doesNotMirrorLtrDocument() {
        screenshotRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                createRemoteCreationDisplayInfo(context, Size(200f * density, 80f * density)),
            playComposableWrapper = ComposableWrappers.blackBackground + ComposableWrappers.rtl,
        ) {
            RowLayoutSample(label = "LTR")
        }
    }

    @Test
    fun hostRtlLayoutDirection_preservesRtlCreatedDocument() {
        screenshotRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                createRemoteCreationDisplayInfo(context, Size(200f * density, 80f * density)),
            creationComposableWrapper = ComposableWrappers.rtl,
            playComposableWrapper = ComposableWrappers.blackBackground + ComposableWrappers.rtl,
        ) {
            RowLayoutSample(label = "RTL")
        }
    }

    @RemoteComposable
    @Composable
    private fun RowLayoutSample(label: String) {
        RemoteRow(
            modifier =
                RemoteModifier.fillMaxSize()
                    .background(Color.DarkGray.rc)
                    .padding(start = 16.rdp, top = 8.rdp, end = 4.rdp, bottom = 8.rdp)
        ) {
            RemoteBox(modifier = RemoteModifier.size(30.rdp).background(Color.Red.rc))
            RemoteBox(modifier = RemoteModifier.size(30.rdp).background(Color.Blue.rc))
            RemoteText(label.rs, color = Color.White.rc)
        }
    }
}
