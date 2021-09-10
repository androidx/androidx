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

package androidx.health.services.client.impl.response

import android.os.Parcelable
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.LocationAvailability
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.proto.DataProto.Availability.AvailabilityCase.AVAILABILITY_NOT_SET
import androidx.health.services.client.proto.DataProto.Availability.AvailabilityCase.DATA_TYPE_AVAILABILITY
import androidx.health.services.client.proto.DataProto.Availability.AvailabilityCase.LOCATION_AVAILABILITY
import androidx.health.services.client.proto.ResponsesProto

/**
 * Response sent on MeasureCallback and ExerciseUpdateListener with a [DataType] and its associated
 * [Availability] status.
 *
 * @hide
 */
public class AvailabilityResponse(
    /** [DataType] of the [AvailabilityResponse]. */
    public val dataType: DataType,
    /** [Availability] of the [AvailabilityResponse]. */
    public val availability: Availability,
) : ProtoParcelable<ResponsesProto.AvailabilityResponse>() {

    override val proto: ResponsesProto.AvailabilityResponse by lazy {
        ResponsesProto.AvailabilityResponse.newBuilder()
            .setDataType(dataType.proto)
            .setAvailability(availability.toProto())
            .build()
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<AvailabilityResponse> = newCreator { bytes ->
            val proto = ResponsesProto.AvailabilityResponse.parseFrom(bytes)
            val availability: Availability =
                when (proto.availability.availabilityCase) {
                    DATA_TYPE_AVAILABILITY ->
                        DataTypeAvailability.fromProto(proto.availability.dataTypeAvailability)
                    LOCATION_AVAILABILITY ->
                        LocationAvailability.fromProto(proto.availability.locationAvailability)
                    null, AVAILABILITY_NOT_SET -> DataTypeAvailability.UNKNOWN
                }
            AvailabilityResponse(DataType(proto.dataType), availability)
        }
    }
}
