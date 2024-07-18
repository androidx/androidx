/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.connect.client.records

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepsRecordTest {

    @Test
    fun validRecord_equals() {
        assertThat(
                StepsRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    count = 10,
                )
            )
            .isEqualTo(
                StepsRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    count = 10,
                )
            )
    }

    @Test
    fun invalidTimes_throws() {
        assertFailsWith<IllegalArgumentException> {
            StepsRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1234L),
                endZoneOffset = null,
                count = 10,
            )
        }
    }

    @Test
    fun invalidSteps_tooFewCount_throws() {
        assertFailsWith<IllegalArgumentException> {
            StepsRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1235L),
                endZoneOffset = null,
                count = 0,
            )
        }
    }

    @Test
    fun invalidSteps_tooLargeCount_throws() {
        assertFailsWith<IllegalArgumentException> {
            StepsRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1235L),
                endZoneOffset = null,
                count = 1000_001,
            )
        }
    }

    @Test
    fun toString_containsMembers() {
        assertThat(
                StepsRecord(
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(1236L),
                        endZoneOffset = null,
                        count = 42,
                    )
                    .toString()
            )
            .isEqualTo(
                "StepsRecord(startTime=1970-01-01T00:00:01.234Z, startZoneOffset=null, endTime=1970-01-01T00:00:01.236Z, endZoneOffset=null, count=42, metadata=Metadata(id='', dataOrigin=DataOrigin(packageName=''), lastModifiedTime=1970-01-01T00:00:00Z, clientRecordId=null, clientRecordVersion=0, device=null, recordingMethod=0))"
            )
    }
}
