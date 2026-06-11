/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.storage

import kotlinx.cinterop.AutofreeScope
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.pin
import kotlinx.cinterop.staticCFunction
import okio.Buffer
import okio.GzipSink
import okio.GzipSource
import okio.buffer
import okio.use

actual internal class DecompressedBytes actual constructor(compressedBytes: ByteArray) {

    /** The first [size] bytes of this contain the decompressed bytes, the rest are zero. */
    actual val buffer: ByteArray =
        Buffer().write(compressedBytes).let { inputBuffer ->
            GzipSource(inputBuffer).buffer().use { it.readByteArray() }
        }

    /** The size of the initial portion of [buffer] containing the decompressed bytes. */
    actual val size: Int = buffer.size
}

actual internal fun ByteArray.compress(): ByteArray =
    Buffer()
        .also { output -> GzipSink(output).buffer().use { sink -> sink.write(this) } }
        .readByteArray()

@OptIn(ExperimentalForeignApi::class)
internal val allocByteArrayCallback:
    CPointer<CFunction<(COpaquePointer?, Int) -> CArrayPointer<ByteVar>?>>? =
    staticCFunction({ allocatorPassThrough, size ->
        allocatorPassThrough!!.asStableRef<ByteArrayAlloc>().get().alloc(size)
    })

/**
 * Pass-through for allocating native byte arrays for use in serialization. The issue is that we
 * don't know the size until the proto is encoded. Passing the AutofreeScope is insufficient because
 * it doesn't record the allocated size of each pointer. The cleanup is handled by an enclosing
 * memScoped block.
 *
 * This gets used like:
 * ```
 * fun encode(...): ByteArray = memScoped {
 *   ByteArrayAlloc(this).let { byteArrayAlloc ->
 *     byteArrayAlloc.get(
 *       FooNative_encode(
 *         byteArrayAlloc.scopedStableRef.asCPointer(),
 *         ...
 *         allocByteArrayCallback,
 *       )
 *     )
 *   }
 * }
 * ```
 */
@OptIn(ExperimentalForeignApi::class)
internal class ByteArrayAlloc(private val scope: AutofreeScope) {
    val byteArraysByPointer = mutableMapOf<CArrayPointer<ByteVar>, ByteArray>()
    val scopedStableRef = StableRef.create(this).also { scope.defer { it.dispose() } }

    fun alloc(size: Int): CArrayPointer<ByteVar>? {
        if (size == 0) {
            return null
        }
        val byteArray = ByteArray(size)
        val pinned = byteArray.pin().also { scope.defer { it.unpin() } }
        val pointer = pinned.addressOf(0)
        byteArraysByPointer[pointer] = byteArray
        return pointer
    }

    fun get(pointer: CArrayPointer<ByteVar>?): ByteArray {
        if (pointer == null) {
            return byteArrayOf()
        }
        return byteArraysByPointer[pointer]!!
    }
}

// This is a typealias to allow documenting behavior of a KMP-common method and allow writing
// exception-handling code in a KMP-common way without requiring JVM consumers to take on a
// dependency on Okio. It is capitalized for consistency with java.io.IOException.
@Suppress("TypealiasDefinition", "AcronymName")
actual public typealias IOException = okio.IOException
