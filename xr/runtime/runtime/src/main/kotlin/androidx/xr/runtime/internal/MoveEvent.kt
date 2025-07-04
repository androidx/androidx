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
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3

/**
 * MoveEvent for XR Runtime Platform.
 *
 * @param moveState state of the move action.
 * @param initialInputRay initial ray origin and direction in activity space.
 * @param currentInputRay current ray origin and direction in activity space.
 * @param previousPose previous pose of the entity, relative to its parent.
 * @param currentPose current pose of the entity, relative to its parent.
 * @param previousScale previous scale of the entity.
 * @param currentScale current scale of the entity.
 * @param initialParent initial Parent of the entity at the start of the move.
 * @param updatedParent updates parent of the entity at the end of the move or null if not updated.
 * @param disposedEntity reports an entity that was disposed and needs to be removed from the sdk
 *   EntityManager.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class MoveEvent(
    @MoveState public val moveState: Int,
    public val initialInputRay: Ray,
    public val currentInputRay: Ray,
    public val previousPose: Pose,
    public val currentPose: Pose,
    public val previousScale: Vector3,
    public val currentScale: Vector3,
    public val initialParent: Entity,
    public val updatedParent: Entity?,
    public val disposedEntity: Entity?,
) {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public annotation class MoveState {
        public companion object {
            public const val MOVE_STATE_START: Int = 0
            public const val MOVE_STATE_ONGOING: Int = 1
            public const val MOVE_STATE_END: Int = 2
        }
    }

    public companion object {
        // TODO: b/350370142 - Use public getter/setter interfaces instead of public fields.
        public const val MOVE_STATE_START: Int = 1
        public const val MOVE_STATE_ONGOING: Int = 2
        public const val MOVE_STATE_END: Int = 3
    }
}
