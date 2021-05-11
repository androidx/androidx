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

import android.os.Parcel
import android.os.Parcelable
import androidx.health.services.client.data.UserActivityState.USER_ACTIVITY_ASLEEP
import androidx.health.services.client.data.UserActivityState.USER_ACTIVITY_EXERCISE
import androidx.health.services.client.data.UserActivityState.USER_ACTIVITY_INACTIVE
import androidx.health.services.client.data.UserActivityState.USER_ACTIVITY_PASSIVE
import androidx.health.services.client.data.UserActivityState.USER_ACTIVITY_UNKNOWN
import java.time.Instant

/**
 * Represents an update from Passive tracking.
 *
 * Provides [DataPoint] s associated with the Passive tracking, in addition to data related to the
 * user's [UserActivityState].
 */
public data class UserActivityInfo(
    /** The [UserActivityState] of the user from Passive tracking. */
    val userActivityState: UserActivityState,

    /**
     * The [ExerciseType] of the user for a [UserActivityState.USER_ACTIVITY_EXERCISE] state, and
     * `null` for other [UserActivityState] s.
     */
    val exerciseType: ExerciseType?,

    /** The time at which the current state took effect. */
    val stateChangeTime: Instant,
) : Parcelable {

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(userActivityState.id)
        dest.writeInt(exerciseType?.id ?: -1)
        dest.writeLong(stateChangeTime.toEpochMilli())
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<UserActivityInfo> =
            object : Parcelable.Creator<UserActivityInfo> {
                override fun createFromParcel(source: Parcel): UserActivityInfo? {
                    val activityState = UserActivityState.fromId(source.readInt()) ?: return null
                    val exerciseTypeId = source.readInt()
                    val exerciseType =
                        if (exerciseTypeId == -1) null else ExerciseType.fromId(exerciseTypeId)
                    val time = Instant.ofEpochMilli(source.readLong())

                    return UserActivityInfo(activityState, exerciseType, time)
                }

                override fun newArray(size: Int): Array<UserActivityInfo?> {
                    return arrayOfNulls(size)
                }
            }

        /** Creates a [UserActivityInfo] for [USER_ACTIVITY_UNKNOWN]. */
        @JvmStatic
        public fun createUnknownTypeState(stateChangeTime: Instant): UserActivityInfo =
            UserActivityInfo(USER_ACTIVITY_UNKNOWN, exerciseType = null, stateChangeTime)

        /** Creates a [UserActivityInfo] for [USER_ACTIVITY_EXERCISE]. */
        @JvmStatic
        public fun createActiveExerciseState(
            exerciseType: ExerciseType,
            stateChangeTime: Instant
        ): UserActivityInfo =
            UserActivityInfo(USER_ACTIVITY_EXERCISE, exerciseType, stateChangeTime)

        /** Creates a [UserActivityInfo] for [USER_ACTIVITY_PASSIVE]. */
        @JvmStatic
        public fun createPassiveActivityState(stateChangeTime: Instant): UserActivityInfo =
            UserActivityInfo(USER_ACTIVITY_PASSIVE, exerciseType = null, stateChangeTime)

        /** Creates a [UserActivityInfo] for [USER_ACTIVITY_INACTIVE]. */
        @JvmStatic
        public fun createInactiveState(stateChangeTime: Instant): UserActivityInfo =
            UserActivityInfo(USER_ACTIVITY_INACTIVE, exerciseType = null, stateChangeTime)

        /** Creates a [UserActivityInfo] for [USER_ACTIVITY_ASLEEP]. */
        @JvmStatic
        public fun createAsleepState(stateChangeTime: Instant): UserActivityInfo =
            UserActivityInfo(USER_ACTIVITY_ASLEEP, exerciseType = null, stateChangeTime)
    }
}
