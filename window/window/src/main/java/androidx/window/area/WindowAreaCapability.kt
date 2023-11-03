/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.area

import android.app.Activity
import androidx.window.core.ExperimentalWindowApi

/**
 * Represents a capability for a [WindowAreaInfo].
 */
@ExperimentalWindowApi
class WindowAreaCapability internal constructor(val operation: Operation, val status: Status) {
    override fun toString(): String {
        return "Operation: $operation: Status: $status"
    }

    /**
     * Represents the status of availability for a specific [WindowAreaCapability]
     */
    @ExperimentalWindowApi
    class Status private constructor(private val description: String) {
        override fun toString(): String {
            return description
        }

        companion object {
            /**
             * Status indicating that the WindowArea feature status is unknown, e.g. a status has
             * not been received from the extensions implementation yet. Note that this is an
             * internal status - external clients should receive [WINDOW_AREA_STATUS_UNSUPPORTED]
             * instead. See [WindowAreaAdapter].
             */
            internal val WINDOW_AREA_STATUS_UNKNOWN = Status("UNKNOWN")

            /**
             * Status indicating that the WindowArea feature is not a supported feature on the
             * device.
             */
            @JvmField
            val WINDOW_AREA_STATUS_UNSUPPORTED = Status("UNSUPPORTED")

            /**
             * Status indicating that the WindowArea feature is currently not available to be
             * enabled. This could be because a different feature is active, or the current device
             * configuration doesn't allow it.
             */
            @JvmField
            val WINDOW_AREA_STATUS_UNAVAILABLE = Status("UNAVAILABLE")

            /**
             * Status indicating that the WindowArea feature is available to be enabled.
             */
            @JvmField
            val WINDOW_AREA_STATUS_AVAILABLE = Status("AVAILABLE")

            /**
             * Status indicating that the WindowArea feature is currently active.
             */
            @JvmField
            val WINDOW_AREA_STATUS_ACTIVE = Status("ACTIVE")
        }
    }

    /**
     * Represents an operation that a [WindowAreaInfo] may support.
     */
    @ExperimentalWindowApi
    class Operation private constructor(private val description: String) {
        override fun toString(): String {
            return description
        }

        companion object {

            /**
             * Operation that transfers an [Activity] into a [WindowAreaInfo]
             */
            @JvmField
            val OPERATION_TRANSFER_ACTIVITY_TO_AREA = Operation("TRANSFER")

            /**
             * Operation that presents additional content into a [WindowAreaInfo]
             */
            @JvmField
            val OPERATION_PRESENT_ON_AREA = Operation("PRESENT")
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is WindowAreaCapability &&
            operation == other.operation &&
            status == other.status
    }

    override fun hashCode(): Int {
        var result = operation.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }
}
