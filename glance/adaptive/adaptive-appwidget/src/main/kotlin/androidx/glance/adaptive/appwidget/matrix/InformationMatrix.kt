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

package androidx.glance.adaptive.appwidget.matrix

import androidx.annotation.RestrictTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.appwidget.components.actionrail.ActionRailDefaults
import androidx.glance.adaptive.appwidget.components.estimateTextWidthDp
import androidx.glance.adaptive.appwidget.components.fitMetric
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.selection.from
import androidx.glance.adaptive.core.selection.HeightTier
import androidx.glance.adaptive.core.selection.SizeTiers
import androidx.glance.adaptive.core.selection.WidthTier
import androidx.glance.adaptive.core.slots.ActionComponent
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.BulbComponent
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.HeroComponent
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.IconGrid
import androidx.glance.adaptive.core.slots.IconTile
import androidx.glance.adaptive.core.slots.ItemList
import androidx.glance.adaptive.core.slots.LeadingComponent
import androidx.glance.adaptive.core.slots.ListItem
import androidx.glance.adaptive.core.slots.PrimaryComponent
import androidx.glance.adaptive.core.slots.SecondaryComponent
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.slots.TrailingComponent

/*
 * The Information Matrix, independent of any rendering backend.
 *
 * A template's selector only declares which components belong in which slots at a size (a
 * [MatrixContent]); the matrix owns everything about the container: padding, spacing, corner
 * radius, and the header's width budget. Each backend's matrix renderer then draws the resolved
 * matrix, so a new template writes one mapping that works on every backend.
 */

/**
 * Largest share of a row header's content width the secondary text may take, so a long secondary
 * text is ellipsized before it squeezes out the primary text.
 */
private const val MATRIX_SECONDARY_MAX_WIDTH_FRACTION = 0.4f

/** Line height of the matrix's text as a multiple of its type size, for height estimates. */
private const val LINE_HEIGHT_RATIO = 1.25f

/** Least padding a card gives up to keep its header slots at their full size. */
private const val MIN_HEADER_PADDING_DP = 8f

/**
 * Smallest diameter of the bulb's circle, so a newly pinned widget the host has not sized yet still
 * draws a valid bulb rather than collapsing.
 */
private const val MIN_BULB_CONTAINER_DP = 48f

/** Defaults every Information Matrix shares, which a card may override. */
internal object MatrixDefaults {

    /** Type size of the header's secondary text. */
    val SecondaryTextSize: TextUnit = 12.sp

    /**
     * Gap between the header's leading, primary and secondary slots.
     *
     * The backends draw it as fixed padding and margins, because a margin cannot be set at runtime
     * before API 31, so a card cannot override it. It is mirrored here for the header's width
     * budget.
     */
    val HeaderSpacing: Dp = 8.dp

    /**
     * Corner radius of the card, `null` for the launcher's own widget radius.
     *
     * The design always follows the system radius, which the layouts use from API 31; below API 31
     * the card is square. A non-`null` radius is applied from API 31 only.
     */
    val CornerRadius: Dp? = null

    /**
     * Edge length of the leading component, from the design's 56 dp progress ring. A stacked header
     * gives it less only in a column too narrow for it, and never more.
     */
    val LeadingSize: Dp = 56.dp

    /**
     * Padding between the widget bounds and the slots: 16 dp for W1 and W2 cards and for every card
     * up to H1, 20 dp for the rest, as the design pads them.
     */
    fun padding(tiers: SizeTiers): Dp =
        if (tiers.width <= WidthTier.W2 || tiers.height <= HeightTier.H1) 16.dp else 20.dp

    /** Vertical gap between the header, hero and action slots. */
    fun slotSpacing(tiers: SizeTiers): Dp =
        when {
            tiers.width == WidthTier.W1 -> 16.dp
            tiers.height == HeightTier.H2 -> 12.dp
            else -> 14.dp
        }

    /** Type size of the primary text, which grows with the width tier. */
    fun primaryTextSize(tiers: SizeTiers): TextUnit =
        when (tiers.width) {
            WidthTier.W1 -> 20.sp
            WidthTier.W4 -> 40.sp
            else -> 30.sp
        }

    /**
     * How much wider than tall the header's trailing component is drawn: the design's trailing
     * button is a 64 dp wide pill as tall as the 56 dp leading icon.
     */
    val TrailingExtraWidth: Dp = 8.dp

    /** Glyph size of an uncontained trailing icon button (e.g. a bare `refresh` icon) at `W2+`. */
    val UncontainedTrailingGlyphSize: Dp = 24.dp

    /** Glyph size of an uncontained trailing icon button inside a narrow `W1` row header. */
    val UncontainedTrailingCompactGlyphSize: Dp = 16.dp

    /**
     * Glyph size of an uncontained trailing icon button: [UncontainedTrailingCompactGlyphSize] in
     * narrow `W1` horizontal headers, [UncontainedTrailingGlyphSize] from `W2` upward.
     */
    fun uncontainedTrailingGlyphSize(tiers: SizeTiers): Dp =
        if (tiers.width == WidthTier.W1) UncontainedTrailingCompactGlyphSize
        else UncontainedTrailingGlyphSize

    /**
     * Touch-target width of an uncontained trailing icon button, centering the 24 dp glyph with 8
     * dp horizontal padding on each side.
     */
    val UncontainedTrailingWidth: Dp = 40.dp

