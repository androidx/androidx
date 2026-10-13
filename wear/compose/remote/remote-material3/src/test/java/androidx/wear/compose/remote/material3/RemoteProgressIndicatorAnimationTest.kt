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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.DrawCircle
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.player.compose.test.utils.TestClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorAnimatedSample
import androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorSample
import androidx.wear.compose.remote.material3.samples.RemoteLinearProgressIndicatorAnimatedSample
import androidx.wear.compose.remote.material3.samples.RemoteLinearProgressIndicatorSample
import androidx.wear.compose.remote.material3.samples.RemoteSegmentedCircularProgressIndicatorAnimatedSample
import androidx.wear.compose.remote.material3.samples.RemoteSegmentedCircularProgressIndicatorBinarySample
import androidx.wear.compose.remote.material3.samples.RemoteSegmentedCircularProgressIndicatorSample
import com.google.common.truth.Truth.assertThat
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [35], qualifiers = "w1000dp-h500dp")
@RunWith(RobolectricTestRunner::class)
class RemoteProgressIndicatorAnimationTest {

    @get:Rule val painterRule = DocumentPainterRule()

    @Test
    fun circular_animatesProgressChanges() {
        val pixels =
            indicatorPixelsAroundChange(from = 0.25f, to = 0.75f) { progress ->
                RemoteCircularProgressIndicator(
                    progress = progress,
                    modifier = RemoteModifier.size(150.rdp),
                    colors = testColors(),
                )
            }

        pixels.assertAnimates()
    }

    @Test
    fun circular_coercesOutOfRangeProgressBeforeAnimating() {
        val painter = painterRule.paintDocument {
            RemoteCircularProgressIndicator(
                progress = RemoteFloat.createNamedRemoteFloat(ProgressName, 2f),
                modifier = RemoteModifier.size(150.rdp),
                colors = testColors(),
            )
        }
        val full = painter.countPixels(::isIndicator)

        painter.setFloat(ProgressName, 0.25f)
        painter.advanceTimeBy(EarlyMillis)
        val early = painter.countPixels(::isIndicator)

        // Because the initial progress of 2f was coerced to 1f before animating, the indicator has
        // already begun shrinking from 1f toward 0.25f after EarlyMillis.
        assertThat(early).isLessThan(full)
        assertThat(early).isGreaterThan(0)
    }

    @Test
    fun linear_animatesProgressChanges() {
        val pixels =
            indicatorPixelsAroundChange(from = 0.25f, to = 0.75f) { progress ->
                RemoteLinearProgressIndicator(
                    progress = progress,
                    modifier = RemoteModifier.size(150.rdp, 20.rdp),
                    colors = testColors(),
                )
            }

        pixels.assertAnimates()
    }

    @Test
    fun linear_followsOverriddenDefaultEffectsSpec() {
        assertFollowsOverriddenSpec(LinearDefaultEffectsScheme) { progress ->
            RemoteLinearProgressIndicator(
                progress = progress,
                modifier = RemoteModifier.size(150.rdp, 20.rdp),
                colors = testColors(),
            )
        }
    }

    @Test
    fun circular_followsOverriddenSlowEffectsSpec() {
        assertFollowsOverriddenSpec(LinearSlowEffectsScheme) { progress ->
            RemoteCircularProgressIndicator(
                progress = progress,
                modifier = RemoteModifier.size(150.rdp),
                colors = testColors(),
            )
        }
    }

    @Test
    fun segmented_animatesProgressChangesByLengthOnly() {
        val painter = painterRule.paintDocument {
            RemoteSegmentedCircularProgressIndicator(
                segmentCount = SegmentCount.ri,
                progress = RemoteFloat.createNamedRemoteFloat(ProgressName, 0f),
                modifier = RemoteModifier.size(150.rdp),
                colors = testColors(),
            )
        }

        // Fills the first segment and half of the second one.
        painter.setFloat(ProgressName, 0.3f)
        painter.advanceTimeBy(EarlyMillis)
        val early = painter.indicatorFrame()
        painter.advanceTimeBy(SettledMillis)
        val settled = painter.indicatorFrame()

        early.assertPartOf(settled)
    }

