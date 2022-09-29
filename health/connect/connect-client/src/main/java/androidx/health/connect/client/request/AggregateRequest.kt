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
package androidx.health.connect.client.request

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.HealthConnectClient

/**
 * Request object to read aggregations for given [AggregateMetric]s in Android Health Platform.
 *
 * @param metrics Set of [AggregateMetric]s to aggregate.
 * @param timeRangeFilter The [TimeRangeFilter] to read from.
 * @param dataOriginFilter Set of [DataOrigin]s to read from, or empty for no filter.
 *
 * @see HealthConnectClient.aggregate
 */
class AggregateRequest(
    internal val metrics: Set<AggregateMetric<*>>,
    internal val timeRangeFilter: TimeRangeFilter,
    internal val dataOriginFilter: Set<DataOrigin> = emptySet(),
)
