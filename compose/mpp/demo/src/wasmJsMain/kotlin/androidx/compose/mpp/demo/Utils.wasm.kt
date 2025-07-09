/*
 * Copyright 2025 The Android Open Source Project
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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package androidx.compose.mpp.demo

import androidx.compose.foundation.internal.readText
import androidx.compose.ui.platform.ClipEntry
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import kotlinx.browser.window
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asDeferred
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response

actual suspend fun ClipEntry?.getPlainText(): String? {
    return this?.readText()
}

actual fun createClipEntryWithPlainText(text: String): ClipEntry {
    return ClipEntry.withPlainText(text)
}

private suspend fun loadResAsync(url: String): Deferred<ArrayBuffer> {
    return window.fetch(url).await<Response>().arrayBuffer().asDeferred()
}

suspend fun loadRes(url: String): ArrayBuffer {
    return loadResAsync(url).await()
}

fun ArrayBuffer.toByteArray(): ByteArray {
    val source = Int8Array(this, 0, byteLength)
    return jsInt8ArrayToKotlinByteArray(source)
}

private fun wasmExportsMemoryBuffer(): ArrayBuffer = js("wasmExports.memory.buffer")
private fun jsExportInt8ArrayToWasm(destination: ArrayBuffer, src: Int8Array, size: Int, dstAddr: Int) {
    val mem8 = Int8Array(destination, dstAddr, size)
    mem8.set(src)
}

internal fun jsInt8ArrayToKotlinByteArray(x: Int8Array): ByteArray {
    val size = x.length

    @OptIn(UnsafeWasmMemoryApi::class)
    return withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(size)
        val dstAddress = memBuffer.address.toInt()
        jsExportInt8ArrayToWasm(wasmExportsMemoryBuffer(),  x, size, dstAddress)
        ByteArray(size) { i -> (memBuffer + i).loadByte() }
    }
}