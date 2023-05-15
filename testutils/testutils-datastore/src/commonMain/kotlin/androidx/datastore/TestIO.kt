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

package androidx.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory.create
import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.Storage
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
abstract class TestIO<F : TestFile, IOE : Throwable>(
    protected val dirName: String = "datastore-test-dir"
) {

    fun getStore(
        serializerConfig: TestingSerializerConfig,
        scope: CoroutineScope,
        coordinatorProducer: () -> InterProcessCoordinator,
        futureFile: () -> TestFile
    ): DataStore<Byte> {
        return create(getStorage(serializerConfig, coordinatorProducer, futureFile), scope = scope)
    }

    abstract fun getStorage(
        serializerConfig: TestingSerializerConfig,
        coordinatorProducer: () -> InterProcessCoordinator,
        futureFile: () -> TestFile = { newTempFile() }
    ): Storage<Byte>

    abstract fun tempDir(
        directoryPath: String? = null,
        makeDirs: Boolean = true,
        parentDir: F? = null
    ): F

    abstract fun newTempFile(tempFolder: F = tempDir()): F

    abstract fun ioException(message: String): IOE

    abstract fun ioExceptionClass(): KClass<IOE>
    abstract fun isDirectory(file: F): Boolean
}

abstract class TestFile {
    abstract fun getAbsolutePath(): String

    /**
     * Deletes the file if it exists.
     * Will return `false` if the file does not exist or cannot be deleted. (similar to File.delete)
     */
    abstract fun delete(): Boolean

    /**
     * Returns true if this file/directory exists.
     */
    abstract fun exists(): Boolean

    /**
     * Creates the file if it doesn't exist.
     * @return `true` if file didn't exist and gets created and false otherwise.
     */
    abstract fun createIfNotExists(): Boolean

    fun deleteIfExists() {
        if (exists()) {
            delete()
        }
    }
}