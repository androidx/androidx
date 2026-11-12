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

package androidx.wear.compose.remote.material3

import android.content.Context
import android.graphics.Typeface
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope.CircularAlignment
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope.CircularPlacement
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.DownloadableTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.FallbackCreateTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.R
import androidx.compose.remote.player.compose.test.utils.RemappingTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.player.compose.test.utils.createMockContextWithFonts
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontVariation.Setting
import androidx.compose.ui.text.font.FontVariation.Settings
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestProfiles
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteTimeTextTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Ignore("Waiting for b/582672416")
    @Test
    fun timeText_withBackground() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteBox(RemoteModifier.fillMaxSize().background(RemoteColor.DarkGray)) {
                RemoteTimeText(
                    modifier = RemoteModifier.fillMaxSize(),
                    time = "10:09".rs,
                )
                RemoteTimeText(
                    modifier = RemoteModifier.fillMaxSize().offset(0.rdp, 35.rdp),
                    time = "10:09".rs,
                    backgroundColor = RemoteColor.Transparent,
                )
            }
        }
    }

    @Ignore("Waiting for b/545086796")
    @Test
    fun timeText_withRobotoFlex_tnumAndPnum() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val font = RemoteFontFamily.Named("google:Roboto Flex")
            RemoteBox(RemoteModifier.fillMaxSize()) {
                RemoteTimeText(
                    modifier = RemoteModifier.fillMaxSize().offset(0.rdp, 20.rdp),
                    time = "10:09".rs,
                    fontFamily = font,
                    fontFeatureSettings = "tnum",
                )
                RemoteTimeText(
                    modifier = RemoteModifier.fillMaxSize().offset(0.rdp, 50.rdp),
                    time = "10:09".rs,
                    fontFamily = font,
                    fontFeatureSettings = "pnum",
                )
            }
        }
    }

    @Test
    fun drawTextOnCircle_allPlacementsAndAlignments() {
        remoteComposeTestRule.runScreenshotTestCustomProfile(width = 500, height = 500) {
            RemoteBox(RemoteModifier.fillMaxSize()) {
                RemoteCanvas(RemoteModifier.fillMaxSize()) {
                    val cx = 250.rf
                    val cy = 250.rf
                    val center = RemoteOffset(cx, cy)
                    val outerRadius = 205.rf
                    val middleRadius = 150.rf
                    val innerRadius = 95.rf

                    val guidePaint = RemotePaint {
                        color = RemoteColor.DarkGray
                        style = RemotePaintingStyle.Stroke
                        strokeWidth = 2.rf
                    }
                    drawCircle(paint = guidePaint, radius = outerRadius, center = center)
                    drawCircle(paint = guidePaint, radius = middleRadius, center = center)
                    drawCircle(paint = guidePaint, radius = innerRadius, center = center)
                    drawLine(
                        paint = guidePaint,
                        start = RemoteOffset(cx, 10.rf),
                        end = RemoteOffset(cx, 490.rf),
                    )

                    val textPaint = RemotePaint {
                        textSize = 9.rsp.toPx()
                        color = RemoteColor.White
                    }

                    // OUTSIDE placement along top half (270°), upright left-to-right
                    drawTextOnCircle(
                        text = "Out Start".rs,
                        centerX = cx,
                        centerY = cy,
                        radius = outerRadius,
                        startAngle = 270.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.Start,
                        placement = CircularPlacement.Outside,
                        paint = textPaint,
                    )
                    drawTextOnCircle(
                        text = "Out Center".rs,
                        centerX = cx,
                        centerY = cy,
                        radius = middleRadius,
                        startAngle = 270.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.Center,
                        placement = CircularPlacement.Outside,
                        paint = textPaint,
                    )
                    drawTextOnCircle(
                        text = "Out End".rs,
                        centerX = cx,
                        centerY = cy,
                        radius = innerRadius,
                        startAngle = 270.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.End,
                        placement = CircularPlacement.Outside,
                        paint = textPaint,
                    )

                    // INSIDE placement along bottom half (90°), upright left-to-right
                    drawTextOnCircle(
                        text = "In Start".rs,
                        centerX = cx,
                        centerY = cy,
                        radius = outerRadius,
                        startAngle = 90.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.Start,
                        placement = CircularPlacement.Inside,
                        paint = textPaint,
                    )
                    drawTextOnCircle(
                        text = "In Center".rs,
                        centerX = cx,
                        centerY = cy,
                        radius = middleRadius,
                        startAngle = 90.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.Center,
                        placement = CircularPlacement.Inside,
                        paint = textPaint,
                    )
                    drawTextOnCircle(
                        text = "In End".rs,
                        centerX = cx,
                        centerY = cy,
                        radius = innerRadius,
                        startAngle = 90.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.End,
                        placement = CircularPlacement.Inside,
                        paint = textPaint,
                    )
                }
            }
        }
    }

    private fun RemoteScreenshotTestRule.runScreenshotTestCustomProfile(
        width: Int = 192,
        height: Int = 192,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        val mockContext =
            createMockContextWithFonts(
                baseContext = context,
                fontStreams =
                    mapOf(
                        "roboto" to context.resources.openRawResource(R.font.robotoflex_variable)
                    ),
                defaultFontKey = "roboto",
            )
        val current = FallbackCreateTypefaceResolver()
        val remappingResolver =
            RemappingTypefaceResolver(current).apply {
                remapName("RobotoFlex", "google:Roboto Flex")
                remapType(0, "google:Roboto Flex")
            }
        val resolver =
            DownloadableTypefaceResolver(
                context = mockContext,
                next = remappingResolver,
                isBlocking = true,
                fontVariationSettingsMap =
                    mapOf(
                        "Roboto Flex" to
                            Settings(
                                FontVariation.weight(1000),
                                FontVariation.width(151f),
                                FontVariation.grade(150),
                                Setting("opsz", 144f),
                                FontVariation.slant(-10f),
                            )
                    ),
            )
        // Trigger a pre-fetch of fonts so they are loaded and cached in
        // DownloadableTypefaceResolver before screenshot capture in CI.
        resolver.prefetchFonts(listOf("google:Roboto Flex"))

        var resolveCalled = false
        val trackingResolver =
            object : TypefaceResolver {
                override fun resolve(
                    fontType: Int,
                    weight: Int,
                    italic: Boolean,
                    fallbackTypeface: Typeface?,
                    fallbackWeight: Int,
                    fallbackItalic: Boolean,
                ): FontInstance {
                    resolveCalled = true
                    return resolver.resolve(
                        fontType,
                        weight,
                        italic,
                        fallbackTypeface,
                        fallbackWeight,
                        fallbackItalic,
                    )
                }

                override fun resolve(
                    fontName: String,
                    weight: Int,
                    italic: Boolean,
                    fallbackTypeface: Typeface?,
                    fallbackWeight: Int,
                    fallbackItalic: Boolean,
                ): FontInstance {
                    resolveCalled = true
                    return resolver.resolve(
                        fontName,
                        weight,
                        italic,
                        fallbackTypeface,
                        fallbackWeight,
                        fallbackItalic,
                    )
                }
            }

        this.runScreenshotTest(
            profile = TestProfiles.androidXWithCoreText,
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            creationComposableWrapper = ComposableWrappers.layoutDirection(layoutDirection),
            playComposableWrapper = ComposableWrappers.blackBackground,
            typefaceResolver = trackingResolver,
            composable = composable,
        )
        assertTrue("Expected TypefaceResolver.resolve to be called during test", resolveCalled)
    }
}