    /**
     * Width of an uncontained trailing icon button: [UncontainedTrailingWidth] at `W2+`, shrinking
     * to [UncontainedTrailingGlyphSize] in narrow `W1` horizontal headers.
     */
    fun uncontainedTrailingWidth(tiers: SizeTiers): Dp =
        if (tiers.width == WidthTier.W1) UncontainedTrailingGlyphSize else UncontainedTrailingWidth

    /**
     * Touch-target height of an uncontained trailing icon button: 40 dp for standard row headers
     * (which already enforce [HeaderRowMinHeight]) and compact headers at `H2+`, shrinking to the
     * 24 dp glyph height only in compact `H0`/`H1` headers.
     */
    fun uncontainedTrailingHeight(tiers: SizeTiers, compactHeader: Boolean = false): Dp =
        if (compactHeader && tiers.height <= HeightTier.H1) UncontainedTrailingGlyphSize
        else UncontainedTrailingWidth

    /** Least height of a header in a row, which the backends declare in its layout. */
    val HeaderRowMinHeight: Dp = 56.dp

    /** Height of the bottom action row, as tall as an icon button's touch target. */
    val ActionRowHeight: Dp = 48.dp

    /** Whether the header stacks: W1 is too narrow for the leading component beside the primary. */
    fun isStacked(tiers: SizeTiers): Boolean = tiers.width == WidthTier.W1
}

/**
 * What a template's selector puts in the matrix at a size: a card or a bulb.
 *
 * The archetype a matrix selector hands to a matrix renderer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface MatrixContent {
    /**
     * Whether any non-blank optional template slot was omitted from this matrix content due to
     * responsive container size compaction.
     */
    public val hasDroppedSlots: Boolean
}

/**
 * Quick actions attached to a [MatrixCardContent] outside the header and bottom action row.
 *
 * Mutually exclusive with [MatrixCardContent.actions] (the bottom supporting row).
 */
internal sealed interface CardQuickActions {
    /** Single floating action button overlaid at the card's bottom-end corner. */
    class Floating(val action: IconButton) : CardQuickActions {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Floating && action == other.action)

        override fun hashCode(): Int = action.hashCode()

        override fun toString(): String = "CardQuickActions.Floating(action=$action)"
    }

    /**
     * Vertical action rail placed full-height at the card's trailing edge on expanded sizes (`W3–W4
     * × H3+`), collapsing to a bottom-end floating action button (`actions[0]`) when shrunk on
     * dynamic backends.
     *
     * Note: dynamic transitions smoothly collapse an expanded `AdaptiveRail` to a floating FAB on
     * shrink; growing a compact `Floating` card back to `W3–W4 × H3+` renders the full rail on the
     * next host size update.
     */
    class AdaptiveRail(actions: List<IconButton>) : CardQuickActions {
        val actions: List<IconButton> = List(actions.size) { i -> actions[i] }

        init {
            require(this.actions.isNotEmpty()) {
                "An adaptive action rail requires at least one action."
            }
        }

        override fun equals(other: Any?): Boolean =
            this === other || (other is AdaptiveRail && actions == other.actions)

        override fun hashCode(): Int = actions.hashCode()

        override fun toString(): String = "CardQuickActions.AdaptiveRail(actions=$actions)"
    }
}

/**
 * Compact-breakpoint adjustments a dynamic renderer (such as RemoteCompose) applies live while the
 * user drags a widget across size tiers, without waiting for a host re-render.
 *
 * Populated by template selectors from their own geometry tokens so the matrix renderer remains
 * template-agnostic.
 *
 * @throws IllegalArgumentException if [compactHeightVerticalPadding] or [compactHeightSlotSpacing]
 *   is negative, or [collapseHeaderTrailingBelowWidthDp] is not positive.
 */
