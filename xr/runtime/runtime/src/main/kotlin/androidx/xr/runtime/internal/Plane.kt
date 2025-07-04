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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2

/** Describes the current best knowledge of a real-world planar surface. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Plane : Trackable {
    /** The [Type] of the plane */
    public val type: Type

    /* The [Label] of the plane */
    public val label: Label

    /** The center of the detected plane. */
    public val centerPose: Pose

    /** The dimensions of the detected plane. */
    public val extents: Vector2

    /** If this plane has been subsumed, returns the plane this plane was merged into. */
    public val subsumedBy: Plane?

    /**
     * Returns the 2D vertices (three or more) of a convex polygon approximating the detected plane.
     */
    public val vertices: List<Vector2>

    /** Simple summary of the normal vector of a plane, for filtering purposes. */
    public class Type private constructor(private val name: Int) {
        public companion object {
            /** A horizontal plane facing upward (e.g. floor or tabletop). */
            @JvmField public val HORIZONTAL_UPWARD_FACING: Type = Type(0)

            /** A horizontal plane facing downward (e.g. a ceiling). */
            @JvmField public val HORIZONTAL_DOWNWARD_FACING: Type = Type(1)

            /** A vertical plane (e.g. a wall). */
            @JvmField public val VERTICAL: Type = Type(2)
        }
    }

    /** A semantic description of a [Plane]. */
    public class Label private constructor(private val name: Int) {
        public companion object {
            /** A plane of unknown type. */
            @JvmField public val UNKNOWN: Label = Label(0)

            /** A plane that represents a wall. */
            @JvmField public val WALL: Label = Label(1)

            /** A plane that represents a floor. */
            @JvmField public val FLOOR: Label = Label(2)

            /** A plane that represents a ceiling. */
            @JvmField public val CEILING: Label = Label(3)

            /** A plane that represents a table. */
            @JvmField public val TABLE: Label = Label(4)
        }
    }
}