    @Test
    fun segmentedBinary_animatesSegmentsOnAndOffByLengthOnly() {
        val painter = painterRule.paintDocument {
            val firstSegmentOn = RemoteBoolean.createNamedRemoteBoolean(SegmentName, false)
            RemoteSegmentedCircularProgressIndicator(
                segmentCount = SegmentCount.ri,
                segmentValue = { segmentIndex ->
                    if (segmentIndex == 0) firstSegmentOn else false.rb
                },
                modifier = RemoteModifier.size(150.rdp),
                colors = testColors(),
            )
        }
        assertThat(painter.countPixels(::isIndicator)).isEqualTo(0)

        painter.setBoolean(SegmentName, true)
        painter.advanceTimeBy(EarlyMillis)
        val turningOn = painter.indicatorFrame()
        painter.advanceTimeBy(SettledMillis)
        val on = painter.indicatorFrame()

        turningOn.assertPartOf(on)

        painter.setBoolean(SegmentName, false)
        painter.advanceTimeBy(EarlyMillis)
        val turningOff = painter.countPixels(::isIndicator)
        painter.advanceTimeBy(LateMillis - EarlyMillis)
        val nearlyOff = painter.countPixels(::isIndicator)
        painter.advanceTimeBy(SettledMillis)
        val off = painter.countPixels(::isIndicator)

        // The segment shrinks back to its start rather than disappearing at once, and its end
        // shrinks with it rather than leaving a full dot behind.
        assertThat(turningOff).isGreaterThan(0)
        assertThat(turningOff).isLessThan(on.pixels)
        assertThat(nearlyOff).isLessThan(MaxNearlyOffPixels)
        assertThat(off).isEqualTo(0)
    }

    @Test
    fun segmentedBinary_followsThemeMotionScheme() {
        val indicator: @Composable @RemoteComposable () -> Unit = {
            val segmentOn = RemoteBoolean.createNamedRemoteBoolean(SegmentName, true)
            RemoteSegmentedCircularProgressIndicator(
                segmentCount = SegmentCount.ri,
                segmentValue = { segmentOn },
            )
        }
        val defaultDocument = painterRule.captureBytes { RemoteMaterialTheme { indicator() } }
        val standardDocument = painterRule.captureBytes {
            RemoteMaterialTheme(motionScheme = RemoteMotionScheme.standard()) { indicator() }
        }
        val otherDocument = painterRule.captureBytes {
            RemoteMaterialTheme(motionScheme = SlowEffectsScheme) { indicator() }
        }

        assertArrayEquals(defaultDocument, standardDocument)
        assertFalse(defaultDocument.contentEquals(otherDocument))
    }

    @Test
    fun segmented_followsOverriddenSlowEffectsSpec() {
        assertFollowsOverriddenSpec(LinearSlowEffectsScheme) { progress ->
            RemoteSegmentedCircularProgressIndicator(
                segmentCount = SegmentCount.ri,
                progress = progress,
                modifier = RemoteModifier.size(150.rdp),
                colors = testColors(),
            )
        }
    }

    @Test
    fun segmentedBinary_followsOverriddenFastEffectsSpec() {
        // Half of the first segment, as the progress overload draws it at half a segment.
        val halfSegment =
            painterRule
                .paintDocument {
                    RemoteSegmentedCircularProgressIndicator(
                        segmentCount = SegmentCount.ri,
                        progress = (0.5f / SegmentCount).rf,
                        modifier = RemoteModifier.size(150.rdp),
                        colors = testColors(),
                    )
                }
                .countPixels(::isIndicator)
        fun pixelsWith(scheme: RemoteMotionScheme): ProgressPixels {
            val painter = painterRule.paintDocument {
                val firstSegmentOn = RemoteBoolean.createNamedRemoteBoolean(SegmentName, false)
                RemoteMaterialTheme(motionScheme = scheme) {
                    RemoteSegmentedCircularProgressIndicator(
                        segmentCount = SegmentCount.ri,
                        segmentValue = { segmentIndex ->
                            if (segmentIndex == 0) firstSegmentOn else false.rb
                        },
                        modifier = RemoteModifier.size(150.rdp),
                        colors = testColors(),
                    )
                }
            }
            val before = painter.countPixels(::isIndicator)
            painter.setBoolean(SegmentName, true)
            painter.advanceTimeBy(HalfLinearSpecMillis)
            val early = painter.countPixels(::isIndicator)
            painter.advanceTimeBy(SettledMillis)
            return ProgressPixels(before, early, settled = painter.countPixels(::isIndicator))
        }
        val overridden = pixelsWith(LinearFastEffectsScheme)
        val standard = pixelsWith(RemoteMotionScheme.standard())

        // Halfway through the linear spec, the segment that turns on is half drawn, while with the
        // standard spring it has already settled by then.
        assertThat(overridden.early.toFloat())
            .isWithin(halfSegment * PixelTolerance)
            .of(halfSegment.toFloat())
        assertThat(standard.early.toFloat())
            .isWithin(standard.settled * PixelTolerance)
            .of(standard.settled.toFloat())
    }

