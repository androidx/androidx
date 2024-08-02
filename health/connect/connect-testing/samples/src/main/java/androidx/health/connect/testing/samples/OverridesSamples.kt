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

@file:Suppress("UNUSED_VARIABLE")

package androidx.health.connect.testing.samples

import androidx.annotation.Sampled
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.testing.AggregationResult
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.testing.stubs.MutableStub
import androidx.health.connect.client.testing.stubs.enqueue
import androidx.health.connect.client.testing.stubs.stub
import java.lang.Exception
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Sampled
fun AggregationResult(client: FakeHealthConnectClient) {
    val result =
        AggregationResult(
            metrics =
                buildMap {
                    put(HeartRateRecord.BPM_AVG, 74.0)
                    put(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL, Duration.ofMinutes(30))
                }
        )
    client.overrides.aggregate = stub(result)
}

@Sampled
fun AggregationByDurationResult(
    client: FakeHealthConnectClient,
    aggregationResult1: AggregationResult,
    startTime1: Instant,
    endTime1: Instant,
    aggregationResult2: AggregationResult,
    startTime2: Instant,
    endTime2: Instant,
) {
    val result = buildList {
        add(
            AggregationResultGroupedByDuration(
                aggregationResult1,
                startTime1,
                endTime1,
                ZoneOffset.UTC
            )
        )
        add(
            AggregationResultGroupedByDuration(
                aggregationResult2,
                startTime2,
                endTime2,
                ZoneOffset.UTC
            )
        )
    }
    client.overrides.aggregateGroupByDuration = stub(default = result)
}

@Sampled
fun AggregationByPeriodResult(
    client: FakeHealthConnectClient,
    aggregationResult1: AggregationResult,
    startTime1: LocalDateTime,
    endTime1: LocalDateTime,
    aggregationResult2: AggregationResult,
    startTime2: LocalDateTime,
    endTime2: LocalDateTime,
) {
    val result = buildList {
        add(AggregationResultGroupedByPeriod(aggregationResult1, startTime1, endTime1))
        add(AggregationResultGroupedByPeriod(aggregationResult2, startTime2, endTime2))
    }
    client.overrides.aggregateGroupByPeriod = stub(default = result)
}

@Sampled
fun StubResponse(
    client: FakeHealthConnectClient,
    result: AggregationResult,
    resultOnce: AggregationResult
) {
    // Sets a default return value for client.aggregate() and a queue with one item.
    client.overrides.aggregate = stub(queue = listOf(resultOnce)) { result }
}

@Sampled
fun StubResponseException(
    client: FakeHealthConnectClient,
    result: AggregationResult,
    exception: Exception
) {
    // Sets a default exception that will be thrown for client.aggregate().
    val aggregationStub = MutableStub<AggregationResult>(exception)
    client.overrides.aggregate = aggregationStub

    // Only the first call to client.aggregate() will return this result. Subsequent calls will
    // throw the default.
    aggregationStub.enqueue(result)

    // Setting a default response removes the default exception.
    aggregationStub.defaultHandler = { AggregationResult() }
}
