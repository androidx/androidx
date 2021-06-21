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

/** Types of user activity states. */
public enum class UserActivityState(public val id: Int) {
    USER_ACTIVITY_UNKNOWN(0),
    USER_ACTIVITY_EXERCISE(1),
    USER_ACTIVITY_PASSIVE(2),
    USER_ACTIVITY_INACTIVE(3),
    USER_ACTIVITY_ASLEEP(4);

    public companion object {
        @JvmStatic
        public fun fromId(id: Int): UserActivityState? = values().firstOrNull { it.id == id }
    }
}
