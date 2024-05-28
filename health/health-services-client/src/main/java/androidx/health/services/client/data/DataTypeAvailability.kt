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
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.Availability.DataTypeAvailability as DataTypeAvailabilityProto
import androidx.health.services.client.proto.DataProto.Availability.DataTypeAvailability.DATA_TYPE_AVAILABILITY_UNKNOWN

/** Availability of a [DataType]. */
public class DataTypeAvailability
private constructor(public override val id: Int, public val name: String) : Availability {

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataTypeAvailability) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public override fun toProto(): DataProto.Availability =
        DataProto.Availability.newBuilder()
            .setDataTypeAvailability(
                DataTypeAvailabilityProto.forNumber(id) ?: DATA_TYPE_AVAILABILITY_UNKNOWN
            )
            .build()

    public companion object {
        /**
         * The availability is unknown, or is represented by a value too new for this library
         * version to parse.
         */
        @JvmField public val UNKNOWN: DataTypeAvailability = DataTypeAvailability(0, "UNKNOWN")

        /** The [DataType] is fully initialized and available. */
        @JvmField public val AVAILABLE: DataTypeAvailability = DataTypeAvailability(1, "AVAILABLE")

        /** The [DataType] is currently acquiring. */
        @JvmField public val ACQUIRING: DataTypeAvailability = DataTypeAvailability(2, "ACQUIRING")

        /** The [DataType] is unavailable because health services cannot acquire it. */
        @JvmField
        public val UNAVAILABLE: DataTypeAvailability = DataTypeAvailability(3, "UNAVAILABLE")

        /** The [DataType] is not available because the device is currently off-body. */
        @JvmField
        public val UNAVAILABLE_DEVICE_OFF_BODY: DataTypeAvailability =
            DataTypeAvailability(4, "UNAVAILABLE_DEVICE_OFF_BODY")

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        public val VALUES: List<DataTypeAvailability> =
            listOf(UNKNOWN, AVAILABLE, ACQUIRING, UNAVAILABLE, UNAVAILABLE_DEVICE_OFF_BODY)

        @JvmStatic
        public fun fromId(id: Int): DataTypeAvailability? = VALUES.firstOrNull { it.id == id }

        internal fun fromProto(proto: DataTypeAvailabilityProto): DataTypeAvailability =
            fromId(proto.number) ?: UNKNOWN
    }
}
