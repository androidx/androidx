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

import androidx.health.connect.client.records.IntervalRecord
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

internal operator fun Duration.div(divisor: Duration): Double {
    if (divisor.isZero) {
        return 0.0
    }
    return toMillis().toDouble() / divisor.toMillis()
}

internal operator fun Instant.minus(other: Instant): Duration {
    return Duration.between(other, this)
}

internal fun TimeRangeFilter.useLocalTime(): Boolean {
    return localStartTime != null || localEndTime != null
}

internal fun LocalDateTime.toInstantWithDefaultZoneFallback(zoneOffset: ZoneOffset?): Instant {
    return atZone(zoneOffset ?: ZoneId.systemDefault()).toInstant()
}

internal val IntervalRecord.duration: Duration
    get() = endTime - startTime