internal class LiveResizeSpec(
    val collapseHeaderInCompactHeight: Boolean = false,
    val collapseLeadingInCompactHeight: Boolean = false,
    val compactHeightVerticalPadding: Dp? = null,
    val compactHeightSlotSpacing: Dp? = null,
    val collapseHeaderHorizontalPaddingInNarrowWidth: Boolean = false,
    val collapseHeaderTrailingBelowWidthDp: Dp? = null,
    val collapseHeaderTrailingInCompactHeight: Boolean = false,
    val compactUncontainedTrailingInCompactHeight: Boolean = false,
) {
    init {
        require(compactHeightVerticalPadding == null || compactHeightVerticalPadding.value >= 0f) {
            "Compact height vertical padding must not be negative, was " +
                "$compactHeightVerticalPadding."
        }
        require(compactHeightSlotSpacing == null || compactHeightSlotSpacing.value >= 0f) {
            "Compact height slot spacing must not be negative, was $compactHeightSlotSpacing."
        }
        require(
            collapseHeaderTrailingBelowWidthDp == null ||
                collapseHeaderTrailingBelowWidthDp.value > 0f
        ) {
            "Collapse header trailing threshold must be positive, was " +
                "$collapseHeaderTrailingBelowWidthDp."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiveResizeSpec) return false
        return collapseHeaderInCompactHeight == other.collapseHeaderInCompactHeight &&
            collapseLeadingInCompactHeight == other.collapseLeadingInCompactHeight &&
            compactHeightVerticalPadding == other.compactHeightVerticalPadding &&
            compactHeightSlotSpacing == other.compactHeightSlotSpacing &&
            collapseHeaderHorizontalPaddingInNarrowWidth ==
                other.collapseHeaderHorizontalPaddingInNarrowWidth &&
            collapseHeaderTrailingBelowWidthDp == other.collapseHeaderTrailingBelowWidthDp &&
            collapseHeaderTrailingInCompactHeight == other.collapseHeaderTrailingInCompactHeight &&
            compactUncontainedTrailingInCompactHeight ==
                other.compactUncontainedTrailingInCompactHeight
    }

    override fun hashCode(): Int {
        var result = collapseHeaderInCompactHeight.hashCode()
        result = 31 * result + collapseLeadingInCompactHeight.hashCode()
        result = 31 * result + (compactHeightVerticalPadding?.hashCode() ?: 0)
        result = 31 * result + (compactHeightSlotSpacing?.hashCode() ?: 0)
        result = 31 * result + collapseHeaderHorizontalPaddingInNarrowWidth.hashCode()
        result = 31 * result + (collapseHeaderTrailingBelowWidthDp?.hashCode() ?: 0)
        result = 31 * result + collapseHeaderTrailingInCompactHeight.hashCode()
        result = 31 * result + compactUncontainedTrailingInCompactHeight.hashCode()
        return result
    }

    override fun toString(): String =
        "LiveResizeSpec(collapseHeaderInCompactHeight=$collapseHeaderInCompactHeight, " +
            "collapseLeadingInCompactHeight=$collapseLeadingInCompactHeight, " +
            "compactHeightVerticalPadding=$compactHeightVerticalPadding, " +
            "compactHeightSlotSpacing=$compactHeightSlotSpacing, " +
            "collapseHeaderHorizontalPaddingInNarrowWidth=" +
            "$collapseHeaderHorizontalPaddingInNarrowWidth, " +
            "collapseHeaderTrailingBelowWidthDp=$collapseHeaderTrailingBelowWidthDp, " +
            "collapseHeaderTrailingInCompactHeight=$collapseHeaderTrailingInCompactHeight, " +
            "compactUncontainedTrailingInCompactHeight=" +
            "$compactUncontainedTrailingInCompactHeight)"
}

/**
 * The main card: header, hero and action slots, top to bottom.
 *
 * Every slot is optional; which ones a template needs is the template's rule, not the matrix's. An
 * absent primary component leaves its position empty, so the secondary one does not move. A
 * selector fills a slot only when the size has room for it and the template has data for it.
 *
 * A card with every slot empty is valid and draws only its background, so a template with no data
 * at a size still renders as an empty card rather than failing.
 *
 * The [actions] list forms the bottom row, leading to trailing, with trailing `null` entries
 * trimmed. An absent action before a present one leaves its position empty and keeps its place, so
 * the actions after it do not move. With no action at all, the row is hidden.
 *
 * A compact card, such as the design's `Linear` strip on the lock screen, is a header-only card
 * with no padding and a smaller [leadingSize] and [primaryTextSize]. A card that sets
 * [compactHeader] (or [leadingSize]) lays its header out as a label, with the secondary text under
 * the primary one (see [ResolvedMatrixCard.compactHeader]).
 *
 * @property leading Component for the leading slot, or `null` to hide it with the gap after it.
 * @property primary Component for the primary slot, or `null` to leave it empty.
 * @property secondary Component for the secondary slot, or `null` to hide it. In a row header, its
 *   width is deducted from [primary]'s width budget.
 * @property trailing Component for the header's trailing slot, or `null` to hide it. A stacked
 *   header has no room for it and hides it.
 * @property hero Component for the hero slot, or `null` to hide it.
 * @property actions Components for the bottom action row, leading to trailing (mutually exclusive
 *   with [quickActions]).
 * @property padding Padding between the widget bounds and the slots, or `null` for
 *   [MatrixDefaults.padding].
 * @property slotSpacing Vertical gap between the slots, or `null` for [MatrixDefaults.slotSpacing].
 * @property cornerRadius Corner radius of the card, or `null` for [MatrixDefaults.CornerRadius].
 * @property leadingSize Edge length of the leading component, or `null` for
 *   [MatrixDefaults.LeadingSize]. A stacked header still shrinks it to fit its column.
 * @property primaryTextSize Type size of the primary text, or `null` for
 *   [MatrixDefaults.primaryTextSize].
 * @property compactHeader Whether to lay out the header as a compact label (no minimum height of
 *   its own; secondary text stacked under primary in a row). Defaults to `leadingSize != null`.
 * @property headerHorizontalPadding Extra horizontal inset applied to a row header (for example, to
 *   align a text-only header with inset list items beneath it).
 * @property stackHeaderInNarrowWidth Whether a narrow [WidthTier.W1] card stacks its header slots
 *   vertically instead of keeping a compact horizontal header row (defaults to `true`).
 * @property quickActions Optional trailing action rail or bottom-end floating action button
 *   (mutually exclusive with bottom [actions]).
 * @property liveResize Optional dynamic-renderer live-resize adjustments, or `null` when static.
 * @throws IllegalArgumentException if [leadingSize] or [headerHorizontalPadding] is negative,
 *   [primaryTextSize] is not positive, [compactHeader] is `false` while [leadingSize] is non-null,
 *   or both [actions] and [quickActions] are populated.
 */
