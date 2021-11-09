/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.data

import android.os.Parcelable
import androidx.health.services.client.data.UserActivityState.USER_ACTIVITY_ASLEEP
import androidx.health.services.client.data.UserActivityState.USER_ACTIVITY_EXERCISE
import androidx.health.services.client.data.UserActivityState.USER_ACTIVITY_PASSIVE
import androidx.health.services.client.data.UserActivityState.USER_ACTIVITY_UNKNOWN
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.UserActivityInfo as UserActivityInfoProto
import java.time.Instant

/**
 * Represents an update from Passive tracking.
 *
 * Provides [DataPoint] s associated with the Passive tracking, in addition to data related to the
 * user's [UserActivityState].
 */
@Suppress("ParcelCreator")
public class UserActivityInfo(
    /** The [UserActivityState] of the user from Passive tracking. */
    public val userActivityState: UserActivityState,

    /**
     * The [ExerciseInfo] of the user for a [UserActivityState.USER_ACTIVITY_EXERCISE] state, and
     * `null` for other [UserActivityState] s.
     */
    public val exerciseInfo: ExerciseInfo?,

    /** The time at which the current state took effect. */
    public val stateChangeTime: Instant,
) : ProtoParcelable<UserActivityInfoProto>() {

    internal constructor(
        proto: DataProto.UserActivityInfo
    ) : this(
        UserActivityState.fromProto(proto.state),
        if (proto.hasExerciseInfo()) ExerciseInfo(proto.exerciseInfo) else null,
        Instant.ofEpochMilli(proto.stateChangeTimeEpochMs)
    )

    /** @hide */
    override val proto: UserActivityInfoProto by lazy {
        val builder =
            UserActivityInfoProto.newBuilder()
                .setState(userActivityState.toProto())
                .setStateChangeTimeEpochMs(stateChangeTime.toEpochMilli())

        exerciseInfo?.let { builder.exerciseInfo = it.proto }
        builder.build()
    }

    override fun toString(): String =
        "UserActivityInfo(" +
            "userActivityState=$userActivityState, " +
            "stateChangeTime=$stateChangeTime, " +
            "exerciseInfo=$exerciseInfo)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<UserActivityInfo> = newCreator { bytes ->
            val proto = UserActivityInfoProto.parseFrom(bytes)
            UserActivityInfo(proto)
        }

        /** Creates a [UserActivityInfo] for [USER_ACTIVITY_UNKNOWN]. */
        @JvmStatic
        public fun createUnknownTypeState(stateChangeTime: Instant): UserActivityInfo =
            UserActivityInfo(USER_ACTIVITY_UNKNOWN, exerciseInfo = null, stateChangeTime)

        /** Creates a [UserActivityInfo] for [USER_ACTIVITY_EXERCISE]. */
        @JvmStatic
        public fun createActiveExerciseState(
            exerciseInfo: ExerciseInfo,
            stateChangeTime: Instant
        ): UserActivityInfo =
            UserActivityInfo(USER_ACTIVITY_EXERCISE, exerciseInfo, stateChangeTime)

        /** Creates a [UserActivityInfo] for [USER_ACTIVITY_PASSIVE]. */
        @JvmStatic
        public fun createPassiveActivityState(stateChangeTime: Instant): UserActivityInfo =
            UserActivityInfo(USER_ACTIVITY_PASSIVE, exerciseInfo = null, stateChangeTime)

        /** Creates a [UserActivityInfo] for [USER_ACTIVITY_ASLEEP]. */
        @JvmStatic
        public fun createAsleepState(stateChangeTime: Instant): UserActivityInfo =
            UserActivityInfo(USER_ACTIVITY_ASLEEP, exerciseInfo = null, stateChangeTime)
    }
}
