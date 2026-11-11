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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [35], qualifiers = "w1000dp-h500dp")
@RunWith(RobolectricTestRunner::class)
class RemoteCircularProgressIndicatorTest {

    @get:Rule val painterRule = DocumentPainterRule(canvasSize = CanvasSizePx)

    @Test
    fun fullCircle_progressOne_closesGapWithoutNotchAndDrawsNoTrack() {
        for ((startAngle, endAngle, gapDp) in
            listOf(Triple(270f, 270f, 4), Triple(297f, 297f, 0), Triple(0f, 360f, 4))) {
            val painter = painterRule.paintDocument {
                RemoteCircularProgressIndicator(
                    progress = 1.0f.rf,
                    modifier = RemoteModifier.fillMaxSize(),
                    startAngle = startAngle.rf,
                    endAngle = endAngle.rf,
                    strokeWidth = StrokeWidthDp.rdp,
                    gapSize = gapDp.rdp,
                    colors = testColors(),
                )
            }

            assertThat(painter.countPixels(::isTrack)).isEqualTo(0)
            for (degrees in 0 until 360) {
                assertThat(isIndicator(painter.ringColorAt(degrees.toFloat()))).isTrue()
            }
        }
    }

    @Test
    fun openArc_progressOne_preservesOpenGapAndDrawsNoTrack() {
        val painter = painterRule.paintDocument {
            RemoteCircularProgressIndicator(
                progress = 1.0f.rf,
                modifier = RemoteModifier.fillMaxSize(),
                startAngle = 135f.rf,
                endAngle = 45f.rf,
                strokeWidth = StrokeWidthDp.rdp,
                gapSize = 0.rdp,
                colors = testColors(),
            )
        }

        assertThat(painter.countPixels(::isTrack)).isEqualTo(0)
        assertThat(isIndicator(painter.ringColorAt(270f))).isTrue()
        val openRegionColor = painter.ringColorAt(90f)
        assertThat(isIndicator(openRegionColor)).isFalse()
        assertThat(isTrack(openRegionColor)).isFalse()
    }

    @Test
    fun progressZero_drawsNoIndicatorDotAndDrawsFullTrack() {
        for ((startAngle, gapDp) in listOf(270f to 4, 297f to 0)) {
            val painter = painterRule.paintDocument {
                RemoteCircularProgressIndicator(
                    progress = 0.0f.rf,
                    modifier = RemoteModifier.fillMaxSize(),
                    startAngle = startAngle.rf,
                    endAngle = startAngle.rf,
                    strokeWidth = StrokeWidthDp.rdp,
                    gapSize = gapDp.rdp,
                    colors = testColors(),
                )
            }

            assertThat(painter.countPixels(::isIndicator)).isEqualTo(0)
            for (degrees in 0 until 360) {
                assertThat(isTrack(painter.ringColorAt(degrees.toFloat()))).isTrue()
            }
        }
    }

    @Test
    fun subGapProgress_drawsDotScaledByProgressAtCenterOfSweep() {
        val startAngle = 297f
        val gapSweep = expectedGapSweepDegrees(diameter = CanvasSizePx, strokePx = StrokeWidthPx)
        val arcRadius = (CanvasSizePx - StrokeWidthPx) / 2f
        val center = CanvasSizePx / 2f

        val pixelCounts = mutableListOf<Int>()
        for (fraction in listOf(0.25f, 0.5f, 1.0f)) {
            val sweepAngle = gapSweep * fraction
            val painter = painterRule.paintDocument {
                RemoteCircularProgressIndicator(
                    progress = (sweepAngle / 360f).rf,
                    modifier = RemoteModifier.fillMaxSize(),
                    startAngle = startAngle.rf,
                    endAngle = startAngle.rf,
                    strokeWidth = StrokeWidthDp.rdp,
                    gapSize = 0.rdp,
                    colors = testColors(),
                )
            }

            val dot = painter.boundsOf(::isIndicator)
            val expectedAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
            assertThat(dot.centerX)
                .isWithin(PixelTolerance)
                .of(center + arcRadius * cos(expectedAngleRad).toFloat())
            assertThat(dot.centerY)
                .isWithin(PixelTolerance)
                .of(center + arcRadius * sin(expectedAngleRad).toFloat())
            assertThat(dot.size).isWithin(PixelTolerance).of(StrokeWidthPx * fraction)
            pixelCounts.add(dot.pixelCount)
        }

        assertThat(pixelCounts).isInStrictOrder()
    }