internal class MatrixCardContent(
    val leading: LeadingComponent? = null,
    val primary: PrimaryComponent? = null,
    val secondary: SecondaryComponent? = null,
    val trailing: TrailingComponent? = null,
    val hero: HeroComponent? = null,
    actions: List<ActionComponent?> = emptyList(),
    val padding: Dp? = null,
    val slotSpacing: Dp? = null,
    val cornerRadius: Dp? = null,
    val leadingSize: Dp? = null,
    val primaryTextSize: TextUnit? = null,
    val compactHeader: Boolean = leadingSize != null,
    val headerHorizontalPadding: Dp = 0.dp,
    val stackHeaderInNarrowWidth: Boolean = true,
    val quickActions: CardQuickActions? = null,
    val liveResize: LiveResizeSpec? = null,
    override val hasDroppedSlots: Boolean = false,
) : MatrixContent {
    /** The action positions of the bottom row, leading to trailing, up to the last non-null one. */
    val actions: List<ActionComponent?> = actions.dropLastWhile { it == null }

    init {
        require(leadingSize == null || leadingSize.value >= 0f) {
            "Leading size must not be negative, was $leadingSize."
        }
        require(primaryTextSize == null || primaryTextSize.value > 0f) {
            "Primary text size must be positive, was $primaryTextSize."
        }
        require(headerHorizontalPadding.value >= 0f) {
            "Header horizontal padding must not be negative, was $headerHorizontalPadding."
        }
        require(compactHeader || leadingSize == null) {
            "A card with an explicit leadingSize must use compactHeader."
        }
        require(quickActions == null || this.actions.isEmpty()) {
            "A card cannot combine bottom actions with quickActions."
        }
    }
}

/**
 * The bulb: a single component that is the whole widget, centered in a circle, with no card.
 *
 * @property component Component for the bulb slot.
 * @property hasDroppedSlots Whether optional template slots were omitted to fit the bulb.
 */
internal class MatrixBulbContent(
    val component: BulbComponent,
    override val hasDroppedSlots: Boolean = false,
) : MatrixContent

/**
 * A [MatrixCardContent] laid out for its container: every size and text a backend needs to draw it.
 *
 * Text is already what the surface shows: a sensitive text is replaced on the lock screen.
 *
 * @property tiers Size tiers the card was laid out at, for the components that adapt to them, such
 *   as a graph's bar spacing.
 * @property padding Padding between the widget bounds and the slots.
 * @property slotSpacing Vertical gap between the slots.
 * @property cornerRadius Corner radius of the card, or `null` for the system radius.
 * @property stacked Whether the header slots run down rather than across.
 * @property leading Component in the leading slot, or `null`.
 * @property leadingSize Edge length the leading component is drawn at.
 * @property compactHeader Whether the header is laid out as a label rather than a headline (see
 *   [MatrixCardContent.compactHeader]): the header has no minimum height of its own, so it is as
 *   tall as its tallest slot, and in a row the secondary text runs under the primary one rather
 *   than beside it.
 * @property primary Component in the primary slot, already fitted to its width budget, or `null`
 *   when empty.
 * @property primaryTextSize Type size of [primary].
 * @property secondary Component in the secondary slot, or `null` when hidden.
 * @property secondaryMaxWidth Widest [secondary] may be drawn before it is ellipsized, or `null`
 *   when it has the whole content width to itself.
 * @property trailing Component in the header's trailing slot, or `null` when hidden.
 * @property trailingSize Edge length [trailing] is drawn at: [MatrixDefaults.LeadingSize] (clamped
 *   to the content height) when contained, or [MatrixDefaults.uncontainedTrailingHeight] when
 *   uncontained.
 * @property hero Component in the hero slot, or `null` when hidden.
 * @property heroHeight Height the hero is drawn in, estimated from the container and the slots
 *   around it, for a hero that lays itself out by it, such as an icon grid capping its rows; `null`
 *   when there is no hero or the container has no size.
 * @property actions The action positions of the bottom row; empty when hidden.
 * @property headerHorizontalPadding Extra horizontal inset applied to a row header.
 * @property headerRowMinHeight Minimum height of a standard row header, clamped to the card's
 *   content height so short containers do not overflow.
 * @property contentHeight Inner content height of the card (`containerHeight - 2 * padding`), or
 *   `null` when the container has no size, used by full-height side slots such as the action rail.
 * @property quickActions Trailing action rail or floating FAB overlay with sensitive descriptions
 *   already replaced on the lock screen, or `null` when hidden.
 * @property liveResize Dynamic-renderer live-resize adjustments, or `null` when static.
 */
internal class ResolvedMatrixCard(
    val tiers: SizeTiers,
    val padding: Dp,
    val slotSpacing: Dp,
    val cornerRadius: Dp?,
    val stacked: Boolean,
    val leading: LeadingComponent?,
    val leadingSize: Dp,
    val compactHeader: Boolean,
    val primary: PrimaryComponent?,
    val primaryTextSize: TextUnit,
    val secondary: SecondaryComponent?,
    val secondaryMaxWidth: Dp?,
    val trailing: TrailingComponent?,
    val trailingSize: Dp,
    val hero: HeroComponent?,
    val heroHeight: Dp?,
    val actions: List<ActionComponent?>,
    val headerHorizontalPadding: Dp = 0.dp,
    val headerRowMinHeight: Dp = MatrixDefaults.HeaderRowMinHeight,
    val contentHeight: Dp? = null,
    val quickActions: CardQuickActions? = null,
    val liveResize: LiveResizeSpec? = null,
)

