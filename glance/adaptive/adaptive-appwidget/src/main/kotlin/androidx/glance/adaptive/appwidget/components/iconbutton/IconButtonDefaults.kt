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

package androidx.glance.adaptive.appwidget.components.iconbutton

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.core.slots.IconButton

/** Design values of the [IconButton] component that every backend draws it with. */
internal object IconButtonDefaults {

    /** Smallest bulb an icon button draws its large glyph in, between the design's two bulbs. */
    private val LargeBulbFrom: Dp = 72.dp

    /** Edge length of the glyph in the design's 56 dp bulb. */
    private val SmallBulbGlyphSize: Dp = 26.dp

    /** Edge length of the glyph in the design's 90 dp bulb. */
    private val LargeBulbGlyphSize: Dp = 44.dp

    /** Diameter of an icon button in an action slot, the platform's minimum touch target. */
    val ActionSize: Dp = 48.dp

    /** Edge length of the glyph of an icon button in an action slot. */
    val ActionGlyphSize: Dp = 24.dp

    /**
     * Edge length of the glyph of an icon button in the header's trailing slot, as a fraction of
     * the button's height, which the matrix matches to the leading component: the design draws a 30
     * dp glyph in its 56 dp tall pill.
     */
    const val TrailingGlyphRatio: Float = 30f / 56f

    /**
     * Edge length of the glyph of an icon button that is the whole widget, in a bulb
     * [containerSize] across: the design draws a 26 dp glyph in its 56 dp bulb and a 44 dp one in
     * its 90 dp bulb.
     */
    fun bulbGlyphSize(containerSize: Dp): Dp =
        if (containerSize >= LargeBulbFrom) LargeBulbGlyphSize else SmallBulbGlyphSize
}
