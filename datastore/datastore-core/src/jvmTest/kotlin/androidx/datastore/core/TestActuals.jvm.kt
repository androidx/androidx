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

package androidx.datastore.core

import java.nio.file.Files

actual fun InputStream.readInt(): Int = this.read()
actual fun OutputStream.writeInt(value: Int)= write(value)

actual class TestIO {
    private val tmpDir = Files.createTempDirectory(
        "datastore-test-io"
    ).also {
        it.toFile().deleteOnExit()
    }
    actual fun <T> newFileStorage(serializer: Serializer<T>): Storage<T> {
        return FileStorage(
            produceFile = {
                Files.createTempFile(
                    tmpDir,
                    "test-file", // prefix
                    ""//suffix
                ).toFile()
            },
            serializer = serializer
        )
    }

    actual fun cleanup() {
        tmpDir.toFile().deleteRecursively()
    }

}