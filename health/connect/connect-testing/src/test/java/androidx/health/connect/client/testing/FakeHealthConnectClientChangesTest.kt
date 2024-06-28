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

import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.testing.testdata.generateRunningRecords
import androidx.health.connect.client.testing.testdata.hydrationRecord1
import androidx.health.connect.client.testing.testdata.runRecord1
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/** Unit tests for functions related to changes in [FakeHealthConnectClient]. */
class FakeHealthConnectClientChangesTest {

    @Test
    fun defaultToken_isOne() = runTest {
        val fake = FakeHealthConnectClient()
        val token = fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(Record::class)))
        assertThat(token).startsWith("1")
    }

    @Test
    fun insertAndGetChanges_returnsUpsertionChange() = runTest {
        val fake = FakeHealthConnectClient()

        val changesToken =
            fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(runRecord1::class)))
        fake.insertRecords(listOf(runRecord1))
        val changesResponse = fake.getChanges(changesToken)

        assertThat(changesResponse.changes).hasSize(1)
        assertThat(changesResponse.changes.first()).isInstanceOf(UpsertionChange::class.java)
    }

    @Test
    fun insertAndDeleteAndGetChanges_returnsUpsertionAndDeletionChanges() = runTest {
        val fake = FakeHealthConnectClient()

        val changesToken =
            fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(runRecord1::class)))
        fake.insertRecords(listOf(runRecord1))
        fake.deleteRecords(
            runRecord1::class,
            clientRecordIdsList = listOf(runRecord1.metadata.clientRecordId!!),
            recordIdsList = emptyList()
        )
        val changesResponse = fake.getChanges(changesToken)

        assertThat(changesResponse.changes).hasSize(2)
        assertThat((changesResponse.changes[0] as UpsertionChange).record.metadata.clientRecordId)
            .isEqualTo(runRecord1.metadata.clientRecordId)
        assertThat(changesResponse.changes[1]).isInstanceOf(DeletionChange::class.java)
    }

    @Test
    fun insertAndDeleteAndGetChangesByRecordIds_returnsOnlyDeletionChanges() = runTest {
        val fake = FakeHealthConnectClient()

        val changesToken =
            fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(runRecord1::class)))
        fake.insertRecords(listOf(runRecord1))
        val recordWithId =
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
        fake.deleteRecords(
            runRecord1::class,
            recordIdsList = listOf(recordWithId.records.first().metadata.id),
            clientRecordIdsList = emptyList()
        )
        val changesResponse = fake.getChanges(changesToken)

        assertThat(changesResponse.changes).hasSize(1)
        assertThat(changesResponse.changes[0]).isInstanceOf(DeletionChange::class.java)
    }

    @Test
    fun pagination_onePage() = runTest {
        val fake = FakeHealthConnectClient()
        val records = generateRunningRecords(3)

        val changesToken =
            fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(records.first()::class)))
        fake.insertRecords(records)
        val changesResponse = fake.getChanges(changesToken)

        assertThat(changesResponse.changes).hasSize(3)
        assertThat(changesResponse.hasMore).isFalse()
        assertThat(changesResponse.nextChangesToken).startsWith("4")
    }

    @Test
    fun pagination_twoPages() = runTest {
        val fake = FakeHealthConnectClient().apply { pageSizeGetChanges = 2 }
        val records = generateRunningRecords(3)

        val changesToken =
            fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(records.first()::class)))
        fake.insertRecords(records)
        val changesResponse = fake.getChanges(changesToken)

        assertThat(changesResponse.changes).hasSize(2)
        assertThat(changesResponse.hasMore).isTrue()
        assertThat(changesResponse.nextChangesToken).startsWith("3")
    }

    @Test
    fun pagination_twoPages_secondPageCorrect() = runTest {
        val fake = FakeHealthConnectClient().apply { pageSizeGetChanges = 2 }
        val records = generateRunningRecords(3)

        val changesToken =
            fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(records.first()::class)))
        fake.insertRecords(records)
        val page1 = fake.getChanges(changesToken)
        val page2 = fake.getChanges(page1.nextChangesToken)

        assertThat(page2.changes).hasSize(1)
        assertThat(page2.hasMore).isFalse()
        assertThat(page2.nextChangesToken).startsWith("4")
    }

    @Test
    fun getChangesToken_differentFilters_differentResults() = runTest {
        val fake = FakeHealthConnectClient()

        val changesToken1 =
            fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(runRecord1::class)))
        val changesToken2 =
            fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(hydrationRecord1::class)))

        fake.insertRecords(listOf(runRecord1))
        val changesResponse1 = fake.getChanges(changesToken1)
        val changesResponse2 = fake.getChanges(changesToken2)

        assertThat(changesResponse1.changes).hasSize(1)
        assertThat(changesResponse2.changes).hasSize(0)
        fake.insertRecords(listOf(hydrationRecord1))

        val changesResponse2AfterInsert = fake.getChanges(changesToken2)
        assertThat(changesResponse2AfterInsert.changes).hasSize(1)
    }

    @Test
    fun getChangesTokenExpiration_resultWithExpiredToken() = runTest {
        val fake = FakeHealthConnectClient()
        val records = generateRunningRecords(3)

        val changesToken =
            fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(runRecord1::class)))
        fake.expireToken(changesToken)
        fake.insertRecords(records)
        val changesResponse = fake.getChanges(changesToken)

        assertThat(changesResponse.changesTokenExpired).isTrue()
    }

    @Test
    fun getChangesDefaultTokenExpiration_noExpiredToken() = runTest {
        val fake = FakeHealthConnectClient()
        val records = generateRunningRecords(3)

        val changesToken =
            fake.getChangesToken(ChangesTokenRequest(recordTypes = setOf(records.first()::class)))
        fake.insertRecords(records)
        val changesResponse = fake.getChanges(changesToken)

        assertThat(changesResponse.changesTokenExpired).isFalse()
    }

    @Test
    fun getChangesToken_noRecordType_throws() = runTest {
        val fake = FakeHealthConnectClient()
        Assert.assertThrows(IllegalArgumentException::class.java) {
            runBlocking { fake.getChangesToken(ChangesTokenRequest(recordTypes = emptySet())) }
        }
    }

    @Test
    fun getChangesToken_dataOriginsSet_throws() = runTest {
        val fake = FakeHealthConnectClient()
        Assert.assertThrows(UnsupportedOperationException::class.java) {
            runBlocking {
                fake.getChangesToken(
                    ChangesTokenRequest(
                        recordTypes = setOf(Record::class),
                        dataOriginFilters = setOf(DataOrigin("test"))
                    )
                )
            }
        }
    }
}
