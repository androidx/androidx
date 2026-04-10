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
package androidx.xr.scenecore

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.PlaneOrientation.Companion.HORIZONTAL
import androidx.xr.scenecore.PlaneOrientation.Companion.VERTICAL

/**
 * Orientation of a plane, to specify valid surfaces for anchoring.
 *
 * For example, see [MovableComponent.createAnchorable].
 */
public class PlaneOrientation private constructor(private val value: Int) {
    // Note: These constants have some overlap with androidx.xr.arcore.Plane.Type, though the
    // constants here are specifically for filtering anchors. Due to the difference in semantics,
    // the constants are not shared between ARCore and SceneCore.

    public companion object {

        /** Any plane orientation is acceptable. */
        // TODO: b/500464864 - Remove this constant.
        @Deprecated("Explicitly enumerate PlaneOrientation constants, or use ALL")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmField
        public val ANY: PlaneOrientation = PlaneOrientation(0)

        /** Specify horizontal planes. */
        @JvmField public val HORIZONTAL: PlaneOrientation = PlaneOrientation(1)

        /** Specify vertical planes. */
        @JvmField public val VERTICAL: PlaneOrientation = PlaneOrientation(2)

        /** Immutable Set containing all PlaneOrientations. */
        @JvmField public val ALL: Set<PlaneOrientation> = setOf(HORIZONTAL, VERTICAL)
    }

    public override fun toString(): String = value.toString()
}

/**
 * The detected semantic type of a plane, to specify valid surfaces for anchoring.
 *
 * For example, see [MovableComponent.createAnchorable].
 */
public class PlaneSemanticType private constructor(private val value: Int) {
    // Note: These constants have some overlap with androidx.xr.arcore.Plane.Label, though the
    // constants here are specifically for filtering anchors. Due to the difference in semantics,
    // the constants are not shared between ARCore and SceneCore.

    public companion object {
        /** Any plane type is acceptable. */
        // TODO: b/500464864 - Remove this constant.
        @Deprecated("Explicitly enumerate PlaneSemanticType constants, or use ALL")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmField
        public val ANY: PlaneSemanticType = PlaneSemanticType(0)

        /** Specify planes that are identified as a wall. */
        @JvmField public val WALL: PlaneSemanticType = PlaneSemanticType(1)

        /** Specify planes that are identified as the floor. */
        @JvmField public val FLOOR: PlaneSemanticType = PlaneSemanticType(2)

        /** Specify planes that are identified as the ceiling. */
        @JvmField public val CEILING: PlaneSemanticType = PlaneSemanticType(3)

        /** Specify planes that are identified as a table. */
        @JvmField public val TABLE: PlaneSemanticType = PlaneSemanticType(4)

        /** Immutable Set containing all PlaneSemanticTypes. */
        @JvmField public val ALL: Set<PlaneSemanticType> = setOf(WALL, FLOOR, CEILING, TABLE)
    }

    public override fun toString(): String = value.toString()
}
