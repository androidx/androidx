/*
 * Copyright 2022 The Android Open Source Project
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

import android.util.Log
import androidx.annotation.FloatRange
import androidx.health.services.client.proto.DataProto

/** Data representing one location point with direction. */
public class LocationData(
    /** Latitude of location. Range from -90.0 to = 90.0. */
    @FloatRange(from = -90.0, to = 90.0) public val latitude: Double,
    /** Longitude of location. Range from -180.0 to = 180.0. */
    @FloatRange(from = -180.0, to = 180.0) public val longitude: Double,
    /** Altitude of location in meters or [ALTITUDE_UNAVAILABLE] if not available. */
    public val altitude: Double = ALTITUDE_UNAVAILABLE,
    /** Bearing in degrees within the range of [0.0 (inclusive), 360.0(exclusive)] or
     * [BEARING_UNAVAILABLE] if not available.
     */
    public val bearing: Double = BEARING_UNAVAILABLE,
) {
    init {
        if (latitude !in -90.0..90.0) {
            Log.w(TAG, "latitude value $latitude is out of range")
        }
        if (longitude !in -180.0..180.0) {
            Log.w(TAG, "longitude value $longitude is out of range")
        }
        if (bearing < -1.0 || bearing >= 360.0) {
            Log.w(TAG, "bearing value $bearing is out of range")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocationData) return false

        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (altitude != other.altitude) return false
        if (bearing != other.bearing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + altitude.hashCode()
        result = 31 * result + bearing.hashCode()
        return result
    }

    override fun toString(): String =
        "LocationData(" +
            "latitude=$latitude," +
            " longitude=$longitude," +
            " altitude=$altitude," +
            " bearing=$bearing" +
            ")"

    internal fun addToValueProtoBuilder(proto: DataProto.Value.Builder) {
        val doubleArrayBuilder = DataProto.Value.DoubleArray.newBuilder().apply {
            addDoubleArray(latitude)
            addDoubleArray(longitude)
            addDoubleArray(altitude)
            addDoubleArray(bearing)
        }

        proto.setDoubleArrayVal(doubleArrayBuilder)
    }

    internal companion object {
        private const val TAG = "LocationData"
        /**
         * When using [DataType.LOCATION], the value is represented as [DoubleArray]. The [Double]
         * value at this index represents the latitude.
         */
        private const val LATITUDE_INDEX: Int = 0

        /**
         * When using [DataType.LOCATION], the value is represented as [DoubleArray]. The [Double]
         * value at this index represents the longitude.
         */
        private const val LONGITUDE_INDEX: Int = 1

        /**
         * When using [DataType.LOCATION], the value is represented as [DoubleArray]. The [Double]
         * value at this index represents the altitude. This value will default to
         * [ALTITUDE_UNAVAILABLE] if it is not available.
         */
        private const val ALTITUDE_INDEX: Int = 2

        /**
         * When using [DataType.LOCATION], the value is represented as [DoubleArray]. The [Double]
         * value at this index represents the bearing. The value will be within the range of
         * 0.0 to 360.0 and default to [BEARING_UNAVAILABLE] if it is not available.
         */
        private const val BEARING_INDEX: Int = 3

        /** When using [DataType.LOCATION], the default value if altitude value is not available. */
        public const val ALTITUDE_UNAVAILABLE: Double = Double.NaN

        /** When using [DataType.LOCATION], the default value if bearing value is not available. */
        public const val BEARING_UNAVAILABLE: Double = Double.NaN

        internal fun fromDataProtoValue(proto: DataProto.Value): LocationData {
            require(proto.hasDoubleArrayVal())

            val latitude = proto.doubleArrayVal.getDoubleArray(LATITUDE_INDEX)
            val longitude = proto.doubleArrayVal.getDoubleArray(LONGITUDE_INDEX)

            val altitude = if (proto.doubleArrayVal.doubleArrayCount > ALTITUDE_INDEX) {
                proto.doubleArrayVal.getDoubleArray(ALTITUDE_INDEX)
            } else {
                ALTITUDE_UNAVAILABLE
            }

            var bearing = if (proto.doubleArrayVal.doubleArrayCount > BEARING_INDEX) {
                proto.doubleArrayVal.getDoubleArray(BEARING_INDEX)
            } else {
                BEARING_UNAVAILABLE
            }

            return LocationData(
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                bearing = bearing
            )
        }
    }
}
