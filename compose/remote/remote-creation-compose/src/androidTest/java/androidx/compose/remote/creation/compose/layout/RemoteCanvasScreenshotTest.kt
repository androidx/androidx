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

package androidx.compose.remote.creation.compose.layout

import android.content.Context
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.compose.remote.creation.compose.vector.Builder
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.DownloadableTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.FallbackCreateTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.R
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.player.compose.test.utils.createMockContextWithFont
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteCanvasScreenshotTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun remoteCanvas_drawText_fontVariations() {
        val width = 300
        val height = 200
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            val paintNull = RemotePaint {
                typeface = RemoteTypeface.Named("RobotoFlex")
                color = Color.White.rc
                textSize = 30f.rf
            }
            val paintW100 = RemotePaint {
                typeface = RemoteTypeface.Named("RobotoFlex")
                color = Color.White.rc
                textSize = 30f.rf
                fontVariationSettings = FontVariation.Settings(FontVariation.weight(100))
            }
            val paintW900 = RemotePaint {
                typeface = RemoteTypeface.Named("RobotoFlex")
                color = Color.White.rc
                textSize = 30f.rf
                fontVariationSettings = FontVariation.Settings(FontVariation.weight(900))
            }

            val text = "Hello Font!".rs

            RemoteCanvas(modifier = RemoteModifier.size(width.rdp, height.rdp)) {
                drawText(text, 10f.rf, 40f.rf, paintNull)
                drawText(text, 10f.rf, 90f.rf, paintW100)
                drawText(text, 10f.rf, 140f.rf, paintW900)
            }
        }
    }

    @Test
    fun remoteCanvas_drawText_typefaceStyles() {
        val width = 300
        val height = 250
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            val paintNormal = RemotePaint {
                typeface = RemoteTypeface.create("sans-serif", RemoteTypeface.Style.Normal)
                color = Color.White.rc
                textSize = 30f.rf
            }
            val paintBold = RemotePaint {
                typeface = RemoteTypeface.create("sans-serif", RemoteTypeface.Style.Bold)
                color = Color.White.rc
                textSize = 30f.rf
            }
            val paintItalic = RemotePaint {
                typeface = RemoteTypeface.create("sans-serif", RemoteTypeface.Style.Italic)
                color = Color.White.rc
                textSize = 30f.rf
            }
            val paintBoldItalic = RemotePaint {
                typeface = RemoteTypeface.create("sans-serif", RemoteTypeface.Style.BoldItalic)
                color = Color.White.rc
                textSize = 30f.rf
            }

            val text = "Hello Style!".rs

            RemoteCanvas(modifier = RemoteModifier.size(width.rdp, height.rdp)) {
                drawText(text, 10f.rf, 40f.rf, paintNormal)
                drawText(text, 10f.rf, 90f.rf, paintBold)
                drawText(text, 10f.rf, 140f.rf, paintItalic)
                drawText(text, 10f.rf, 190f.rf, paintBoldItalic)
            }
        }
    }

    @Test
    fun remoteCanvas_drawPath_remotePathBuilder() {
        val width = 200
        val height = 200
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            val builder =
                RemotePath.Builder {
                    moveTo(10f.rf, 10f.rf)
                    lineTo(190f.rf, 10f.rf)
                    lineTo(100f.rf, 190f.rf)
                    close()
                }

            RemoteCanvas(modifier = RemoteModifier.size(width.rdp, height.rdp)) {
                val path = builder.build(this)

                val paint = RemotePaint {
                    color = Color.Red.rc
                    style = PaintingStyle.Fill
                }
                drawPath(path, paint)
            }
        }
    }

    @Test
    fun remoteCanvas_drawText_customRemoteTypeface() {
        val width = 400
        val height = 120
        val mockContext =
            createMockContextWithFont(
                baseContext = context,
                fontInputStream = context.resources.openRawResource(R.font.inconsolata_regular),
            )
        val resolver =
            DownloadableTypefaceResolver(
                context = mockContext,
                next = FallbackCreateTypefaceResolver(),
                isBlocking = true,
            )

        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
            typefaceResolver = resolver,
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(width.rdp, height.rdp)) {
                val paintDefault = RemotePaint {
                    typeface = RemoteTypeface.Default
                    color = Color.White.rc
                    textSize = 14.rf * remoteDensity.density
                }
                val paintInconsolata = RemotePaint {
                    typeface = RemoteTypeface.create("google:inconsolata")
                    color = Color.White.rc
                    textSize = 14f.rf * remoteDensity.density
                }
                drawText("Hello Default!".rs, 10f.rf, 40f.rf, paintDefault)
                drawText("Hello Inconsolata!".rs, 10f.rf, 90f.rf, paintInconsolata)
            }
        }
    }
}
