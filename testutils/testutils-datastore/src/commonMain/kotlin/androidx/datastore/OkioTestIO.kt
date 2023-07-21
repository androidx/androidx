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
import kotlin.random.Random
import kotlin.reflect.KClass
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

open class OkioTestIO(dirName: String = "test-dir") : TestIO<OkioPath, IOException>(dirName) {
    private val fileSystem: FileSystem = FileSystem.SYSTEM
    override fun getStorage(
        serializerConfig: TestingSerializerConfig,
        coordinatorProducer: () -> InterProcessCoordinator,
        futureFile: () -> TestFile
    ): Storage<Byte> {
        return OkioStorage(
            fileSystem = fileSystem,
            serializer = TestingOkioSerializer(serializerConfig),
            coordinatorProducer = { _, _ -> coordinatorProducer() }
        ) {
            futureFile().getAbsolutePath().toPath()
        }
    }

    override fun tempDir(
        directoryPath: String?,
        makeDirs: Boolean,
        parentDir: OkioPath?
    ): OkioPath {
        return if (parentDir != null) {
            if (directoryPath != null) {
                val newPath = if (directoryPath.startsWith("/"))
                    directoryPath.substring(1) else directoryPath
                val dir = parentDir.path / newPath
                if (makeDirs) {
                    fileSystem.createDirectories(dir)
                }
                OkioPath(fileSystem, dir)
            } else {
                OkioPath(fileSystem, parentDir.path / randomFileName(dirName))
            }
        } else {
            if (directoryPath != null) {
                val newPath = if (directoryPath.startsWith("/"))
                    directoryPath.substring(1) else directoryPath
                val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / randomFileName(dirName) / newPath
                if (makeDirs) {
                    fileSystem.createDirectories(dir)
                }
                OkioPath(fileSystem, dir)
            } else {
                OkioPath(
                    fileSystem, FileSystem.SYSTEM_TEMPORARY_DIRECTORY /
                        randomFileName(dirName)
                )
            }
        }
    }

    override fun newTempFile(tempFolder: OkioPath): OkioPath {
        return OkioPath(fileSystem, tempFolder.path / randomFileName(dirName))
    }

    private fun randomFileName( // LAME :)
        prefix: String = "test-file"
    ): String {
        return prefix + (0 until 15).joinToString(separator = "") {
            ('a' + Random.nextInt(from = 0, until = 26)).toString()
        }
    }

    override fun ioException(message: String): IOException {
        return IOException(message)
    }

    override fun ioExceptionClass(): KClass<IOException> =
        IOException::class

    override fun isDirectory(file: OkioPath): Boolean {
        return fileSystem.metadata(file.path).isDirectory
    }
}

class OkioPath(private val fileSystem: FileSystem, val path: Path) : TestFile() {

    override fun getAbsolutePath(): String {
        return path.toString()
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

    override fun createIfNotExists(): Boolean {
        if (exists()) return false
        path.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.write(path, mustCreate = true) { "" }
        return true
    }
}