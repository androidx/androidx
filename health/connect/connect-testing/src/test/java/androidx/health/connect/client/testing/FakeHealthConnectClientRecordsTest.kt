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

import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.testing.testdata.generateRunningRecords
import androidx.health.connect.client.testing.testdata.runRecord1
import androidx.health.connect.client.testing.testdata.runRecord1Updated
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

/** Unit tests for functions related to [Record] in [FakeHealthConnectClient]. */
class FakeHealthConnectClientRecordsTest {

    @Test
    fun insertSingleRecordAndRead_responseHasOneRecord() {
        runTest {
            val fake = FakeHealthConnectClient()
            fake.insertRecords(listOf(runRecord1))
            val response =
                fake.readRecords(
                    ReadRecordsRequest(
                        timeRangeFilter =
                            TimeRangeFilter(
                                startTime = runRecord1.startTime.minusSeconds(1),
                                endTime = runRecord1.endTime.plusSeconds(1)
                            ),
                        recordType = runRecord1::class
                    )
                )
            assertThat(response.records).hasSize(1)
            assertThat(response.records.first().metadata.clientRecordId)
                .isEqualTo(runRecord1.metadata.clientRecordId)
        }
    }

    @Test
    fun updateRecordAndRead_responseHasUpdated() {
        // Given
        val fakeRecord = runRecord1
        val updatedRecord = runRecord1Updated
        runTest {
            val fake = FakeHealthConnectClient()
            fake.insertRecords(listOf(fakeRecord))
            fake.updateRecords(listOf(updatedRecord))
            val responseOldRange =
                fake.readRecords(
                    ReadRecordsRequest(
                        timeRangeFilter =
                            TimeRangeFilter(
                                startTime = fakeRecord.startTime.minusSeconds(1),
                                endTime = fakeRecord.endTime.plusSeconds(1)
                            ),
                        recordType = fakeRecord::class
                    )
                )

            val responseNewRange =
                fake.readRecords(
                    ReadRecordsRequest(
                        timeRangeFilter =
                            TimeRangeFilter(
                                startTime = updatedRecord.startTime.minusSeconds(1),
                                endTime = updatedRecord.endTime.plusSeconds(1)
                            ),
                        recordType = fakeRecord::class
                    )
                )

            assertThat(responseOldRange.records).hasSize(0)
            assertThat(responseNewRange.records).hasSize(1)
        }
    }

    @Test
    fun insertMultipleRecordsAndDeleteclientRecordIds_responseIsEmpty() = runTest {
        val fake = FakeHealthConnectClient()
        val records = generateRunningRecords(5)
        fake.insertRecords(records)

        val clientRecordIds = records.map { it.metadata.clientRecordId!! }
        fake.deleteRecords(records.first()::class, emptyList(), clientRecordIds)

        val response =
            fake.readRecords(
                ReadRecordsRequest(
                    timeRangeFilter =
                        TimeRangeFilter(
                            startTime = records.first().startTime.minusSeconds(1),
                            endTime = records.last().endTime.plusSeconds(1)
                        ),
                    recordType = records.first()::class
                )
            )
        assertThat(response.records).isEmpty()
    }

    @Test
    fun insertMultipleRecordsAndDeleteRecordIds_responseIsEmpty() = runTest {
        val fake = FakeHealthConnectClient()
        val records = generateRunningRecords(5)
        fake.insertRecords(records)

        val responseBeforeDeletion =
            fake.readRecords(
                ReadRecordsRequest(
                    timeRangeFilter =
                        TimeRangeFilter(
                            startTime = records.first().startTime.minusSeconds(1),
                            endTime = records.last().endTime.plusSeconds(1)
                        ),
                    recordType = records.first()::class
                )
            )

        val recordIds = responseBeforeDeletion.records.map { it.metadata.id }
        fake.deleteRecords(records.first()::class, recordIds, emptyList())

        val response =
            fake.readRecords(
                ReadRecordsRequest(
                    timeRangeFilter =
                        TimeRangeFilter(
                            startTime = records.first().startTime.minusSeconds(1),
                            endTime = records.last().endTime.plusSeconds(1)
                        ),
                    recordType = records.first()::class
                )
            )
        assertThat(response.records).isEmpty()
    }

    @Test
    fun insertMultipleRecordsAndDeleteRecordByTimeRangeFilter_responseIsEmpty() = runTest {
        val fake = FakeHealthConnectClient()
        val records = generateRunningRecords(5)
        fake.insertRecords(records)

        fake.deleteRecords(
            recordType = records.first()::class,
            timeRangeFilter =
                TimeRangeFilter(records.first().startTime.minusMillis(1), records.first().endTime)
        )

        val response =
            fake.readRecords(
                ReadRecordsRequest(
                    timeRangeFilter =
                        TimeRangeFilter(
                            startTime = records.first().startTime.minusSeconds(1),
                            endTime = records.last().endTime.plusSeconds(1)
                        ),
                    recordType = records.first()::class
                )
            )
        // Only one record should be presend
        assertThat(response.records).hasSize(4)
    }

    @Test
    fun insertMultipleRecordsAndReadRecord() = runTest {
        val fake = FakeHealthConnectClient()
        val records = generateRunningRecords(5)
        fake.insertRecords(records)

        val recordToRead = records[1]
        val response = fake.readRecord(recordToRead::class, recordToRead.metadata.clientRecordId!!)

        assertThat(response.record.metadata.clientRecordId)
            .isEqualTo(recordToRead.metadata.clientRecordId)
    }

