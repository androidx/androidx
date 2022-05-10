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

package androidx.datastore.io

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath

actual class DatastoreOutput(val bufferedSink:BufferedSink) {
    fun toOutputStream(): OutputStream {
        return bufferedSink.outputStream()
    }

}
actual class DatastoreInput(val bufferedSource:BufferedSource) {
    fun toInputStream(): InputStream {
        return bufferedSource.inputStream()
    }
}

fun <T> createFileStorage(produceFile: () -> File, serializer: Serializer<T>):Storage<T> {
    return OkioStorage(FileSystem.SYSTEM, {produceFile().absolutePath.toPath()}, serializer)
}