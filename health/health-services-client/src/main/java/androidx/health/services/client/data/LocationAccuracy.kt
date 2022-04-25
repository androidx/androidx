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

/** Accuracy for a [DataType.LOCATION] data point. */
@Suppress("ParcelCreator")
public class LocationAccuracy
@JvmOverloads
constructor(
    /** Represents the estimated horizontal accuracy of the location, radial, in meters. */
    public val horizontalPositionErrorMeters: Double,
    /**
     * Represents the estimated vertical accuracy of the location, radial, in meters, or it will
     * equal [LocationAccuracy.UNAVAILABLE] if it cannot be provided.
     */
    // TODO(b/227475943): open up visibility
    internal val verticalPositionErrorMeters: Double = -1.0,
) : DataPointAccuracy() {

    internal constructor(
        proto: DataProto.DataPointAccuracy
    ) : this(
        proto.locationAccuracy.horizontalPositionError,
        if (proto.locationAccuracy.hasVerticalPositionError()) {
            proto.locationAccuracy.verticalPositionError
        } else {
            UNAVAILABLE
        }
    )

    /** @hide */
    override val proto: DataProto.DataPointAccuracy by lazy {
        DataProto.DataPointAccuracy.newBuilder()
            .setLocationAccuracy(
                LocationAccuracyProto.newBuilder()
                    .setHorizontalPositionError(horizontalPositionErrorMeters)
                    .setVerticalPositionError(verticalPositionErrorMeters)
                    .build()
            )
            .build()
    }

    override fun toString(): String =
        "LocationAccuracy(horizontalPositionErrorMeters=$horizontalPositionErrorMeters," +
            "verticalPositionErrorMeters=$verticalPositionErrorMeters)"

    public companion object {

        /** Value used when a `verticalPositionError` is not available. */
        // TODO(b/227475943): open up visibility
        internal const val UNAVAILABLE: Double = -1.0

        @JvmField
        public val CREATOR: Parcelable.Creator<LocationAccuracy> = newCreator {
            val proto = DataProto.DataPointAccuracy.parseFrom(it)
            LocationAccuracy(proto)
        }
    }
}
