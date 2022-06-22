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

package androidx.datastore.core.okio

import androidx.datastore.core.Storage
import kotlin.random.Random
import okio.FileSystem
import okio.Path

// TODO: move to its own module
class TestIO(private val dirName: String = "datastore-test-dir") {
    var onProduceFileCallback: () -> Unit = {}
    private val fileSystem = FileSystem.SYSTEM
    private fun randomFileName( // LAME :)
        prefix: String = "test-file"
    ): String {
        return prefix + (0 until 15).joinToString(separator = "") {
            ('a' + Random.nextInt(from = 0, until = 26)).toString()
        }
    }
    private val tmpDir = tempDir()

    fun tempDir(): Path {
        return FileSystem.SYSTEM_TEMPORARY_DIRECTORY / randomFileName(dirName)
    }

    fun <T> newFileStorage(
        serializer: OkioSerializer<T>,
        prefix: String = "test-file"
    ): Storage<T> {
        val storage = OkioStorage(
            fileSystem = fileSystem,
            serializer = serializer
        ) {
            onProduceFileCallback()
            tmpDir / randomFileName(prefix)
        }
        return storage
    }

    fun <T> newFileStorage(
        serializer: OkioSerializer<T>,
        testFile: Path
    ): Storage<T> {
        return OkioStorage(fileSystem, serializer) { testFile }
    }
}