/*
 * Copyright 2025 The Android Open Source Project
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
@file:JvmName("MetadataTestHelper")

package androidx.health.connect.client.testing

import androidx.health.connect.client.impl.converters.records.metadata
import androidx.health.connect.client.impl.converters.records.toProto
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.platform.client.proto.DataProto
import java.time.Instant

/**
 * Creates a new [Metadata] object by copying existing fields and overriding specified properties
 * for testing.
 *
 * This method facilitates the creation of [Metadata] instances with controlled values, particularly
 * useful for unit testing scenarios where specific metadata properties need to be set. It
 * constructs a new [Metadata] object based on the current instance, allowing the modification of
 * the `id`, `dataOrigin`, and `lastModifiedTime` fields.
 *
 * @param id The ID to assign to the new [Metadata]. If not provided, the ID from the current
 *   [Metadata] instance will be used.
 * @param dataOrigin The [DataOrigin] to assign to the new [Metadata]. If not provided, the data
 *   origin from the current [Metadata] instance will be used.
 * @param lastModifiedTime The [Instant] representing the last modified time. If not provided, the
 *   last modified time from the current [Metadata] instance will be used.
 */
@JvmOverloads
public fun Metadata.populatedWithTestValues(
    id: String = this.id,
    dataOrigin: DataOrigin = this.dataOrigin,
    lastModifiedTime: Instant = this.lastModifiedTime,
): Metadata {
    val obj = this
    val dataProto =
        DataProto.DataPoint.newBuilder()
            .setRecordingMethod(this.recordingMethod)
            .setUid(id)
            .setUpdateTimeMillis(lastModifiedTime.toEpochMilli())
            .setDataOrigin(
                DataProto.DataOrigin.newBuilder().setApplicationId(dataOrigin.packageName).build()
            )
            .apply {
                obj.device?.let { setDevice(it.toProto()) }
                obj.clientRecordId?.let { setClientId(it) }
                if (obj.clientRecordVersion > 0) {
                    setClientVersion(obj.clientRecordVersion)
                }
            }
            .build()

    return dataProto.metadata
}
