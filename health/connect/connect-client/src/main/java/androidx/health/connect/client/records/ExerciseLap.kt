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
 * Captures the time of a lap within an exercise session.
 *
 * <p>Each lap contains the start and end time and optional [Length] of the lap (e.g. pool
 * length while swimming or a track lap while running). There may or may not be direct correlation
 * with [ExerciseSegment] start and end times, e.g. [ExerciseSessionRecord] of type
 * running without any segments can be divided into laps of different lengths.
 *
 * @see ExerciseSessionRecord
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ExerciseLap(
    public val startTime: Instant,
    public val endTime: Instant,
    /** Lap length in [Length] unit. Optional field. Valid range: 0-1000000 meters. */
    public val length: Length? = null,
) {
    init {
        require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
        if (length != null) {
            require(length.inMeters in 0.0..1000000.0) { "length valid range: 0-1000000." }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseLap) return false

        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (length != other.length) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + length.hashCode()
        return result
    }
}