    @Test
    fun segmented_drawsPartialDotsOnlyForAnimatedSegments() {
        val constantDocument = painterRule.captureDocument {
            RemoteSegmentedCircularProgressIndicator(
                segmentCount = SegmentCount.ri,
                progress = 0.3f.rf,
            )
        }
        val animatedDocument = painterRule.captureDocument {
            RemoteSegmentedCircularProgressIndicator(
                segmentCount = SegmentCount.ri,
                progress = RemoteFloat.createNamedRemoteFloat(ProgressName, 0.3f),
            )
        }
        val binaryDocument = painterRule.captureDocument {
            val firstSegmentOn = RemoteBoolean.createNamedRemoteBoolean(SegmentName, false)
            RemoteSegmentedCircularProgressIndicator(
                segmentCount = SegmentCount.ri,
                segmentValue = { segmentIndex ->
                    if (segmentIndex == 0) firstSegmentOn else true.rb
                },
            )
        }

        // The end of a segment is drawn as a partial dot, a circle, only while it can animate.
        assertThat(constantDocument.countDrawCircles()).isEqualTo(0)
        assertThat(animatedDocument.countDrawCircles()).isEqualTo(SegmentCount)
        assertThat(binaryDocument.countDrawCircles()).isEqualTo(1)
    }

    @Test
    fun animatedProgress_followsThemeMotionScheme() {
        for (sample in AnimatedSamples) {
            val defaultDocument = painterRule.captureBytes { RemoteMaterialTheme { sample() } }
            val standardDocument = painterRule.captureBytes {
                RemoteMaterialTheme(motionScheme = RemoteMotionScheme.standard()) { sample() }
            }
            val otherDocument = painterRule.captureBytes {
                RemoteMaterialTheme(motionScheme = FastEffectsScheme) { sample() }
            }

            assertArrayEquals(defaultDocument, standardDocument)
            assertFalse(defaultDocument.contentEquals(otherDocument))
        }
    }

    @Test
    fun constantProgress_isNotAnimated() {
        for (sample in ConstantSamples) {
            val defaultDocument = painterRule.captureBytes { RemoteMaterialTheme { sample() } }
            for (scheme in listOf(FastEffectsScheme, SlowEffectsScheme)) {
                val otherDocument = painterRule.captureBytes {
                    RemoteMaterialTheme(motionScheme = scheme) { sample() }
                }

                assertArrayEquals(defaultDocument, otherDocument)
            }
        }
    }

    @Test
    fun indeterminate_trimsUseStandardEasing() {
        // 250 ms into the 5 s loop is 20% into the first trim, where EasingStandard has drawn the
        // arc to 80% * 0.5 = 40% of the circle, and the legacy curve only to 80% * 0.13 = 11%.
        val painter =
            painterRule.paintDocument(clock = TestClock(epochMilli = 250L)) {
                RemoteCircularProgressIndicator(
                    modifier = RemoteModifier.size(150.rdp),
                    colors = testColors(),
                )
            }

        val indicatorPixels = painter.countPixels(::isIndicator)
        val trackPixels = painter.countPixels(::isTrack)

        val indicatorFraction = indicatorPixels.toFloat() / (indicatorPixels + trackPixels)
        assertThat(indicatorFraction).isGreaterThan(0.3f)
        assertThat(indicatorFraction).isLessThan(0.5f)
    }

