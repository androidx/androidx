/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.data.client.aggregate

import androidx.annotation.RestrictTo
import java.time.Instant
import java.time.ZoneOffset

/**
 * Represents an aggregation result row.
 *
 * See [HealthDataClient.aggregateGroupByDuration]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("NewApi")
class AggregateDataRowGroupByDuration
internal constructor(
    public val data: AggregateDataRow,
    public val startTime: Instant,
    public val endTime: Instant,
    public val zoneOffset: ZoneOffset,
) {
    init {
        require(startTime.isBefore(endTime)) { "start time must be before end time" }
    }
}
