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

package androidx.xr.runtime

import kotlin.time.ComparableTimeMark

/**
 * Represents the state of the XR system at a specific point in time.
 *
 * Instances of this class can be accessed via the [Session.state] [StateFlow] property. This class
 * may include extension properties provided by implementations of the [StateExtender] interface
 * found during [Session] creation.
 *
 * @property timeMark at which the state was computed.
 */
public class CoreState(public val timeMark: ComparableTimeMark) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CoreState) return false
        if (timeMark != other.timeMark) return false
        return true
    }

    override fun hashCode(): Int = timeMark.hashCode()

    override fun toString(): String = "CoreState(timeMark=$timeMark)"
}
