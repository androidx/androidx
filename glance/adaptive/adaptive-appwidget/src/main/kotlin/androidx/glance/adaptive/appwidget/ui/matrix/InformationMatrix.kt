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
