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

package androidx.glance.adaptive.appwidget.templates.dashboard

import androidx.annotation.RestrictTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.appwidget.components.icongrid.IconGridDefaults
import androidx.glance.adaptive.appwidget.matrix.MatrixBulbContent
import androidx.glance.adaptive.appwidget.matrix.MatrixCardContent
import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.selection.from
import androidx.glance.adaptive.core.selection.ArchetypeSelector
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.selection.HeightTier
import androidx.glance.adaptive.core.selection.SizeTiers
import androidx.glance.adaptive.core.selection.WidthTier
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.IconGrid
import androidx.glance.adaptive.core.slots.IconTile
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.DashboardTemplate

/**
 * Maps a [DashboardTemplate] onto the Information Matrix, for any backend's matrix renderer, such
 * as [androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer].
 *
 * This is the whole of the Dashboard template: which component goes in which slot at each width and
 * height tier, following the design's Dashboard density matrix. Everything about the container
 * belongs to the matrix.
 *
 * - **W2 to W4**: the header (its contained glyph, its title with the subtitle under it, and from
 *   W3 its trailing button) above rows of [IconTile] actions, two to four across by width. H1 has
 *   room for the header alone, H2 and H3 for one row (taller at H3), and H4 for two.
 * - **W1**: a column of the header's title, with the subtitle from H3, above one action, or two at
 *   H4.
 * - **Without a header** the actions take the whole widget: one row up to H2 and two from H3.
 * - **Mini**: at H0, such as on the lock screen, a single round button, the bulb, at W1, and a
 *   compact strip with a bare glyph and a title wider. Both show the header, or else the first
 *   action. The bulb runs the header's trailing button's action, or else the first action's. The
 *   strip, like the design's `Linear`, is a glyph and a title only and takes no taps, as a card
 *   itself never does.
 *
 * Actions past what a size shows are left out: a size shows the first ones, in order. Fewer actions
 * than a row holds keep the width's column width, start-aligned. Tiles draw their labels only where
 * a row is tall enough for them: under a header from H3, never in the narrow column, and without a
 * header wherever a row fits a label. A tile is always described by its label, for accessibility
 * services.
 *
 * Not registered by default.
 */
