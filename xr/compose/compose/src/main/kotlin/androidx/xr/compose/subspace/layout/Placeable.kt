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

package androidx.xr.compose.subspace.layout

import androidx.xr.runtime.math.Pose

/**
 * A [Placeable] corresponds to a child layout that can be positioned by its parent layout. Most
 * [Placeable]s are the result of a [Measurable.measure] call.
 *
 * Based on [androidx.compose.ui.layout.Placeable].
 */
public abstract class Placeable {
    /** The measured width of the layout, in pixels. */
    public var measuredWidth: Int = 0

    /** The measured height of the layout, in pixels. */
    public var measuredHeight: Int = 0

    /** The measured depth of the layout, in pixels. */
    public var measuredDepth: Int = 0

    /** Positions the [Placeable] at [position] in its parent's coordinate system. */
    protected abstract fun placeAt(pose: Pose)

    /** Receiver scope that permits explicit placement of a [Placeable]. */
    public abstract class PlacementScope {
        /**
         * The [SubspaceLayoutCoordinates] of this layout, if known or `null` if the layout hasn't
         * been placed yet.
         */
        public open val coordinates: SubspaceLayoutCoordinates?
            get() = null

        /** Place a [Placeable] at the [Pose] in its parent's coordinate system. */
        public fun Placeable.place(pose: Pose) {
            placeAt(pose)
        }
    }
}
