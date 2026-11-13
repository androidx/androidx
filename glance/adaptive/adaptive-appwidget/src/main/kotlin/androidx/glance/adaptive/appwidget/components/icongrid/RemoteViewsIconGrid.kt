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

package androidx.glance.adaptive.appwidget.components.icongrid

import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.unit.Dp
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.components.RemoteViewsActions
import androidx.glance.adaptive.appwidget.components.addChild
import androidx.glance.adaptive.appwidget.components.density
import androidx.glance.adaptive.appwidget.components.setHorizontalItemSpacing
import androidx.glance.adaptive.appwidget.components.setMaxSize
import androidx.glance.adaptive.appwidget.components.setOptionalText
import androidx.glance.adaptive.appwidget.components.setVerticalItemSpacing
import androidx.glance.adaptive.appwidget.components.tappableStableId
import androidx.glance.adaptive.appwidget.components.toPx
import androidx.glance.adaptive.appwidget.components.visibility
import androidx.glance.adaptive.core.slots.IconGrid
import androidx.glance.adaptive.core.slots.IconTile

/**
 * Draws the icon grid for the hero slot: [IconGrid.rows] rows that share the hero's height, each
 * with [IconGrid.columns] cells that share its width, [IconGridDefaults.Spacing] apart.
 *
 * The rows fill the hero however tall it is, as the design's elongated tiles do, so a tall widget
 * has tall tiles with their content at the top rather than a band of empty card around them.
 *
 * Each tile is its glyph at its top start, with its label and subtitle beneath it when
 * [IconGrid.showLabels] is `true`, as many lines of them as the row has room for (see [textLines]).
 * The tile is described by its label and subtitle either way, for accessibility services. An empty
 * cell keeps its place with its tile hidden, so the tiles keep the size they have in a full grid. A
 * tile that ignores taps is given no click, since any click makes a view clickable, and is added
 * under a stable ID apart from a tappable one (see [tappableStableId]), so the host never recycles
 * one into the other.
 *
 * The glyphs are capped at [IconGridDefaults.TileGlyphSize] (see [setMaxSize]), so their drawables
 * must be declared at least that large to be drawn at it.
 *
 * @param taps Where the tiles' taps go, and the context the views are built for.
 * @param heroHeight Estimated height of the hero the grid fills, or `null` when unknown, in which
 *   case tiles draw all of their text. It only budgets the text lines (see [textLines]): the
 *   estimate comes from the size the host reports, which can be smaller than the size it draws at,
 *   so it is a lower bound.
 * @return The grid's views.
 */
internal fun IconGrid.buildRemoteViews(taps: RemoteViewsActions, heroHeight: Dp?): RemoteViews {
    val context = taps.context
    val density = context.density
    val gridId = R.id.glance_adaptive_appwidget_remoteviews_icon_grid
    val rowId = R.id.glance_adaptive_appwidget_remoteviews_icon_grid_row
    val grid =
        RemoteViews(context.packageName, R.layout.glance_adaptive_appwidget_remoteviews_icon_grid)
    // Emptied first, because the host reapplies an update onto the previous hierarchy.
    grid.removeAllViews(gridId)

    val gapsDp = (rows - 1) * IconGridDefaults.Spacing.value
    val rowHeightDp = heroHeight?.let { (it.value - gapsDp) / rows }
    val lines = textLines(rowHeightDp, context.resources.configuration.fontScale)

    for (row in 0 until rows) {
        val rowViews =
            RemoteViews(
                context.packageName,
                R.layout.glance_adaptive_appwidget_remoteviews_icon_grid_row,
            )
        rowViews.setVerticalItemSpacing(
            rowId,
            IconGridDefaults.Spacing,
            isLast = row == rows - 1,
            density = density,
        )
        rowViews.removeAllViews(rowId)
        for (column in 0 until columns) {
            val tile = tileAt(row, column)
            rowViews.addChild(
                rowId,
                buildCell(
                    taps,
                    tile,
                    textLines = if (showLabels) lines else 0,
                    isLast = column == columns - 1,
                ),
                tappableStableId(column, taps.isTappable(tile?.action)),
            )
        }
        grid.addChild(gridId, rowViews, stableId = row)
    }
    return grid
}

/**
 * Lines of text a tile in a row [rowHeightDp] tall has room for beneath its glyph, at the user's
 * [fontScale] (evaluated at composition time when the [RemoteViews] are built), or as many as a
 * tile ever draws when the row's height is unknown.
 */
internal fun textLines(rowHeightDp: Float?, fontScale: Float): Int {
    val most = IconGridDefaults.MaxLabelLines + 1
    if (rowHeightDp == null) return most
    val room = rowHeightDp - IconGridDefaults.TileChromeHeight.value
    val lineHeightDp = IconGridDefaults.LabelLineHeight.value * fontScale
    return (room / lineHeightDp).toInt().coerceIn(0, most)
}

/**
 * One cell of the grid, showing [tile] with up to [textLines] lines of its label and subtitle
 * beneath its glyph, or with its tile hidden when the cell is empty.
 *
 * The subtitle takes a line only when the label keeps at least one; the label takes the rest, up to
 * [IconGridDefaults.MaxLabelLines].
 */
private fun buildCell(
    taps: RemoteViewsActions,
    tile: IconTile?,
    textLines: Int,
    isLast: Boolean,
): RemoteViews {
    val context = taps.context
    val density = context.density
    val cell =
        RemoteViews(context.packageName, R.layout.glance_adaptive_appwidget_remoteviews_icon_tile)
    cell.setHorizontalItemSpacing(
        R.id.glance_adaptive_appwidget_remoteviews_icon_tile_cell,
        IconGridDefaults.Spacing,
        isLast = isLast,
        density = density,
    )
    val tileId = R.id.glance_adaptive_appwidget_remoteviews_icon_tile
    // Set either way: an empty cell and an inert tile share a stable ID and a layout, so the host
    // may reapply one onto the other.
    if (tile == null) {
        cell.setViewVisibility(tileId, View.INVISIBLE)
        return cell
    }
    cell.setViewVisibility(tileId, View.VISIBLE)
    val glyphId = R.id.glance_adaptive_appwidget_remoteviews_icon_tile_glyph
    cell.setImageViewResource(glyphId, tile.icon)
    cell.setMaxSize(glyphId, IconGridDefaults.TileGlyphSize.toPx(density))

    val subtitle = tile.subtitle?.value
    val subtitleShown = subtitle != null && textLines >= 2
    val labelLines =
        (textLines - if (subtitleShown) 1 else 0).coerceAtMost(IconGridDefaults.MaxLabelLines)
    val labelId = R.id.glance_adaptive_appwidget_remoteviews_icon_tile_label
    cell.setViewVisibility(
        R.id.glance_adaptive_appwidget_remoteviews_icon_tile_text,
        visibility(labelLines > 0),
    )
    cell.setOptionalText(
        labelId,
        tile.label.takeIf { labelLines > 0 },
        IconGridDefaults.LabelTextSize.value,
    )
    if (labelLines > 0) cell.setInt(labelId, "setMaxLines", labelLines)
    cell.setOptionalText(
        R.id.glance_adaptive_appwidget_remoteviews_icon_tile_subtitle,
        subtitle.takeIf { subtitleShown },
        IconGridDefaults.LabelTextSize.value,
    )
    // The tile is what is focused and tapped, so it carries the text for accessibility services.
    cell.setContentDescription(
        tileId,
        if (subtitle != null) "${tile.label}, $subtitle" else tile.label,
    )
    taps.pendingIntentFor(tile.action)?.let { cell.setOnClickPendingIntent(tileId, it) }
    return cell
}