// TODO(b/580007927): Confirm with UX whether the design's `5, 8` and `5, 6, 8` tile numbering on
//  narrower widths pins the last action as an overflow tile (e.g. Contacts "…") instead of showing
//  a plain prefix.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object DashboardMatrixSelector :
    ArchetypeSelector<DashboardTemplate, AppWidgetSurface, MatrixContent> {

    override fun select(template: DashboardTemplate, surface: AppWidgetSurface): MatrixContent {
        val tiers = SizeTiers.from(surface.dimensions, surface)
        return when {
            tiers.height == HeightTier.H0 && tiers.width == WidthTier.W1 -> bulb(template)
            tiers.height == HeightTier.H0 -> strip(template)
            tiers.width == WidthTier.W1 -> column(template, tiers)
            !template.hasHeader -> actionsCard(template, tiers, surface.dimensions)
            else -> headerCard(template, tiers)
        }
    }

    /**
     * The design's `Bulb`: the header's glyph, or the first action's, as a round button. It is
     * described by the header's trailing button, or else the header's title, or else the first
     * action, along with that description's sensitive replacement.
     */
    private fun bulb(template: DashboardTemplate): MatrixContent {
        val first = template.firstAction()
        val icon = template.leading as? Icon
        val title = template.primary as? TemplateText
        val trailing = template.trailing as? IconButton
        return MatrixBulbContent(
            component =
                IconButton(
                    icon = icon?.icon ?: first.icon,
                    contentDescription =
                        when {
                            trailing != null -> trailing.contentDescription
                            title != null -> title.value
                            else -> first.label
                        },
                    action = trailing?.action ?: first.action,
                    sensitiveReplacement =
                        when {
                            trailing != null -> trailing.sensitiveReplacement
                            title != null -> title.sensitiveReplacement
                            else -> first.sensitiveReplacement
                        },
                ),
            hasDroppedSlots =
                template.hasHeader || template.actions.size > 1 || first.subtitle != null,
        )
    }

    /**
     * The design's `Linear` strip: the header's bare glyph and title, or the first action's. It has
     * no interactive component, so it takes no taps.
     */
    // TODO(b/580007927): Decide with UX what tapping a Mini layout should do.
    private fun strip(template: DashboardTemplate): MatrixContent {
        val first = template.firstAction()
        val icon = template.leading as? Icon
        return MatrixCardContent(
            leading = Icon(icon?.icon ?: first.icon, icon?.contentDescription, contained = false),
            primary = template.primary ?: TemplateText(first.label, first.sensitiveReplacement),
            padding = 0.dp,
            leadingSize = STRIP_ICON_SIZE_DP.dp,
            primaryTextSize = STRIP_TEXT_SIZE_SP.sp,
            hasDroppedSlots =
                template.hasHeader || template.actions.size > 1 || first.subtitle != null,
        )
    }

    private fun DashboardTemplate.firstAction(): IconTile {
        require(actions.isNotEmpty()) { "DashboardTemplate requires at least one action." }
        return actions.first()
    }

    /**
     * The narrow column: the header's texts above one or two actions, one per row. Its tiles draw
     * their glyphs alone, as the design does, and its slots sit closer than a card's default, so
     * that a tile keeps its glyph's size at H1.
     */
    private fun column(template: DashboardTemplate, tiers: SizeTiers): MatrixContent {
        val rows =
            when {
                !template.hasHeader -> if (tiers.height >= HeightTier.H3) 2 else 1
                tiers.height == HeightTier.H4 -> 2
                else -> 1
            }
        val showSecondary = tiers.height >= HeightTier.H3
        return MatrixCardContent(
            primary = template.primary,
            secondary = template.secondary?.takeIf { showSecondary },
            hero = grid(template, columns = 1, rows = rows, showLabels = false),
            padding =
                if (tiers.height == HeightTier.H1) COLUMN_H1_PADDING_DP.dp else CARD_PADDING_DP.dp,
            slotSpacing = COLUMN_SLOT_SPACING_DP.dp,
            cornerRadius = CARD_CORNER_RADIUS_DP.dp,
            primaryTextSize = COLUMN_TEXT_SIZE_SP.sp,
            hasDroppedSlots =
                template.leading != null ||
                    (template.secondary != null && !showSecondary) ||
                    template.trailing != null ||
                    template.actions.size > rows,
        )
    }

    /**
     * The header above rows of actions. At H2 the row under the header is too short for a label, so
     * tiles draw their labels from H3, as the design's tall actions do.
     */
    private fun headerCard(template: DashboardTemplate, tiers: SizeTiers): MatrixContent {
        val rows =
            when (tiers.height) {
                HeightTier.H1 -> 0
                HeightTier.H4 -> 2
                else -> 1
            }
        val columns = columns(tiers.width)
        // A two-column card is too narrow for the trailing button beside the header's texts.
        val showTrailing = tiers.width >= WidthTier.W3
        val leading =
            (template.leading as? Icon)?.let {
                Icon(it.icon, it.contentDescription, contained = true)
            } ?: template.leading
        return MatrixCardContent(
            leading = leading,
            primary = template.primary,
            secondary = template.secondary,
            trailing = template.trailing?.takeIf { showTrailing },
            hero =
                if (rows > 0) {
                    grid(
                        template,
                        columns = columns,
                        rows = rows,
                        showLabels = tiers.height >= HeightTier.H3,
                    )
                } else {
                    null
                },
            padding = CARD_PADDING_DP.dp,
            slotSpacing =
                if (tiers.height <= HeightTier.H2) {
                    SHORT_HEADER_SLOT_SPACING_DP.dp
                } else {
                    HEADER_SLOT_SPACING_DP.dp
                },
            cornerRadius = CARD_CORNER_RADIUS_DP.dp,
            leadingSize = HEADER_ICON_SIZE_DP.dp,
            primaryTextSize = HEADER_TEXT_SIZE_SP.sp,
            hasDroppedSlots =
                (template.trailing != null && !showTrailing) ||
                    template.actions.size > columns * rows,
        )
    }

    /**
     * Actions alone, taking the whole card. Its tiles draw their labels when a row is at least
     * [IconGridDefaults.LabelledTileMinHeight] tall in a card of [dimensions].
     */
    private fun actionsCard(
        template: DashboardTemplate,
        tiers: SizeTiers,
        dimensions: Dimensions,
    ): MatrixContent {
        val rows = if (tiers.height >= HeightTier.H3) 2 else 1
        val columns = columns(tiers.width)
        val rowHeightDp =
            (dimensions.heightDp -
                2 * CARD_PADDING_DP -
                (rows - 1) * IconGridDefaults.Spacing.value) / rows
        return MatrixCardContent(
            hero =
                grid(
                    template,
                    columns = columns,
                    rows = rows,
                    showLabels = rowHeightDp >= IconGridDefaults.LabelledTileMinHeight.value,
                ),
            padding = CARD_PADDING_DP.dp,
            cornerRadius = CARD_CORNER_RADIUS_DP.dp,
            hasDroppedSlots = template.actions.size > columns * rows,
        )
    }

    /** Tiles across a row at [width]: the design shows two at W2, three at W3 and four at W4. */
    private fun columns(width: WidthTier): Int =
        when (width) {
            WidthTier.W1 -> 1
            WidthTier.W2 -> 2
            WidthTier.W3 -> 3
            WidthTier.W4 -> 4
        }

    /**
     * The first actions as a grid of [columns] by up to [rows] tiles. The grid keeps the width's
     * columns however few actions there are, so tiles are always as wide as the design draws them
     * at that width: a short row, or the last row, starts at the leading edge and leaves its
     * trailing cells empty. The tiles draw their labels if [showLabels].
     */
    private fun grid(
        template: DashboardTemplate,
        columns: Int,
        rows: Int,
        showLabels: Boolean,
    ): IconGrid {
        val count = minOf(template.actions.size, columns * rows)
        val tiles = List(count) { i -> template.actions[i] }
        val usedRows = minOf(rows, (count + columns - 1) / columns)
        return IconGrid(
            tiles,
            columns = columns,
            rows = usedRows,
            showLabels = showLabels,
        )
    }
}