/**
 * A [MatrixBulbContent] laid out for its container.
 *
 * @property component Component in the bulb slot.
 * @property containerSize Diameter of the circle the component sits in.
 */
internal class ResolvedMatrixBulb(val component: BulbComponent, val containerSize: Dp)

/**
 * Lays [content] out for its placement on [surface].
 *
 * The tiers decide the defaults; the container's size decides the header's budget: a stacked header
 * (when [MatrixCardContent.stackHeaderInNarrowWidth] is `true` at [WidthTier.W1]) places its
 * leading component alone across the content width, and a row header spans the content height, so a
 * container tighter than its header target along its binding axis gives up padding first (down to 8
 * dp) and only then shrinks the leading, contained trailing, and row min-height slots. When a
 * full-height [CardQuickActions.AdaptiveRail] is present, its width and spacing are deducted from
 * the main column's horizontal budget. In a row, the primary text gets what the leading component,
 * the secondary text and the gaps leave, and is shortened to fit it; the secondary text takes at
 * most 40% of the row, and is ellipsized past it. A contained trailing component takes its resolved
 * size plus [MatrixDefaults.TrailingExtraWidth] from the row, while an uncontained trailing button
 * takes [MatrixDefaults.uncontainedTrailingWidth]; a stacked header drops it. On the lock screen,
 * sensitive text and action descriptions are replaced first.
 *
 * @param content The components to lay out.
 * @param surface The target surface, carrying the container size as
 *   [androidx.glance.adaptive.core.selection.GlanceSurface.dimensions].
 * @param fontScale The user's font scale, by which text lines are taller in height estimates.
 * @return The laid out card.
 */
