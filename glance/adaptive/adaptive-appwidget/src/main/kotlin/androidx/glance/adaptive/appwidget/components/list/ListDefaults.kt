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

package androidx.glance.adaptive.appwidget.components.list

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.core.selection.WidthTier
import androidx.glance.adaptive.core.slots.ItemList

/** Design values of the [ItemList] component shared across backends. */
internal object ListDefaults {

    /** Maximum number of inline checklist entries rendered inside a detail card preview. */
    const val MAX_INLINE_CHECKLIST_ENTRIES: Int = 4

    /** Vertical gap between list item cards. */
    val Spacing: Dp = 4.dp

    /** Corner radius of featured list item cards. */
    val FeaturedCornerRadius: Dp = 16.dp

    /** Corner radius of compact list item cards. */
    val CompactCornerRadius: Dp = 14.dp

    /** Corner radius of multi-line detail cards. */
    val DetailCardCornerRadius: Dp = 14.dp

    /** Corner radius of outlined list items and section dividers. */
    val OutlinedCornerRadius: Dp = 11.dp

    /** Horizontal padding of compact rows at `W2+`. */
    val ItemHorizontalPadding: Dp = 12.dp

    /** Horizontal padding of compact rows in narrow `W1` columns. */
    val NarrowHorizontalPadding: Dp = 8.dp

    /** Horizontal gap between an inline compact subtitle and its title. */
    val InlineSubtitleGap: Dp = 8.dp

    /** Base type size of a compact list item's title and focused subtitle lines at `W2+`. */
    private val SecondaryTextSize: TextUnit = 11.sp

    /** Type size of a featured/stacked compact item's title at `W2+`. */
    private val FocusedTitleTextSize: TextUnit = 14.sp

    /** Type size of a compact row's tertiary detail line (e.g. location). */
    val TertiaryTextSize: TextUnit = 10.sp

    /** Type size of a section divider row. */
    val DividerTextSize: TextUnit = 9.sp

    /** Edge length of an optional uncontained leading or trailing icon inside a list item. */
    val ItemIconSize: Dp = 20.dp

    /** Diameter of an optional contained circular trailing icon inside a list item. */
    val ContainedItemIconSize: Dp = 28.dp

    /** Edge length of an optional small icon paired with tertiary text (e.g. location pin). */
    val TertiaryIconSize: Dp = 13.dp

    /** Edge length of a standalone checkbox glyph in a checkbox row. */
    val CheckboxSize: Dp = 16.dp

    /** Edge length of an inline checkbox glyph inside a detail card preview. */
    val InlineCheckboxSize: Dp = 13.dp

    /**
     * Minimum height applied to a scrollable featured (`Focused`) first row at `H3+` so hero cards
     * retain visual prominence even when the list scrolls.
     */
    val ScrollableFeaturedMinHeight: Dp = 84.dp

    /**
     * Upper height cap for a stretched featured item (`ItemListLayoutMode.ExpandFeatured`) so tall
     * non-scrollable widgets (e.g. `W1×H5`) do not balloon item 0 into excessive whitespace.
     */
    val MaxExpandedFeaturedHeight: Dp = 150.dp

    /** Type size of a compact item's title for [widthTier], [stacked] layout, and [uncontained]. */
    fun compactTitleTextSize(
        widthTier: WidthTier,
        stacked: Boolean,
        uncontained: Boolean,
    ): TextUnit =
        when {
            widthTier == WidthTier.W1 -> 10.sp
            stacked && !uncontained -> FocusedTitleTextSize
            else -> SecondaryTextSize
        }

    /**
     * Type size of a compact item's subtitle for [widthTier], [stacked] layout, and [uncontained].
     */
    fun compactSubtitleTextSize(
        widthTier: WidthTier,
        stacked: Boolean,
        uncontained: Boolean,
    ): TextUnit =
        when {
            widthTier == WidthTier.W1 -> 9.sp
            stacked && !uncontained -> SecondaryTextSize
            else -> 10.sp
        }

    /** Maximum width of an inline compact subtitle at [widthTier] so the title stays readable. */
    fun compactSubtitleMaxWidth(widthTier: WidthTier): Dp =
        when (widthTier) {
            WidthTier.W1 -> 24.dp
            WidthTier.W2 -> 44.dp
            WidthTier.W3 -> 76.dp
            WidthTier.W4 -> 112.dp
        }

    /** Type size of a standalone checkbox row's label at [widthTier]. */
    fun checkboxTextSize(widthTier: WidthTier): TextUnit =
        if (widthTier == WidthTier.W1) 10.sp else 11.5.sp

    /** Type size of a detail card's headline at [widthTier]. */
    fun detailCardTitleTextSize(widthTier: WidthTier): TextUnit =
        if (widthTier == WidthTier.W1) 10.sp else 13.sp

    /** Type size of a detail card's body text at [widthTier]. */
    fun detailCardBodyTextSize(widthTier: WidthTier): TextUnit =
        if (widthTier == WidthTier.W1) 9.5.sp else 11.sp

    /** Type size of an inline checklist entry inside a detail card at [widthTier]. */
    fun inlineChecklistTextSize(widthTier: WidthTier): TextUnit =
        if (widthTier == WidthTier.W1) 9.5.sp else 10.5.sp
}
