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
import androidx.health.services.client.proto.DataProto.Availability.LocationAvailability as LocationAvailabilityProto
import androidx.health.services.client.proto.DataProto.Availability.LocationAvailability.LOCATION_AVAILABILITY_UNKNOWN

/** Availability of a [DataType.LOCATION] data type. */
public class LocationAvailability
private constructor(public override val id: Int, public val name: String) : Availability {

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocationAvailability) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public override fun toProto(): DataProto.Availability =
        DataProto.Availability.newBuilder()
            .setLocationAvailability(
                LocationAvailabilityProto.forNumber(id) ?: LOCATION_AVAILABILITY_UNKNOWN
            )
            .build()

    public companion object {
        /**
         * The availability is unknown, or is represented by a value too new for this library
         * version to parse.
         */
        @JvmField public val UNKNOWN: LocationAvailability = LocationAvailability(0, "UNKNOWN")

        /** Location is not available. */
        @JvmField
        public val UNAVAILABLE: LocationAvailability = LocationAvailability(1, "UNAVAILABLE")

        /** The on-device location service is disabled, so location cannot be acquired. */
        @JvmField public val NO_GNSS: LocationAvailability = LocationAvailability(2, "NO_GNSS")

        /** Acquiring location. */
        @JvmField public val ACQUIRING: LocationAvailability = LocationAvailability(3, "ACQUIRING")

        /** Acquired location through connected phone. */
        @JvmField
        public val ACQUIRED_TETHERED: LocationAvailability =
            LocationAvailability(4, "ACQUIRED_TETHERED")

        /** Acquired location through watch. */
        @JvmField
        public val ACQUIRED_UNTETHERED: LocationAvailability =
            LocationAvailability(5, "ACQUIRED_UNTETHERED")

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        public val VALUES: List<LocationAvailability> =
            listOf(UNKNOWN, UNAVAILABLE, NO_GNSS, ACQUIRING, ACQUIRED_TETHERED, ACQUIRED_UNTETHERED)

        @JvmStatic
        public fun fromId(id: Int): LocationAvailability? = VALUES.firstOrNull { it.id == id }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal fun fromProto(proto: LocationAvailabilityProto): LocationAvailability =
            fromId(proto.number) ?: UNKNOWN
    }
}
