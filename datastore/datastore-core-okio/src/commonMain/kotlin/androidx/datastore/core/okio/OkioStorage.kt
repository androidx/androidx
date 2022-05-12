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
import androidx.datastore.core.StorageImpl
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path


class OkioStorage<T>(
    private val fileSystem: FileSystem,
    private val producePath: () -> Path,
    private val serializer: OkioSerializer<T>
) : StorageImpl<T>() {
    private val canonicalPath by lazy {
        val originalPath = producePath()
        check(originalPath.isAbsolute) {
            "figure out how to make this canoncnical"
        }
        // TODO we need to convert this to canonical path but okio cannot do it if it does not
        //  exist so we need to do it more carefull i guess
        //  fileSystem.canonicalize(path = originalPath)
        val canonicalPath = originalPath
        activePaths.update {
            check(!it.contains(canonicalPath)) {
                """
                There are multiple DataStores active for the same file: $originalPath. You
                should either maintain your DataStore as a singleton or confirm that there
                is no two DataStore's active on the same file (by confirming that the scope
                is cancelled.
                """.trimIndent()
            }
            it + canonicalPath
        }
        canonicalPath
    }
    override suspend fun readData(): T {
        println("READING")
        return try {
            // TODO current implementation of FileStorage always calls serializer even when
            //  the file does not exist. OTOH, okio throws file not found exception. Unfortunately
            //  the DataStoreFactory.testCorruptionHandlerInstalled test does not account fot this.
            //  need to decide what to do,
            fileSystem.read(
                file = canonicalPath
            ) {
                serializer.readFrom(this)
            }
        } catch (th: FileNotFoundException) {
            println("CAUGHT EXCEPTION $th /${th::class.qualifiedName}")
            if (fileSystem.exists(path = canonicalPath)) {
                throw th
            }
            serializer.defaultValue
        }
    }

    override suspend fun writeData(newData: T) {
        val parentDir = canonicalPath.parent ?: error("must have a parent path")
        fileSystem.createDirectories(
            dir = parentDir,
            mustCreate = false
        )
        val scratchPath = parentDir / "${canonicalPath.name}${Storage.SCRATCH_SUFFIX}"
        try {
            fileSystem.delete(
                path = scratchPath,
                mustExist = false
            )
            fileSystem.write(
                file = scratchPath,
                mustCreate = false
            ) {
                serializer.writeTo(newData,this)
            }
            fileSystem.atomicMove(scratchPath, canonicalPath)
        } catch (ex: okio.IOException) {
            if (fileSystem.exists(scratchPath)) {
                fileSystem.delete(scratchPath)
            }
            throw ex
        }
    }

    override fun onComplete() {
        activePaths.update {
            it.also {
                it - canonicalPath
            }
        }
    }

    companion object {
        private val activePaths = atomic(emptySet<Path>())
    }

    override fun delete(): Boolean {
        fileSystem.delete(canonicalPath)
        return fileSystem.exists(canonicalPath)
    }
}