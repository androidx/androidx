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

import android.os.Parcelable
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.DataPointAccuracy.LocationAccuracy as LocationAccuracyProto
import androidx.annotation.FloatRange

/** Accuracy for a [DataType.LOCATION] data point. */
@Suppress("ParcelCreator")
public class LocationAccuracy(
    /** Represents the estimated horizontal accuracy of the location, radial, in meters.
     * Range starting from 0.0.
     *
     * @throws IllegalArgumentException if [horizontalPositionErrorMeters] is negative
     */
    @FloatRange(from = 0.0) public val horizontalPositionErrorMeters: Double,

    /**
     * Represents the estimated vertical accuracy of the location, radial, in meters, or it will
     * be <code>{@value Double#MAX_VALUE}</code> if it cannot be provided. Range starting from 0.0.
     *
     * @throws IllegalArgumentException if [verticalPositionErrorMeters] is negative
     */
    @FloatRange(from = 0.0) public val verticalPositionErrorMeters: Double = Double.MAX_VALUE,
) : DataPointAccuracy() {
    init {
        if (horizontalPositionErrorMeters < 0.0) {
            throw IllegalArgumentException(
                "horizontalPositionErrorMeters value " +
                    "$horizontalPositionErrorMeters is out of range"
            )
        }
        if (verticalPositionErrorMeters < 0.0) {
            throw IllegalArgumentException(
                "verticalPositionErrorMeters value " +
                    "$verticalPositionErrorMeters is out of range"
            )
        }
    }

    internal constructor(
        proto: DataProto.DataPointAccuracy
    ) : this(
        proto.locationAccuracy.horizontalPositionError,
        if (proto.locationAccuracy.hasVerticalPositionError()) {
            proto.locationAccuracy.verticalPositionError
        } else {
            Double.MAX_VALUE
        }
    )

    /** @hide */
    override val proto: DataProto.DataPointAccuracy = getDataPointAccuracyProto()

    private fun getDataPointAccuracyProto(): DataProto.DataPointAccuracy {
        val locationAccuracyProtoBuilder =
            LocationAccuracyProto.newBuilder()
                .setHorizontalPositionError(horizontalPositionErrorMeters)
                .setVerticalPositionError(verticalPositionErrorMeters)

        return DataProto.DataPointAccuracy.newBuilder()
            .setLocationAccuracy(locationAccuracyProtoBuilder)
            .build()
    }

    override fun toString(): String =
        "LocationAccuracy(horizontalPositionErrorMeters=$horizontalPositionErrorMeters," +
            "verticalPositionErrorMeters=$verticalPositionErrorMeters)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<LocationAccuracy> = newCreator {
            val proto = DataProto.DataPointAccuracy.parseFrom(it)
            LocationAccuracy(proto)
        }
    }
}
