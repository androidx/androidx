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
package androidx.glance.adaptive.appwidget.templates.track

import androidx.annotation.DrawableRes
import androidx.collection.floatListOf
import androidx.collection.intListOf
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.TrackTemplate

/**
 * Placeholder data of the Track widget, for the current phase.
 *
 * [TrackTemplate] does not yet carry the hero, the second and third actions or the ring glyph, so
 * both Track renderers fill those slots from here until the payload grows them.
 */
internal object TrackPlaceholderData {
    const val SECONDARY_STATUS = "3,118 to go"

    const val BADGE_LABEL = "Z2"
    const val BADGE_OUTLINE_COLOR = 0xFFFFE347.toInt()
    val BadgeDotColors = intListOf(0xFFFFA4A4.toInt(), 0xFFA0FF47.toInt(), 0xFF47A6FF.toInt())

    val GraphValues = floatListOf(1_700f, 3_000f, 4_600f, 10_000f, 3_400f, 9_100f, 10_000f)
    val GraphLabels = listOf("T", "F", "S", "S", "M", "T", "W")
    const val GRAPH_GOAL = 10_000f
    val GraphDescription = "7 Day Avg" to "13,843 steps"

    /** Second supporting value. */
    val SecondaryStatus = TemplateText(SECONDARY_STATUS)

    /** Third supporting value: a badge flanked by status dots. */
    val StatusBadge =
        Badge(label = BADGE_LABEL, outlineColor = BADGE_OUTLINE_COLOR, dotColors = BadgeDotColors)

    /** Hero: a week of daily step counts against a daily step goal. */
    val WeekGraph = weekGraph(description = null)

    /** [WeekGraph] captioned with the week's average, for the tallest containers. */
    val CaptionedWeekGraph = weekGraph(description = GraphDescription)

    /**
     * Glyph drawn on the progress ring.
     *
     * Declared at 32 dp, larger than any glyph drawn, because a RemoteViews image can only be
     * capped, not sized, before API 31.
     *
     * Declared with an explicit type and a getter because the docs build analyses these sources
     * without the generated `R` class, and cannot infer a type it has to resolve `R` to find.
     */
    @get:DrawableRes
    val Glyph: Int
        get() = R.drawable.glance_adaptive_appwidget_remoteviews_track_placeholder_steps_glyph

    private fun weekGraph(description: Pair<String, String>?): Graph =
        Graph(
            values = GraphValues,
            labels = GraphLabels,
            goal = GRAPH_GOAL,
            description = description,
        )
}
