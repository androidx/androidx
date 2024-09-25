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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round

// This file is a collection of shared functions and vals among animation-relevant modifiers.

internal val InvalidIntSize = IntSize(-1, -1)

internal val InvalidOffset = IntOffset(Int.MIN_VALUE, Int.MIN_VALUE)

internal val InvalidIntRect = IntRect(0, 0, -1, -1)

internal fun Placeable.PlacementScope.lookaheadOffset(lookaheadScope: LookaheadScope): IntOffset =
    with(lookaheadScope) {
        lookaheadScopeCoordinates.localLookaheadPositionOf(coordinates!!).round()
    }

internal fun Placeable.PlacementScope.convertOffsetToLookaheadCoordinates(
    offset: IntOffset,
    lookaheadScope: LookaheadScope
): IntOffset =
    with(lookaheadScope) {
        offset - lookaheadScopeCoordinates.localPositionOf(coordinates!!, Offset.Zero).round()
    }

internal val IntRect.isValid
    get() = this != InvalidIntRect

internal val IntSize.isValid
    get() = this != InvalidIntSize

internal val IntOffset.isValid
    get() = this != InvalidOffset

internal class Bounds {
    var topLeft: IntOffset = InvalidOffset
    var size: IntSize = InvalidIntSize

    val isValid
        get() = topLeft.isValid && size.isValid

    val rect
        get() =
            if (isValid) {
                IntRect(topLeft, size)
            } else {
                InvalidIntRect
            }
}