    /**
     * Paints the indicator at a progress of [from], changes the progress to [to], and returns the
     * indicator pixel counts before the change, [earlyMillis] after it and once settled.
     */
    private fun indicatorPixelsAroundChange(
        from: Float,
        to: Float,
        earlyMillis: Long = EarlyMillis,
        content: @Composable @RemoteComposable (progress: RemoteFloat) -> Unit,
    ): ProgressPixels {
        val painter = painterRule.paintDocument {
            content(RemoteFloat.createNamedRemoteFloat(ProgressName, from))
        }
        val before = painter.countPixels(::isIndicator)

        painter.setFloat(ProgressName, to)
        painter.advanceTimeBy(earlyMillis)
        val early = painter.countPixels(::isIndicator)

        painter.advanceTimeBy(SettledMillis)
        val settled = painter.countPixels(::isIndicator)

        return ProgressPixels(before = before, early = early, settled = settled)
    }

    /**
     * Checks that [indicator] animates progress changes with the spec that [motionScheme] overrides
     * with [LinearSpec]: halfway through that spec, the indicator looks like it does at the halfway
     * progress, while with the standard scheme it has already settled by then.
     */
    private fun assertFollowsOverriddenSpec(
        motionScheme: RemoteMotionScheme,
        indicator: @Composable @RemoteComposable (progress: RemoteFloat) -> Unit,
    ) {
        val halfway = painterRule.paintDocument { indicator(0.5f.rf) }.countPixels(::isIndicator)
        fun pixelsWith(scheme: RemoteMotionScheme) =
            indicatorPixelsAroundChange(
                from = 0.25f,
                to = 0.75f,
                earlyMillis = HalfLinearSpecMillis,
            ) { progress ->
                RemoteMaterialTheme(motionScheme = scheme) { indicator(progress) }
            }
        val overridden = pixelsWith(motionScheme)
        val standard = pixelsWith(RemoteMotionScheme.standard())

        assertThat(overridden.early.toFloat())
            .isWithin(halfway * PixelTolerance)
            .of(halfway.toFloat())
        assertThat(standard.early.toFloat())
            .isWithin(standard.settled * PixelTolerance)
            .of(standard.settled.toFloat())
    }

