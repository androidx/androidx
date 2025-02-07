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
 * Creates an AnchorPlacement for a MovableComponent.
 *
 * <p> This will enable the MovableComponent to automatically anchor the attached entity to a new
 * entity
 */
public class AnchorPlacement() {
    internal val planeTypeFilter: MutableSet<@PlaneTypeValue Int> = HashSet<@PlaneTypeValue Int>()
    internal val planeSemanticFilter: MutableSet<@PlaneSemanticValue Int> =
        HashSet<@PlaneSemanticValue Int>()

    public companion object {
        /**
         * Creates an anchor placement for anchoring to planes.
         *
         * Setting a [PlaneType] or [PlaneSemantic] filter means that the [Entity] with a
         * [MovableComponent] will be anchored to a plane of that matches at least one of the
         * specified [PlaneType] filters and at least one specified [PlaneSemantic] filters if it is
         * released nearby. If no [PlaneType] or no [PlaneSemantic] is set the [Entity] will not be
         * anchored.
         *
         * <p> When an entity is anchored to the plane the pose will be rotated so that it's
         * Z-vector will be pointing our of the plane (i.e. if it is a panel it will be flat along
         * the plane. The onMoveEnd callback can be used to listen for the [Entity] being anchored,
         * reanchored, or unanchored. When anchored the parent will be updated to a new anchor
         * entity. When unanchored the parent will be set to the [ActivitySpace].
         *
         * @param planeTypeFilter The set of plane types to filter by.
         * @param planeSemanticFilter The set of plane semantics to filter by.
         */
        @JvmStatic
        @JvmOverloads
        public fun createForPlanes(
            planeTypeFilter: Set<@PlaneTypeValue Int> = setOf(PlaneType.ANY),
            planeSemanticFilter: Set<@PlaneSemanticValue Int> = setOf(PlaneSemantic.ANY),
        ): AnchorPlacement {
            val placement = AnchorPlacement()
            placement.planeTypeFilter.addAll(planeTypeFilter)
            placement.planeSemanticFilter.addAll(planeSemanticFilter)
            return placement
        }
    }
}
