/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.health.connect.client.records

import androidx.annotation.RestrictTo
import androidx.health.connect.client.units.Length
import java.time.Instant

/**
 * Captures a route associated with an exercise session a user does.
 *
 * Contains a sequence of location points, with timestamps, which do not have to be in order.
 *
 * Location points contain a timestamp, longitude, latitude, and optionally altitude, horizontal and
 * vertical accuracy.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ExerciseRoute(route: List<Location>) {
    public val route: List<Location> = route.sortedWith { a, b -> a.time.compareTo(b.time) }

    init {
        for (i in 0 until this.route.lastIndex) {
            require(this.route[i].time.isBefore(this.route[i + 1].time))
        }
    }
    internal fun isWithin(startTime: Instant, endTime: Instant): Boolean {
        // startTime is inclusive, endTime is exclusive
        return !route.first().time.isBefore(startTime) && route.last().time.isBefore(endTime)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseRoute) return false

        return route == other.route
    }

    override fun hashCode(): Int {
        return route.hashCode()
    }

    /**
     * Represents a single location point recorded during an exercise.
     *
     * @param time The point in time when the location was recorded; Required field.
     * @param latitude Latitude of the location point; Required field; Valid range [-90; 90]
     * @param longitude Longitude of the location point; Required field; Valid range [-180; 180]
     * @param altitude in [Length] unit. Optional field. Valid range: non-negative numbers.
     * @param horizontalAccuracy in [Length] unit. Optional field. Valid range: non-negative
     *   numbers.
     * @param verticalAccuracy in [Length] unit. Optional field. Valid range: non-negative numbers.
     * @see ExerciseRoute
     */
    public class Location(
        val time: Instant,
        val latitude: Double,
        val longitude: Double,
        val horizontalAccuracy: Length? = null,
        val verticalAccuracy: Length? = null,
        val altitude: Length? = null
    ) {

        companion object {
            private const val MIN_LONGITUDE = -180.0
            private const val MAX_LONGITUDE = 180.0
            private const val MIN_LATITUDE = -90.0
            private const val MAX_LATITUDE = 90.0
        }

        init {
            latitude.requireNotLess(other = MIN_LATITUDE, name = "latitude")
            latitude.requireNotMore(other = MAX_LATITUDE, name = "latitude")
            longitude.requireNotLess(other = MIN_LONGITUDE, name = "longitude")
            longitude.requireNotMore(other = MAX_LONGITUDE, name = "longitude")
            horizontalAccuracy?.requireNotLess(
                other = horizontalAccuracy.zero(),
                name = "horizontalAccuracy"
            )
            verticalAccuracy?.requireNotLess(
                other = verticalAccuracy.zero(),
                name = "verticalAccuracy"
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Location) return false

            if (time != other.time) return false
            if (latitude != other.latitude) return false
            if (longitude != other.longitude) return false
            if (horizontalAccuracy != other.horizontalAccuracy) return false
            if (verticalAccuracy != other.verticalAccuracy) return false
            if (altitude != other.altitude) return false

            return true
        }

        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + latitude.hashCode()
            result = 31 * result + longitude.hashCode()
            result = 31 * result + (horizontalAccuracy?.hashCode() ?: 0)
            result = 31 * result + (verticalAccuracy?.hashCode() ?: 0)
            result = 31 * result + (altitude?.hashCode() ?: 0)
            return result
        }
    }
}
