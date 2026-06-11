/*
 * Copyright (C) 2025 The Android Open Source Project
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

import androidx.ink.nativeloader.cinterop.BrushFamilySerializationNative_createFromProto
import androidx.ink.nativeloader.cinterop.BrushFamilySerializationNative_encode
import androidx.ink.nativeloader.cinterop.BrushFamilySerializationNative_encodeMultiple
import androidx.ink.nativeloader.cinterop.MultipleBrushFamiliesNative_createFromProto
import androidx.ink.nativeloader.cinterop.MultipleBrushFamiliesNative_free
import androidx.ink.nativeloader.cinterop.MultipleBrushFamiliesNative_getBrushFamilyCount
import androidx.ink.nativeloader.cinterop.MultipleBrushFamiliesNative_releaseBrushFamily
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.AutofreeScope
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pin
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCStringArray
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned

actual internal object BrushFamilySerializationNative {

    /**
     * Serializes a [BrushFamily] to a [ByteArray] using the provided texture map represented in
     * corresponding arrays of keys (client texture IDs) and values (PNG bytes).
     */
    actual fun encode(
        nativeBrushFamilyPointer: Long,
        textureMap: Map<String, ByteArray>?,
    ): ByteArray = memScoped {
        val scope = this@memScoped
        ByteArrayAlloc(scope).let { byteArrayAlloc ->
            val nativeTextureMap = textureMap?.let { NativeTextureMap(scope, it) }
            byteArrayAlloc.get(
                BrushFamilySerializationNative_encode(
                    byteArrayAlloc.scopedStableRef.asCPointer(),
                    nativeBrushFamilyPointer,
                    nativeTextureMap?.textureIds,
                    nativeTextureMap?.bitmaps,
                    nativeTextureMap?.bitmapLengths,
                    textureMap?.size ?: 0,
                    allocByteArrayCallback,
                )
            )
        }
    }

    actual fun encodeMultiple(
        nativeBrushFamilyPointers: LongArray,
        textureMap: Map<String, ByteArray>?,
    ): ByteArray = memScoped {
        val scope = this@memScoped
        ByteArrayAlloc(scope).let { byteArrayAlloc ->
            val nativeTextureMap = textureMap?.let { NativeTextureMap(scope, it) }
            byteArrayAlloc.get(
                BrushFamilySerializationNative_encodeMultiple(
                    byteArrayAlloc.scopedStableRef.asCPointer(),
                    nativeBrushFamilyPointers.toCValues().getPointer(scope),
                    nativeBrushFamilyPointers.size,
                    nativeTextureMap?.textureIds,
                    nativeTextureMap?.bitmaps,
                    nativeTextureMap?.bitmapLengths,
                    textureMap?.size ?: 0,
                    allocByteArrayCallback,
                )
            )
        }
    }

    /**
     * Constructs an unowned heap-allocated native [BrushFamily] from a serialized proto, passed in
     * as a [ByteArray]. [onDecodeTexture] is called for each client texture ID in the `BrushFamily`
     * proto. `maxVersion` is used to determine the maximum version supported by the deserializer.
     * Proto objects with a `min_version` of greater than `maxVersion` will be rejected.
     */
    actual fun createFromProto(
        brushFamilyByteArray: ByteArray,
        length: Int,
        onDecodeTexture: OnDecodeTexturePngBytes?,
        maxVersion: Int,
    ): Long = memScoped {
        val scope = this@memScoped
        brushFamilyByteArray.usePinned { pinnedBytes ->
            OnDecodeTextureCallbackContext(scope, onDecodeTexture).let {
                BrushFamilySerializationNative_createFromProto(
                    jni_env_pass_through = null,
                    on_decode_texture_pass_through = it.scopedStableRef.asCPointer(),
                    if (brushFamilyByteArray?.isEmpty() ?: true) null else pinnedBytes.addressOf(0),
                    length,
                    maxVersion,
                    throwForNonOkStatusCallback,
                    onDecodeTextureCallback,
                )
            }
        }
    }
}

actual internal object MultipleBrushFamiliesNative {
    /**
     * Returns a pointer to a heap-allocated `std::vector<std::unique_ptr<BrushFamily>>`, allowing
     * handoff to individual `BrushFamily` objects to be guarded by cleanup logic.
     */
    actual fun createFromProto(
        brushFamilyByteArray: ByteArray,
        length: Int,
        onDecodeTexture: OnDecodeTexturePngBytes?,
        maxVersion: Int,
    ): Long = memScoped {
        val scope = this@memScoped
        brushFamilyByteArray.usePinned { pinnedBytes ->
            OnDecodeTextureCallbackContext(scope, onDecodeTexture).let {
                MultipleBrushFamiliesNative_createFromProto(
                    jni_env_pass_through = null,
                    on_decode_texture_pass_through = it.scopedStableRef.asCPointer(),
                    if (brushFamilyByteArray.isEmpty()) null else pinnedBytes.addressOf(0),
                    length,
                    maxVersion,
                    throwForNonOkStatusCallback,
                    onDecodeTextureCallback,
                )
            }
        }
    }

    actual fun getBrushFamilyCount(nativePointer: Long): Int =
        MultipleBrushFamiliesNative_getBrushFamilyCount(nativePointer)

    actual fun releaseBrushFamily(nativePointer: Long, index: Int): Long =
        MultipleBrushFamiliesNative_releaseBrushFamily(nativePointer, index)

    actual fun free(nativePointer: Long) = MultipleBrushFamiliesNative_free(nativePointer)
}

@OptIn(ExperimentalForeignApi::class)
private class OnDecodeTextureCallbackContext(
    val scope: AutofreeScope,
    val callback: OnDecodeTexturePngBytes?,
) {
    val scopedStableRef = StableRef.create(this).also { scope.defer { it.dispose() } }
}

@OptIn(ExperimentalForeignApi::class)
private val onDecodeTextureCallback:
    CPointer<
        CFunction<
            (COpaquePointer?, CPointer<ByteVar>?, CPointer<ByteVar>?, Int) -> CPointer<ByteVar>?
        >
    > =
    staticCFunction({ onDecodeTextureCallbackContextPassThrough, encodedId, bitmap, bitmapSize ->
        onDecodeTextureCallbackContextPassThrough!!
            .asStableRef<OnDecodeTextureCallbackContext>()
            .get()
            .let {
                it.callback
                    ?.onDecodeTexture(encodedId!!.toKString(), bitmap?.readBytes(bitmapSize))
                    ?.cstr
                    ?.getPointer(it.scope) ?: encodedId
            }
    })

private class NativeTextureMap(scope: AutofreeScope, textureMap: Map<String, ByteArray>) {
    val keysList = textureMap.keys.toList()
    val textureIds: CPointer<CPointerVar<ByteVar>> = keysList.toCStringArray(scope)
    val pinnedByteArrays = mutableListOf<Pinned<ByteArray>>()

    init {
        scope.defer {
            for (pinned in pinnedByteArrays) {
                pinned.unpin()
            }
        }
    }

    val bitmaps = scope.allocArray<CPointerVar<ByteVar>>(keysList.size)
    val bitmapLengths = scope.allocArray<IntVar>(keysList.size)

    init {
        for (i in 0 until keysList.size) {
            val bytes = textureMap[keysList[i]]!!
            bitmaps[i] =
                if (bytes.isEmpty()) {
                    null
                } else {
                    bytes.pin().also { pinnedByteArrays.add(it) }.addressOf(0)
                }
            bitmapLengths[i] = bytes.size
        }
    }
}
