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
class SleepSessionRecordTest {

    @Test
    fun validRecord_equals() {
        assertThat(
            SleepSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                title = "title",
                notes = "note",
                stages = listOf(
                    SleepSessionRecord.Stage(
                        startTime = Instant.ofEpochMilli(1234),
                        endTime = Instant.ofEpochMilli(1236),
                        stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                    ),
                ),
            )
        )
            .isEqualTo(
                SleepSessionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    title = "title",
                    notes = "note",
                    stages = listOf(
                        SleepSessionRecord.Stage(
                            startTime = Instant.ofEpochMilli(1234),
                            endTime = Instant.ofEpochMilli(1236),
                            stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                        ),
                    ),
                )
            )
    }

    @Test
    fun record_invalidTimes_throws() {
        assertFailsWith<IllegalArgumentException> {
            SleepSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1234L),
                endZoneOffset = null,
                title = "title",
                notes = "note",
            )
        }
    }

    @Test
    fun record_stageOutOfRange_throws() {
        assertFailsWith<IllegalArgumentException> {
            SleepSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1235L),
                endZoneOffset = null,
                title = "title",
                notes = "note",
                stages = listOf(
                    SleepSessionRecord.Stage(
                        startTime = Instant.ofEpochMilli(1233L),
                        endTime = Instant.ofEpochMilli(1235L),
                        stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                    )
                )
            )
        }

        assertFailsWith<IllegalArgumentException> {
            SleepSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1235L),
                endZoneOffset = null,
                title = "title",
                notes = "note",
                stages = listOf(
                    SleepSessionRecord.Stage(
                        startTime = Instant.ofEpochMilli(1234L),
                        endTime = Instant.ofEpochMilli(1236L),
                        stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                    )
                )
            )
        }
    }

    @Test
    fun record_stagesOverlap_throws() {
        assertFailsWith<IllegalArgumentException> {
            SleepSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                title = "title",
                notes = "note",
                stages = listOf(
                    SleepSessionRecord.Stage(
                        startTime = Instant.ofEpochMilli(1234L),
                        endTime = Instant.ofEpochMilli(1236L),
                        stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                    ),
                    SleepSessionRecord.Stage(
                        startTime = Instant.ofEpochMilli(1235L),
                        endTime = Instant.ofEpochMilli(1236L),
                        stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                    ),
                )
            )
        }
    }

    @Test
    fun stage_equals() {
        assertThat(
            SleepSessionRecord.Stage(
                startTime = Instant.ofEpochMilli(1234),
                endTime = Instant.ofEpochMilli(1236),
                stage = SleepSessionRecord.STAGE_TYPE_DEEP,
            )
        )
            .isEqualTo(
                SleepSessionRecord.Stage(
                    startTime = Instant.ofEpochMilli(1234),
                    endTime = Instant.ofEpochMilli(1236),
                    stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                )
            )
    }

    @Test
    fun stage_invalidTime_throws() {
        assertFailsWith<IllegalArgumentException> {
            SleepSessionRecord.Stage(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(1234L),
                stage = SleepStageRecord.STAGE_TYPE_AWAKE
            )
        }
    }
}
