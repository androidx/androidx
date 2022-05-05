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

import androidx.datastore.core.kmp.KmpSerializer
import androidx.datastore.core.kmp.KmpStorage
import kotlin.random.Random
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

internal fun BufferedSource.toInputStream() = object : InputStream(this) {}
internal fun BufferedSink.toOutputStream() = object : OutputStream(this) {}

actual class TestIO actual constructor(dirName: String) {
    // TODO could use fake filesysyem but we actually rather test with real filesystem
    private val fileSystem = FileSystem.SYSTEM
    private fun randomFileName( // LAME :)
        prefix: String
    ): String {
        return prefix + (0 until 15).joinToString(separator = "") {
            ('a' + Random.nextInt(from = 0, until = 26)).toString()
        }
    }
    private val tmpDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / randomFileName(dirName)
    actual fun <T> newFileStorage(
        serializer: KmpSerializer<T>,
        prefix: String
    ): StorageImpl<T> {
        val storage = KmpStorage(
            fileSystem = fileSystem,
            producePath = {
                onProduceFileCallback()
                tmpDir / randomFileName(prefix)
            },
            serializer = serializer
        )
        return storage
    }

    actual fun <T> newFileStorage(
        serializer: KmpSerializer<T>,
        testFile: TestFile
    ): StorageImpl<T> {
        return KmpStorage(fileSystem, {testFile}, serializer)
    }

    actual var onProduceFileCallback: () -> Unit = {}

    actual fun cleanup() {
        fileSystem.deleteRecursively(tmpDir)
    }


    actual fun createTempFile(filename: String): TestFile {
        return filename.toPath()
    }

    actual fun outputStream(testFile: TestFile): OutputStream {
        return fileSystem.sink(
            file = testFile,
            mustCreate = false
        ).buffer().toOutputStream()
    }

    actual fun inputStream(testFile: TestFile): InputStream {
        return fileSystem.source(testFile).buffer().toInputStream()
    }
}

actual typealias TestFile = okio.Path