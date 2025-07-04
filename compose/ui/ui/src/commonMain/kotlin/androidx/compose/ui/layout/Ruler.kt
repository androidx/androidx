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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Placeable.PlacementScope

/**
 * A line that can be used to align layout children inside a [Placeable.PlacementScope].
 *
 * @see Placeable.PlacementScope.current
 * @see MeasureScope.layout
 * @see RulerScope.provides
 * @see RulerScope.providesRelative
 */
sealed class Ruler(internal val calculate: (PlacementScope.(Float) -> Float)?) {
    /**
     * Returns the coordinate for the [Ruler], defined with the [coordinate] value at
     * [sourceCoordinates] and read at [targetCoordinates].
     */
    internal abstract fun calculateCoordinate(
        coordinate: Float,
        sourceCoordinates: LayoutCoordinates,
        targetCoordinates: LayoutCoordinates,
    ): Float
}

/**
 * A vertical [Ruler]. Defines a line that can be used by parent layouts to align or position their
 * children horizontally. The position of the ruler can be retrieved with
 * [Placeable.PlacementScope.current] and can be set with [MeasureScope.layout] using
 * [RulerScope.provides] or [RulerScope.providesRelative].
 */
class VerticalRuler private constructor(calculation: (PlacementScope.(Float) -> Float)?) :
    Ruler(calculation) {
    /**
     * Creates a [VerticalRuler] whose values are directly provided. The developer can set the ruler
     * value in [MeasureScope.layout] using [RulerScope.provides] or [RulerScope.providesRelative].
     */
    constructor() : this(null)

    override fun calculateCoordinate(
        coordinate: Float,
        sourceCoordinates: LayoutCoordinates,
        targetCoordinates: LayoutCoordinates,
    ): Float {
        val offset = Offset(coordinate, sourceCoordinates.size.height / 2f)
        return targetCoordinates.localPositionOf(sourceCoordinates, offset).x
    }

    companion object {
        /**
         * Creates a [VerticalRuler] derived from the greater value of all [VerticalRuler]s in
         * [rulers] that supply a value. This is the bottom-most of all provided ruler values.
         */
        fun maxOf(vararg rulers: VerticalRuler) = derived { defaultValue ->
            mergeRulerValues(true, rulers, defaultValue)
        }

        /**
         * Creates a [VerticalRuler] derived from the least value of all [VerticalRuler]s in
         * [rulers] that supply a value. This is the top-most of all provided ruler values.
         */
        fun minOf(vararg rulers: VerticalRuler) = derived { defaultValue ->
            mergeRulerValues(false, rulers, defaultValue)
        }

        /**
         * Creates a [VerticalRuler] whose values are derived from values available in the
         * [PlacementScope], such as other [VerticalRuler]s.
         *
         * @param calculation A function that calculates the value of the ruler
         * @sample androidx.compose.ui.samples.DerivedVerticalRulerUsage
         * @see minOf
         * @see maxOf
         */
        fun derived(calculation: PlacementScope.(defaultValue: Float) -> Float) =
            VerticalRuler(calculation)
    }
}

/**
 * A horizontal [Ruler]. Defines a line that can be used by parent layouts to align or position
 * their children vertically. The position of the ruler can be retrieved with
 * [Placeable.PlacementScope.current] and can be set with [MeasureScope.layout] using
 * [RulerScope.provides].
 */
class HorizontalRuler private constructor(calculation: (PlacementScope.(Float) -> Float)?) :
    Ruler(calculation) {
    /**
     * Creates a [HorizontalRuler] whose values are directly provided. The developer can set the
     * ruler value in [MeasureScope.layout] using [RulerScope.provides] or
     * [RulerScope.providesRelative].
     */
    constructor() : this(null)

    override fun calculateCoordinate(
        coordinate: Float,
        sourceCoordinates: LayoutCoordinates,
        targetCoordinates: LayoutCoordinates,
    ): Float {
        val offset = Offset(sourceCoordinates.size.width / 2f, coordinate)
        return targetCoordinates.localPositionOf(sourceCoordinates, offset).y
    }

    companion object {
        /**
         * Creates a [HorizontalRuler] derived from the greater value of all [HorizontalRuler]s in
         * [rulers] that supply a value. This is the right-most of all provided ruler values.
         */
        fun maxOf(vararg rulers: HorizontalRuler) = HorizontalRuler { defaultValue ->
            mergeRulerValues(true, rulers, defaultValue)
        }

        /**
         * Creates a [HorizontalRuler] derived from the least value of all [HorizontalRuler]s in
         * [rulers] that supply a value. This is the left-most of all provided ruler values.
         */
        fun minOf(vararg rulers: HorizontalRuler) = HorizontalRuler { defaultValue ->
            mergeRulerValues(false, rulers, defaultValue)
        }

        /**
         * Creates a [HorizontalRuler] whose values are derived from values available in the
         * [PlacementScope], such as other [HorizontalRuler]s.
         *
         * @param calculation A function that calculates the value of the ruler
         * @sample androidx.compose.ui.samples.DerivedHorizontalRulerUsage
         * @see minOf
         * @see maxOf
         */
        fun derived(calculation: PlacementScope.(defaultValue: Float) -> Float) =
            HorizontalRuler(calculation)
    }
}

private fun PlacementScope.mergeRulerValues(
    useGreater: Boolean,
    rulers: Array<out Ruler>,
    defaultValue: Float,
): Float {
    var value = Float.NaN
    rulers.forEach {
        val nextValue = it.current(Float.NaN)
        if (value.isNaN() || (useGreater == (nextValue > value))) {
            value = nextValue
        }
    }
    return if (value.isNaN()) defaultValue else value
}
