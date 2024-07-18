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
class HeartRateRecordTest {

    @Test
    fun validRecord_equals() {
        assertThat(
                HeartRateRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    samples = listOf()
                )
            )
            .isEqualTo(
                HeartRateRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    samples = listOf()
                )
            )
    }

    @Test
    fun sameStartEndTime_validRecord_equals() {
        assertThat(
                HeartRateRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1234L),
                    endZoneOffset = null,
                    samples = listOf()
                )
            )
            .isEqualTo(
                HeartRateRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1234L),
                    endZoneOffset = null,
                    samples = listOf()
                )
            )
    }

    @Test
    fun invalidTimes_throws() {
        assertFailsWith<IllegalArgumentException> {
            HeartRateRecord(
                startTime = Instant.ofEpochMilli(1235L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1234L),
                endZoneOffset = null,
                samples = listOf()
            )
        }
    }

    @Test
    fun invalidBeatsPerMinute_lessThan1_throws() {
        assertFailsWith<IllegalArgumentException> {
            HeartRateRecord.Sample(
                time = Instant.ofEpochMilli(1235L),
                beatsPerMinute = 0L,
            )
        }
    }

    @Test
    fun invalidBeatsPerMinute_moreThan300_throws() {
        assertFailsWith<IllegalArgumentException> {
            HeartRateRecord.Sample(
                time = Instant.ofEpochMilli(1235L),
                beatsPerMinute = 301L,
            )
        }
    }

    @Test
    fun toString_containsMembers() {
        assertThat(
                HeartRateRecord(
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(1236L),
                        endZoneOffset = null,
                        samples =
                            listOf(
                                HeartRateRecord.Sample(
                                    time = Instant.ofEpochMilli(1234L),
                                    beatsPerMinute = 64
                                )
                            )
                    )
                    .toString()
            )
            .isEqualTo(
                "HeartRateRecord(startTime=1970-01-01T00:00:01.234Z, startZoneOffset=null, endTime=1970-01-01T00:00:01.236Z, endZoneOffset=null, samples=[Sample(time=1970-01-01T00:00:01.234Z, beatsPerMinute=64)], metadata=Metadata(id='', dataOrigin=DataOrigin(packageName=''), lastModifiedTime=1970-01-01T00:00:00Z, clientRecordId=null, clientRecordVersion=0, device=null, recordingMethod=0))"
            )
    }
}