internal fun resolve(
    content: MatrixCardContent,
    surface: AppWidgetSurface,
    fontScale: Float = 1f,
): ResolvedMatrixCard {
    val dimensions = surface.dimensions
    val tiers = SizeTiers.from(dimensions, surface)
    val widthDp = dimensions.widthDp.toFloat()
    val heightDp = dimensions.heightDp.toFloat()
    val stacked = content.stackHeaderInNarrowWidth && MatrixDefaults.isStacked(tiers)
    val spacingDp = (content.slotSpacing ?: MatrixDefaults.slotSpacing(tiers)).value
    val leading = content.leading
    // TODO(b/578041570): Keep both the value and its sensitive replacement and let the renderer
    //  pick, so a RemoteCompose renderer can redact at runtime. fitTo must then keep the
    //  replacement too, and fit it separately.
    val locked = surface == AppWidgetSurface.MOBILE_LOCK_SCREEN
    val leadingDefaultDp = (content.leadingSize ?: MatrixDefaults.LeadingSize).value
    val secondary = content.secondary?.shownOn(locked)
    // A compact header runs the secondary text under the primary one rather than beside it.
    val compactHeader = content.compactHeader
    // A stacked header has no room beside its slots for a trailing component.
    val trailing = content.trailing?.takeUnless { stacked }?.shownOn(locked)
    val uncontainedTrailing = (trailing as? IconButton)?.contained == false
    val hasHeader =
        leading != null || content.primary != null || secondary != null || trailing != null

    var paddingDp = (content.padding ?: MatrixDefaults.padding(tiers)).value
    if (content.padding == null) {
        if (leading != null && stacked && widthDp > 0f) {
            paddingDp =
                paddingDp.coerceAtMost(
                    ((widthDp - leadingDefaultDp) / 2f).coerceAtLeast(MIN_HEADER_PADDING_DP)
                )
        }
        val headerTargetHeightDp =
            maxOf(
                if (leading != null) leadingDefaultDp else 0f,
                if (trailing != null && !uncontainedTrailing) MatrixDefaults.LeadingSize.value
                else 0f,
                if (!stacked && !compactHeader && hasHeader) MatrixDefaults.HeaderRowMinHeight.value
                else 0f,
            )
        if (headerTargetHeightDp > 0f && heightDp > 0f) {
            paddingDp =
                paddingDp.coerceAtMost(
                    ((heightDp - headerTargetHeightDp) / 2f).coerceAtLeast(MIN_HEADER_PADDING_DP)
                )
        }
    }
    val railWidthDeductionDp =
        if (content.quickActions is CardQuickActions.AdaptiveRail) {
            ActionRailDefaults.Width.value + ActionRailDefaults.Spacing.value
        } else {
            0f
        }
    val contentWidthDp = (widthDp - 2 * paddingDp - railWidthDeductionDp).coerceAtLeast(0f)
    val contentHeightDp = (heightDp - 2 * paddingDp).coerceAtLeast(0f)
    val leadingSizeDp =
        if (leading == null) {
            0f
        } else {
            var capped = leadingDefaultDp
            if (stacked && widthDp > 0f) capped = capped.coerceAtMost(contentWidthDp)
            if (heightDp > 0f) capped = capped.coerceAtMost(contentHeightDp)
            capped.coerceAtLeast(0f)
        }
    val headerRowMinHeightDp =
        if (heightDp > 0f) {
            MatrixDefaults.HeaderRowMinHeight.value.coerceAtMost(contentHeightDp)
        } else {
            MatrixDefaults.HeaderRowMinHeight.value
        }

    val headerHorizontalPadding = if (stacked) 0.dp else content.headerHorizontalPadding
    val rowHeaderWidthDp = (contentWidthDp - 2 * headerHorizontalPadding.value).coerceAtLeast(0f)
    val secondaryBeside = !stacked && !compactHeader
    // A stacked secondary text has a line to itself, and an unsized container has no width to
    // share.
    val secondaryBesideMaxWidthDp =
        if (secondaryBeside && secondary != null && widthDp > 0f) {
            (rowHeaderWidthDp * MATRIX_SECONDARY_MAX_WIDTH_FRACTION).coerceAtLeast(0f)
        } else {
            null
        }
    val primaryTextSize = content.primaryTextSize ?: MatrixDefaults.primaryTextSize(tiers)
    val resolvedTrailingSizeDp =
        when {
            trailing == null -> 0f
            uncontainedTrailing ->
                MatrixDefaults.uncontainedTrailingHeight(tiers, compactHeader).value
            heightDp > 0f -> MatrixDefaults.LeadingSize.value.coerceAtMost(contentHeightDp)
            else -> MatrixDefaults.LeadingSize.value
        }
    var primaryMaxWidthDp = if (stacked) contentWidthDp else rowHeaderWidthDp
    if (!stacked) {
        if (leading != null) primaryMaxWidthDp -= leadingSizeDp + MatrixDefaults.HeaderSpacing.value
        if (trailing != null) {
            primaryMaxWidthDp -=
                if (uncontainedTrailing) {
                    MatrixDefaults.uncontainedTrailingWidth(tiers).value +
                        MatrixDefaults.HeaderSpacing.value
                } else {
                    resolvedTrailingSizeDp +
                        MatrixDefaults.TrailingExtraWidth.value +
                        MatrixDefaults.HeaderSpacing.value
                }
        }
        secondary
            ?.takeIf { secondaryBeside }
            ?.let {
                val secondaryWidthDp = it.estimatedWidthDp()
                primaryMaxWidthDp -=
                    MatrixDefaults.HeaderSpacing.value +
                        secondaryWidthDp.coerceAtMost(secondaryBesideMaxWidthDp ?: secondaryWidthDp)
            }
    }
    primaryMaxWidthDp = primaryMaxWidthDp.coerceAtLeast(0f)
    // Under the primary text, the secondary text has the primary text's width.
    val secondaryMaxWidthDp =
        if (compactHeader && !stacked && secondary != null && widthDp > 0f) {
            primaryMaxWidthDp
        } else {
            secondaryBesideMaxWidthDp
        }

    val actions = List(content.actions.size) { i -> content.actions[i]?.shownOn(locked) }
    val quickActions = content.quickActions?.redactedIf(locked)
    val resolvedContentHeightDp = if (heightDp > 0f) contentHeightDp else null
    val heroHeightDp =
        if (content.hero != null && resolvedContentHeightDp != null) {
            var available = resolvedContentHeightDp
            if (hasHeader) {
                available -=
                    spacingDp +
                        estimatedHeaderHeightDp(
                            stacked = stacked,
                            compact = compactHeader,
                            headerRowMinHeightDp = headerRowMinHeightDp,
                            leadingSizeDp = if (leading != null) leadingSizeDp else 0f,
                            trailingSizeDp = resolvedTrailingSizeDp,
                            primaryTextSizeSp =
                                if (content.primary != null) primaryTextSize.value else 0f,
                            hasSecondary = secondary != null,
                            fontScale = fontScale,
                        )
            }
            if (actions.indices.any { actions[it] != null }) {
                available -= spacingDp + MatrixDefaults.ActionRowHeight.value
            }
            available.coerceAtLeast(0f)
        } else {
            null
        }

    return ResolvedMatrixCard(
        tiers = tiers,
        padding = paddingDp.dp,
        slotSpacing = spacingDp.dp,
        cornerRadius = content.cornerRadius ?: MatrixDefaults.CornerRadius,
        stacked = stacked,
        leading = leading,
        leadingSize = leadingSizeDp.dp,
        compactHeader = compactHeader,
        primary = content.primary?.shownOn(locked)?.fitTo(primaryMaxWidthDp, primaryTextSize),
        primaryTextSize = primaryTextSize,
        secondary = secondary,
        secondaryMaxWidth = secondaryMaxWidthDp?.dp,
        trailing = trailing,
        trailingSize = resolvedTrailingSizeDp.dp,
        hero = content.hero?.shownOn(locked),
        heroHeight = heroHeightDp?.dp,
        actions = actions,
        headerHorizontalPadding = headerHorizontalPadding,
        headerRowMinHeight = headerRowMinHeightDp.dp,
        contentHeight = resolvedContentHeightDp?.dp,
        quickActions = quickActions,
        liveResize = content.liveResize,
    )
}

/** This component shortened to fit [maxWidthDp] at [textSize]. */
private fun PrimaryComponent.fitTo(
    maxWidthDp: Float,
    textSize: TextUnit,
): PrimaryComponent =
    when (this) {
        is TemplateText ->
            TemplateText(fitMetric(value, maxWidthDp, textSize), isStruckThrough = isStruckThrough)
    }

/** This component as the surface shows it: on the lock screen, sensitive text is replaced. */
private fun PrimaryComponent.shownOn(locked: Boolean): PrimaryComponent =
    when (this) {
        is TemplateText -> redactedIf(locked)
    }

