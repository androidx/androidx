/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.health.connect.client.impl.platform

import androidx.health.connect.client.impl.platform.aggregate.InstantTimeRange
import androidx.health.connect.client.impl.platform.aggregate.LocalTimeRange
import androidx.health.connect.client.impl.platform.aggregate.TimeRange
import androidx.health.connect.client.records.IntervalRecord
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

internal operator fun Duration.div(divisor: Duration): Double {
    // We don't expect division by zero to occur. If it does, there is a mistake in the business
    // logic and the best way to surface it is to let this return the default Infinity value kotlin
    // returns on floating point division
    return toMillis().toDouble() / divisor.toMillis()
}

internal operator fun Instant.minus(other: Instant): Duration {
    return Duration.between(other, this)
}

internal fun LocalDateTime.toInstantWithDefaultZoneFallback(
    zoneOffset: ZoneOffset? = null
): Instant {
    return atZone(zoneOffset ?: ZoneId.systemDefault()).toInstant()
}

internal fun Instant.toLocalTimeWithDefaultZoneFallback(zoneOffset: ZoneId? = null): LocalDateTime {
    return LocalDateTime.ofInstant(this, zoneOffset ?: ZoneId.systemDefault())
}

internal val IntervalRecord.duration: Duration
    get() = endTime - startTime

internal fun Instant.isWithin(timeRange: TimeRange<*>, zoneOffset: ZoneOffset? = null): Boolean {
    return when (timeRange) {
        is InstantTimeRange -> !isBefore(timeRange.startTime) && isBefore(timeRange.endTime)
        is LocalTimeRange ->
            !isBefore(timeRange.startTime.toInstantWithDefaultZoneFallback(zoneOffset)) &&
                isBefore(timeRange.endTime.toInstantWithDefaultZoneFallback(zoneOffset))
    }
}
