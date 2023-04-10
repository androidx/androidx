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

import androidx.datastore.core.FileStorage
import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.Storage
import androidx.datastore.core.TestingSerializer
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.reflect.KClass

class FileTestIO(dirName: String = "test-dir") : TestIO<JavaIOFile, IOException>(dirName) {

    override fun tempDir(
        directoryPath: String?,
        makeDirs: Boolean,
        parentDir: JavaIOFile?
    ): JavaIOFile {
        return if (parentDir != null) {
            if (directoryPath != null) {
                val tempPath = File(parentDir.file, directoryPath)
                if (makeDirs) {
                    tempPath.mkdirs()
                }
                tempPath.toJavaFile()
            } else {
                val tempPath = File(parentDir.file, "tempPath")
                if (makeDirs) {
                    tempPath.mkdirs()
                }
                tempPath.toJavaFile()
            }
        } else {
            if (directoryPath != null) {
                val tempRoot = File.createTempFile("placeholder", "placeholder").parentFile
                val tempPath = File(tempRoot, directoryPath)
                if (makeDirs) {
                    tempPath.mkdirs()
                }
                tempPath.toJavaFile()
            } else {
                File.createTempFile("temp", "tmp").parentFile!!.toJavaFile()
            }
        }
    }

    override fun newTempFile(tempFolder: JavaIOFile): JavaIOFile {
        /**
         * We need to create a new temp file without creating it so we use a UUID to decide the
         * file name but also try 100 times in case a collision happens.
         */
        // TODO use a consistent naming scheme (e.g. incrementing counter) when b/276983736 is
        //  fixed. We'll need these TestIO classes to do proper cleanup after themselves to be able
        //  do that.
        repeat(100) {
            val randName = UUID.randomUUID().toString().take(10)
            val tmpFile = File(tempFolder.file, "temp-$randName-temp")
            if (!tmpFile.exists()) {
                return tmpFile.toJavaFile()
            }
        }
        error("Unable to create temp file in $tempFolder")
    }

    override fun getStorage(
        serializerConfig: TestingSerializerConfig,
        coordinatorProducer: () -> InterProcessCoordinator,
        futureFile: () -> TestFile
    ): Storage<Byte> {
        return FileStorage(TestingSerializer(serializerConfig), { coordinatorProducer() }) {
            (futureFile() as JavaIOFile).file
        }
    }

    override fun isDirectory(file: JavaIOFile): Boolean {
        return file.file.isDirectory
    }

    override fun ioException(message: String): IOException {
        return IOException(message)
    }

    override fun ioExceptionClass(): KClass<IOException> =
        IOException::class
}

class JavaIOFile(val file: File) : TestFile() {
    override fun getAbsolutePath(): String {
        return file.absolutePath
    }

    override fun delete(): Boolean {
        return file.delete()
    }

    override fun exists(): Boolean {
        return file.exists()
    }

    override fun createIfNotExists(): Boolean {
        if (exists()) return false
        file.createNewFile()
        return true
    }
}

fun File.toJavaFile(): JavaIOFile {
    return JavaIOFile(this)
}