    @Test
    fun insertMultipleRecordsAndDeleteFour_responseIsOne() = runTest {
        val fake = FakeHealthConnectClient()
        val records = generateRunningRecords(5)
        fake.insertRecords(records)

        val clientRecordIds = records.drop(1).map { it.metadata.clientRecordId!! }
        fake.deleteRecords(records.first()::class, emptyList(), clientRecordIds)

        val response =
            fake.readRecords(
                ReadRecordsRequest(
                    timeRangeFilter =
                        TimeRangeFilter(
                            startTime = records.first().startTime.minusSeconds(1),
                            endTime = records.last().endTime.plusSeconds(1)
                        ),
                    recordType = records.first()::class
                )
            )
        assertThat(response.records).hasSize(1)
    }

    @Test
    fun insertMultipleRecords_DeleteDifferentPackageRecordIds_throws() = runTest {
        val fake = FakeHealthConnectClient(packageName = "com.other.package")
        val records = generateRunningRecords(5) // Uses default package name
        fake.insertRecords(records)

        // Need to fetch the records from the API to be able to read their new IDs.
        val recordsResponse =
            fake.readRecords(
                ReadRecordsRequest(
                    timeRangeFilter =
                        TimeRangeFilter(
                            startTime = records.first().startTime.minusSeconds(1),
                            endTime = records.last().endTime.plusSeconds(1)
                        ),
                    recordType = records.first()::class
                )
            )

        val recordIds = recordsResponse.records.map { it.metadata.id }

        assertThrows(SecurityException::class.java) {
            runBlocking {
                fake.deleteRecords(
                    recordType = records.first()::class,
                    recordIdsList = recordIds,
                    clientRecordIdsList = emptyList()
                )
            }
        }
    }

    @Test
    fun insertMultipleRecords_DeleteDifferentPackageClientRecords_throws() = runTest {
        val fake = FakeHealthConnectClient(packageName = "com.other.package")
        val records = generateRunningRecords(5)
        fake.insertRecords(records)

        val clientRecordIds = records.map { it.metadata.clientRecordId!! }

        assertThrows(SecurityException::class.java) {
            runBlocking { fake.deleteRecords(records.first()::class, emptyList(), clientRecordIds) }
        }
    }

    @Test
    fun insertMultipleRecords_UpdateDifferentPackageClientRecords_throws() = runTest {
        val fake = FakeHealthConnectClient(packageName = "com.other.package")
        val records = generateRunningRecords(5)
        fake.insertRecords(records)

        assertThrows(SecurityException::class.java) {
            runBlocking { fake.updateRecords(listOf(records.first())) }
        }
    }

    @Test
    fun insertMultipleRecords_UpdateNonExistingClientRecords_doesNotThrow() = runTest {
        val fake = FakeHealthConnectClient()
        val records = generateRunningRecords(5)
        fake.insertRecords(records)

        // Try to delete a record that doesn't exist
        fake.deleteRecords(
            records.first()::class,
            recordIdsList = listOf(runRecord1.metadata.clientRecordId!!),
            emptyList()
        )
        // Does not throw
    }

    @Test
    fun pagination_singlePage() = runTest {
        val fake = FakeHealthConnectClient()
        val numberOfRecords = 3
        val records = generateRunningRecords(numberOfRecords)

        fake.insertRecords(records)

        val pagedRequest1 =
            ReadRecordsRequest(
                timeRangeFilter =
                    TimeRangeFilter(
                        startTime = records.first().startTime.minusSeconds(1),
                        endTime = records.last().endTime.plusSeconds(1)
                    ),
                recordType = records.first()::class,
                pageSize = 5,
            )
        val page1 = fake.readRecords(pagedRequest1)

        assertThat(page1.records).hasSize(numberOfRecords)
        assertThat(page1.pageToken).isNull()
    }

    @Test
    fun pagination_threePages() = runTest {
        val fake = FakeHealthConnectClient()
        val records = generateRunningRecords(5)

        fake.insertRecords(records)

        val pagedRequest1 =
            ReadRecordsRequest(
                timeRangeFilter =
                    TimeRangeFilter(
                        startTime = records.first().startTime.minusSeconds(1),
                        endTime = records.last().endTime.plusSeconds(1)
                    ),
                recordType = records.first()::class,
                pageSize = 2,
            )
        val page1 = fake.readRecords(pagedRequest1)

        val pagedRequest2 =
            ReadRecordsRequest(
                timeRangeFilter =
                    TimeRangeFilter(
                        startTime = records.first().startTime.minusSeconds(1),
                        endTime = records.last().endTime.plusSeconds(1)
                    ),
                recordType = records.first()::class,
                pageToken = page1.pageToken,
                pageSize = 2,
            )
        val page2 = fake.readRecords(pagedRequest2)
        val pagedRequest3 =
            ReadRecordsRequest(
                timeRangeFilter =
                    TimeRangeFilter(
                        startTime = records.first().startTime.minusSeconds(1),
                        endTime = records.last().endTime.plusSeconds(1)
                    ),
                recordType = records.first()::class,
                pageToken = page2.pageToken,
                pageSize = 2,
            )
        val page3 = fake.readRecords(pagedRequest3)

        assertThat(page1.records).hasSize(2)
        assertThat(page2.records).hasSize(2)
        assertThat(page3.records).hasSize(1)
        assertThat(page3.pageToken).isNull()
        assertThat(page2.records.first().title).isEqualTo(records[2].title)
        assertThat(page3.records.first().title).isEqualTo(records[4].title)
    }
}
