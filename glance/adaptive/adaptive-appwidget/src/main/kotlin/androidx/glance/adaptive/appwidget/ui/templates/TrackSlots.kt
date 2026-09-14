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

import androidx.annotation.RestrictTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HeightTier
import androidx.glance.adaptive.core.ui.selection.SizeTiers
import androidx.glance.adaptive.core.ui.selection.WidthTier

/** Height of the title slot when it lays out horizontally, sized to the progress ring. */
private const val TITLE_ROW_HEIGHT_DP = 56f

/** Height of the supporting slot. */
private const val SUPPORTING_ROW_HEIGHT_DP = 24f

/** Height of one line of stacked title content — the headline metric or the unit label. */
private const val STACKED_LINE_HEIGHT_DP = 24f

/** Gap between lines of a stacked title slot. */
private const val STACKED_LINE_SPACING_DP = 8f

/** Height reserved for the hero chart's axis label row. */
private const val CHART_LABEL_ROW_HEIGHT_DP = 12f

/** Height of the hero chart's leading/trailing description row. */
private const val CHART_DESCRIPTION_HEIGHT_DP = 16f

/** Gap between the hero chart's bars and its description row. */
private const val CHART_DESCRIPTION_SPACING_DP = 8f

/**
 * Slack left unclaimed when sizing the hero.
 *
 * The layout is measured by `RemoteViews` at display time, not here, so the arithmetic is an
 * estimate. Giving a little back keeps a rounding difference from clipping the supporting row.
 */
private const val HERO_SAFETY_MARGIN_DP = 4f

/** Shortest the hero bars are allowed to get before the chart stops being worth drawing. */
private const val MIN_BAR_MAX_HEIGHT_DP = 10f

/** Vertical density of the Track hero slot. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class TrackHeroDensity {
    /** Hero is omitted entirely — the container is too short for it. */
    NONE,

    /** Bars and their axis labels only, no description row. */
    COMPACT,

    /** Bars, axis labels, and the leading/trailing description row. */
    FULL,
}

