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

import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.Availability.LocationAvailability as LocationAvailabilityProto
import androidx.health.services.client.proto.DataProto.Availability.LocationAvailability.LOCATION_AVAILABILITY_UNKNOWN

/** Availability of a [DataType.LOCATION] data type. */
public enum class LocationAvailability(public override val id: Int) : Availability {
    UNKNOWN(0),

    /** Location is not available. */
    UNAVAILABLE(1),

    /** The on-device GPS is disabled, so location cannot be acquired. */
    NO_GPS(2),

    /** Acquiring location. */
    ACQUIRING(3),

    /** Acquired location through connected phone. */
    ACQUIRED_TETHERED(4),

    /** Acquired location through watch. */
    ACQUIRED_UNTETHERED(5);

    /** @hide */
    public override fun toProto(): DataProto.Availability =
        DataProto.Availability.newBuilder()
            .setLocationAvailability(
                LocationAvailabilityProto.forNumber(id) ?: LOCATION_AVAILABILITY_UNKNOWN
            )
            .build()

    public companion object {
        @JvmStatic
        public fun fromId(id: Int): LocationAvailability? = values().firstOrNull { it.id == id }

        /** @hide */
        public fun fromProto(proto: LocationAvailabilityProto): LocationAvailability =
            fromId(proto.number) ?: UNKNOWN
    }
}
