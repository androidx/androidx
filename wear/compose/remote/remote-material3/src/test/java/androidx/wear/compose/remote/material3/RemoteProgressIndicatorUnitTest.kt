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
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.player.compose.test.utils.TestClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [35], qualifiers = "w1000dp-h500dp")
@RunWith(RobolectricTestRunner::class)
class RemoteProgressIndicatorUnitTest {

    @get:Rule val painterRule = DocumentPainterRule()

    @Test
    fun circular_endAngleLessThanStartAngle_drawsClockwisePastZero() {
        // From 270 degrees (12 o'clock) clockwise past 0 degrees (3 o'clock) to 90 degrees
        // (6 o'clock) covers the right half of the circle, not the left half.
        val painter = painterRule.paintDocument {
            RemoteCircularProgressIndicator(
                progress = 1f.rf,
                startAngle = 270f.rf,
                endAngle = 90f.rf,
                modifier = RemoteModifier.size(200.rdp),
                colors = testColors(),
            )
        }

        val rightPixels = painter.countRegionPixels(110 until 200, ::isIndicator)
        val farLeftPixels = painter.countRegionPixels(0 until 80, ::isIndicator)
        assertThat(rightPixels).isGreaterThan(0)
        assertThat(farLeftPixels).isEqualTo(0)
    }

    @Test
    fun segmented_endAngleLessThanStartAngle_drawsClockwisePastZero() {
        val painter = painterRule.paintDocument {
            RemoteSegmentedCircularProgressIndicator(
                segmentCount = 4.ri,
                progress = 1f.rf,
                startAngle = 270f.rf,
                endAngle = 90f.rf,
                modifier = RemoteModifier.size(200.rdp),
                colors = testColors(),
            )
        }

        val rightPixels = painter.countRegionPixels(110 until 200, ::isIndicator)
        val farLeftPixels = painter.countRegionPixels(0 until 80, ::isIndicator)
        assertThat(rightPixels).isGreaterThan(0)
        assertThat(farLeftPixels).isEqualTo(0)
    }

    @Test
    fun curved_negativeSweepAngle_drawsNothing() {
        val constantNegative = painterRule.paintDocument {
            RemoteCurvedProgressIndicator(
                progress = 0.5f.rf,
                sweepAngle = (-90f).rf,
                modifier = RemoteModifier.size(200.rdp),
                colors = testColors(),
            )
        }
        assertThat(constantNegative.countDrawnPixels()).isEqualTo(0)

        val dynamicPainter = painterRule.paintDocument {
            RemoteCurvedProgressIndicator(
                progress = 0.5f.rf,
                sweepAngle = RemoteFloat.createNamedRemoteFloat(SweepName, 90f),
                modifier = RemoteModifier.size(200.rdp),
                colors = testColors(),
            )
        }
        assertThat(dynamicPainter.countDrawnPixels()).isGreaterThan(0)

        dynamicPainter.setFloat(SweepName, -90f)
        assertThat(dynamicPainter.countDrawnPixels()).isEqualTo(0)
    }

    @Test
    fun introOutroCurved_negativeSweepAngle_drawsNothing() {
        val constantNegative = painterRule.paintDocument {
            RemoteIntroOutroCurvedProgressIndicator(
                progress = 0.5f.rf,
                sweepAngle = (-90f).rf,
                modifier = RemoteModifier.size(200.rdp),
                colors = testColors(),
            )
        }
        assertThat(constantNegative.countDrawnPixels()).isEqualTo(0)

        val dynamicPainter = painterRule.paintDocument {
            RemoteIntroOutroCurvedProgressIndicator(
                progress = 0.5f.rf,
                sweepAngle = RemoteFloat.createNamedRemoteFloat(SweepName, 90f),
                modifier = RemoteModifier.size(200.rdp),
                colors = testColors(),
            )
        }
        assertThat(dynamicPainter.countDrawnPixels()).isGreaterThan(0)

        dynamicPainter.setFloat(SweepName, -90f)
        assertThat(dynamicPainter.countDrawnPixels()).isEqualTo(0)
    }

    @Test
    fun linear_nonPositiveStrokeWidth_drawsNothing() {
        // Default height modifier (.height(coercedStrokeWidth)) must not crash on negative width.
        val defaultHeightNegative = painterRule.paintDocument {
            RemoteLinearProgressIndicator(
                progress = 0.5f.rf,
                strokeWidth = (-8).rdp,
                colors = testColors(),
            )
        }
        assertThat(defaultHeightNegative.countDrawnPixels()).isEqualTo(0)

        // Even with an explicit canvas height, zero or negative strokeWidth draws nothing (neither
        // a hairline stroke nor the end-of-range dot).
        val explicitHeightZero = painterRule.paintDocument {
            RemoteLinearProgressIndicator(
                progress = 0.5f.rf,
                strokeWidth = 0.rdp,
                modifier = RemoteModifier.size(150.rdp, 20.rdp),
                colors = testColors(),
            )
        }
        assertThat(explicitHeightZero.countDrawnPixels()).isEqualTo(0)

        val dynamicPainter = painterRule.paintDocument {
            RemoteLinearProgressIndicator(
                progress = 0.5f.rf,
                strokeWidth = namedStrokeWidth(12f),
                modifier = RemoteModifier.size(150.rdp, 20.rdp),
                colors = testColors(),
            )
        }
        assertThat(dynamicPainter.countDrawnPixels()).isGreaterThan(0)

        dynamicPainter.setFloat(StrokeWidthName, -8f)
        assertThat(dynamicPainter.countDrawnPixels()).isEqualTo(0)
    }

