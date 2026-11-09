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

package androidx.glance.adaptive.appwidget.screenshot

import androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer
import androidx.glance.adaptive.appwidget.templates.track.TrackMatrixSelector
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.templates.TrackTemplate
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Screenshots of the Track template for the design's Steps payload at every container of the
 * design's density matrix that Track draws, plus a large font scale and the dark theme.
 *
 * The lock screen's Mini strip is not covered: Track has no layout for it yet.
 *
 * Track draws through the library's default pipeline: [TrackMatrixSelector], then
 * [RemoteViewsMatrixRenderer].
 */
@LargeTest
// Goldens are drawn on the API 35 emulator, as the screenshot rule requires.
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(Parameterized::class)
class TrackScreenshotTest(private val case: Case) {

    @get:Rule val screenshotRule = templateScreenshotRule()

    @Test
    fun matchesGolden() {
        screenshotRule.assertTemplateAgainstGolden(
            case.template,
            TrackRenderer,
            case.size,
            golden = case.golden,
            nightMode = case.nightMode,
            fontScale = case.fontScale,
        )
    }

    /**
     * One screenshot.
     *
     * @property golden Name of the golden, which also names the case.
     */
    class Case
    internal constructor(
        internal val golden: String,
        internal val template: TrackTemplate,
        internal val size: TierSize,
        internal val nightMode: Boolean = false,
        internal val fontScale: Float = 1f,
    ) {
        override fun toString(): String = golden
    }

    companion object {
        /** Draws Track as the library does by default for every AppWidget surface. */
        private val TrackRenderer = TemplateRenderer(TrackMatrixSelector, RemoteViewsMatrixRenderer)

        /** Daily steps: a ring, the step count, a week of steps and supporting values. */
        private val Steps =
            track(primary = "11,056", secondary = "Steps", progress = 0.55f, supporting = "55%")

        /**
         * A Track payload, named by the design's slots. This is the only code that builds a
         * [TrackTemplate], so the payloads can move to a richer Track API in one place.
         *
         * The current [TrackTemplate] carries only these four values. The ring's glyph, the hero
         * graph, and the second and third supporting values come from the library's placeholder
         * data.
         *
         * @param primary The headline value.
         * @param secondary The label of the headline value.
         * @param progress The ring's progress, or `null` for no ring.
         * @param supporting The first supporting value.
         */
        private fun track(
            primary: String,
            secondary: String,
            progress: Float?,
            supporting: String,
        ): TrackTemplate =
            TrackTemplate(
                title = primary,
                subtitle = secondary,
                progress = progress,
                statusText = supporting,
            )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Case> = buildList {
            for (size in TierSize.entries) {
                if (size == TierSize.MINI_STRIP) continue
                add(Case("track_steps_${size.name.lowercase()}", Steps, size))
            }
            add(Case("track_steps_w4h4_fontscale2", Steps, TierSize.W4H4, fontScale = 2f))
            add(Case("track_steps_w4h4_dark", Steps, TierSize.W4H4, nightMode = true))
        }
    }
}
