/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.Px
import androidx.ui.unit.PxPosition
import androidx.ui.unit.round

/**
 * A [Placeable] corresponds to a child layout that can be positioned by its
 * parent layout. Most [Placeable]s are the result of a [Measurable.measure] call.
 *
 * A `Placeable` should never be stored between measure calls.
 */
abstract class Placeable {
    /**
     * The width, in pixels, of the measured layout. This is the `width`
     * value passed into [MeasureScope.layout].
     */
    val width: IntPx get() = size.width

    /**
     * The height, in pixels, of the measured layout. This is the `height`
     * value passed into [MeasureScope.layout].
     */
    val height: IntPx get() = size.height

    /**
     * Returns the position of an [alignment line][AlignmentLine],
     * or `null` if the line is not provided.
     */
    abstract operator fun get(line: AlignmentLine): IntPx?

    /**
     * The measured size of this Placeable.
     */
    abstract val size: IntPxSize

    /**
     * Positions the [Placeable] at [position] in its parent's coordinate system.
     */
    protected abstract fun performPlace(position: IntPxPosition)

    /**
     * Receiver scope that permits explicit placement of a [Placeable].
     *
     * While a [Placeable] may be placed at any time, this explicit receiver scope is used
     * to discourage placement outside of [MeasureScope.layout] positioning blocks.
     * This permits Compose UI to perform additional layout optimizations allowing repositioning
     * a [Placeable] without remeasuring its original [Measurable] if factors contributing to its
     * potential measurement have not changed.
     */
    companion object PlacementScope {
        /**
         * Place a [Placeable] at [position] in its parent's coordinate system.
         */
        fun Placeable.place(position: IntPxPosition) {
            performPlace(position)
        }

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system.
         */
        fun Placeable.place(position: PxPosition) {
            performPlace(position.round())
        }

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system.
         */
        fun Placeable.place(x: IntPx, y: IntPx) = place(IntPxPosition(x, y))

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system.
         */
        fun Placeable.place(x: Px, y: Px) = place(IntPxPosition(x.round(), y.round()))
    }
}