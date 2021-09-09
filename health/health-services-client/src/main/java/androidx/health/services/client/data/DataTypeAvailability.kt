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
import androidx.health.services.client.proto.DataProto.Availability.DataTypeAvailability as DataTypeAvailabilityProto
import androidx.health.services.client.proto.DataProto.Availability.DataTypeAvailability.DATA_TYPE_AVAILABILITY_UNKNOWN

/** Availability of a [DataType]. */
public enum class DataTypeAvailability(public override val id: Int) : Availability {
    UNKNOWN(0),
    AVAILABLE(1),
    ACQUIRING(2),
    UNAVAILABLE(3);

    /** @hide */
    public override fun toProto(): DataProto.Availability =
        DataProto.Availability.newBuilder()
            .setDataTypeAvailability(
                DataTypeAvailabilityProto.forNumber(id) ?: DATA_TYPE_AVAILABILITY_UNKNOWN
            )
            .build()

    public companion object {
        @JvmStatic
        public fun fromId(id: Int): DataTypeAvailability? = values().firstOrNull { it.id == id }

        /** @hide */
        public fun fromProto(proto: DataTypeAvailabilityProto): DataTypeAvailability =
            fromId(proto.number) ?: UNKNOWN
    }
}
