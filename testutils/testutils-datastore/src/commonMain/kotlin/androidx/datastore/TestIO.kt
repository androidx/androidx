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
import androidx.datastore.core.Storage
import androidx.datastore.core.DataStoreFactory.create
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope

abstract class TestIO<F : TestFile, IOE : Throwable>(
    protected val dirName: String = "datastore-test-dir"
) {

    fun getStore(
        serializerConfig: TestingSerializerConfig,
        scope: CoroutineScope,
        futureFile: () -> TestFile
    ): DataStore<Byte> {
        return create(getStorage(serializerConfig, futureFile), scope = scope)
    }

    abstract fun getStorage(
        serializerConfig: TestingSerializerConfig,
        futureFile: () -> TestFile = { newTempFile() }
    ): Storage<Byte>

    abstract fun tempDir(directoryPath: String? = null, makeDirs: Boolean = true): F
    abstract fun newTempFile(tempFolder: F = tempDir()): F

    abstract fun ioException(message: String): IOE

    abstract fun ioExceptionClass(): KClass<IOE>
    abstract fun isDirectory(file: F): Boolean
}

abstract class TestFile {
    abstract fun getAbsolutePath(): String
    abstract fun delete(): Boolean
}