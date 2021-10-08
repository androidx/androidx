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
import androidx.health.services.client.proto.DataProto.Availability.AvailabilityCase
import androidx.health.services.client.proto.DataProto.Availability.DataTypeAvailability as DataTypeAvailabilityProto

/** Availability of a [DataType]. */
public interface Availability {
    public val id: Int

    /** @hide */
    public fun toProto(): DataProto.Availability =
        DataProto.Availability.newBuilder()
            .setDataTypeAvailability(DataTypeAvailabilityProto.DATA_TYPE_AVAILABILITY_UNKNOWN)
            .build()

    public companion object {
        /** @hide */
        @JvmStatic
        public fun fromProto(proto: DataProto.Availability): Availability =
            when (proto.availabilityCase) {
                AvailabilityCase.DATA_TYPE_AVAILABILITY ->
                    DataTypeAvailability.fromProto(proto.dataTypeAvailability)
                AvailabilityCase.LOCATION_AVAILABILITY ->
                    LocationAvailability.fromProto(proto.locationAvailability)
                null, AvailabilityCase.AVAILABILITY_NOT_SET -> DataTypeAvailability.UNKNOWN
            }
    }
}