    /**
     * Returns the indicator pixels of a segmented indicator, and the thickness of the indicator
     * where it is thickest in the first segment.
     */
    private fun DocumentPainterRule.DocumentPainter.indicatorFrame(): IndicatorFrame {
        // The track is drawn in full, so the drawn pixels are bounded by the ring.
        var left = width
        var top = height
        var right = -1
        var bottom = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = colorAt(x, y)
                if (isIndicator(color) || isTrack(color)) {
                    left = min(left, x)
                    top = min(top, y)
                    right = max(right, x)
                    bottom = max(bottom, y)
                }
            }
        }
        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        val outerRadius = (right - left) / 2f

        // Walk rays from the center across the first segment, and measure how much of each ray
        // is indicator.
        var thickness = 0f
        for (degrees in FirstSegmentStartAngle until FirstSegmentStartAngle + 360 / SegmentCount) {
            val angle = Math.toRadians(degrees.toDouble())
            var rayThickness = 0f
            var radius = 0f
            while (radius <= outerRadius) {
                val x = (centerX + radius * cos(angle)).roundToInt()
                val y = (centerY + radius * sin(angle)).roundToInt()
                if (isIndicator(colorAt(x, y))) rayThickness += RayStep
                radius += RayStep
            }
            thickness = max(thickness, rayThickness)
        }
        return IndicatorFrame(pixels = countPixels(::isIndicator), thickness = thickness)
    }

    /** Returns how many circles this document draws, counting the operations of all containers. */
    private fun CoreDocument.countDrawCircles(): Int = countDrawCircles(operations)

    private fun countDrawCircles(operations: List<Operation>): Int = operations.sumOf { operation ->
        when (operation) {
            is DrawCircle -> 1
            is Container -> countDrawCircles(operation.list)
            else -> 0
        }
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

    private class ProgressPixels(val before: Int, val early: Int, val settled: Int) {
        /** Checks that the indicator grew gradually from [before] to [settled]. */
        fun assertAnimates() {
            assertThat(before).isGreaterThan(0)
            assertThat(early).isGreaterThan(before)
            assertThat(early).isLessThan(settled)
        }
    }

    private class IndicatorFrame(val pixels: Int, val thickness: Float) {
        /**
         * Checks that this frame, taken during an animation, draws part of the [settled] indicator
         * at its full stroke width, so only the length of the indicator animates.
         */
        fun assertPartOf(settled: IndicatorFrame) {
            assertThat(pixels).isGreaterThan(0)
            assertThat(pixels).isLessThan(settled.pixels)
            assertThat(settled.thickness).isGreaterThan(MinStrokeThickness)
            assertThat(thickness).isWithin(ThicknessTolerance).of(settled.thickness)
        }
    }

    private companion object {
        const val ProgressName = "progress"
        const val SegmentName = "segment"
        const val SegmentCount = 5

        /** A moment into the progress animation, before it settles. */
        const val EarlyMillis = 50L

        /** Late in the fast effects animation of a segment, which then is a sliver of the gap. */
        const val LateMillis = 150L

        /** Far fewer pixels than a full dot, which has about 130 at the default stroke width. */
        const val MaxNearlyOffPixels = 20

        /** Long after the progress animation settles. */
        const val SettledMillis = 3000L

        /** How long [LinearSpec] takes, in milliseconds. */
        const val LinearSpecMillis = 1000

        /** Halfway through [LinearSpec], which then has covered half of a change. */
        const val HalfLinearSpecMillis = LinearSpecMillis / 2L

        /** How much a pixel count may differ from the expected one, as a fraction of it. */
        const val PixelTolerance = 0.1f

        /** A spec that animates at a constant rate, unlike the springs of the standard scheme. */
        val LinearSpec: RemoteAnimationSpec =
            remoteTween(durationMillis = LinearSpecMillis, easing = RemoteEasing.Linear)

        /** The standard scheme, with [LinearSpec] as its slow effects spec. */
        val LinearSlowEffectsScheme: RemoteMotionScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun slowEffectsSpec(): RemoteAnimationSpec = LinearSpec
            }

        /** The standard scheme, with [LinearSpec] as its default effects spec. */
        val LinearDefaultEffectsScheme: RemoteMotionScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun defaultEffectsSpec(): RemoteAnimationSpec = LinearSpec
            }

        /** The standard scheme, with [LinearSpec] as its fast effects spec. */
        val LinearFastEffectsScheme: RemoteMotionScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun fastEffectsSpec(): RemoteAnimationSpec = LinearSpec
            }

        /**
         * Where the first segment starts, at the top, in degrees clockwise from the 3 o'clock
         * position.
         */
        const val FirstSegmentStartAngle = 270

        /** The step of the ray that measures the thickness of the indicator, in pixels. */
        const val RayStep = 0.25f

        /** Less than the default stroke width, 12 dp, which is 12 px in these tests. */
        const val MinStrokeThickness = 10f

        /** How much the measured thickness may vary between frames, in pixels. */
        const val ThicknessTolerance = 1.5f

        /** A scheme whose slow and default effects specs differ from the standard scheme. */
        val FastEffectsScheme: RemoteMotionScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun slowEffectsSpec(): RemoteAnimationSpec = fastEffectsSpec()

                override fun defaultEffectsSpec(): RemoteAnimationSpec = fastEffectsSpec()
            }

        /** A scheme whose fast and default effects specs differ from the standard scheme. */
        val SlowEffectsScheme: RemoteMotionScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun fastEffectsSpec(): RemoteAnimationSpec = slowEffectsSpec()

                override fun defaultEffectsSpec(): RemoteAnimationSpec = slowEffectsSpec()
            }

        val AnimatedSamples: List<@Composable @RemoteComposable () -> Unit> =
            listOf(
                { RemoteCircularProgressIndicatorAnimatedSample() },
                { RemoteLinearProgressIndicatorAnimatedSample() },
                { RemoteSegmentedCircularProgressIndicatorAnimatedSample() },
            )

        val ConstantSamples: List<@Composable @RemoteComposable () -> Unit> =
            listOf(
                { RemoteCircularProgressIndicatorSample() },
                { RemoteLinearProgressIndicatorSample() },
                { RemoteSegmentedCircularProgressIndicatorSample() },
                { RemoteSegmentedCircularProgressIndicatorBinarySample() },
            )
    }
}
