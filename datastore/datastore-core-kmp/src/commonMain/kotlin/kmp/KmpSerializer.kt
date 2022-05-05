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

package androidx.datastore.core.kmp

import androidx.datastore.core.InputStream
import androidx.datastore.core.OutputStream
import androidx.datastore.core.Serializer
import okio.BufferedSink
import okio.BufferedSource


abstract class KmpSerializer<T> : Serializer<T> {

    final override suspend fun readFrom(input: InputStream): T {
        return readFrom(input.asBufferedSource())
    }

    final override suspend fun writeTo(t: T, output: OutputStream) {
        return writeTo(t, output.asBufferedSink())
    }

    abstract suspend fun readFrom(source:BufferedSource): T

    abstract suspend fun writeTo(t: T, sink:BufferedSink)
}