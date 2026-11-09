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
import androidx.glance.adaptive.appwidget.matrix.IconGrid

/**
 * Design values of the [IconGrid] component that every backend draws it with.
 *
 * The tile's 16 dp corner radius is declared by its drawable, as a corner radius cannot be set at
 * runtime before API 31.
 */
internal object IconGridDefaults {

    /** Gap between the tiles of a grid, across and down. */
    val Spacing: Dp = 4.dp

    /** Edge length of a tile's glyph. */
    val TileGlyphSize: Dp = 30.dp

    /** Type size of a tile's label. */
    val LabelTextSize: TextUnit = 12.sp

    /**
     * Height a tile needs to draw its label beneath its glyph: the glyph, a line of the label and
     * some room around them, on the design's 8 dp grid. A grid whose rows are shorter should not
     * show labels (see [IconGrid.showLabels]).
     */
    val LabelledTileMinHeight: Dp = 56.dp
}
