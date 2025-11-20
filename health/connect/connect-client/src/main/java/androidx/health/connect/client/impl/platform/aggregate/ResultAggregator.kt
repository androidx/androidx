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

package androidx.health.connect.client.impl.platform.aggregate

import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.Record
import java.time.temporal.Temporal

/**
 * Implementation of [Aggregator] that aggregates into [AggregationResult].
 *
 * @param processor the [AggregationProcessor] that does the actual computation after records are
 *   filtered.
 */
internal class ResultAggregator<T : Record, U : Temporal>(
    private val timeRange: TimeRange<U>,
    private val processor: AggregationProcessor<T>,
) : Aggregator<T, AggregationResult> {

    override fun filterAndAggregate(record: T) {
        if (AggregatorUtils.contributesToAggregation(record, timeRange)) {
            processor.processRecord(record)
        }
    }

    override fun getResult(): AggregationResult {
        return processor.getProcessedAggregationResult()
    }
}
