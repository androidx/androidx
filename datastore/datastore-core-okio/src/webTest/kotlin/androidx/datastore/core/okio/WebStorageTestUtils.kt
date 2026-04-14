/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.datastore.core.okio

import androidx.datastore.core.Storage
import androidx.datastore.core.readData
import kotlin.test.assertContentEquals
import okio.BufferedSink
import okio.BufferedSource

@Suppress("MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT")
internal val rawByteSerializer =
    object : OkioSerializer<ByteArray> {
        override val defaultValue: ByteArray = byteArrayOf()

        override suspend fun readFrom(source: BufferedSource): ByteArray {
            return source.readByteArray()
        }

        override suspend fun writeTo(t: ByteArray, sink: BufferedSink) {
            sink.write(t)
        }
    }

internal suspend fun runBinaryDataTest(storage: Storage<ByteArray>, cleanup: suspend () -> Unit) {
    // Binary data with invalid UTF-8 sequences
    val originalData =
        byteArrayOf(
            0x89.toByte(),
            0x50.toByte(),
            0x4e.toByte(),
            0x47.toByte(),
            0xff.toByte(),
            0xd8.toByte(),
            0xff.toByte(),
            0xe0.toByte(),
        )

    val connection = storage.createConnection()
    try {
        connection.writeScope { writeData(originalData) }

        val readData = connection.readScope { readData() }
        // Content should be equal, if not we have corrupted data
        assertContentEquals(originalData, readData)
    } finally {
        connection.close()
        cleanup()
    }
}
