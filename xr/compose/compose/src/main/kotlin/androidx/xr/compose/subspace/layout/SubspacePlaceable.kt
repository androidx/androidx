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
 * A [SubspacePlaceable] corresponds to a child layout that can be positioned by its parent layout.
 * Most [SubspacePlaceables][SubspacePlaceable] are the result of a [SubspaceMeasurable.measure]
 * call.
 *
 * Based on [androidx.compose.ui.layout.Placeable].
 */
public abstract class SubspacePlaceable {
    /** The measured width of the layout, in pixels. */
    public var measuredWidth: Int = 0
        protected set

    /** The measured height of the layout, in pixels. */
    public var measuredHeight: Int = 0
        protected set

    /** The measured depth of the layout, in pixels. */
    public var measuredDepth: Int = 0
        protected set

    /** Positions the [SubspacePlaceable] at [pose] in its parent's coordinate system. */
    protected abstract fun placeAt(pose: Pose)

    /** Receiver scope that permits explicit placement of a [SubspacePlaceable]. */
    public abstract class SubspacePlacementScope {
        /**
         * The [SubspaceLayoutCoordinates] of this layout, if known or `null` if the layout hasn't
         * been placed yet.
         */
        public open val coordinates: SubspaceLayoutCoordinates?
            get() = null

        /** Place a [SubspacePlaceable] at the [Pose] in its parent's coordinate system. */
        public fun SubspacePlaceable.place(pose: Pose) {
            placeAt(pose)
        }
    }
}
