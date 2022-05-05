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

import androidx.datastore.core.okio.OkioSerializer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files

actual class TestIO actual constructor(dirName: String) {
    private val tmpDir = Files.createTempDirectory(
        dirName
    ).also {
        it.toFile().deleteOnExit()
    }

    actual fun <T> newFileStorage(serializer: OkioSerializer<T>, prefix: String):
        StorageImpl<T> {
        return FileStorage(
            produceFile = {
                onProduceFileCallback()
                Files.createTempFile(
                    tmpDir,
                    prefix, // prefix
                    "" // suffix
                ).toFile()
            },
            serializer = serializer
        )

    }

    actual fun <T> newFileStorage(serializer: OkioSerializer<T>, testFile: TestFile): StorageImpl<T> {
        return FileStorage({ testFile }, serializer)
    }


    actual var onProduceFileCallback: () -> Unit = {}


    actual fun cleanup() {
        tmpDir.toFile().deleteRecursively()
    }

    actual fun createTempFile(filename: String): TestFile {
        return Files.createTempFile(
            tmpDir,
            filename, // prefix
            "" // suffix
        ).toFile()
    }

    actual fun outputStream(testFile: TestFile): OutputStream {
        return FileOutputStream(testFile)
    }

    actual fun inputStream(testFile: TestFile): InputStream {
        return FileInputStream(testFile)
    }
}

actual typealias TestFile = File