    @Test
    fun circularAndCurvedAndSegmented_nonPositiveStrokeWidth_drawsNothing() {
        val indicators: List<@Composable @RemoteComposable (RemoteDp) -> Unit> =
            listOf(
                { strokeWidth ->
                    RemoteCircularProgressIndicator(
                        progress = 0.5f.rf,
                        strokeWidth = strokeWidth,
                        modifier = RemoteModifier.size(150.rdp),
                        colors = testColors(),
                    )
                },
                { strokeWidth ->
                    RemoteCircularProgressIndicator(
                        strokeWidth = strokeWidth,
                        modifier = RemoteModifier.size(150.rdp),
                        colors = testColors(),
                    )
                },
                { strokeWidth ->
                    RemoteCurvedProgressIndicator(
                        progress = 0.5f.rf,
                        strokeWidth = strokeWidth,
                        modifier = RemoteModifier.size(150.rdp),
                        colors = testColors(),
                    )
                },
                { strokeWidth ->
                    RemoteIntroOutroCurvedProgressIndicator(
                        progress = 0.5f.rf,
                        strokeWidth = strokeWidth,
                        modifier = RemoteModifier.size(150.rdp),
                        colors = testColors(),
                    )
                },
                { strokeWidth ->
                    RemoteSegmentedCircularProgressIndicator(
                        segmentCount = 4.ri,
                        progress = 0.5f.rf,
                        strokeWidth = strokeWidth,
                        modifier = RemoteModifier.size(150.rdp),
                        colors = testColors(),
                    )
                },
                { strokeWidth ->
                    RemoteSegmentedCircularProgressIndicator(
                        segmentCount = 4.ri,
                        segmentValue = { (it < 2).rb },
                        strokeWidth = strokeWidth,
                        modifier = RemoteModifier.size(150.rdp),
                        colors = testColors(),
                    )
                },
            )

        for (indicator in indicators) {
            val constantNegative =
                painterRule.paintDocument(clock = TestClock(epochMilli = 250L)) {
                    indicator((-8).rdp)
                }
            assertThat(constantNegative.countDrawnPixels()).isEqualTo(0)

            val dynamicPainter =
                painterRule.paintDocument(clock = TestClock(epochMilli = 250L)) {
                    indicator(namedStrokeWidth(12f))
                }
            assertThat(dynamicPainter.countDrawnPixels()).isGreaterThan(0)

            dynamicPainter.setFloat(StrokeWidthName, -8f)
            assertThat(dynamicPainter.countDrawnPixels()).isEqualTo(0)
        }
    }

    @Test
    fun calculateRecommendedGapSize_negativeStrokeWidth_coercesToZero() {
        val gap = RemoteProgressIndicatorDefaults.calculateRecommendedGapSize((-12).rdp)
        assertThat(gap.constantValueOrNull).isEqualTo(0f)
    }

    private fun namedStrokeWidth(initialDp: Float): RemoteDp =
        RemoteFloat.createNamedRemoteFloat(StrokeWidthName, initialDp).asRemoteDp()

    private fun DocumentPainterRule.DocumentPainter.countDrawnPixels(): Int = countPixels { color ->
        isIndicator(color) || isTrack(color)
    }

    private fun DocumentPainterRule.DocumentPainter.countRegionPixels(
        xRange: IntRange,
        predicate: (Color) -> Boolean,
    ): Int {
        var count = 0
        for (y in 0 until height) {
            for (x in xRange) {
                if (x in 0 until width && predicate(colorAt(x, y))) {
                    count++
                }
            }
        }
        return count
    }

    @Composable
    @RemoteComposable
    private fun testColors(): RemoteProgressIndicatorColors =
        RemoteProgressIndicatorDefaults.colors(
            indicatorColor = RemoteColor.Red,
            trackColor = RemoteColor.Blue,
        )

    private fun isIndicator(color: Color): Boolean = color.red > 0.5f && color.blue < 0.5f

    private fun isTrack(color: Color): Boolean = color.blue > 0.5f && color.red < 0.5f

    private companion object {
        const val SweepName = "sweep"
        const val StrokeWidthName = "strokeWidth"
    }
}
