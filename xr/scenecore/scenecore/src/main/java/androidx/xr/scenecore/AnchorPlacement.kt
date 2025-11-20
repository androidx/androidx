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

/**
 * AnchorPlacement for setting how an [Entity] should be anchored in a [MovableComponent].
 *
 * When an AnchorPlacement is added to a MovableComponent, the attached Entity may be automatically
 * anchored and reparented to a new Entity at the end of the movement.
 */
public class AnchorPlacement
private constructor(
    public val anchorablePlaneOrientations: Set<@PlaneOrientationValue Int>,
    public val anchorablePlaneSemanticTypes: Set<@PlaneSemanticTypeValue Int>,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnchorPlacement) return false

        if (anchorablePlaneOrientations != other.anchorablePlaneOrientations) return false
        if (anchorablePlaneSemanticTypes != other.anchorablePlaneSemanticTypes) return false
        return true
    }

    override fun hashCode(): Int {
        var result = anchorablePlaneOrientations.hashCode()
        result = 31 * result + anchorablePlaneSemanticTypes.hashCode()
        return result
    }

    override fun toString(): String {
        return "AnchorPlacement(anchorablePlaneOrientations=${anchorablePlaneOrientations}, anchorablePlaneSemanticTypes=${anchorablePlaneSemanticTypes})"
    }

    public companion object {
        /**
         * Creates an anchor placement for anchoring to planes.
         *
         * Setting a [PlaneOrientation] or [PlaneSemanticType] filter means that the [Entity] with a
         * [MovableComponent] will be anchored to a plane of that matches at least one of the
         * specified PlaneOrientation filters and at least one specified PlaneSemanticType filters
         * if it is released nearby. By default this the Entity will be anchored to any plane. If no
         * PlaneOrientation or no PlaneSemanticType is set the Entity will not be anchored. This
         * setting requires [androidx.xr.runtime.Session.configure] to be called with
         * [androidx.xr.runtime.Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL] in order to be
         * able to discover planes.
         *
         * When an Entity is anchored to the plane the pose will be rotated so that it's Z-vector
         * will be pointing out of the plane (i.e. if it is a panel it will be flat along the
         * plane). The [EntityMoveListener.onMoveEnd] callback can be used to listen for the Entity
         * being anchored, reanchored, or unanchored. When anchored the parent will be updated to a
         * new anchor Entity. When unanchored the parent will be set to the [ActivitySpace].
         *
         * @param anchorablePlaneOrientations The set of plane types to filter by.
         * @param anchorablePlaneSemanticTypes The set of plane semantics to filter by.
         */
        @JvmStatic
        @JvmOverloads
        public fun createForPlanes(
            anchorablePlaneOrientations: Set<@PlaneOrientationValue Int> =
                setOf(PlaneOrientation.ANY),
            anchorablePlaneSemanticTypes: Set<@PlaneSemanticTypeValue Int> =
                setOf(PlaneSemanticType.ANY),
        ): AnchorPlacement {
            return AnchorPlacement(
                anchorablePlaneOrientations = anchorablePlaneOrientations.toSet(),
                anchorablePlaneSemanticTypes = anchorablePlaneSemanticTypes.toSet(),
            )
        }
    }
}
