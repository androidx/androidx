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
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.R
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontVariation
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Embedded player ([RcPlayer]) screenshot tests verifying font variation settings and font feature
 * settings across both component ([RemoteText]) and canvas ([RemoteCanvas]) rendering using local
 * resource fonts.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerFontSettingsScreenshotTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val remoteComposeTestRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = context,
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val localResourceTypefaceResolver = RcTypefaceResolver { request ->
        when (request.fontFamily?.lowercase()) {
            "inconsolata" -> context.resources.getFont(R.font.inconsolata_regular)
            "karla" -> context.resources.getFont(R.font.karla_regular)
            else -> context.resources.getFont(R.font.robotoflex_variable)
        }
    }

    @Test
    fun componentText_variationSettings() {
        val robotoFlex = RemoteFontFamily.Named("RobotoFlex")
        val sampleText = "Roboto Flex Variant".rs

        runEmbeddedFontScreenshotTest(widthDp = 240, rowHeightDp = 40, rowCount = 3) {
            RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                RemoteBox(
                    modifier = RemoteModifier.fillMaxWidth().height(40.rdp).padding(start = 8.rdp)
                ) {
                    RemoteText(
                        text = sampleText,
                        color = Color.White.rc,
                        fontSize = 18.rsp,
                        fontFamily = robotoFlex,
                        fontVariationSettings =
                            FontVariation.Settings(
                                FontVariation.weight(100),
                                FontVariation.width(25f),
                            ),
                    )
                }
                RemoteBox(
                    modifier = RemoteModifier.fillMaxWidth().height(40.rdp).padding(start = 8.rdp)
                ) {
                    RemoteText(
                        text = sampleText,
                        color = Color.White.rc,
                        fontSize = 18.rsp,
                        fontFamily = robotoFlex,
                        fontVariationSettings =
                            FontVariation.Settings(
                                FontVariation.weight(400),
                                FontVariation.width(100f),
                            ),
                    )
                }
                RemoteBox(
                    modifier = RemoteModifier.fillMaxWidth().height(40.rdp).padding(start = 8.rdp)
                ) {
                    RemoteText(
                        text = sampleText,
                        color = Color.White.rc,
                        fontSize = 18.rsp,
                        fontFamily = robotoFlex,
                        fontVariationSettings =
                            FontVariation.Settings(
                                FontVariation.weight(900),
                                FontVariation.width(151f),
                                FontVariation.slant(-10f),
                            ),
                    )
                }
            }
        }
    }

    @Test
    fun componentText_featureSettings() {
        val robotoFlex = RemoteFontFamily.Named("RobotoFlex")
        val sampleText = "11111 vs 88888".rs

        runEmbeddedFontScreenshotTest(widthDp = 240, rowHeightDp = 40, rowCount = 2) {
            RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                RemoteBox(
                    modifier = RemoteModifier.fillMaxWidth().height(40.rdp).padding(start = 8.rdp)
                ) {
                    RemoteText(
                        text = sampleText,
                        color = Color.White.rc,
                        fontSize = 20.rsp,
                        fontFamily = robotoFlex,
                        fontFeatureSettings = "pnum",
                    )
                }
                RemoteBox(
                    modifier = RemoteModifier.fillMaxWidth().height(40.rdp).padding(start = 8.rdp)
                ) {
                    RemoteText(
                        text = sampleText,
                        color = Color.White.rc,
                        fontSize = 20.rsp,
                        fontFamily = robotoFlex,
                        fontFeatureSettings = "tnum",
                    )
                }
            }
        }
    }

    @Test
    fun canvasText_variationSettings() {
        val sampleText = "Roboto Flex Variant".rs

        runEmbeddedFontScreenshotTest(widthDp = 240, rowHeightDp = 40, rowCount = 3) {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                val textSizePx = 18f.rf * remoteDensity.density
                val x = 8f.rf * remoteDensity.density
                val rowHeightPx = 40f.rf * remoteDensity.density
                val baselineOffsetPx = 26f.rf * remoteDensity.density

                val paintThinCondensed = RemotePaint {
                    typeface = RemoteTypeface.Named("RobotoFlex")
                    color = Color.White.rc
                    textSize = textSizePx
                    fontVariationSettings =
                        FontVariation.Settings(
                            FontVariation.weight(100),
                            FontVariation.width(25f),
                        )
                }
                val paintRegular = RemotePaint {
                    typeface = RemoteTypeface.Named("RobotoFlex")
                    color = Color.White.rc
                    textSize = textSizePx
                    fontVariationSettings =
                        FontVariation.Settings(
                            FontVariation.weight(400),
                            FontVariation.width(100f),
                        )
                }
                val paintBlackExpandedSlanted = RemotePaint {
                    typeface = RemoteTypeface.Named("RobotoFlex")
                    color = Color.White.rc
                    textSize = textSizePx
                    fontVariationSettings =
                        FontVariation.Settings(
                            FontVariation.weight(900),
                            FontVariation.width(151f),
                            FontVariation.slant(-10f),
                        )
                }

                drawText(sampleText, x, baselineOffsetPx, paintThinCondensed)
                drawText(sampleText, x, rowHeightPx + baselineOffsetPx, paintRegular)
                drawText(
                    sampleText,
                    x,
                    rowHeightPx * 2f.rf + baselineOffsetPx,
                    paintBlackExpandedSlanted,
                )
            }
        }
    }

    @Test
    fun canvasText_featureSettings() {
        val sampleText = "11111 vs 88888".rs

        runEmbeddedFontScreenshotTest(widthDp = 240, rowHeightDp = 40, rowCount = 2) {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                val textSizePx = 20f.rf * remoteDensity.density
                val x = 8f.rf * remoteDensity.density
                val rowHeightPx = 40f.rf * remoteDensity.density
                val baselineOffsetPx = 28f.rf * remoteDensity.density

                val paintPnum = RemotePaint {
                    typeface = RemoteTypeface.Named("RobotoFlex")
                    color = Color.White.rc
                    textSize = textSizePx
                    fontFeatureSettings = "pnum"
                }
                val paintTnum = RemotePaint {
                    typeface = RemoteTypeface.Named("RobotoFlex")
                    color = Color.White.rc
                    textSize = textSizePx
                    fontFeatureSettings = "tnum"
                }

                drawText(sampleText, x, baselineOffsetPx, paintPnum)
                drawText(sampleText, x, rowHeightPx + baselineOffsetPx, paintTnum)
            }
        }
    }

    private fun runEmbeddedFontScreenshotTest(
        widthDp: Int,
        rowHeightDp: Int,
        rowCount: Int,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).roundToInt()
        val heightPx = (rowHeightDp * rowCount * density).roundToInt()
        val displayInfo =
            RemoteCreationDisplayInfo(
                widthPx,
                heightPx,
                context.resources.displayMetrics.densityDpi,
                context.resources.configuration.fontScale,
            )
        remoteComposeTestRule.setContent(
            remoteCreationDisplayInfo = displayInfo,
            playComposableWrapper = ComposableWrappers.blackBackground,
            typefaceResolver = localResourceTypefaceResolver,
            composable = composable,
        )
        remoteComposeTestRule.composeTestRule.waitForIdle()
        remoteComposeTestRule.composeTestRule.mainClock.advanceTimeByFrame()
        remoteComposeTestRule.composeTestRule.waitForIdle()

        val bitmap =
            remoteComposeTestRule.composeTestRule
                .onNodeWithTag(RemoteEmbeddedScreenshotTestRule.ROOT_TEST_TAG)
                .captureToImage()
                .asAndroidBitmap()
        assertRowsVisiblyDifferent(bitmap, rowCount)
        remoteComposeTestRule.verifyScreenshot()
    }

    private fun assertRowsVisiblyDifferent(bitmap: Bitmap, rowCount: Int) {
        val rowHeight = bitmap.height / rowCount
        val rows =
            List(rowCount) { index ->
                Bitmap.createBitmap(bitmap, 0, index * rowHeight, bitmap.width, rowHeight)
            }
        for (i in rows.indices) {
            val nonBlackPixels = countNonBlackPixels(rows[i])
            assertWithMessage("Row $i should contain rendered text pixels")
                .that(nonBlackPixels)
                .isGreaterThan(100)
        }
        for (i in 0 until rowCount) {
            for (j in i + 1 until rowCount) {
                val diffPixels = countDifferingPixels(rows[i], rows[j])
                assertWithMessage("Row $i and Row $j should be visibly different")
                    .that(diffPixels)
                    .isGreaterThan(100)
            }
        }
    }

    private fun countNonBlackPixels(bitmap: Bitmap): Int {
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (bitmap.getPixel(x, y) != AndroidColor.BLACK) {
                    count++
                }
            }
        }
        return count
    }

    private fun countDifferingPixels(a: Bitmap, b: Bitmap): Int {
        val width = minOf(a.width, b.width)
        val height = minOf(a.height, b.height)
        var diff = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (a.getPixel(x, y) != b.getPixel(x, y)) {
                    diff++
                }
            }
        }
        return diff
    }
}
