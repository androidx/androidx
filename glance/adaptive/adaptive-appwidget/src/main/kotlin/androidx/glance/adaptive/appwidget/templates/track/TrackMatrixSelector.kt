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

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.appwidget.matrix.MatrixBulbContent
import androidx.glance.adaptive.appwidget.matrix.MatrixCardContent
import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.selection.from
import androidx.glance.adaptive.core.selection.ArchetypeSelector
import androidx.glance.adaptive.core.selection.HeightTier
import androidx.glance.adaptive.core.selection.SizeTiers
import androidx.glance.adaptive.core.selection.WidthTier
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.TrackTemplate

/**
 * Maps a [TrackTemplate] onto the Information Matrix, for any backend's matrix renderer, such as
 * [androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer].
 *
 * This is the whole of the Track template: which component goes in which slot at each width and
 * height tier. Everything about the container (padding, spacing, the header's width budget, the
 * slots a short container cannot fit, the hero's height) belongs to the matrix.
 *
 * - **Width** decides horizontal richness: W1 stacks the header, and wider tiers progressively
 *   admit the unit label and more action values.
 * - **Height** decides which rows appear: the actions, then the hero (with its description at H4).
 * - The narrowest, shortest containers have no room for a card, so the design draws the widget as a
 *   single ring with the metric inside: the bulb.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object TrackMatrixSelector :
    ArchetypeSelector<TrackTemplate, AppWidgetSurface, MatrixContent> {

    override fun select(
        template: TrackTemplate,
        surface: AppWidgetSurface,
    ): MatrixContent {
        val tiers = SizeTiers.from(surface.dimensions, surface)
        val isNarrow = tiers.width == WidthTier.W1
        val title = template.title.takeIf { it.isNotBlank() }
        val subtitle = template.subtitle.toTextOrNull()
        val statusText = template.statusText.toTextOrNull()
        val progress = template.progress.validProgress()
        // The bulb is the headline inside the ring, so without a headline the card stays.
        if (isNarrow && tiers.height <= HeightTier.H1 && title != null) {
            return MatrixBulbContent(
                component = ContainedRing(progress, title, TrackPlaceholderData.Glyph),
                hasDroppedSlots = subtitle != null || statusText != null,
            )
        }

        // W1 has a single column, so height alone decides whether the unit label fits.
        val showSecondary =
            if (isNarrow) tiers.height >= HeightTier.H3
            else tiers.width == WidthTier.W3 || tiers.width == WidthTier.W4
        val actionCount =
            when {
                isNarrow -> if (tiers.height >= HeightTier.H4) 1 else 0
                tiers.height < HeightTier.H2 -> 0
                tiers.width == WidthTier.W2 -> 1
                tiers.width == WidthTier.W3 -> 2
                else -> 3
            }
        val hero =
            when {
                isNarrow || tiers.height < HeightTier.H3 -> null
                tiers.height == HeightTier.H4 -> TrackPlaceholderData.CaptionedWeekGraph
                else -> TrackPlaceholderData.WeekGraph
            }

        return MatrixCardContent(
            // The ring tracks the template's progress, so without progress the slot stays empty.
            leading = progress?.let { Ring(it, TrackPlaceholderData.Glyph) },
            primary = title?.let { TemplateText(it) },
            secondary = subtitle?.takeIf { showSecondary },
            hero = hero,
            // A missing status text leaves its position empty rather than shifting the others.
            actions =
                listOf(
                        statusText,
                        TrackPlaceholderData.SecondaryStatus,
                        TrackPlaceholderData.StatusBadge,
                    )
                    .subList(0, actionCount),
            hasDroppedSlots =
                (subtitle != null && !showSecondary) || (statusText != null && actionCount == 0),
        )
    }
}

/** This text as a component, or `null` when there is none to show. */
private fun String?.toTextOrNull(): TemplateText? =
    this?.takeIf { it.isNotBlank() }?.let(::TemplateText)

/**
 * This progress clamped to `[0.0, 1.0]`, or `null` when absent or not a number. [TrackTemplate]
 * does not check it, while the ring components do.
 */
private fun Float?.validProgress(): Float? = this?.takeUnless { it.isNaN() }?.coerceIn(0f, 1f)
