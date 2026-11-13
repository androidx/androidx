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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.core.slots.IconGrid

/**
 * Design values of the [IconGrid] component that every backend draws it with.
 *
 * The tile's 16 dp corner radius is declared by its drawable, as a corner radius cannot be set at
 * runtime before API 31.
 */
internal object IconGridDefaults {

    /** Gap between the tiles of a grid, across and down. */
    val Spacing: Dp = 6.dp

    /** Edge length of a tile's glyph. */
    val TileGlyphSize: Dp = 30.dp

    /** Type size of a tile's label and subtitle. */
    val LabelTextSize: TextUnit = 12.sp

    /** Height of a line of a tile's label or subtitle at the default font scale. */
    val LabelLineHeight: Dp = 16.dp

    /**
     * Height a tile takes besides its text: its padding (4 dp above, 8 dp below), its glyph and the
     * 2 dp gap between the glyph and the text.
     */
    val TileChromeHeight: Dp = 44.dp

    /** Most lines a tile's label wraps to. */
    const val MaxLabelLines: Int = 2

    /**
     * Height a tile needs to draw a line of its label beneath its glyph: [TileChromeHeight] and
     * [LabelLineHeight]. A grid whose rows are shorter should not show labels (see
     * [IconGrid.showLabels]).
     */
    val LabelledTileMinHeight: Dp = 60.dp
}
