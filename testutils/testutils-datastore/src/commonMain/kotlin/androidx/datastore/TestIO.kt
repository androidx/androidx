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
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope

abstract class TestIO<F : TestFile<F>, IOE : Throwable>(getTmpDir: () -> F) {
    private val testRoot: F =
        getTmpDir().let { createNewRandomChild(parent = it, namePrefix = "datastore-testio-") }

    init {
        testRoot.mkdirs(mustCreate = true)
    }

    fun getStore(
        serializerConfig: TestingSerializerConfig,
        scope: CoroutineScope,
        coordinatorProducer: () -> InterProcessCoordinator,
        futureFile: () -> F,
    ): DataStore<Byte> {
        return create(getStorage(serializerConfig, coordinatorProducer, futureFile), scope = scope)
    }

    abstract fun getStorage(
        serializerConfig: TestingSerializerConfig,
        coordinatorProducer: () -> InterProcessCoordinator,
        futureFile: () -> F = { newTempFile() },
    ): Storage<Byte>

    private fun randomName(prefix: String): String {
        return prefix +
            (0 until 15).joinToString(separator = "") {
                ('a' + Random.nextInt(from = 0, until = 26)).toString()
            }
    }

    protected fun createNewRandomChild(parent: F, namePrefix: String = "test-file-"): F {
        while (true) {
            val child = parent.resolve(randomName(namePrefix))
            if (!child.exists()) {
                return child
            }
        }
    }

    /** Returns a new file instance without creating it or its parents. */
    fun newTempFile(parentFile: F? = null, relativePath: String? = null): F {
        val parent = parentFile ?: testRoot
        return if (relativePath == null) {
            createNewRandomChild(parent = parent)
        } else {
            parent.resolve(relativePath)
        }
    }

    abstract fun ioException(message: String): IOE

    abstract fun ioExceptionClass(): KClass<IOE>
}

abstract class TestFile<T : TestFile<T>> {
    /** The name of the file, including the extension */
    abstract val name: String

    /** Get the canonical path for the file. */
    abstract fun path(): String

    /**
     * Deletes the file if it exists. Will return `false` if the file does not exist or cannot be
     * deleted. (similar to File.delete)
     */
    abstract fun delete(): Boolean

    /** Returns true if this file/directory exists. */
    abstract fun exists(): Boolean

    /**
     * Creates a directory from the file.
     *
     * If it is a regular file, will throw. If [mustCreate] is `true` and directory already exists,
     * will throw.
     */
    abstract fun mkdirs(mustCreate: Boolean = false)

    /** Returns `true` if this exists and a regular file on the filesystem. */
    abstract fun isRegularFile(): Boolean

    /** Returns `true` if this exists and is a directory on the filesystem. */
    abstract fun isDirectory(): Boolean

    /**
     * Resolves the given [relative] relative to `this`. (similar to File.resolve in kotlin stdlib).
     *
     * Note that this path is sanitized to ensure it is not a root path (e.g. does not start with
     * `/`)
     */
    fun resolve(relative: String): T {
        return if (relative.startsWith("/")) {
            protectedResolve(relative.substring(startIndex = 1))
        } else {
            protectedResolve(relative)
        }
    }

    /** Implemented by the subclasses to resolve child from sanitized name. */
    abstract fun protectedResolve(relative: String): T

    /** Returns the parent file or null if this has no parent. */
    abstract fun parentFile(): T?

    /**
     * Writes the given [body] test into the file using the default encoding (kotlin stdlib's
     * String.encodeToByteArray)
     */
    fun write(body: String) {
        write(body.encodeToByteArray())
    }

    /**
     * Overrides the file with the given contents. If parent directories do not exist, they'll be
     * created.
     */
    fun write(body: ByteArray) {
        if (isDirectory()) {
            error("Cannot write to a directory")
        }
        parentFile()?.mkdirs(mustCreate = false)
        parentFile()?.mkdirs()
        protectedWrite(body)
    }

    /** Reads the byte contents of the file. */
    fun readBytes(): ByteArray {
        check(exists()) { "File does not exist" }
        return protectedReadBytes()
    }

    fun readText() = readBytes().decodeToString()

    /**
     * Writes the given [body] into the file. This is called after necessary checks are done so
     * implementers should only focus on writing the contents.
     */
    abstract fun protectedWrite(body: ByteArray)

    /**
     * Reads the byte contents of the file. This is called after necessary checks are done so
     * implementers should only focus on reading the bytes.
     */
    abstract fun protectedReadBytes(): ByteArray
}
