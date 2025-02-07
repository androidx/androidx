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

package androidx.xr.arcore

import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.Hand as RuntimeHand
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Contains the tracking information of one of the user's hands. */
public class Hand internal constructor(internal val runtimeHand: RuntimeHand) : Updatable {
    /** * Companion object holding info to the left and right hands. */
    public companion object {
        /**
         * Returns the Hand object that corresponds to the user's left hand when available, or null
         * when the platform does not support the feature.
         *
         * @param session the currently active [Session].
         */
        @JvmStatic
        public fun left(session: Session): Hand? {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.leftHand
        }

        /**
         * Returns the Hand object that corresponds to the user's right hand when available, or null
         * when the platform does not support the feature.
         *
         * @param session the currently active [Session].
         */
        @JvmStatic
        public fun right(session: Session): Hand? {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.rightHand
        }

        private fun getPerceptionStateExtender(session: Session): PerceptionStateExtender {
            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }
            return perceptionStateExtender
        }
    }

    /**
     * The representation of the current state of [Hand].
     *
     * @property isActive whether the hand is currently being tracked.
     * @property handJoints the current pose of each joint in the hand.
     */
    public class State(
        public val isActive: Boolean,
        public val handJoints: Map<HandJointType, Pose>,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return isActive == other.isActive && handJoints == other.handJoints
        }

        override fun hashCode(): Int {
            var result = isActive.hashCode()
            result = 31 * result + handJoints.hashCode()
            return result
        }
    }

    private val _state = MutableStateFlow<State>(State(isActive = false, handJoints = emptyMap()))
    /** The current [State] of this hand. */
    public val state: StateFlow<State> = _state.asStateFlow()

    override suspend fun update() {
        _state.emit(State(isActive = runtimeHand.isActive, handJoints = runtimeHand.handJoints))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Hand) return false
        return runtimeHand == other.runtimeHand
    }

    override fun hashCode(): Int = runtimeHand.hashCode()
}
