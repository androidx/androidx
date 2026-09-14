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

package androidx.glance.adaptive.appwidget.ui.templates

import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.adaptive.appwidget.ui.components.BarChartBlock
import androidx.glance.adaptive.appwidget.ui.components.PrimaryValueBlock
import androidx.glance.adaptive.appwidget.ui.components.ProgressRingBlock
import androidx.glance.adaptive.appwidget.ui.components.SpecialBadgeBlock
import androidx.glance.adaptive.appwidget.ui.components.SupportingValueBlock
import androidx.glance.adaptive.appwidget.ui.components.TertiaryLabelBlock
import androidx.glance.adaptive.appwidget.ui.components.TrackBar
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.templates.TrackTemplate
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider

/** Corner radius used below API 31, where the launcher's own radius is not queryable. */
private val FallbackCornerRadius = 28.dp

/** Height of the supporting row. */
private val SupportingRowHeight = 24.dp

/** Gap between the progress ring and the headline metric. */
private val TitleSlotSpacing = 8.dp

/**
 * Hero chart columns.
 *
 * [TrackTemplate] carries no time series, so the hero renders this fixed week until the payload
 * grows one. Everything else on screen is driven by real template data.
 */
private val PlaceholderBars =
    listOf(
        TrackBar(label = "T", value = 0.17f),
        TrackBar(label = "F", value = 0.30f),
        TrackBar(label = "S", value = 0.46f),
        TrackBar(label = "S", value = 1.00f, emphasized = true),
        TrackBar(label = "M", value = 0.34f),
        TrackBar(label = "T", value = 0.91f),
        TrackBar(label = "W", value = 1.00f, emphasized = true),
    )

/** Hero chart caption. Placeholder for the same reason as [PlaceholderBars]. */
private val PlaceholderChartDescription = "7 Day Avg" to "13,843 steps"

/** Second supporting value. Placeholder for the same reason as [PlaceholderBars]. */
private const val PLACEHOLDER_SECONDARY_STATUS = "3,118 to go"

/**
 * Badge label in the third supporting slot. Placeholder for the same reason as [PlaceholderBars].
 */
private const val PLACEHOLDER_BADGE_LABEL = "Z2"

/** Status dot colors flanking the badge. Placeholder for the same reason as [PlaceholderBars]. */
private val PlaceholderBadgeDots =
    listOf(
        ColorProvider(Color(0xFFFFA4A4)),
        ColorProvider(Color(0xFFA0FF47)),
        ColorProvider(Color(0xFF47A6FF)),
    )

/** Badge outline color. Placeholder for the same reason as [PlaceholderBars]. */
private val PlaceholderBadgeOutline = ColorProvider(Color(0xFFFFE347))

/**
 * Glance renderer for [TrackTemplate] on platform AppWidget surfaces.
 *
 * Substitutes `@Composable () -> Unit` for the host content type of [TemplateRenderer]: [render]
 * resolves the slot plan eagerly — outside composition, so it stays unit testable — and returns the
 * Glance content to emit for it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object TrackTemplateRenderer :
    TemplateRenderer<TrackTemplate, AppWidgetGlanceSurface, @Composable () -> Unit> {

    /**
     * Resolves the slot plan for [template] under [constraints] and returns the Glance content
     * rendering it.
     *
     * @param template The template data payload.
     * @param constraints Container size in DP plus the target AppWidget surface.
     * @return Glance content for the resolved [TrackSlots].
     */
    override fun render(
        template: TrackTemplate,
        constraints: HostConstraints<AppWidgetGlanceSurface>,
    ): @Composable () -> Unit {
        val slots = TrackSizeSelector.select(template, constraints)
        return { TrackLayout(template, slots) }
    }
}

/**
 * The Track component, at every breakpoint.
 *
 * There is one hierarchy — title, hero and supporting — and [slots] decides how much of it is
 * emitted and at what size. No breakpoint is a separate layout, including [TrackSlots.stackTitle]
 * containers, which differ only in the title slot's orientation.
 *
 * The three slots are distributed vertically by what is present:
 * - **Hero present**: it takes `defaultWeight()` and absorbs all the slack, so the chart grows with
 *   the container while title and supporting keep their intrinsic heights.
 * - **Hero absent, supporting present**: a weighted spacer takes the slack instead, pinning
 *   supporting to the bottom edge and title to the top.
 * - **Title only**: the column centers it.
 *
 * @param template The template data payload.
 * @param slots Which slots to emit, and at what size.
 */
