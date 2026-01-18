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

/** Represents a capability for a [WindowArea]. */
public class WindowAreaCapability
internal constructor(public val operation: Operation, public val status: Status) {
    override fun toString(): String {
        return "WindowAreaCapability [Operation: $operation: Status: $status]"
    }

    /** Represents the status of availability for a specific [WindowAreaCapability] */
    public class Status private constructor(private val rawValue: Int) {
        override fun toString(): String {
            return when (this) {
                WINDOW_AREA_STATUS_UNSUPPORTED -> "UNSUPPORTED"
                WINDOW_AREA_STATUS_UNAVAILABLE -> "UNAVAILABLE"
                WINDOW_AREA_STATUS_AVAILABLE -> "AVAILABLE"
                WINDOW_AREA_STATUS_ACTIVE -> "ACTIVE"
                else -> "UNKNOWN"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null) return false
            if (other !is Status) return false

            return rawValue == other.rawValue
        }

        override fun hashCode(): Int {
            return rawValue
        }

        public companion object {
            /**
             * Status indicating that the WindowArea feature status is unknown, e.g. a status has
             * not been received from the extensions implementation yet. Note that this is an
             * internal status - external clients should receive [WINDOW_AREA_STATUS_UNSUPPORTED]
             * instead. See [androidx.window.area.adapter.WindowAreaAdapter].
             */
            internal val WINDOW_AREA_STATUS_UNKNOWN = Status(0)

            /**
             * Status indicating that the WindowArea feature is not a supported feature on the
             * device.
             */
            @JvmField public val WINDOW_AREA_STATUS_UNSUPPORTED: Status = Status(1)

            /**
             * Status indicating that the WindowArea feature is currently not available to be
             * enabled. This could be because a different feature is active, or the current device
             * configuration doesn't allow it.
             */
            @JvmField public val WINDOW_AREA_STATUS_UNAVAILABLE: Status = Status(2)

            /** Status indicating that the WindowArea feature is available to be enabled. */
            @JvmField public val WINDOW_AREA_STATUS_AVAILABLE: Status = Status(3)

            /** Status indicating that the WindowArea feature is currently active. */
            @JvmField public val WINDOW_AREA_STATUS_ACTIVE: Status = Status(4)
        }
    }

    /** Represents an operation that a [WindowArea] may support. */
    public class Operation private constructor(private val rawValue: Int) {
        override fun toString(): String {
            return when (this) {
                OPERATION_TRANSFER_TO_AREA -> "TRANSFER"
                OPERATION_PRESENT_ON_AREA -> "PRESENT"
                else -> "UNKNOWN"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null) return false
            if (other::class != Operation::class) return false

            other as Operation

            return rawValue == other.rawValue
        }

        override fun hashCode(): Int {
            return rawValue
        }

        public companion object {

            /** Operation that moves the device into a [WindowArea] */
            @JvmField public val OPERATION_TRANSFER_TO_AREA: Operation = Operation(0)

            /** Operation that presents additional content into a [WindowArea] */
            @JvmField public val OPERATION_PRESENT_ON_AREA: Operation = Operation(1)
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
