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

import androidx.collection.floatListOf
import androidx.collection.intListOf
import androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer
import androidx.glance.adaptive.appwidget.templates.track.TrackMatrixSelector
import androidx.glance.adaptive.appwidget.test.R as TestR
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.ChartLabel
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
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
         * Builds a Track Steps payload using semantic slot components matching the design's
         * goldens.
         *
         * @param primary The headline value.
         * @param secondary The label of the headline value.
         * @param progress The ring's progress.
         * @param supporting The first supporting value.
         */
        private fun track(
            primary: String,
            secondary: String,
            progress: Float,
            supporting: String,
        ): TrackTemplate =
            TrackTemplate(
                leading =
                    Ring(
                        progress = progress,
                        icon =
                            TestR.drawable.glance_adaptive_appwidget_track_screenshot_steps_glyph,
                    ),
                primary = TemplateText(primary),
                secondary = TemplateText(secondary),
                hero =
                    Graph(
                        values =
                            floatListOf(1_700f, 3_000f, 4_600f, 10_000f, 3_400f, 9_100f, 10_000f),
                        labels = listOf("T", "F", "S", "S", "M", "T", "W"),
                        goal = 10_000f,
                        startLabel = ChartLabel("7 Day Avg"),
                        endLabel = ChartLabel("13,843 steps"),
                    ),
                supporting =
                    listOf(
                        TemplateText(supporting),
                        TemplateText("3,118 to go"),
                        Badge(
                            label = "Z2",
                            outlineColor = 0xFFFFE347.toInt(),
                            dotColors =
                                intListOf(
                                    0xFFFFA4A4.toInt(),
                                    0xFFA0FF47.toInt(),
                                    0xFF47A6FF.toInt(),
                                ),
                        ),
                    ),
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