/** Padding of every Dashboard card but the narrow column at H1. */
private const val CARD_PADDING_DP = 12f

/** Corner radius of every Dashboard card. */
private const val CARD_CORNER_RADIUS_DP = 32f

/** Edge length of the header's icon circle, and height of its trailing button. */
private const val HEADER_ICON_SIZE_DP = 56f

/** Type size of the header's title. */
private const val HEADER_TEXT_SIZE_SP = 18f

/** Gap between the header and the actions, as the design spaces them. */
private const val HEADER_SLOT_SPACING_DP = 18f

/** Gap between the header and the short row of actions at H2, which has no room for more. */
private const val SHORT_HEADER_SLOT_SPACING_DP = 8f

/**
 * Padding of the narrow column at H1, tighter than a card's, so its tile keeps its glyph's size.
 */
private const val COLUMN_H1_PADDING_DP = 8f

/** Gap between the narrow column's title and its actions. */
private const val COLUMN_SLOT_SPACING_DP = 8f

/** Type size of the narrow column's title. */
private const val COLUMN_TEXT_SIZE_SP = 14f

/** Edge length of the glyph in the compact strip, from the design's 28 dp tall `Linear`. */
private const val STRIP_ICON_SIZE_DP = 16f

/** Type size of the compact strip's title. */
private const val STRIP_TEXT_SIZE_SP = 14f