/** This component as the surface shows it: on the lock screen, sensitive text is replaced. */
private fun SecondaryComponent.shownOn(locked: Boolean): SecondaryComponent =
    when (this) {
        is TemplateText -> redactedIf(locked)
    }

/** This component as the surface shows it: on the lock screen, sensitive text is replaced. */
private fun ActionComponent.shownOn(locked: Boolean): ActionComponent =
    when (this) {
        is TemplateText -> redactedIf(locked)
        is Badge -> this
        is IconButton -> redactedIf(locked)
    }

/** This component as the surface shows it: on the lock screen, sensitive text is replaced. */
private fun TrailingComponent.shownOn(locked: Boolean): TrailingComponent =
    when (this) {
        is IconButton -> redactedIf(locked)
    }

/** This component as the surface shows it: on the lock screen, sensitive text is replaced. */
private fun HeroComponent.shownOn(locked: Boolean): HeroComponent =
    when (this) {
        is Graph -> this
        is IconGrid -> redactedIf(locked)
        is ItemList -> redactedIf(locked)
    }

/** This text, or its sensitive replacement (without strikethrough) when [locked]. */
private fun TemplateText.redactedIf(locked: Boolean): TemplateText {
    val replacement = sensitiveReplacement
    return if (locked && replacement != null) {
        TemplateText(replacement)
    } else {
        this
    }
}

/** This button, described by its sensitive replacement when [locked]. */
private fun IconButton.redactedIf(locked: Boolean): IconButton {
    val replacement = sensitiveReplacement
    return if (locked && replacement != null) {
        IconButton(icon, replacement, action, contained = contained)
    } else {
        this
    }
}

/** Quick actions, with sensitive button descriptions replaced when [locked]. */
private fun CardQuickActions.redactedIf(locked: Boolean): CardQuickActions {
    if (!locked) return this
    return when (this) {
        is CardQuickActions.Floating -> {
            val shown = action.redactedIf(locked = true)
            if (shown === action) this else CardQuickActions.Floating(shown)
        }
        is CardQuickActions.AdaptiveRail -> {
            var redacted: MutableList<IconButton>? = null
            for (i in actions.indices) {
                val button = actions[i]
                val shown = button.redactedIf(locked = true)
                if (shown !== button) {
                    if (redacted == null) {
                        redacted = ArrayList(actions.size)
                        for (j in 0 until i) {
                            redacted.add(actions[j])
                        }
                    }
                    redacted.add(shown)
                } else {
                    redacted?.add(button)
                }
            }
            if (redacted != null) CardQuickActions.AdaptiveRail(redacted) else this
        }
    }
}

/** This grid, with every sensitive tile described by its replacement when [locked]. */
private fun IconGrid.redactedIf(locked: Boolean): IconGrid =
    if (
        locked &&
            tiles.indices.any { i ->
                tiles[i].sensitiveReplacement != null ||
                    tiles[i].subtitle?.sensitiveReplacement != null
            }
    ) {
        IconGrid(List(tiles.size) { i -> tiles[i].redacted() }, columns, rows, showLabels)
    } else {
        this
    }

/** This tile, with its label and its subtitle replaced by their sensitive replacements. */
private fun IconTile.redacted(): IconTile {
    val shownSubtitle = subtitle?.redactedIf(locked = true)
    if (sensitiveReplacement == null && shownSubtitle === subtitle) return this
    return IconTile(icon, sensitiveReplacement ?: label, action, subtitle = shownSubtitle)
}

/** This list, with every sensitive item field replaced when [locked]. */
private fun ItemList.redactedIf(locked: Boolean): ItemList {
    if (!locked) return this
    var redactedItems: MutableList<ListItem>? = null
    for (i in items.indices) {
        val item = items[i]
        val shown = item.redacted()
        if (shown !== item) {
            if (redactedItems == null) {
                redactedItems = ArrayList(items.size)
                for (j in 0 until i) {
                    redactedItems.add(items[j])
                }
            }
            redactedItems.add(shown)
        } else {
            redactedItems?.add(item)
        }
    }
    return if (redactedItems != null) {
        ItemList(
            items = redactedItems,
            isScrollable = isScrollable,
            showTertiaryText = showTertiaryText,
            layoutMode = layoutMode,
        )
    } else {
        this
    }
}

private fun TemplateText.withoutStrikethrough(): TemplateText =
    if (isStruckThrough) TemplateText(value, sensitiveReplacement = sensitiveReplacement) else this

