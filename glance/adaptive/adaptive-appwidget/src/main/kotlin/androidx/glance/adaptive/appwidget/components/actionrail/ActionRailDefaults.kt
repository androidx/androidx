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

package androidx.glance.adaptive.appwidget.components.actionrail

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.core.selection.HeightTier

/**
 * Design geometry and capacity tokens shared by the trailing vertical action rail and compact
 * bottom-end floating action button across backends.
 */
internal object ActionRailDefaults {

    /** Maximum number of actions shown in the trailing vertical rail at [HeightTier.H3]. */
    const val MAX_ACTIONS_H3: Int = 4

    /** Maximum number of actions shown in the trailing vertical rail at [HeightTier.H4] and up. */
    const val MAX_ACTIONS_H4: Int = 6

    /** Width of the trailing vertical action rail on expanded cards (`W3–W4 × H3+`). */
    val Width: Dp = 32.dp

    /** Horizontal gap between the main card column and the trailing action rail. */
    val Spacing: Dp = 8.dp

    /** Width of the primary pill button at the top of the trailing action rail. */
    val PrimaryWidth: Dp = 30.dp

    /** Height of the primary pill button at the top of the trailing action rail at `H4+`. */
    val PrimaryHeight: Dp = 40.dp

    /** Height of the primary pill button at the top of the trailing action rail at `H3`. */
    val PrimaryCompactHeight: Dp = 34.dp

    /** Glyph size of the primary button in the action rail and floating FAB. */
    val PrimaryGlyphSize: Dp = 16.dp

    /** Touch-target height of secondary icon buttons in the trailing action rail. */
    val SecondaryHeight: Dp = 24.dp

    /** Glyph size of secondary icon buttons in the trailing action rail. */
    val SecondaryGlyphSize: Dp = 15.dp

    /** Vertical gap between buttons inside the trailing action rail. */
    val ButtonSpacing: Dp = 8.dp

    /** Diameter of the bottom-end floating action button overlaid on compact cards. */
    val FloatingFabSize: Dp = 32.dp

    /** Inset from the bottom and trailing card edges for the floating action button overlay. */
    val FloatingFabInset: Dp = 8.dp

    /**
     * Maximum number of rail buttons that fit without clipping inside [availableHeightDp] at
     * [heightTier], or the tier cap when [availableHeightDp] is `null` (unsized container).
     */
    fun maxFittingActions(availableHeightDp: Float?, heightTier: HeightTier): Int {
        val tierCap = if (heightTier >= HeightTier.H4) MAX_ACTIONS_H4 else MAX_ACTIONS_H3
        if (availableHeightDp == null) return tierCap
        val primaryHeight =
            if (heightTier >= HeightTier.H4) PrimaryHeight.value else PrimaryCompactHeight.value
        val remaining = availableHeightDp - primaryHeight
        if (remaining < 0f) return 1
        val secondaryStep = ButtonSpacing.value + SecondaryHeight.value
        val secondaryCount = (remaining / secondaryStep).toInt().coerceAtLeast(0)
        return (1 + secondaryCount).coerceAtMost(tierCap)
    }
}
