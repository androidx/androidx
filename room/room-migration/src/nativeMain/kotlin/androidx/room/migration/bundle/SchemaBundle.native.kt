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

package androidx.room.migration.bundle

import androidx.annotation.RestrictTo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.BufferedSink
import okio.BufferedSource

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
actual class SchemaBundle
actual constructor(
    @SerialName("formatVersion") actual val formatVersion: Int,
    @SerialName("database") actual val database: DatabaseBundle
) : SchemaEquality<SchemaBundle> {

    actual override fun isSchemaEqual(other: SchemaBundle): Boolean {
        return formatVersion == other.formatVersion &&
            SchemaEqualityUtil.checkSchemaEquality(database, other.database)
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class) // For decodeFromBufferedSource
        fun deserialize(source: BufferedSource): SchemaBundle =
            json.decodeFromBufferedSource(source)

        @OptIn(ExperimentalSerializationApi::class) // For encodeToBufferedSink
        fun serialize(bundle: SchemaBundle, sink: BufferedSink) {
            json.encodeToBufferedSink(bundle, sink)
        }
    }
}
