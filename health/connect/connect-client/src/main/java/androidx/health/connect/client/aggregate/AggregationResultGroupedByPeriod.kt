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
package androidx.health.connect.client.aggregate

import androidx.annotation.RestrictTo
import java.time.LocalDateTime

/**
 * Contains an aggregation result within a period slice.
 *
 * @property result contains [AggregationResult] with metrics included in the request.
 * @property startTime start time of the slice.
 * @property endTime end time of the slice.
 * @see [androidx.health.connect.client.HealthConnectClient.aggregateGroupByPeriod]
 */
class AggregationResultGroupedByPeriod
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    val result: AggregationResult,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
) {
    init {
        require(startTime.isBefore(endTime)) { "start time must be before end time" }
    }
}
