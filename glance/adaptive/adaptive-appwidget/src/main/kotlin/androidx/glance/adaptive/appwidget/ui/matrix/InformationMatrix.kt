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

package androidx.glance.adaptive.appwidget.ui.matrix

import androidx.annotation.RestrictTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.appwidget.ui.components.estimateTextWidthDp
import androidx.glance.adaptive.appwidget.ui.components.fitMetric
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.ui.selection.from
import androidx.glance.adaptive.core.ui.selection.HeightTier
import androidx.glance.adaptive.core.ui.selection.SizeTiers
import androidx.glance.adaptive.core.ui.selection.WidthTier

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

/** Least padding a stacked card gives up to keep its leading component at its full size. */
private const val MIN_STACKED_PADDING_DP = 8f

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

    /** Whether the header stacks: W1 is too narrow for the leading component beside the primary. */
    fun isStacked(tiers: SizeTiers): Boolean = tiers.width == WidthTier.W1
}

/**
 * What a template's selector puts in the matrix at a size: a card or a bulb.
 *
 * The archetype a matrix selector hands to a matrix renderer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public sealed interface MatrixContent

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
 * @property leading Component for the leading slot, or `null` to hide it with the gap after it.
 * @property primary Component for the primary slot, or `null` to leave it empty.
 * @property secondary Component for the secondary slot, or `null` to hide it.
 * @property hero Component for the hero slot, or `null` to hide it.
 * @property actions Components for the bottom action row, leading to trailing.
 * @property padding Padding between the widget bounds and the slots, or `null` for
 *   [MatrixDefaults.padding].
 * @property slotSpacing Vertical gap between the slots, or `null` for [MatrixDefaults.slotSpacing].
 * @property cornerRadius Corner radius of the card, or `null` for [MatrixDefaults.CornerRadius].
 */
internal class MatrixCardContent(
    val leading: LeadingComponent? = null,
    val primary: PrimaryComponent? = null,
    val secondary: SecondaryComponent? = null,
    val hero: HeroComponent? = null,
    actions: List<ActionComponent?> = emptyList(),
    val padding: Dp? = null,
    val slotSpacing: Dp? = null,
    val cornerRadius: Dp? = null,
) : MatrixContent {

    /** The action positions of the bottom row, leading to trailing, up to the last non-null one. */
    val actions: List<ActionComponent?> = actions.dropLastWhile { it == null }
}

/**
 * The bulb: a single component that is the whole widget, centered in a circle, with no card.
 *
 * @property component Component for the bulb slot.
 */
internal class MatrixBulbContent(val component: BulbComponent) : MatrixContent

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
 * @property primary Component in the primary slot, already fitted to its width budget, or `null`
 *   when empty.
 * @property primaryTextSize Type size of [primary].
 * @property secondary Component in the secondary slot, or `null` when hidden.
 * @property secondaryMaxWidth Widest [secondary] may be drawn before it is ellipsized, or `null`
 *   when it has the whole content width to itself.
 * @property hero Component in the hero slot, or `null` when hidden.
 * @property actions The action positions of the bottom row; empty when hidden.
 */
