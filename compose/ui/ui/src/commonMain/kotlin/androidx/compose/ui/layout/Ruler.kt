/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.compose.ui.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset

/**
 * A line that can be used to align layout children inside a [Placeable.PlacementScope].
 *
 * [Ruler] values are only valid when there is no [rotation][Modifier.rotate].
 * @see Placeable.PlacementScope.current
 * @see MeasureScope.layout
 * @see RulerScope.provides
 * @see RulerScope.providesRelative
 */
sealed class Ruler {
    /**
     * Returns the coordinate for the [Ruler], defined with the [coordinate] value at
     * [sourceCoordinates] and read at [targetCoordinates].
     */
    internal abstract fun calculateCoordinate(
        coordinate: Float,
        sourceCoordinates: LayoutCoordinates,
        targetCoordinates: LayoutCoordinates
    ): Float
}

/**
 * A vertical [Ruler]. Defines a line that can be used by parent layouts
 * to align or position their children horizontally. The position of the ruler
 * can be retrieved with [Placeable.PlacementScope.current] and can be set with
 * [MeasureScope.layout] using [RulerScope.provides] or [RulerScope.providesRelative].
 */
class VerticalRuler() : Ruler() {
    override fun calculateCoordinate(
        coordinate: Float,
        sourceCoordinates: LayoutCoordinates,
        targetCoordinates: LayoutCoordinates
    ): Float {
        val offset = Offset(coordinate, sourceCoordinates.size.height / 2f)
        return targetCoordinates.localPositionOf(sourceCoordinates, offset).x
    }
}

/**
 * A horizontal [Ruler]. Defines a line that can be used by parent layouts
 * to align or position their children vertically. The position of the ruler
 * can be retrieved with [Placeable.PlacementScope.current] and can be set with
 * [MeasureScope.layout] using [RulerScope.provides] or [RulerScope.providesRelative].
 */
class HorizontalRuler : Ruler() {
    override fun calculateCoordinate(
        coordinate: Float,
        sourceCoordinates: LayoutCoordinates,
        targetCoordinates: LayoutCoordinates
    ): Float {
        val offset = Offset(sourceCoordinates.size.width / 2f, coordinate)
        return targetCoordinates.localPositionOf(sourceCoordinates, offset).y
    }
}
