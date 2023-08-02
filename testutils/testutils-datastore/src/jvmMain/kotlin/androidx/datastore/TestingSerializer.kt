/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore.core

import androidx.datastore.TestingSerializerConfig
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class TestingSerializer(
    val config: TestingSerializerConfig = TestingSerializerConfig(),
) : Serializer<Byte> {
    override suspend fun readFrom(input: InputStream): Byte {
        if (config.failReadWithCorruptionException) {
            throw CorruptionException(
                "CorruptionException",
                IOException()
            )
        }

        if (config.failingRead) {
            throw IOException("I was asked to fail on reads")
        }

        val read = input.read()
        if (read == -1) {
            return 0
        }
        return read.toByte()
    }

    override suspend fun writeTo(t: Byte, output: OutputStream) {
        if (config.failingWrite) {
            throw IOException("I was asked to fail on writes")
        }
        output.write(t.toInt())
    }

    override val defaultValue: Byte
        get() = config.defaultValue
}