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

import androidx.annotation.RestrictTo
import androidx.health.services.client.proto.DataProto.UserActivityState as UserActivityStateProto

/** Types of user activity states. */
public class UserActivityState(public val id: Int, public val name: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserActivityState) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id

    override fun toString(): String = name

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun toProto(): UserActivityStateProto =
        UserActivityStateProto.forNumber(id) ?: UserActivityStateProto.USER_ACTIVITY_STATE_UNKNOWN

    public companion object {
        /**
         * The current activity state cannot be determined, or it is a new state that this library
         * version is too old to recognize.
         */
        @JvmField
        public val USER_ACTIVITY_UNKNOWN: UserActivityState =
            UserActivityState(0, "USER_ACTIVITY_UNKNOWN")

        /** The user is currently exercising. */
        @JvmField
        public val USER_ACTIVITY_EXERCISE: UserActivityState =
            UserActivityState(1, "USER_ACTIVITY_EXERCISE")

        /** The user is awake but is not currently exercising. */
        @JvmField
        public val USER_ACTIVITY_PASSIVE: UserActivityState =
            UserActivityState(2, "USER_ACTIVITY_PASSIVE")

        /** The user is asleep. */
        @JvmField
        public val USER_ACTIVITY_ASLEEP: UserActivityState =
            UserActivityState(3, "USER_ACTIVITY_ASLEEP")

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        public val VALUES: List<UserActivityState> =
            listOf(
                USER_ACTIVITY_UNKNOWN,
                USER_ACTIVITY_EXERCISE,
                USER_ACTIVITY_PASSIVE,
                USER_ACTIVITY_ASLEEP,
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun fromProto(proto: UserActivityStateProto): UserActivityState =
            VALUES.firstOrNull { it.id == proto.number } ?: USER_ACTIVITY_UNKNOWN
    }
}
