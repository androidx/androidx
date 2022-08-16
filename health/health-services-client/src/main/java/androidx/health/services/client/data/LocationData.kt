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

import androidx.health.services.client.proto.DataProto

/** Data representing one location point with direction. */
public class LocationData(
    /** Latitude of location. */
    public val latitude: Double,
    /** Longitude of location. */
    public val longitude: Double,
    /** Altitude of location in meters or `null` if not available. */
    public val altitude: Double? = null,
    /** Bearing in degrees or `null` if not available. */
    public val bearing: Double? = null,
) {
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
            addDoubleArray(altitude ?: Double.MAX_VALUE)
            addDoubleArray(bearing ?: Double.MAX_VALUE)
        }

        proto.setDoubleArrayVal(doubleArrayBuilder)
    }

    internal companion object {
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
         * [Double.MAX_VALUE] if it is not available.
         */
        private const val ALTITUDE_INDEX: Int = 2

        /**
         * When using [DataType.LOCATION], the value is represented as [DoubleArray]. The [Double]
         * value at this index represents the bearing. This value will default to [Double.MAX_VALUE]
         * if it is not available.
         */
        private const val BEARING_INDEX: Int = 3

        internal fun fromDataProtoValue(proto: DataProto.Value): LocationData {
            require(proto.hasDoubleArrayVal())

            val latitude = proto.doubleArrayVal.getDoubleArray(LATITUDE_INDEX)
            val longitude = proto.doubleArrayVal.getDoubleArray(LONGITUDE_INDEX)

            // Altitude and bearing are optional. There are two indications we need to look for to
            // determine their absence and set them to null: the array being too short, or they are
            // set to Double.MAX_VALUE.
            var altitude: Double? = null
            if (proto.doubleArrayVal.doubleArrayCount > ALTITUDE_INDEX) {
                altitude = proto.doubleArrayVal.getDoubleArray(ALTITUDE_INDEX)
                if (altitude == Double.MAX_VALUE) {
                    altitude = null
                }
            }

            var bearing: Double? = proto.doubleArrayVal.getDoubleArray(BEARING_INDEX)
            if (proto.doubleArrayVal.doubleArrayCount > BEARING_INDEX) {
                bearing = proto.doubleArrayVal.getDoubleArray(BEARING_INDEX)
                if (bearing == Double.MAX_VALUE) {
                    bearing = null
                }
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