    @Test
    fun subGapCountdownAnimation_shrinksSmoothlyToZeroPixels() {
        val gapSweep = expectedGapSweepDegrees(diameter = CanvasSizePx, strokePx = StrokeWidthPx)
        val fullGapProgress = gapSweep / 360f
        val halfGapProgress = fullGapProgress * 0.5f
        val painter = painterRule.paintDocument {
            RemoteCircularProgressIndicator(
                progress = RemoteFloat.createNamedRemoteFloat(ProgressName, fullGapProgress),
                modifier = RemoteModifier.fillMaxSize(),
                startAngle = 297f.rf,
                endAngle = 297f.rf,
                strokeWidth = StrokeWidthDp.rdp,
                gapSize = 0.rdp,
                colors = testColors(),
            )
        }

        val fullDotPixels = painter.countPixels(::isIndicator)
        painter.setFloat(ProgressName, halfGapProgress)
        painter.advanceTimeBy(SettledMillis)
        val halfDotPixels = painter.countPixels(::isIndicator)
        painter.setFloat(ProgressName, 0f)
        painter.advanceTimeBy(SettledMillis)
        val zeroPixels = painter.countPixels(::isIndicator)

        assertThat(fullDotPixels).isGreaterThan(0)
        assertThat(halfDotPixels).isGreaterThan(0)
        assertThat(halfDotPixels).isLessThan(fullDotPixels)
        assertThat(zeroPixels).isEqualTo(0)
    }

    @Test
    fun subGapTrack_nearFullProgress_drawsNoFullSizeTrackCap() {
        val gapSweep = expectedGapSweepDegrees(diameter = CanvasSizePx, strokePx = StrokeWidthPx)
        val fullCapPixels = PI * (StrokeWidthPx / 2f) * (StrokeWidthPx / 2f)

        val trackPixels =
            listOf(2f, 0.5f).map { remainingTrackFraction ->
                val remainingTrackSweep = gapSweep * remainingTrackFraction
                val painter = painterRule.paintDocument {
                    RemoteCircularProgressIndicator(
                        progress = ((360f - remainingTrackSweep) / 360f).rf,
                        modifier = RemoteModifier.fillMaxSize(),
                        startAngle = 270f.rf,
                        endAngle = 270f.rf,
                        strokeWidth = StrokeWidthDp.rdp,
                        gapSize = 0.rdp,
                        colors = testColors(),
                    )
                }
                painter.countPixels(::isTrack)
            }

        // A track longer than the gap is an arc, while one within it is at most a smaller dot.
        assertThat(trackPixels[0].toDouble()).isGreaterThan(fullCapPixels)
        assertThat(trackPixels[1].toDouble()).isLessThan(fullCapPixels)
    }

    @Test
    fun nonPositiveStrokeWidth_drawsNothing() {
        for (strokeDp in listOf(0, -8)) {
            for (progress in listOf(0f, 0.005f, 0.5f, 1f)) {
                val painter = painterRule.paintDocument {
                    RemoteCircularProgressIndicator(
                        progress = progress.rf,
                        modifier = RemoteModifier.fillMaxSize(),
                        strokeWidth = strokeDp.rdp,
                        gapSize = 4.rdp,
                        colors = testColors(),
                    )
                }

                assertThat(painter.countPixels { isIndicator(it) || isTrack(it) }).isEqualTo(0)
            }
        }
    }

    private fun DocumentPainterRule.DocumentPainter.ringColorAt(degrees: Float): Color {
        val arcRadius = (CanvasSizePx - StrokeWidthPx) / 2f
        val center = CanvasSizePx / 2f
        val rad = Math.toRadians(degrees.toDouble())
        val x = (center + arcRadius * cos(rad)).roundToInt()
        val y = (center + arcRadius * sin(rad)).roundToInt()
        return colorAt(x, y)
    }

    /** Returns the bounds of the pixels whose color matches [predicate]. */
    private fun DocumentPainterRule.DocumentPainter.boundsOf(
        predicate: (Color) -> Boolean
    ): PixelBounds {
        val pixels = pixelsWhere(predicate)
        assertThat(pixels).isNotEmpty()
        val xs = pixels.map { it % width }
        val ys = pixels.map { it / width }
        return PixelBounds(
            pixelCount = pixels.size,
            // Pixel centers are at +0.5.
            centerX = (xs.min() + xs.max() + 1) / 2f,
            centerY = (ys.min() + ys.max() + 1) / 2f,
            size = maxOf(xs.max() - xs.min(), ys.max() - ys.min()) + 1f,
        )
    }

    private fun expectedGapSweepDegrees(diameter: Float, strokePx: Float): Float {
        val chordRatio = (strokePx / (diameter - strokePx)).coerceIn(0f, 1f)
        return (asin(chordRatio) * (360.0 / PI)).toFloat()
    }

    @Composable
    @RemoteComposable
    private fun testColors(): RemoteProgressIndicatorColors =
        RemoteProgressIndicatorDefaults.colors(
            indicatorColor = Color.Red.rc,
            trackColor = Color.Blue.rc,
        )

    private fun isIndicator(color: Color): Boolean = color.red > 0.5f && color.blue < 0.5f

    private fun isTrack(color: Color): Boolean = color.blue > 0.5f && color.red < 0.5f

    private class PixelBounds(
        val pixelCount: Int,
        val centerX: Float,
        val centerY: Float,
        val size: Float,
    )

    private companion object {
        const val CanvasSizePx = 200f
        const val StrokeWidthDp = 8
        const val StrokeWidthPx = 8f
        const val PixelTolerance = 1.5f
        const val ProgressName = "progress"
        const val SettledMillis = 3000L
    }
}
