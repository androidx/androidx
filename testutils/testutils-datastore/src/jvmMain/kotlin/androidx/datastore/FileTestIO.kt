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
import kotlin.reflect.KClass

class FileTestIO : TestIO<JavaIOFile, IOException>(
    getTmpDir = {
        File(System.getProperty("java.io.tmpdir")).toJavaFile()
    }
) {
    override fun getStorage(
        serializerConfig: TestingSerializerConfig,
        coordinatorProducer: () -> InterProcessCoordinator,
        futureFile: () -> JavaIOFile
    ): Storage<Byte> {
        return FileStorage(TestingSerializer(serializerConfig), { coordinatorProducer() }) {
            futureFile().file
        }
    }

    override fun ioException(message: String): IOException {
        return IOException(message)
    }

    override fun ioExceptionClass(): KClass<IOException> =
        IOException::class
}

class JavaIOFile(val file: File) : TestFile<JavaIOFile>() {
    override val name: String
        get() = file.name

    override fun delete(): Boolean {
        return file.delete()
    }

    override fun exists(): Boolean {
        return file.exists()
    }

    override fun mkdirs(mustCreate: Boolean) {
        if (file.exists()) {
            check(!mustCreate) {
                "file $file already exists"
            }
        }
        file.mkdirs()
        check(file.isDirectory) {
            "Failed to create directories for $file"
        }
    }

    override fun isRegularFile(): Boolean {
        return file.isFile
    }

    override fun isDirectory(): Boolean {
        return file.isDirectory
    }

    override fun protectedResolve(relative: String): JavaIOFile {
        return file.resolve(relative).toJavaFile()
    }

    override fun parentFile(): JavaIOFile? {
        return file.parentFile?.toJavaFile()
    }

    override fun protectedWrite(body: ByteArray) {
        file.writeBytes(body)
    }

    override fun protectedReadBytes(): ByteArray {
        return file.readBytes()
    }
}

fun File.toJavaFile(): JavaIOFile {
    return JavaIOFile(this)
}
