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

import android.content.ContentResolver
import android.provider.Settings.System
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.HandJointType
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Hand as RuntimeHand
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Contains the tracking information of one of the user's hands. */
public class Hand internal constructor(internal val runtimeHand: RuntimeHand) : Updatable {
    /** * Companion object holding info to the left and right hands. */
    public companion object {

        internal const val PRIMARY_HAND_SETTING_NAME = "primary_hand"

        /**
         * Returns the Hand object that corresponds to the user's left hand when available.
         *
         * @param session the currently active [Session].
         * @throws [IllegalStateException] if [Session.config] is set to
         *   [Config.HandTrackingMode.DISABLED].
         */
        @JvmStatic
        public fun left(session: Session): Hand? {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            val config = perceptionStateExtender.xrResourcesManager.lifecycleManager.config
            check(config.handTracking != Config.HandTrackingMode.DISABLED) {
                "Config.HandTrackingMode is set to DISABLED."
            }
            return perceptionStateExtender.xrResourcesManager.leftHand
        }

        /**
         * Returns the Hand object that corresponds to the user's right hand when available.
         *
         * @param session the currently active [Session].
         * @throws [IllegalStateException] if [Session.config] is set to
         *   [Config.HandTrackingMode.DISABLED].
         */
        @JvmStatic
        public fun right(session: Session): Hand? {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            val config = perceptionStateExtender.xrResourcesManager.lifecycleManager.config
            check(config.handTracking != Config.HandTrackingMode.DISABLED) {
                "Config.HandTrackingMode is set to DISABLED."
            }
            return perceptionStateExtender.xrResourcesManager.rightHand
        }

        /**
         * Returns the handedness of the user's primary hand.
         *
         * @param resolver the [ContentResolver] to use to retrieve the setting.
         * @return the [HandSide] of the user's primary hand. If the setting is not configured,
         *   returns [HandSide.UNKNOWN].
         */
        @JvmStatic
        public fun getPrimaryHandSide(resolver: ContentResolver): HandSide =
            HandSide.values()[
                    System.getInt(resolver, PRIMARY_HAND_SETTING_NAME, HandSide.UNKNOWN.ordinal)]

        private fun getPerceptionStateExtender(session: Session): PerceptionStateExtender {
            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }
            return perceptionStateExtender
        }
    }

    /** The handedness of the user's hand. */
    public enum class HandSide {
        LEFT,
        RIGHT,
        /** The handedness is not available if it is not explicitly set. */
        UNKNOWN,
    }

    /**
     * The representation of the current state of [Hand].
     *
     * @param trackingState the current [TrackingState] of the hand.
     */
    public class State
    internal constructor(
        public val trackingState: TrackingState,
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public val handJointsBuffer: FloatBuffer,
    ) {

        private class JointsMap(
            val trackingState: TrackingState,
            val handJointsBuffer: FloatBuffer,
        ) : Map<HandJointType, Pose> {
            override val entries: Set<Map.Entry<HandJointType, Pose>>
                get() =
                    if (trackingState == TrackingState.TRACKING) {
                        RuntimeHand.parseHandJoint(trackingState, handJointsBuffer).entries.toSet()
                    } else {
                        emptySet()
                    }

            override val keys: Set<HandJointType>
                get() =
                    if (trackingState == TrackingState.TRACKING) HandJointType.values().toSet()
                    else emptySet()

            override val size: Int
                get() = keys.size

            override val values: Collection<Pose>
                get() = entries.map { it.value }

            override fun containsKey(key: HandJointType): Boolean {
                return keys.contains(key)
            }

            override fun containsValue(value: Pose): Boolean {
                return values.contains(value)
            }

            override fun get(key: HandJointType): Pose? =
                if (trackingState == TrackingState.TRACKING) locateHandJointFromBuffer(key)
                else null

            override fun isEmpty(): Boolean {
                return trackingState != TrackingState.TRACKING
            }

            private fun locateHandJointFromBuffer(handJointType: HandJointType): Pose {
                val buffer = handJointsBuffer.duplicate()
                val floatOffset = handJointType.ordinal * FLOATS_PER_POSE
                buffer.position(floatOffset)
                val qx = buffer.get()
                val qy = buffer.get()
                val qz = buffer.get()
                val qw = buffer.get()
                val px = buffer.get()
                val py = buffer.get()
                val pz = buffer.get()
                return Pose(Vector3(px, py, pz), Quaternion(qx, qy, qz, qw))
            }
        }

        /**
         * Returns the current pose of each joint in the hand.
         *
         * @return a map of [HandJointType] to [Pose] representing the current pose of each joint in
         *   the hand.
         */
        public val handJoints: Map<HandJointType, Pose> = JointsMap(trackingState, handJointsBuffer)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return trackingState == other.trackingState &&
                handJointsBuffer == other.handJointsBuffer
        }

        override fun hashCode(): Int {
            var result = trackingState.hashCode()
            result = 31 * result + handJointsBuffer.hashCode()
            return result
        }

        private companion object {
            private const val FLOATS_PER_POSE = 7
        }
    }

    private val _state =
        MutableStateFlow<State>(State(TrackingState.PAUSED, ByteBuffer.allocate(0).asFloatBuffer()))
    /** The current [State] of this hand. */
    public val state: StateFlow<State> = _state.asStateFlow()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override suspend fun update() {
        _state.emit(State(runtimeHand.trackingState, runtimeHand.handJointsBuffer))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Hand) return false
        return runtimeHand == other.runtimeHand
    }

    override fun hashCode(): Int = runtimeHand.hashCode()
}
