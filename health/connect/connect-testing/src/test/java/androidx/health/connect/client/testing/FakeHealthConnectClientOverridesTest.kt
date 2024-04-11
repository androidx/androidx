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

package androidx.health.connect.client.testing

import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.testing.stubs.stub
import androidx.health.connect.client.testing.testdata.dummyAggregateGbdRequest
import androidx.health.connect.client.testing.testdata.dummyAggregateGbpRequest
import androidx.health.connect.client.testing.testdata.dummyAggregateRequest
import androidx.health.connect.client.testing.testdata.dummyReadRecordsRequest
import androidx.health.connect.client.testing.testdata.runRecord1
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

/** Verifies the overrides in [FakeHealthConnectClient]. */
class FakeHealthConnectClientOverridesTest {

    private val fake = FakeHealthConnectClient()

    @Test
    fun aggregateFunctions_noStub_throwIseExceptions() {
        // When a global exception is not set and no stubs are set

        // aggregate throws
        assertThrows(IllegalStateException::class.java) {
            runBlocking { fake.aggregate(dummyAggregateRequest) }
        }

        // aggregateGroupByPeriod
        assertThrows(IllegalStateException::class.java) {
            runBlocking { fake.aggregateGroupByPeriod(dummyAggregateGbpRequest) }
        }

        // aggregateGroupByDuration
        assertThrows(IllegalStateException::class.java) {
            runBlocking { fake.aggregateGroupByDuration(dummyAggregateGbdRequest) }
        }
    }

    /* Record exceptions */

    @Test
    fun insertRecords_stubbedException_throws() = runTest {
        val expectedException = Exception()
        fake.overrides.insertRecords = stub { throw expectedException }

        assertThrows(expectedException::class.java) {
            runBlocking { fake.insertRecords(listOf(runRecord1)) }
        }
    }

    @Test
    fun readRecords_stubbedException_throws() = runTest {
        val expectedException = Exception()
        fake.overrides.readRecords = stub { throw expectedException }

        assertThrows(expectedException::class.java) {
            runBlocking { fake.readRecords(dummyReadRecordsRequest) }
        }
    }

    @Test
    fun readRecord_stubbedException_throws() = runTest {
        val expectedException = Exception()
        fake.overrides.readRecord = stub { throw expectedException }

        assertThrows(expectedException::class.java) {
            runBlocking { fake.readRecord(recordType = runRecord1::class, recordId = "test") }
        }
    }

    @Test
    fun updateRecords_stubbedException_throws() = runTest {
        val expectedException = Exception()
        fake.overrides.updateRecords = stub { throw expectedException }

        assertThrows(expectedException::class.java) {
            runBlocking { fake.updateRecords(listOf(runRecord1)) }
        }
    }

    @Test
    fun deleteRecords_stubbedException_throws() = runTest {
        val expectedException = Exception()
        fake.overrides.deleteRecords = stub { throw expectedException }

        assertThrows(expectedException::class.java) {
            runBlocking {
                fake.deleteRecords(
                    recordType = runRecord1::class,
                    timeRangeFilter =
                        TimeRangeFilter(runRecord1.startTime.minusMillis(1), runRecord1.endTime)
                )
            }
        }
    }

    /* Aggregation exceptions */

    @Test
    fun aggregate_stubbedException_throws() = runTest {
        val expectedException = Exception()
        fake.overrides.aggregate = stub { throw expectedException }

        assertThrows(expectedException::class.java) {
            runBlocking { fake.aggregate(dummyAggregateRequest) }
        }
    }

    @Test
    fun aggregatePeriod_stubbedException_throws() = runTest {
        val expectedException = Exception()
        fake.overrides.aggregateGroupByPeriod = stub { throw expectedException }

        assertThrows(expectedException::class.java) {
            runBlocking { fake.aggregateGroupByPeriod(dummyAggregateGbpRequest) }
        }
    }

    @Test
    fun aggregateDuration_stubbedException_throws() = runTest {
        val expectedException = Exception()
        fake.overrides.aggregateGroupByDuration = stub { throw expectedException }

        assertThrows(expectedException::class.java) {
            runBlocking { fake.aggregateGroupByDuration(dummyAggregateGbdRequest) }
        }
    }

    /* Aggregation stubbed elements */

    @Test
    fun aggregate_stubbedElement() = runTest {
        fake.overrides.aggregate =
            stub(
                default =
                    AggregationResult(
                        metrics =
                            buildMap {
                                put(HeartRateRecord.BPM_AVG, 74L) // Long
                                put(StepsCadenceRecord.RATE_AVG, 60.0) // Double
                            }
                    )
            )
        val response = fake.aggregate(dummyAggregateRequest)

        assertThat(response.longValues).hasSize(1)
        assertThat(response.doubleValues).hasSize(1)
    }

    @Test
    fun aggregateGroupByPeriod_stubbedElement() = runTest {
        fake.overrides.aggregateGroupByPeriod =
            stub(
                default =
                    listOf(
                        AggregationResultGroupedByPeriod(
                            AggregationResult(
                                metrics =
                                    buildMap {
                                        put(HeartRateRecord.BPM_AVG, 74L) // Long
                                        put(StepsCadenceRecord.RATE_AVG, 60.0) // Double
                                    }
                            ),
                            startTime = LocalDateTime.now(),
                            endTime = LocalDateTime.now()
                        )
                    )
            )
        val response = fake.aggregateGroupByPeriod(dummyAggregateGbpRequest)

        assertThat(response).hasSize(1)
        assertThat(response.first().result.longValues).hasSize(1)
        assertThat(response.first().result.doubleValues).hasSize(1)
    }

    @Test
    fun aggregateGroupByDuration_stubbedElement() = runTest {
        fake.overrides.aggregateGroupByDuration =
            stub(
                default =
                    listOf(
                        AggregationResultGroupedByDuration(
                            AggregationResult(
                                metrics =
                                    buildMap {
                                        put(HeartRateRecord.BPM_AVG, 74L) // Long
                                        put(StepsCadenceRecord.RATE_AVG, 60.0) // Double
                                    }
                            ),
                            startTime = runRecord1.startTime,
                            endTime = runRecord1.endTime,
                            zoneOffset = runRecord1.startZoneOffset!!
                        )
                    )
            )
        val response = fake.aggregateGroupByDuration(dummyAggregateGbdRequest)

        assertThat(response).hasSize(1)
        assertThat(response.first().result.longValues).hasSize(1)
        assertThat(response.first().result.doubleValues).hasSize(1)
    }

    /* Changes stubs */

    @Test
    fun getChanges_stubbedException_throws() = runTest {
        val expectedException = Exception()
        fake.overrides.getChanges = stub { throw expectedException }
        assertThrows(expectedException::class.java) { runBlocking { fake.getChanges("test") } }
    }

    @Test
    fun getChangesToken_stubbedException_throws() = runTest {
        val expectedException = Exception()
        fake.overrides.getChangesToken = stub { throw expectedException }
        assertThrows(expectedException::class.java) {
            runBlocking { fake.getChangesToken(ChangesTokenRequest(recordTypes = emptySet())) }
        }
    }
}