@Composable
@VisibleForTesting
internal fun TrackLayout(template: TrackTemplate, slots: TrackSlots) {
    Column(
        modifier =
            GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .widgetCornerRadius()
                .padding(slots.contentPadding),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        TitleSlot(template, slots)

        if (slots.showHero) {
            Spacer(GlanceModifier.height(slots.slotSpacing))
            BarChartBlock(
                bars = PlaceholderBars,
                barMaxHeight = slots.barMaxHeight,
                barSpacing = slots.barSpacing,
                showLabels = true,
                description = if (slots.showHeroDescription) PlaceholderChartDescription else null,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            )
        }

        if (slots.showSupporting) {
            if (slots.showHero) {
                Spacer(GlanceModifier.height(slots.slotSpacing))
            } else {
                Spacer(GlanceModifier.defaultWeight())
            }
            SupportingSlot(template, slots)
        }
    }
}

/**
 * Progress ring, headline metric and — where it fits — the trailing unit label.
 *
 * The same three blocks either way; [TrackSlots.stackTitle] only decides whether they run across or
 * down, which is the whole of what the narrowest breakpoint changes.
 */
@Composable
private fun TitleSlot(template: TrackTemplate, slots: TrackSlots) {
    val label = template.subtitle
    val showLabel = slots.showTertiary && label != null

    if (slots.stackTitle) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            ProgressRingBlock(progress = template.progress, size = slots.ringSize)
            Spacer(GlanceModifier.height(TitleSlotSpacing))
            PrimaryValueBlock(value = template.title, fontSize = slots.primaryTextSize)
            if (showLabel) {
                Spacer(GlanceModifier.height(TitleSlotSpacing))
                TertiaryLabelBlock(label!!)
            }
        }
    } else {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(slots.ringSize),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            ProgressRingBlock(progress = template.progress, size = slots.ringSize)
            Spacer(GlanceModifier.width(TitleSlotSpacing))
            PrimaryValueBlock(
                value = template.title,
                fontSize = slots.primaryTextSize,
                modifier = GlanceModifier.defaultWeight(),
            )
            if (showLabel) {
                Spacer(GlanceModifier.width(TitleSlotSpacing))
                TertiaryLabelBlock(label!!)
            }
        }
    }
}

/**
 * Bottom row of supporting values.
 *
 * A single value centers; two or more spread to the edges, which is how the design distributes
 * them. Glance has no `SpaceBetween` arrangement, so the gaps are weighted spacers.
 */
@Composable
private fun SupportingSlot(template: TrackTemplate, slots: TrackSlots) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(SupportingRowHeight),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment =
            if (slots.supportingCount == 1) Alignment.Horizontal.CenterHorizontally
            else Alignment.Horizontal.Start,
    ) {
        template.statusText?.let { status ->
            SupportingValueBlock(value = status, color = GlanceTheme.colors.onSurfaceVariant)
        }
        if (slots.supportingCount >= 2) {
            Spacer(GlanceModifier.defaultWeight())
            SupportingValueBlock(
                value = PLACEHOLDER_SECONDARY_STATUS,
                color = GlanceTheme.colors.primary,
            )
        }
        if (slots.supportingCount >= 3) {
            Spacer(GlanceModifier.defaultWeight())
            SpecialBadgeBlock(
                label = PLACEHOLDER_BADGE_LABEL,
                outlineColor = PlaceholderBadgeOutline,
                dotColors = PlaceholderBadgeDots,
            )
        }
    }
}

/**
 * Rounds the widget to the launcher's own grid curvature.
 *
 * `system_app_widget_background_radius` only exists from API 31, so older platforms fall back to a
 * fixed [FallbackCornerRadius].
 */
private fun GlanceModifier.widgetCornerRadius(): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        cornerRadius(FallbackCornerRadius)
    }