internal class ResolvedMatrixCard(
    val tiers: SizeTiers,
    val padding: Dp,
    val slotSpacing: Dp,
    val cornerRadius: Dp?,
    val stacked: Boolean,
    val leading: LeadingComponent?,
    val leadingSize: Dp,
    val primary: PrimaryComponent?,
    val primaryTextSize: TextUnit,
    val secondary: SecondaryComponent?,
    val secondaryMaxWidth: Dp?,
    val hero: HeroComponent?,
    val actions: List<ActionComponent?>,
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
 * The tiers decide the defaults; the container's width decides the header's horizontal budget: a
 * stacked header's leading component is alone across the content width, so a column narrower than
 * it gives up padding first (down to 8 dp) and only then shrinks the leading component. In a row,
 * the primary text gets what the leading component, the secondary text and the gaps leave, and is
 * shortened to fit it; the secondary text takes at most 40% of the row, and is ellipsized past it.
 * On the lock screen, sensitive text is replaced first.
 *
 * @param content The components to lay out.
 * @param surface The target surface, carrying the container size as
 *   [androidx.glance.adaptive.core.ui.selection.GlanceSurface.dimensions].
 * @return The laid out card.
 */
internal fun resolve(
    content: MatrixCardContent,
    surface: AppWidgetSurface,
): ResolvedMatrixCard {
    val dimensions = surface.dimensions
    val tiers = SizeTiers.from(dimensions, surface)
    val widthDp = dimensions.widthDp.toFloat()
    val stacked = MatrixDefaults.isStacked(tiers)
    val spacingDp = (content.slotSpacing ?: MatrixDefaults.slotSpacing(tiers)).value
    val leading = content.leading
    // TODO(b/578041570): Keep both the value and its sensitive replacement and let the renderer
    //  pick, so a RemoteCompose renderer can redact at runtime. fitTo must then keep the
    //  replacement too, and fit it separately.
    val locked = surface == AppWidgetSurface.MOBILE_LOCK_SCREEN
    val leadingDefaultDp = MatrixDefaults.LeadingSize.value

    var paddingDp = (content.padding ?: MatrixDefaults.padding(tiers)).value
    if (stacked && leading != null && widthDp > 0f && content.padding == null) {
        paddingDp =
            paddingDp.coerceAtMost(
                ((widthDp - leadingDefaultDp) / 2f).coerceAtLeast(MIN_STACKED_PADDING_DP)
            )
    }
    val contentWidthDp = widthDp - 2 * paddingDp
    val leadingSizeDp =
        when {
            leading == null -> 0f
            stacked && widthDp > 0f ->
                leadingDefaultDp.coerceAtMost(contentWidthDp).coerceAtLeast(0f)
            else -> leadingDefaultDp
        }

    val secondary = content.secondary?.shownOn(locked)
    // A stacked secondary text has a line to itself, and an unsized container has no width to
    // share.
    val secondaryMaxWidthDp =
        if (!stacked && secondary != null && widthDp > 0f) {
            (contentWidthDp * MATRIX_SECONDARY_MAX_WIDTH_FRACTION).coerceAtLeast(0f)
        } else {
            null
        }
    val primaryTextSize = MatrixDefaults.primaryTextSize(tiers)
    var primaryMaxWidthDp = contentWidthDp
    if (!stacked) {
        if (leading != null) primaryMaxWidthDp -= leadingSizeDp + MatrixDefaults.HeaderSpacing.value
        secondary?.let {
            val secondaryWidthDp = it.estimatedWidthDp()
            primaryMaxWidthDp -=
                MatrixDefaults.HeaderSpacing.value +
                    secondaryWidthDp.coerceAtMost(secondaryMaxWidthDp ?: secondaryWidthDp)
        }
    }
    primaryMaxWidthDp = primaryMaxWidthDp.coerceAtLeast(0f)

    return ResolvedMatrixCard(
        tiers = tiers,
        padding = paddingDp.dp,
        slotSpacing = spacingDp.dp,
        cornerRadius = content.cornerRadius ?: MatrixDefaults.CornerRadius,
        stacked = stacked,
        leading = leading,
        leadingSize = leadingSizeDp.dp,
        primary = content.primary?.shownOn(locked)?.fitTo(primaryMaxWidthDp, primaryTextSize),
        primaryTextSize = primaryTextSize,
        secondary = secondary,
        secondaryMaxWidth = secondaryMaxWidthDp?.dp,
        hero = content.hero,
        actions = List(content.actions.size) { i -> content.actions[i]?.shownOn(locked) },
    )
}

/** This component shortened to fit [maxWidthDp] at [textSize]. */
private fun PrimaryComponent.fitTo(
    maxWidthDp: Float,
    textSize: TextUnit,
): PrimaryComponent =
    when (this) {
        is TemplateText -> TemplateText(fitMetric(value, maxWidthDp, textSize))
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
    }

/** This text, or its sensitive replacement when [locked]. */
private fun TemplateText.redactedIf(locked: Boolean): TemplateText {
    val replacement = sensitiveReplacement
    return if (locked && replacement != null) TemplateText(replacement) else this
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
 *   [androidx.glance.adaptive.core.ui.selection.GlanceSurface.dimensions].
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
    }
