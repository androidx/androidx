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

package androidx.health.connect.client.records

import androidx.health.connect.client.records.MindfulnessSessionRecord.Companion.MINDFULNESS_SESSION_TYPE_INT_TO_STRING_MAP
import androidx.health.connect.client.records.MindfulnessSessionRecord.Companion.MINDFULNESS_SESSION_TYPE_STRING_TO_INT_MAP
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.records.metadata.Metadata.Companion.RECORDING_METHOD_AUTOMATICALLY_RECORDED
import androidx.health.connect.client.records.metadata.Metadata.Companion.RECORDING_METHOD_MANUAL_ENTRY
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlin.reflect.typeOf
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindfulnessSessionRecordTest {

    @Test
    fun equals_validRecord() {
        EqualsTester()
            .addEqualityGroup(
                MindfulnessSessionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    title = "title",
                    notes = "note",
                    mindfulnessSessionType =
                        MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING
                ),
                MindfulnessSessionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    title = "title",
                    notes = "note",
                    mindfulnessSessionType =
                        MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING
                )
            )
            .addEqualityGroup(
                MindfulnessSessionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    title = "different title",
                    notes = "different note",
                    mindfulnessSessionType =
                        MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING
                )
            )
            .testEquals()
    }

    @Test
    fun constructor_invalidTimes_throws() {
        assertFailsWith<IllegalArgumentException> {
            MindfulnessSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1234L),
                endZoneOffset = null,
                metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                title = "title",
                notes = "note",
                mindfulnessSessionType =
                    MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION
            )
        }
    }

    @Test
    fun toString_containsMembers() {
        assertThat(
                MindfulnessSessionRecord(
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(1236L),
                        endZoneOffset = null,
                        metadata =
                            Metadata(recordingMethod = RECORDING_METHOD_AUTOMATICALLY_RECORDED),
                        title = "title",
                        notes = "note",
                        mindfulnessSessionType =
                            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT
                    )
                    .toString()
            )
            .isEqualTo(
                "MindfulnessSessionRecord(startTime=1970-01-01T00:00:01.234Z, startZoneOffset=null, endTime=1970-01-01T00:00:01.236Z, endZoneOffset=null, mindfulnessSessionType=5, title=title, notes=note, metadata=Metadata(id='', dataOrigin=DataOrigin(packageName=''), lastModifiedTime=1970-01-01T00:00:00Z, clientRecordId=null, clientRecordVersion=0, device=null, recordingMethod=2))"
            )
    }

    @Test
    fun allMindfulnessSessionTypeEnums_hasMapping() {
        val allEnums =
            MindfulnessSessionRecord.Companion::class
                .members
                .asSequence()
                .filter { it -> it.name.startsWith("MINDFULNESS_SESSION_TYPE") }
                .filter { it -> it.returnType == typeOf<Int>() }
                .map { it -> it.call(MindfulnessSessionRecord.Companion) }
                .toHashSet()

        assertThat(MINDFULNESS_SESSION_TYPE_STRING_TO_INT_MAP.values.toSet())
            .containsExactlyElementsIn(allEnums)
        assertThat(MINDFULNESS_SESSION_TYPE_INT_TO_STRING_MAP.keys)
            .containsExactlyElementsIn(allEnums)
    }
}
