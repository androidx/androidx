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
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.test.utils.TestClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorAnimatedSample
import androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorSample
import androidx.wear.compose.remote.material3.samples.RemoteLinearProgressIndicatorAnimatedSample
import androidx.wear.compose.remote.material3.samples.RemoteLinearProgressIndicatorSample
import com.google.common.truth.Truth.assertThat
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
            val otherDocument = painterRule.captureBytes {
                RemoteMaterialTheme(motionScheme = FastEffectsScheme) { sample() }
            }

            assertArrayEquals(defaultDocument, otherDocument)
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

    private companion object {
        const val ProgressName = "progress"

        /** A moment into the progress animation, before it settles. */
        const val EarlyMillis = 50L

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

        /** A scheme whose slow and default effects specs differ from the standard scheme. */
        val FastEffectsScheme: RemoteMotionScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun slowEffectsSpec(): RemoteAnimationSpec = fastEffectsSpec()

                override fun defaultEffectsSpec(): RemoteAnimationSpec = fastEffectsSpec()
            }

        val AnimatedSamples: List<@Composable @RemoteComposable () -> Unit> =
            listOf(
                { RemoteCircularProgressIndicatorAnimatedSample() },
                { RemoteLinearProgressIndicatorAnimatedSample() },
            )

        val ConstantSamples: List<@Composable @RemoteComposable () -> Unit> =
            listOf(
                { RemoteCircularProgressIndicatorSample() },
                { RemoteLinearProgressIndicatorSample() },
            )
    }
}
