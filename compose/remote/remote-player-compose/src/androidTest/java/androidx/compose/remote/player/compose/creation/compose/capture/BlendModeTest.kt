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

package androidx.compose.remote.player.compose.creation.compose.capture

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * A test for BlendMode in RemoteCanvas, see
 * [BlendMode](https://developer.android.com/reference/android/graphics/BlendMode)
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class BlendModeTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun all_blend_modes() {
        var document: CoreDocument? = null
        remoteComposeTestRule.setContent(
            remoteCreationDisplayInfo = createCreationDisplayInfo(context, Size(2000f, 2500f)),
            onCoreDocumentCreated = { document = it },
        ) {
            AllBlendModes()
        }

        remoteComposeTestRule.verifyScreenshot()
    }

    @RemoteComposable
    @Composable
    private fun AllBlendModes() {
        val chunkedBlendModes = testCases.chunked(4)
        RemoteColumn {
            for (rowItems in chunkedBlendModes) {
                RemoteRow {
                    for (testCase in rowItems) {
                        RemoteBlendModeVisual(testCase)
                    }
                }
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun RemoteBlendModeVisual(testCase: BlendModeTestCase) {
        RemoteBox(
            RemoteModifier.size(100.rdp).border(1.rdp, Color.Black.rc).padding(8.rdp),
            contentAlignment = RemoteAlignment.TopStart,
        ) {
            RemoteCanvas(RemoteModifier.size(100.rdp)) {
                val w = width
                val h = height

                val paint = RemotePaint {
                    remoteStyle = RemotePaintingStyle.Fill
                    color = Color.Magenta.rc
                }

                // Draw dst
                drawCircle(
                    paint = paint,
                    center = RemoteOffset(w * 2f / 3f, h * 1f / 3f),
                    radius = w / 3f,
                )

                // Draw src
                paint.color = Color.Blue.rc
                paint.remoteBlendMode = testCase.blendMode
                drawRect(
                    paint = paint,
                    topLeft = RemoteOffset(0f.rf, h * 1f / 3f),
                    size = RemoteSize(w * 2f / 3f, h * 2f / 3f),
                )
            }
            RemoteText(testCase.label.rs, fontSize = 12.rsp)
        }
    }

    private data class BlendModeTestCase(val blendMode: RemoteBlendMode, val label: String)

    companion object {
        private val testCases =
            listOf(
                BlendModeTestCase(blendMode = RemoteBlendMode.Clear, label = "CLEAR"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Src, label = "SRC"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Dst, label = "DST"),
                BlendModeTestCase(blendMode = RemoteBlendMode.SrcOver, label = "SRC_OVER"),
                BlendModeTestCase(blendMode = RemoteBlendMode.DstOver, label = "DST_OVER"),
                BlendModeTestCase(blendMode = RemoteBlendMode.SrcIn, label = "SRC_IN"),
                BlendModeTestCase(blendMode = RemoteBlendMode.DstIn, label = "DST_IN"),
                BlendModeTestCase(blendMode = RemoteBlendMode.SrcOut, label = "SRC_OUT"),
                BlendModeTestCase(blendMode = RemoteBlendMode.DstOut, label = "DST_OUT"),
                BlendModeTestCase(blendMode = RemoteBlendMode.SrcAtop, label = "SRC_ATOP"),
                BlendModeTestCase(blendMode = RemoteBlendMode.DstAtop, label = "DST_ATOP"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Xor, label = "XOR"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Plus, label = "PLUS"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Modulate, label = "MODULATE"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Screen, label = "SCREEN"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Overlay, label = "OVERLAY"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Darken, label = "DARKEN"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Lighten, label = "LIGHTEN"),
                BlendModeTestCase(blendMode = RemoteBlendMode.ColorDodge, label = "COLOR_DODGE"),
                BlendModeTestCase(blendMode = RemoteBlendMode.ColorBurn, label = "COLOR_BURN"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Hardlight, label = "HARD_LIGHT"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Softlight, label = "SOFT_LIGHT"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Difference, label = "DIFFERENCE"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Exclusion, label = "EXCLUSION"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Multiply, label = "MULTIPLY"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Hue, label = "HUE"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Saturation, label = "SATURATION"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Color, label = "COLOR"),
                BlendModeTestCase(blendMode = RemoteBlendMode.Luminosity, label = "LUMINOSITY"),
            )
    }
}
