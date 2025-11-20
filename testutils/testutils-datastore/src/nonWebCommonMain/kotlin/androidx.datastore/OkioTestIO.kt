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

import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioStorage
import kotlin.reflect.KClass
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.SYSTEM

open class OkioTestIO(private val fileSystem: FileSystem = FileSystem.SYSTEM) :
    TestIO<OkioPath, IOException>(
        getTmpDir = {
            OkioPath(fileSystem = fileSystem, path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY)
        }
    ) {
    override fun getStorage(
        serializerConfig: TestingSerializerConfig,
        coordinatorProducer: () -> InterProcessCoordinator,
        futureFile: () -> OkioPath,
    ): Storage<Byte> {
        return OkioStorage(
            fileSystem = fileSystem,
            serializer = TestingOkioSerializer(serializerConfig),
            coordinatorProducer = { _, _ -> coordinatorProducer() },
        ) {
            futureFile().path
        }
    }

    override fun ioException(message: String): IOException {
        return IOException(message)
    }

    override fun ioExceptionClass(): KClass<IOException> = IOException::class
}

class OkioPath(private val fileSystem: FileSystem, val path: Path) : TestFile<OkioPath>() {
    override val name: String
        get() = path.name

    override fun path(): String {
        return path.normalized().toString()
    }

    override fun delete(): Boolean {
        if (!fileSystem.exists(path)) {
            // to be consistent with the TestFile API.
            return false
        }
        fileSystem.delete(path)
        return !fileSystem.exists(path)
    }

    override fun exists(): Boolean {
        return fileSystem.exists(path)
    }

    override fun mkdirs(mustCreate: Boolean) {
        if (exists()) {
            check(fileSystem.metadataOrNull(path)?.isDirectory == true) {
                "$path already exists but it is not a directory"
            }
            check(!mustCreate) { "Directory $path already exists" }
        }
        fileSystem.createDirectories(path, mustCreate = mustCreate)
    }

    override fun isRegularFile(): Boolean {
        return fileSystem.metadataOrNull(path)?.isRegularFile == true
    }

    override fun isDirectory(): Boolean {
        return fileSystem.metadataOrNull(path)?.isDirectory == true
    }

    override fun protectedResolve(relative: String): OkioPath {
        return OkioPath(fileSystem, path / relative)
    }

    override fun parentFile(): OkioPath? {
        return path.parent?.let { OkioPath(fileSystem = fileSystem, path = it) }
    }

    override fun protectedWrite(body: ByteArray) {
        fileSystem.write(path) {
            write(body)
            flush()
        }
    }

    override fun protectedReadBytes(): ByteArray {
        return fileSystem.read(path) { readByteArray() }
    }
}
