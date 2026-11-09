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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.content.res.Resources
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemoteCompositingStrategy
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests comparing [RemoteCompositingStrategy.Auto],
 * [RemoteCompositingStrategy.Offscreen], and [RemoteCompositingStrategy.ModulateAlpha] in
 * [RcPlayer].
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerCompositingStrategyScreenshotTest {

    private val density = Resources.getSystem().displayMetrics.density

    @get:Rule
    val screenshotRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = ApplicationProvider.getApplicationContext(),
                    size = Size(WIDTH_DP * density, HEIGHT_DP * density),
                ),
        )

    @Test
    fun compositingStrategies() {
        val entries =
            listOf(
                "Auto" to RemoteCompositingStrategy.Auto,
                "Offscreen" to RemoteCompositingStrategy.Offscreen,
                "Modulate" to RemoteCompositingStrategy.ModulateAlpha,
            )
        screenshotRule.runScreenshotTest {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().background(Color(0xFFF5F5F5).rc),
                verticalArrangement = RemoteArrangement.spacedBy(6.rdp),
            ) {
                RemoteRow(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .background(Color.DarkGray.rc)
                            .padding(horizontal = 8.rdp, vertical = 4.rdp),
                    horizontalArrangement = RemoteArrangement.SpaceAround,
                ) {
                    for ((label, _) in entries) {
                        RemoteBox(
                            modifier = RemoteModifier.size(76.rdp, 16.rdp),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteText(label.rs, color = Color.White.rc, fontSize = 11.rsp)
                        }
                    }
                }

                RemoteBox(
                    modifier = RemoteModifier.padding(horizontal = 8.rdp),
                    contentAlignment = RemoteAlignment.CenterStart,
                ) {
                    RemoteText(
                        "alpha = 0.5 overlap (Blue -> Red)".rs,
                        color = Color.DarkGray.rc,
                        fontSize = 10.rsp,
                    )
                }

                RemoteRow(
                    modifier = RemoteModifier.fillMaxWidth().padding(horizontal = 8.rdp),
                    horizontalArrangement = RemoteArrangement.SpaceAround,
                ) {
                    for ((_, strategy) in entries) {
                        RemoteBox(
                            modifier = RemoteModifier.size(76.rdp).background(Color.White.rc),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteCanvas(
                                modifier =
                                    RemoteModifier.size(64.rdp)
                                        .graphicsLayer(
                                            alpha = 0.5f.rf,
                                            shape = RemoteRectangleShape,
                                            compositingStrategy = strategy,
                                        )
                            ) {
                                val subSize = RemoteSize(width * 0.7f.rf, height * 0.7f.rf)
                                val redOffset = RemoteOffset(width * 0.3f.rf, height * 0.3f.rf)
                                drawRect(
                                    paint = RemotePaint { color = Color.Blue.rc },
                                    size = subSize,
                                )
                                drawRect(
                                    paint = RemotePaint { color = Color.Red.rc },
                                    topLeft = redOffset,
                                    size = subSize,
                                )
                            }
                        }
                    }
                }

                RemoteBox(
                    modifier = RemoteModifier.padding(horizontal = 8.rdp),
                    contentAlignment = RemoteAlignment.CenterStart,
                ) {
                    RemoteText(
                        "alpha = 1.0 Clear cutout over Yellow".rs,
                        color = Color.DarkGray.rc,
                        fontSize = 10.rsp,
                    )
                }

                RemoteRow(
                    modifier = RemoteModifier.fillMaxWidth().padding(horizontal = 8.rdp),
                    horizontalArrangement = RemoteArrangement.SpaceAround,
                ) {
                    for ((_, strategy) in entries) {
                        RemoteBox(
                            modifier = RemoteModifier.size(76.rdp).background(Color.Yellow.rc),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteCanvas(
                                modifier =
                                    RemoteModifier.size(56.rdp)
                                        .graphicsLayer(
                                            alpha = 1f.rf,
                                            shape = RemoteRectangleShape,
                                            compositingStrategy = strategy,
                                        )
                            ) {
                                drawRect(paint = RemotePaint { color = Color.Blue.rc })
                                drawCircle(
                                    paint =
                                        RemotePaint {
                                            color = Color.Black.rc
                                            blendMode = RemoteBlendMode.Clear
                                        },
                                    radius = width * 0.3f.rf,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val WIDTH_DP = 264f
        private const val HEIGHT_DP = 248f
    }
}
