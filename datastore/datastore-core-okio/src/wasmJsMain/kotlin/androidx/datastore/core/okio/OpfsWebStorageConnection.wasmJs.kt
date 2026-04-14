/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalWasmJsInterop::class)

package androidx.datastore.core.okio

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.ReadScope
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.js.js
import kotlinx.coroutines.await
import okio.Buffer
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

internal actual class OpfsWebStorageConnection<T>
actual constructor(
    private val name: String,
    serializer: OkioSerializer<T>,
    actual override val coordinator: WebInterProcessCoordinator,
) : StorageConnection<T> {
    private val closed = AtomicBoolean(false)
    private var cachedFileHandle: WasmFileSystemFileHandle? = null
    private val readScope = OpfsWebReadScope(serializer, { getFileHandle() })
    private val writeScope = OpfsWebWriteScope(serializer, { getFileHandle() })

    internal suspend fun getFileHandle(): WasmFileSystemFileHandle {
        if (cachedFileHandle == null) {
            val root = getWasmOpfsRoot().await<WasmFileSystemDirectoryHandle>()
            cachedFileHandle =
                root.getFileHandle(name, createCreateOptions()).await<WasmFileSystemFileHandle>()
        }
        return cachedFileHandle!!
    }

    actual override suspend fun <R> readScope(
        block: suspend ReadScope<T>.(locked: Boolean) -> R
    ): R {
        checkNotClosed()
        return coordinator.tryLock { locked -> readScope.block(locked) }
    }

    actual override suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit) {
        checkNotClosed()
        coordinator.lock { writeScope.block() }
    }

    private fun checkNotClosed() {
        check(!closed.get()) { "StorageConnection has already been disposed." }
    }

    actual override fun close() {
        closed.set(true)
        coordinator.removeStorageEventListener()
    }
}

internal actual open class OpfsWebReadScope<T>(
    private val serializer: OkioSerializer<T>,
    private val getFileHandle: suspend () -> WasmFileSystemFileHandle,
) : ReadScope<T> {
    private val closed = AtomicBoolean(false)

    protected fun checkClose() {
        check(!closed.get()) { "This scope has already been closed." }
    }

    actual override suspend fun readData(): T {
        checkClose()
        try {
            val fileHandle = getFileHandle()
            val file = fileHandle.getFile().await<org.w3c.files.File>()
            val buffer = getArrayBufferFromBlob(file).await<ArrayBuffer>()
            val readData = serializer.readFrom(Buffer().write(buffer.asByteArray()))
            return readData
        } catch (e: Throwable) {
            if (e.isFileNotFound()) {
                return serializer.defaultValue
            }
            throw CorruptionException("Unable to read from OPFS", e)
        }
    }

    actual override fun close() {
        closed.set(true)
    }
}

internal actual class OpfsWebWriteScope<T>(
    private val serializer: OkioSerializer<T>,
    private val getFileHandle: suspend () -> WasmFileSystemFileHandle,
) : OpfsWebReadScope<T>(serializer, getFileHandle), WriteScope<T> {

    actual override suspend fun writeData(value: T) {
        checkClose()
        val fileHandle = getFileHandle()
        val stream = fileHandle.createWritable().await<WasmFileSystemWritableFileStream>()
        try {
            val buffer = Buffer()
            serializer.writeTo(value, buffer)

            // Convert the Okio Buffer directly to JS Int8Array
            stream.write(buffer.toInt8Array()).await<JsAny?>()
        } finally {
            stream.close().await<JsAny?>()
        }
    }
}

private fun getArrayBufferFromBlob(blob: org.w3c.files.Blob): Promise<ArrayBuffer> =
    js("blob.arrayBuffer()")

private fun getInt8(array: Int8Array, index: Int): Int = js("array[index]")

private fun setInt8(array: Int8Array, index: Int, value: Int): Unit = js("array[index] = value")

internal actual fun ArrayBuffer.asByteArray(): ByteArray {
    val buffer = Int8Array(this)
    return ByteArray(buffer.length) { getInt8(buffer, it).toByte() }
}

internal actual fun Buffer.toInt8Array(): Int8Array {
    val byteArray = this.readByteArray()
    val array = Int8Array(byteArray.size)
    for (i in byteArray.indices) {
        setInt8(array, i, byteArray[i].toInt())
    }
    return array
}