private fun ListItem.redacted(): ListItem =
    when (this) {
        is ListItem.Compact -> {
            val shownTitle = title.redactedIf(locked = true)
            val shownSubtitle = subtitle?.redactedIf(locked = true)
            val shownTertiary = tertiaryText?.redactedIf(locked = true)
            if (
                shownTitle === title && shownSubtitle === subtitle && shownTertiary === tertiaryText
            ) {
                this
            } else {
                // Drop strikethrough across the redacted row so item-level strikethrough on an
                // unredacted sibling field (e.g. a non-sensitive time range) doesn't leak
                // declined/completed state.
                ListItem.Compact(
                    title = shownTitle.withoutStrikethrough(),
                    subtitle = shownSubtitle?.withoutStrikethrough(),
                    tertiaryText = shownTertiary?.withoutStrikethrough(),
                    tertiaryIcon = tertiaryIcon,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    trailingChip = trailingChip,
                    action = action,
                    style = style,
                )
            }
        }
        is ListItem.Checkbox -> {
            val shownTitle = title.redactedIf(locked = true)
            // Reset isChecked when redacted so completion state isn't leaked via icon/TalkBack or
            // left visually inconsistent with the non-struck replacement text.
            if (shownTitle === title) {
                this
            } else {
                ListItem.Checkbox(shownTitle, isChecked = false, action = action)
            }
        }
        is ListItem.DetailCard -> {
            val shownTitle = title.redactedIf(locked = true)
            val shownBody = body?.redactedIf(locked = true)
            var redactedChecklist: MutableList<ListItem.DetailCard.ChecklistEntry>? = null
            for (i in checklistItems.indices) {
                val entry = checklistItems[i]
                val shownText = entry.text.redactedIf(locked = true)
                if (shownText !== entry.text) {
                    if (redactedChecklist == null) {
                        redactedChecklist = ArrayList(checklistItems.size)
                        for (j in 0 until i) {
                            redactedChecklist.add(checklistItems[j])
                        }
                    }
                    // Reset isChecked when redacted so completion state isn't leaked or left
                    // inconsistent with the non-struck replacement text.
                    redactedChecklist.add(
                        ListItem.DetailCard.ChecklistEntry(shownText, isChecked = false)
                    )
                } else {
                    redactedChecklist?.add(entry)
                }
            }
            if (shownTitle === title && shownBody === body && redactedChecklist == null) {
                this
            } else {
                ListItem.DetailCard(
                    title = shownTitle,
                    body = shownBody,
                    checklistItems = redactedChecklist ?: checklistItems,
                    action = action,
                    style = style,
                )
            }
        }
        is ListItem.Divider -> {
            val shownTitle = title.redactedIf(locked = true)
            if (shownTitle === title) this else ListItem.Divider(shownTitle)
        }
        // ListItem is not sealed, so new item types can be added without breaking callers; this
        // is only reachable if one is added without being handled here.
        else -> error("Unsupported ListItem type: ${this::class.java.name}")
    }

/** Height of a line of text [sizeSp] large, in DP, at [fontScale]. */
private fun lineHeightDp(sizeSp: Float, fontScale: Float): Float =
    sizeSp * fontScale * LINE_HEIGHT_RATIO

/**
 * Height of a header, in DP, estimated from its slots: a stacked header runs them down, a compact
 * one is as tall as its tallest slot with the secondary text under the primary one, and a row is at
 * least [MatrixDefaults.HeaderRowMinHeight].
 */
private fun estimatedHeaderHeightDp(
    stacked: Boolean,
    compact: Boolean,
    headerRowMinHeightDp: Float,
    leadingSizeDp: Float,
    trailingSizeDp: Float,
    primaryTextSizeSp: Float,
    hasSecondary: Boolean,
    fontScale: Float,
): Float {
    val primaryDp = if (primaryTextSizeSp > 0f) lineHeightDp(primaryTextSizeSp, fontScale) else 0f
    val secondaryDp =
        if (hasSecondary) lineHeightDp(MatrixDefaults.SecondaryTextSize.value, fontScale) else 0f
    return when {
        stacked -> {
            val leadingDp =
                if (leadingSizeDp > 0f) leadingSizeDp + MatrixDefaults.HeaderSpacing.value else 0f
            leadingDp + primaryDp + secondaryDp
        }
        compact -> maxOf(leadingSizeDp, trailingSizeDp, primaryDp + secondaryDp)
        else ->
            maxOf(
                headerRowMinHeightDp,
                leadingSizeDp,
                trailingSizeDp,
                primaryDp,
                secondaryDp,
            )
    }
}

/** Width this component takes beside the primary slot, in DP. */
private fun SecondaryComponent.estimatedWidthDp(): Float =
    when (this) {
        is TemplateText -> estimateTextWidthDp(value, MatrixDefaults.SecondaryTextSize.value)
    }

/**
 * Lays [content] out for its placement on [surface]: the circle spans the container's shorter side.
 * On the lock screen, a sensitive value is replaced first.
 *
 * @param content The component to lay out.
 * @param surface The target surface, carrying the container size as
 *   [androidx.glance.adaptive.core.selection.GlanceSurface.dimensions].
 * @return The laid out bulb.
 */
internal fun resolve(
    content: MatrixBulbContent,
    surface: AppWidgetSurface,
): ResolvedMatrixBulb {
    // TODO(b/578041570): As for the card, keep both values for a runtime-redacting renderer.
    val locked = surface == AppWidgetSurface.MOBILE_LOCK_SCREEN
    val dimensions = surface.dimensions
    val containerDp =
        minOf(dimensions.widthDp, dimensions.heightDp)
            .toFloat()
            .coerceAtLeast(MIN_BULB_CONTAINER_DP)
    return ResolvedMatrixBulb(content.component.shownOn(locked), containerDp.dp)
}

/** This component as the surface shows it: on the lock screen, a sensitive value is replaced. */
private fun BulbComponent.shownOn(locked: Boolean): BulbComponent =
    when (this) {
        is ContainedRing -> {
            val replacement = sensitiveReplacement
            if (locked && replacement != null) ContainedRing(progress, replacement, icon) else this
        }
        is IconButton -> redactedIf(locked)
    }
