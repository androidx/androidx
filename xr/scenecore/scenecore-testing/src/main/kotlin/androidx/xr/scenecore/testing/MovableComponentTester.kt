/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.runtime.MoveEvent as RtMoveEvent
import androidx.xr.scenecore.testing.internal.FakeMovableComponent as InternalFakeMovableComponent

/**
 * A test-only accessor for [MovableComponent] that enables direct manipulation and inspection of
 * its internal state.
 */
public class MovableComponentTester
internal constructor(
    private val rtMovableComponent: InternalFakeMovableComponent,
    internal val movableComponent: MovableComponent,
) {

    private enum class State {
        IDLE,
        STARTED,
        UPDATING,
    }

    private var state = State.IDLE
    private var initialInputRay: Ray? = null
    private var initialParent: Entity? = null
    private var currentInputRay: Ray? = null
    private var currentPose: Pose? = null
    private var currentScale: Vector3? = null

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [MovableComponent].
         *
         * This function provides a [MovableComponentTester] instance, which can be used to inspect
         * and manipulate its underlying data in the test environment.
         *
         * @param movableComponent The component for which to retrieve the test data accessor.
         * @return A [MovableComponentTester] instance for the given component.
         */
        internal fun create(movableComponent: MovableComponent): MovableComponentTester {
            return MovableComponentTester(
                (movableComponent.rtMovableComponent as FakeMovableComponent).fakeInternal,
                movableComponent,
            )
        }
    }

    /**
     * Whether this component is system-movable.
     *
     * This is set to `true` when created via [MovableComponent.createSystemMovable] or
     * [MovableComponent.createAnchorable], and `false` when created via
     * [MovableComponent.createCustomMovable].
     */
    public val isSystemMovable: Boolean
        get() = rtMovableComponent.systemMovable

    /**
     * Whether this component scales in Z.
     *
     * This corresponds to the `scaleInZ` parameter used in [MovableComponent.createCustomMovable]
     * or [MovableComponent.createSystemMovable]. It is always `false` when created via
     * [MovableComponent.createAnchorable].
     */
    public val isScaleInZ: Boolean
        get() = rtMovableComponent.scaleInZ

    /**
     * Whether this component is user-anchorable.
     *
     * This is set to `true` when created via [MovableComponent.createAnchorable], and `false`
     * otherwise.
     */
    public val isAnchorable: Boolean
        get() = rtMovableComponent.userAnchorable

    /**
     * Simulates the start of a move event from the runtime, notifying all registered listeners.
     *
     * This triggers callbacks registered via [MovableComponent.addMoveListener].
     *
     * @param initialInputRay The initial ray origin and direction in activity space.
     * @param initialPose The initial pose of the entity, relative to its parent.
     * @param initialScale The initial scale of the entity.
     * @param initialParent The parent of the entity at the start of the move.
     * @throws IllegalStateException If a move is already in progress.
     */
    public fun triggerOnMoveStart(
        initialInputRay: Ray,
        initialPose: Pose,
        initialScale: Vector3,
        initialParent: Entity,
    ) {
        check(state == State.IDLE) { "Cannot start a new move while one is already in progress." }

        this.initialInputRay = initialInputRay
        this.initialParent = initialParent
        this.currentInputRay = initialInputRay
        this.currentPose = initialPose
        this.currentScale = initialScale
        this.state = State.STARTED

        val rtEvent =
            RtMoveEvent(
                moveState = RtMoveEvent.MOVE_STATE_START,
                initialInputRay = initialInputRay,
                currentInputRay = initialInputRay,
                previousPose = initialPose,
                currentPose = initialPose,
                previousScale = initialScale,
                currentScale = initialScale,
                initialParent = initialParent.rtEntity,
                updatedParent = null,
                disposedEntity = null,
            )
        rtMovableComponent.onMoveEvent(rtEvent)
    }

    /**
     * Simulates an update to an ongoing move event from the runtime, notifying all registered
     * listeners.
     *
     * This triggers callbacks registered via [MovableComponent.addMoveListener].
     *
     * @param currentInputRay The current ray origin and direction in activity space.
     * @param currentPose The current pose of the entity, relative to its parent.
     * @param currentScale The current scale of the entity.
     * @throws IllegalStateException If [triggerOnMoveStart] has not been called.
     */
    public fun triggerOnMoveUpdate(currentInputRay: Ray, currentPose: Pose, currentScale: Vector3) {
        check(state == State.STARTED || state == State.UPDATING) {
            "Must call triggerOnMoveStart before triggerOnMoveUpdate."
        }

        val previousPose = this.currentPose!!
        val previousScale = this.currentScale!!

        this.currentInputRay = currentInputRay
        this.currentPose = currentPose
        this.currentScale = currentScale
        this.state = State.UPDATING

        val rtEvent =
            RtMoveEvent(
                moveState = RtMoveEvent.MOVE_STATE_ONGOING,
                initialInputRay = initialInputRay!!,
                currentInputRay = currentInputRay,
                previousPose = previousPose,
                currentPose = currentPose,
                previousScale = previousScale,
                currentScale = currentScale,
                initialParent = initialParent!!.rtEntity,
                updatedParent = null,
                disposedEntity = null,
            )
        rtMovableComponent.onMoveEvent(rtEvent)
    }

    /**
     * Simulates the end of a move event from the runtime, notifying all registered listeners.
     *
     * This triggers callbacks registered via [MovableComponent.addMoveListener].
     *
     * @param finalInputRay The final ray origin and direction in activity space.
     * @param finalPose The final pose of the entity, relative to its parent.
     * @param finalScale The final scale of the entity.
     * @param updatedParent The updated parent of the entity if it was changed during the move.
     * @throws IllegalStateException If [triggerOnMoveStart] has not been called.
     */
    @JvmOverloads
    public fun triggerOnMoveEnd(
        finalInputRay: Ray,
        finalPose: Pose,
        finalScale: Vector3,
        updatedParent: Entity? = null,
    ) {
        check(state == State.STARTED || state == State.UPDATING) {
            "Must call triggerOnMoveStart before triggerOnMoveEnd."
        }

        val previousPose = this.currentPose!!
        val previousScale = this.currentScale!!

        val rtEvent =
            RtMoveEvent(
                moveState = RtMoveEvent.MOVE_STATE_END,
                initialInputRay = initialInputRay!!,
                currentInputRay = finalInputRay,
                previousPose = previousPose,
                currentPose = finalPose,
                previousScale = previousScale,
                currentScale = finalScale,
                initialParent = initialParent!!.rtEntity,
                updatedParent = updatedParent?.rtEntity,
                disposedEntity = null,
            )
        rtMovableComponent.onMoveEvent(rtEvent)

        // Clear all cached state.
        this.initialInputRay = null
        this.initialParent = null
        this.currentInputRay = null
        this.currentPose = null
        this.currentScale = null
        this.state = State.IDLE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MovableComponentTester

        if (rtMovableComponent != other.rtMovableComponent) return false
        if (movableComponent != other.movableComponent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtMovableComponent.hashCode()
        result = 31 * result + movableComponent.hashCode()
        return result
    }
}
