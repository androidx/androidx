/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.RectRulers
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * Represents the margins of a pane within a pane scaffold.
 *
 * Note that the margins are specified as offsets from the edges of the scaffold. To specify the
 * internal spacer size please do it via [PaneScaffoldDirective.horizontalPartitionSpacerSize] and
 * [PaneScaffoldDirective.verticalPartitionSpacerSize].
 *
 * This is typically set by the [paneMargins] modifier.
 *
 * @see paneMargins
 */
@Immutable
sealed interface PaneMargins {
    fun Placeable.PlacementScope.getPaneLeft(measuredLeft: Int) = measuredLeft

    fun Placeable.PlacementScope.getPaneTop(measuredTop: Int) = measuredTop

    fun Placeable.PlacementScope.getPaneRight(measuredRight: Int, parentRight: Int) = measuredRight

    fun Placeable.PlacementScope.getPaneBottom(measuredBottom: Int, parentBottom: Int) =
        measuredBottom

    private class Unspecified : PaneMargins

    companion object {
        /**
         * Represents no margins being set.
         *
         * When set to [Unspecified], the pane's position will not be affected by margins. The edges
         * of the pane may touch the edges of the scaffold.
         */
        val Unspecified: PaneMargins = Unspecified()
    }
}

@Immutable
internal class PaneMarginsImpl(
    fixedMargins: PaddingValues = PaddingValues(),
    insets: List<RectRulers>,
    density: Density,
    layoutDirection: LayoutDirection,
) : PaneMargins {
    private val fixedMarginLeft =
        with(density) { fixedMargins.calculateLeftPadding(layoutDirection).roundToPx() }
    private val fixedMarginTop = with(density) { fixedMargins.calculateTopPadding().roundToPx() }
    private val fixedMarginRight =
        with(density) { fixedMargins.calculateRightPadding(layoutDirection).roundToPx() }
    private val fixedMarginBottom =
        with(density) { fixedMargins.calculateBottomPadding().roundToPx() }
    private val rulers = insets

    override fun Placeable.PlacementScope.getPaneLeft(measuredLeft: Int): Int =
        maxOf(
            measuredLeft,
            fixedMarginLeft,
            rulers.maxOfOrNull { it.left.current(0f).roundToInt() } ?: 0,
        )

    override fun Placeable.PlacementScope.getPaneTop(measuredTop: Int): Int =
        maxOf(
            measuredTop,
            fixedMarginTop,
            rulers.maxOfOrNull { it.top.current(0f).roundToInt() } ?: 0,
        )

    override fun Placeable.PlacementScope.getPaneRight(measuredRight: Int, parentRight: Int): Int =
        minOf(
            measuredRight,
            parentRight - fixedMarginRight,
            rulers.minOfOrNull { it.right.current(Float.MAX_VALUE).roundToInt() } ?: parentRight,
        )

    override fun Placeable.PlacementScope.getPaneBottom(
        measuredBottom: Int,
        parentBottom: Int,
    ): Int =
        minOf(
            measuredBottom,
            parentBottom - fixedMarginBottom,
            rulers.minOfOrNull { it.bottom.current(Float.MAX_VALUE).roundToInt() } ?: parentBottom,
        )
}
