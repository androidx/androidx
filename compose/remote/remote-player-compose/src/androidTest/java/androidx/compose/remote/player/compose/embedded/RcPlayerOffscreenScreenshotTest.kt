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

import android.content.res.Resources
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for nested `DrawToBitmap` offscreen canvas targets and `DrawContent` group layer
 * rendering in [RcPlayer].
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerOffscreenScreenshotTest {

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
    fun drawContentWithGroupAlpha() {
        screenshotRule.runScreenshotTest {
            RemoteColumn(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(WIDTH_DP.rdp, 24.rdp).background(Color.DarkGray.rc),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText(
                        "Group alpha 0.5 (Red occludes Blue)".rs,
                        color = Color.White.rc,
                        fontSize = 11.rsp,
                    )
                }
                RemoteBox(
                    modifier = RemoteModifier.fillMaxSize().background(Color.LightGray.rc),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    // Backdrop stripe beneath the group so alpha < 1 is clearly visible through
                    // both the blue and red children, while the red child still occludes the blue
                    // child within the group layer.
                    RemoteBox(
                        modifier = RemoteModifier.size(168.rdp, 28.rdp).background(Color.Yellow.rc)
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(120.rdp, 88.rdp).drawWithContent {
                                document.rcPaint.setAlpha(0.5f).commit()
                                document.drawComponentContent()
                                document.rcPaint.setAlpha(1f).commit()
                            }
                    ) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(76.rdp, 56.rdp)
                                    .background(Color.Blue.rc)
                                    .padding(4.rdp),
                            contentAlignment = RemoteAlignment.TopStart,
                        ) {
                            RemoteText("Blue".rs, color = Color.White.rc, fontSize = 11.rsp)
                        }
                        RemoteBox(
                            modifier =
                                RemoteModifier.padding(start = 44.rdp, top = 32.rdp)
                                    .size(76.rdp, 56.rdp)
                                    .background(Color.Red.rc)
                                    .padding(4.rdp),
                            contentAlignment = RemoteAlignment.BottomEnd,
                        ) {
                            RemoteText("Red".rs, color = Color.White.rc, fontSize = 11.rsp)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun nestedDrawToBitmap() {
        screenshotRule.runScreenshotTest {
            RemoteColumn(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(WIDTH_DP.rdp, 24.rdp).background(Color.DarkGray.rc),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText(
                        "Nested bitmaps: Inner -> Outer x2".rs,
                        color = Color.White.rc,
                        fontSize = 11.rsp,
                    )
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxSize()
                            .background(Color.LightGray.rc)
                            .drawWithContent {
                                val outerId = document.createBitmap(160, 100)
                                val innerId = document.createBitmap(56, 56)

                                // Redirect to outer offscreen bitmap and fill with blue.
                                document.drawOnBitmap(outerId, 0, Color(0xFF1E88E5).toArgb())

                                // Nested redirect to inner offscreen bitmap and fill with red +
                                // yellow
                                // square.
                                document.drawOnBitmap(innerId, 0, Color(0xFFE53935).toArgb())
                                document.rcPaint.setColor(Color.Yellow.toArgb()).commit()
                                document.drawRect(14f, 14f, 42f, 42f)

                                // Pop inner offscreen target back to outer bitmap and stamp innerId
                                // twice (full size + mini) alongside a green bar.
                                document.drawOnBitmap(0)
                                document.rcPaint.setColor(Color.White.toArgb()).commit()
                                document.drawBitmap(innerId, 12f, 22f, 68f, 78f, "inner1")
                                document.drawBitmap(innerId, 80f, 34f, 112f, 66f, "inner2")
                                document.rcPaint.setColor(Color.Green.toArgb()).commit()
                                document.drawRect(124f, 22f, 148f, 78f)

                                // Pop outer offscreen target back to root canvas and stamp outerId
                                // twice (large on the left + miniature twin on the right).
                                document.drawOnBitmap(0)
                                document.rcPaint.setColor(Color.White.toArgb()).commit()
                                document.drawBitmap(outerId, 16f, 20f, 256f, 180f, "outerLarge")
                                document.drawBitmap(outerId, 276f, 52f, 412f, 148f, "outerMini")
                            }
                )
            }
        }
    }

    companion object {
        private const val WIDTH_DP = 200f
        private const val HEIGHT_DP = 144f
    }
}
