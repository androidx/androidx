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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.remote.player.compose.embedded.rawDimensionDp
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment

/*
 * Shared mapping from RemoteCompose layout positioning to Compose Alignment/Arrangement.
 *
 * The positioning constants are identical across every layout manager — BoxLayout, ColumnLayout,
 * RowLayout, FitBoxLayout, FlowLayout each redeclare the same values (START=1, CENTER=2, END=3,
 * TOP=4, BOTTOM=5, and on the Row/Column axis SPACE_BETWEEN=6, SPACE_EVENLY=7, SPACE_AROUND=8) —
 * so these mappers key on the shared int value and serve all of them. `spacedBy` is a fixed gap
 * (dp) layered on START/CENTER/END; the SPACE_* distributions are mutually exclusive with it and
 * take precedence.
 */

private const val START = 1
private const val CENTER = 2
private const val END = 3
private const val TOP = 4
private const val BOTTOM = 5
private const val SPACE_BETWEEN = 6
private const val SPACE_EVENLY = 7
private const val SPACE_AROUND = 8

/**
 * The 2D [Alignment] for a Box/FitBox `contentAlignment` from its horizontal + vertical
 * positioning. Expressed as a [BiasAlignment] (bias -1 = start/top, 0 = center, +1 = end/bottom),
 * which is exactly what the named `Alignment.TopStart`/`Center`/… constants are.
 */
internal fun boxContentAlignment(horizontal: Int, vertical: Int): Alignment {
    val horizontalBias =
        when (horizontal) {
            CENTER -> 0f
            END -> 1f
            else -> -1f
        }
    val verticalBias =
        when (vertical) {
            CENTER -> 0f
            BOTTOM -> 1f
            else -> -1f
        }
    return BiasAlignment(horizontalBias, verticalBias)
}

/** A Column's cross-axis ([Alignment.Horizontal]) from its horizontal positioning. */
internal fun columnHorizontalAlignment(positioning: Int): Alignment.Horizontal =
    when (positioning) {
        CENTER -> Alignment.CenterHorizontally
        END -> Alignment.End
        else -> Alignment.Start
    }

/** A Row's cross-axis ([Alignment.Vertical]) from its vertical positioning. */
internal fun rowVerticalAlignment(positioning: Int): Alignment.Vertical =
    when (positioning) {
        CENTER -> Alignment.CenterVertically
        BOTTOM -> Alignment.Bottom
        else -> Alignment.Top
    }

/**
 * A Column's main-axis [Arrangement.Vertical] from its vertical positioning + `spacedBy` gap. The
 * gap is a raw value scaled per the document's density [behavior] (against [density]); see
 * [rawDimensionDp].
 */
internal fun columnVerticalArrangement(
    positioning: Int,
    spacedBy: Float,
    behavior: Int,
    density: Float,
): Arrangement.Vertical {
    val gap = rawDimensionDp(spacedBy, behavior, density)
    return when (positioning) {
        CENTER ->
            if (spacedBy > 0f) Arrangement.spacedBy(gap, Alignment.CenterVertically)
            else Arrangement.Center
        BOTTOM ->
            if (spacedBy > 0f) Arrangement.spacedBy(gap, Alignment.Bottom) else Arrangement.Bottom
        SPACE_BETWEEN -> Arrangement.SpaceBetween
        SPACE_EVENLY -> Arrangement.SpaceEvenly
        SPACE_AROUND -> Arrangement.SpaceAround
        else -> if (spacedBy > 0f) Arrangement.spacedBy(gap) else Arrangement.Top
    }
}

/**
 * A Row's main-axis [Arrangement.Horizontal] from its horizontal positioning + `spacedBy` gap. The
 * gap is a raw value scaled per the document's density [behavior] (against [density]); see
 * [rawDimensionDp].
 */
internal fun rowHorizontalArrangement(
    positioning: Int,
    spacedBy: Float,
    behavior: Int,
    density: Float,
): Arrangement.Horizontal {
    val gap = rawDimensionDp(spacedBy, behavior, density)
    return when (positioning) {
        CENTER ->
            if (spacedBy > 0f) Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            else Arrangement.Center
        END -> if (spacedBy > 0f) Arrangement.spacedBy(gap, Alignment.End) else Arrangement.End
        SPACE_BETWEEN -> Arrangement.SpaceBetween
        SPACE_EVENLY -> Arrangement.SpaceEvenly
        SPACE_AROUND -> Arrangement.SpaceAround
        else -> if (spacedBy > 0f) Arrangement.spacedBy(gap) else Arrangement.Start
    }
}
