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

import androidx.annotation.IntDef
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray

/**
 * A high-level move event which is sent in response to the User interacting with the Entity.
 *
 * @param moveState State of the move action i.e. move started, ongoing or ended.
 * @param initialInputRay Ray for the user's input at initial update.
 * @param currentInputRay Ray for the user's input at the new update.
 * @param previousPose Pose before this event, relative to its parent.
 * @param currentPose Pose when this event is applied, relative to its parent.
 * @param previousScale Scale before this event.
 * @param currentScale Scale when this event is applied.
 */
internal class MoveEvent(
    @MoveState public val moveState: Int,
    public val initialInputRay: Ray,
    public val currentInputRay: Ray,
    public val previousPose: Pose,
    public val currentPose: Pose,
    public val previousScale: Float,
    public val currentScale: Float,
    public val initialParent: Entity,
    public val updatedParent: Entity?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MoveEvent) return false

        if (moveState != other.moveState) return false
        if (initialInputRay != other.initialInputRay) return false
        if (currentInputRay != other.currentInputRay) return false
        if (previousPose != other.previousPose) return false
        if (currentPose != other.currentPose) return false
        if (previousScale != other.previousScale) return false
        if (currentScale != other.currentScale) return false
        if (initialParent != other.initialParent) return false
        if (updatedParent != other.updatedParent) return false
        return true
    }

    override fun hashCode(): Int {
        var result = moveState.hashCode()
        result = 31 * result + initialInputRay.hashCode()
        result = 31 * result + currentInputRay.hashCode()
        result = 31 * result + previousPose.hashCode()
        result = 31 * result + currentPose.hashCode()
        result = 31 * result + previousScale.hashCode()
        result = 31 * result + currentScale.hashCode()
        result = 31 * result + initialParent.hashCode()
        if (updatedParent != null) {
            result = 31 * result + updatedParent.hashCode()
        }
        return result
    }

    public companion object {
        public const val MOVE_STATE_START: Int = 1
        public const val MOVE_STATE_ONGOING: Int = 2
        public const val MOVE_STATE_END: Int = 3
    }

    @IntDef(value = [MOVE_STATE_START, MOVE_STATE_ONGOING, MOVE_STATE_END])
    public annotation class MoveState
}

/** Listener for move actions. Callbacks are invoked as user interacts with the entity. */
public interface MoveListener {
    /**
     * Called when the user starts moving the entity.
     *
     * @param entity The entity being moved.
     * @param initialInputRay Ray for the user's input at initial update.
     * @param initialPose Initial Pose of the entity relative to its parent.
     * @param initialScale Initial scale of the entity.
     * @param initialParent Initial parent of the entity.
     */
    public fun onMoveStart(
        entity: Entity,
        initialInputRay: Ray,
        initialPose: Pose,
        initialScale: Float,
        initialParent: Entity,
    ) {}

    /**
     * Called continuously while the user is moving the entity.
     *
     * @param entity The entity being moved.
     * @param currentInputRay Ray for the user's input at the new update.
     * @param currentPose Pose of the entity during this event relative to its parent.
     * @param currentScale Scale of the entity during this event.
     */
    public fun onMoveUpdate(
        entity: Entity,
        currentInputRay: Ray,
        currentPose: Pose,
        currentScale: Float,
    ) {}

    /**
     * Called when the user has finished moving the entity.
     *
     * @param entity The entity being moved.
     * @param finalInputRay Ray for the user's input at the final update.
     * @param finalPose Pose of the entity during this event relative to its parent.
     * @param finalScale Scale of the entity during this event.
     * @param updatedParent If anchorPlacement is set, the entity may have a new parent when the
     *   movement completes. This will be a new AnchorEntity, if it was anchored or re-anchored
     *   during the movement, or the activity space, if it was unanchored. This will be null if
     *   there was no updated parent on the entity.
     */
    public fun onMoveEnd(
        entity: Entity,
        finalInputRay: Ray,
        finalPose: Pose,
        finalScale: Float,
        updatedParent: Entity?,
    ) {}
}