/**
 * The resolved Track layout: which slots are present at a given size, and how large each is.
 *
 * Track is a single component across every breakpoint rather than a family of layouts, so this *is*
 * the archetype — [TrackSizeSelector] resolves it and [TrackLayout] renders exactly what it
 * describes. The rules it encodes:
 * - **Width** decides horizontal richness. [WidthTier.W1] is too narrow to place the ring beside
 *   the headline metric, so the title slot stacks ([stackTitle]); wider tiers lay it out in a row
 *   and progressively admit the unit label and more supporting values.
 * - **Height** decides which rows survive. The title always does; supporting and then the hero
 *   appear as room allows.
 * - At [WidthTier.W1] height takes over the role width plays elsewhere, because there is only ever
 *   one column: the unit label arrives at [HeightTier.H3] and the supporting value at
 *   [HeightTier.H4].
 *
 * Resolution is free of Compose, so the whole breakpoint matrix is verifiable in a plain JVM test.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TrackSlots
internal constructor(
    /** Whether the title slot stacks vertically instead of laying out in a row. */
    public val stackTitle: Boolean,
    /** Whether the unit label beside the headline metric is shown. */
    public val showTertiary: Boolean,
    /** Vertical density of the hero (chart) slot. */
    public val heroDensity: TrackHeroDensity,
    /** How many supporting values the bottom row has room for, `0..3`. */
    public val supportingCount: Int,
    /** Padding between the widget bounds and its content. */
    public val contentPadding: Dp,
    /** Vertical gap between the title, hero and supporting slots. */
    public val slotSpacing: Dp,
    /** Type size of the headline metric. */
    public val primaryTextSize: TextUnit,
    /** Edge length of the progress ring. */
    public val ringSize: Dp,
    /** Height of a hero chart bar at full value. */
    public val barMaxHeight: Dp,
    /** Horizontal gap between adjacent hero chart bars. */
    public val barSpacing: Dp,
) {
    /** Whether the hero slot is emitted at all. */
    public val showHero: Boolean
        get() = heroDensity != TrackHeroDensity.NONE

    /** Whether the supporting row is emitted at all. */
    public val showSupporting: Boolean
        get() = supportingCount > 0

    /** Whether the hero chart carries its leading/trailing description row. */
    public val showHeroDescription: Boolean
        get() = heroDensity == TrackHeroDensity.FULL

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackSlots) return false
        return stackTitle == other.stackTitle &&
            showTertiary == other.showTertiary &&
            heroDensity == other.heroDensity &&
            supportingCount == other.supportingCount &&
            contentPadding == other.contentPadding &&
            slotSpacing == other.slotSpacing &&
            primaryTextSize == other.primaryTextSize &&
            ringSize == other.ringSize &&
            barMaxHeight == other.barMaxHeight &&
            barSpacing == other.barSpacing
    }

    override fun hashCode(): Int {
        var result = stackTitle.hashCode()
        result = 31 * result + showTertiary.hashCode()
        result = 31 * result + heroDensity.hashCode()
        result = 31 * result + supportingCount
        result = 31 * result + contentPadding.hashCode()
        result = 31 * result + slotSpacing.hashCode()
        result = 31 * result + primaryTextSize.hashCode()
        result = 31 * result + ringSize.hashCode()
        result = 31 * result + barMaxHeight.hashCode()
        result = 31 * result + barSpacing.hashCode()
        return result
    }

    override fun toString(): String =
        "TrackSlots(stackTitle=$stackTitle, showTertiary=$showTertiary, " +
            "heroDensity=$heroDensity, supportingCount=$supportingCount, " +
            "contentPadding=$contentPadding, slotSpacing=$slotSpacing, " +
            "primaryTextSize=$primaryTextSize, ringSize=$ringSize, " +
            "barMaxHeight=$barMaxHeight, barSpacing=$barSpacing)"

    public companion object {
        /**
         * Resolves the Track layout for a container of [dimensions] at [tiers].
         *
         * [tiers] decides *which* slots appear; [dimensions] then sizes the hero to whatever
         * vertical space the other slots leave behind, so the chart grows with its container
         * instead of snapping to the tier's minimum height.
         *
         * @param tiers Resolved container breakpoints.
         * @param dimensions Actual container size in DP.
         * @return The layout plan for that container.
         */
        public fun from(tiers: SizeTiers, dimensions: Dimensions): TrackSlots {
            val isNarrow = tiers.width == WidthTier.W1
            val isWidest = tiers.width == WidthTier.W4

            // W1 has a single column, so height alone decides how much of the title survives.
            val showTertiary =
                if (isNarrow) tiers.height >= HeightTier.H3
                else tiers.width == WidthTier.W3 || isWidest

            // W1 is never wide enough for a chart.
            val heroDensity =
                if (isNarrow) TrackHeroDensity.NONE
                else
                    when (tiers.height) {
                        HeightTier.H0,
                        HeightTier.H1,
                        HeightTier.H2 -> TrackHeroDensity.NONE
                        HeightTier.H3 -> TrackHeroDensity.COMPACT
                        HeightTier.H4 -> TrackHeroDensity.FULL
                    }

            val supportingCount =
                if (isNarrow) {
                    if (tiers.height >= HeightTier.H4) 1 else 0
                } else if (tiers.height >= HeightTier.H2) {
                    when (tiers.width) {
                        WidthTier.W1,
                        WidthTier.W2 -> 1
                        WidthTier.W3 -> 2
                        WidthTier.W4 -> 3
                    }
                } else {
                    0
                }

            val paddingDp = if (isNarrow || tiers.height <= HeightTier.H1) 16f else 20f
            val spacingDp = if (tiers.height == HeightTier.H2) 12f else 14f
            val ringSizeDp = if (isNarrow) 54f else 56f

            return TrackSlots(
                stackTitle = isNarrow,
                showTertiary = showTertiary,
                heroDensity = heroDensity,
                supportingCount = supportingCount,
                contentPadding = paddingDp.dp,
                slotSpacing = spacingDp.dp,
                primaryTextSize =
                    when {
                        isNarrow -> 18.sp
                        isWidest -> 40.sp
                        else -> 30.sp
                    },
                ringSize = ringSizeDp.dp,
                barMaxHeight =
                    resolveBarMaxHeight(
                        heightDp = dimensions.heightDp.toFloat(),
                        paddingDp = paddingDp,
                        spacingDp = spacingDp,
                        titleHeightDp = stackedTitleHeight(isNarrow, ringSizeDp, showTertiary),
                        heroDensity = heroDensity,
                        hasSupporting = supportingCount > 0,
                    ),
                barSpacing =
                    when (tiers.width) {
                        WidthTier.W1,
                        WidthTier.W2 -> 6.dp
                        WidthTier.W3 -> 8.dp
                        WidthTier.W4 -> 16.dp
                    },
            )
        }

        /** Height the title slot occupies, stacked or in a row. */
        private fun stackedTitleHeight(
            stacked: Boolean,
            ringSizeDp: Float,
            showTertiary: Boolean,
        ): Float =
            if (!stacked) {
                TITLE_ROW_HEIGHT_DP
            } else {
                ringSizeDp +
                    STACKED_LINE_SPACING_DP +
                    STACKED_LINE_HEIGHT_DP +
                    if (showTertiary) STACKED_LINE_SPACING_DP + STACKED_LINE_HEIGHT_DP else 0f
            }

        /**
         * Sizes a full value hero bar to the vertical space the other slots leave behind.
         *
         * Glance cannot measure its container, so the bars cannot simply be told to fill it: the
         * space is worked out here from the container height and given to the chart as an absolute
         * value.
         */
        private fun resolveBarMaxHeight(
            heightDp: Float,
            paddingDp: Float,
            spacingDp: Float,
            titleHeightDp: Float,
            heroDensity: TrackHeroDensity,
            hasSupporting: Boolean,
        ): Dp {
            if (heroDensity == TrackHeroDensity.NONE) return MIN_BAR_MAX_HEIGHT_DP.dp

            var consumed = titleHeightDp + spacingDp
            if (hasSupporting) consumed += spacingDp + SUPPORTING_ROW_HEIGHT_DP
            val heroDp = heightDp - 2 * paddingDp - consumed

            val chrome =
                CHART_LABEL_ROW_HEIGHT_DP +
                    if (heroDensity == TrackHeroDensity.FULL) {
                        CHART_DESCRIPTION_SPACING_DP + CHART_DESCRIPTION_HEIGHT_DP
                    } else {
                        0f
                    }
            return (heroDp - chrome - HERO_SAFETY_MARGIN_DP).coerceAtLeast(MIN_BAR_MAX_HEIGHT_DP).dp
        }
    }
}
