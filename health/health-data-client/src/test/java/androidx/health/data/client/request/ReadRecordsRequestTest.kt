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
package androidx.health.data.client.request

import androidx.health.data.client.records.Steps
import androidx.health.data.client.time.TimeRangeFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadRecordsRequestTest {

    private val closedTimeRange =
        TimeRangeFilter.exact(Instant.ofEpochMilli(1234L), Instant.ofEpochMilli(1235L))

    @Test
    fun limitAndSizeTogether_throws() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                ReadRecordsRequest(
                    recordType = Steps::class,
                    timeRangeFilter = TimeRangeFilter.empty(),
                    limit = 10,
                    pageSize = 10
                )
            }
        assertEquals("pageSize and limit can't be used at the same time", exception.message)
    }

    @Test
    fun openEndedTimeRange_withoutLimitOrPageSize_throws() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                ReadRecordsRequest(
                    recordType = Steps::class,
                    timeRangeFilter = TimeRangeFilter.empty()
                )
            }
        assertEquals(
            "When timeRangeFilter is open-ended, either limit or pageSize must be set",
            exception.message
        )
    }

    @Test
    fun openEndedTimeRange_withLimit_success() {
        ReadRecordsRequest(
            recordType = Steps::class,
            timeRangeFilter = TimeRangeFilter.empty(),
            limit = 10
        )
    }

    @Test
    fun openEndedTimeRange_withPageSize_success() {
        ReadRecordsRequest(
            recordType = Steps::class,
            timeRangeFilter = TimeRangeFilter.empty(),
            pageSize = 10
        )
    }

    @Test
    fun closedTimeRange_success() {
        ReadRecordsRequest(recordType = Steps::class, timeRangeFilter = closedTimeRange)
    }

    @Test
    fun pageTokenWithoutPageSize_throws() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                ReadRecordsRequest(
                    recordType = Steps::class,
                    timeRangeFilter = closedTimeRange,
                    pageToken = "token"
                )
            }
        assertEquals("pageToken must be set with pageSize", exception.message)
    }

    @Test
    fun pageTokenWithPageSize_success() {
        ReadRecordsRequest(
            recordType = Steps::class,
            timeRangeFilter = closedTimeRange,
            pageSize = 10,
            pageToken = "token"
        )
    }
}
