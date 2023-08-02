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
package androidx.health.connect.client.impl.converters.response

import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.platform.client.proto.ChangeProto
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.ResponseProto
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChangesResponseConverterTest {
    @Test
    fun unknownChangeTypeIgnored() {
        val proto =
            ResponseProto.GetChangesResponse.newBuilder()
                .addChanges(ChangeProto.DataChange.getDefaultInstance())
                .build()

        val changesResponse = toChangesResponse(proto)
        assertThat(changesResponse.changes).isEmpty()
    }

    @Test
    fun upsertChange() {
        val proto =
            ResponseProto.GetChangesResponse.newBuilder()
                .addChanges(
                    ChangeProto.DataChange.newBuilder()
                        .setUpsertDataPoint(
                            DataProto.DataPoint.newBuilder()
                                .setUid("uid")
                                .setDataType(DataProto.DataType.newBuilder().setName("Steps"))
                                .putValues(
                                    "count",
                                    DataProto.Value.newBuilder().setLongVal(120L).build()
                                )
                                .setStartTimeMillis(1234L)
                                .setEndTimeMillis(5678L)
                                .setDataOrigin(
                                    DataProto.DataOrigin.newBuilder().setApplicationId("pkg1")
                                )
                                .setUpdateTimeMillis(9999L)
                        )
                )
                .build()

        val changesResponse = toChangesResponse(proto)
        assertThat(changesResponse.changes).hasSize(1)
        assertThat((changesResponse.changes[0] as? UpsertionChange)?.record)
            .isEqualTo(
                StepsRecord(
                    count = 120,
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(5678L),
                    endZoneOffset = null,
                    metadata =
                        Metadata(
                            id = "uid",
                            lastModifiedTime = Instant.ofEpochMilli(9999L),
                            dataOrigin = DataOrigin(packageName = "pkg1"),
                        )
                )
            )
    }

    @Test
    fun deletionChange() {
        val proto =
            ResponseProto.GetChangesResponse.newBuilder()
                .addChanges(ChangeProto.DataChange.newBuilder().setDeleteUid("deleteUid").build())
                .build()

        val changesResponse = toChangesResponse(proto)
        assertThat(changesResponse.changes).hasSize(1)
        assertThat((changesResponse.changes[0] as? DeletionChange)?.recordId).isEqualTo("deleteUid")
    }

    @Test
    fun tokenExpired() {
        val proto =
            ResponseProto.GetChangesResponse.newBuilder().setChangesTokenExpired(true).build()

        val changesResponse = toChangesResponse(proto)
        assertThat(changesResponse.changesTokenExpired).isTrue()
    }
}
