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
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.DownloadableTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.FallbackCreateTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.RemappingTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
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

    @Ignore("Waiting for b/4205105")
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

    @Ignore("Waiting for b/4205105")
    @Test
    fun drawTextOnCircle_allPlacementsAndAlignments() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteBox(RemoteModifier.fillMaxSize()) {
                RemoteCanvas(RemoteModifier.fillMaxSize()) {
                    val paint = RemotePaint {
                        textSize = 12.rsp.toPx()
                        color = RemoteColor(0xFFFFFFFF.toInt())
                    }
                    val text = "10:09".rs
                    val cx = 96.rf
                    val cy = 96.rf
                    val r = 50.rf

                    // OUTSIDE placement (START, CENTER, END)
                    drawTextOnCircle(
                        text = text,
                        centerX = cx,
                        centerY = cy,
                        radius = r,
                        startAngle = 270.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.Start,
                        placement = CircularPlacement.Outside,
                        paint = paint,
                    )
                    drawTextOnCircle(
                        text = text,
                        centerX = cx,
                        centerY = cy,
                        radius = r,
                        startAngle = 0.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.Center,
                        placement = CircularPlacement.Outside,
                        paint = paint,
                    )
                    drawTextOnCircle(
                        text = text,
                        centerX = cx,
                        centerY = cy,
                        radius = r,
                        startAngle = 90.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.End,
                        placement = CircularPlacement.Outside,
                        paint = paint,
                    )

                    // INSIDE placement (START, CENTER, END)
                    drawTextOnCircle(
                        text = text,
                        centerX = cx,
                        centerY = cy,
                        radius = r,
                        startAngle = 180.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.Start,
                        placement = CircularPlacement.Inside,
                        paint = paint,
                    )
                    drawTextOnCircle(
                        text = text,
                        centerX = cx,
                        centerY = cy,
                        radius = r + 20.rf,
                        startAngle = 270.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.Center,
                        placement = CircularPlacement.Inside,
                        paint = paint,
                    )
                    drawTextOnCircle(
                        text = text,
                        centerX = cx,
                        centerY = cy,
                        radius = r + 20.rf,
                        startAngle = 0.rf,
                        warpRadiusOffset = 0.rf,
                        alignment = CircularAlignment.End,
                        placement = CircularPlacement.Inside,
                        paint = paint,
                    )
                }
            }
        }
    }

    private fun RemoteScreenshotTestRule.runScreenshotTestCustomProfile(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        val current = FallbackCreateTypefaceResolver()
        val remappingResolver =
            RemappingTypefaceResolver(current).apply {
                remapName("RobotoFlex", "google:Roboto Flex")
                remapType(0, "google:Roboto Flex")
            }
        val resolver =
            DownloadableTypefaceResolver(
                context = context,
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
                    192,
                    192,
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
