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
