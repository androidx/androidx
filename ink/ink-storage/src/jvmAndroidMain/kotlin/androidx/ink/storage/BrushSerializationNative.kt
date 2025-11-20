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

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.nio.ByteBuffer

@UsedByNative
internal object BrushSerializationNative {
    init {
        NativeLoader.load()
    }

    /**
     * Internal callback for decoding a BrushFamily proto. The PNG bytes are kept as an
     * implementation detail and are not exposed.
     *
     * @param clientTextureId The client-provided texture ID.
     * @param pngBytes The PNG bytes of the texture bitmap, or null if none was encoded.
     * @return The texture ID to use in the decoded BrushFamily.
     */
    fun interface TextureDecodeCallback {
        @UsedByNative fun onDecodeTexture(clientTextureId: String, pngBytes: ByteArray?): String
    }

    fun newBrushFamilyFromProto(
        brushFamilyDirectByteBuffer: ByteBuffer?,
        brushFamilyByteArray: ByteArray?,
        offset: Int,
        length: Int,
        callback: TextureDecodeCallback = TextureDecodeCallback { id, _ -> id },
    ): Long =
        newBrushFamilyFromProtoInternal(
            brushFamilyDirectByteBuffer,
            brushFamilyByteArray,
            offset,
            length,
            callback,
        )

    /**
     * Serializes a [BrushFamily] to a [ByteArray] using the provided texture map of keys (client
     * texture IDs) to values (PNG bytes).
     */
    fun serializeBrushFamily(
        nativeBrushFamilyPointer: Long,
        map: Map<String, ByteArray>,
    ): ByteArray =
        serializeBrushFamily(
            nativeBrushFamilyPointer,
            map.keys.toTypedArray(),
            map.values.toTypedArray(),
        )

    @UsedByNative external fun serializeBrush(nativeBrushPointer: Long): ByteArray

    /**
     * Serializes a [BrushFamily] to a [ByteArray] using the provided texture map represented in
     * corresponding arrays of keys (client texture IDs) and values (PNG bytes).
     */
    @UsedByNative
    private external fun serializeBrushFamily(
        nativeBrushFamilyPointer: Long,
        textureMapKeys: Array<String>,
        textureMapValues: Array<ByteArray>,
    ): ByteArray

    @UsedByNative external fun serializeBrushCoat(nativeBrushCoatPointer: Long): ByteArray

    @UsedByNative external fun serializeBrushTip(nativeBrushTipPointer: Long): ByteArray

    @UsedByNative external fun serializeBrushPaint(nativeBrushPaintPointer: Long): ByteArray

    /**
     * Constructs an unowned heap-allocated native `Brush` from a serialized proto, which can be
     * passed in as either a direct [ByteBuffer] or a [ByteArray].
     */
    @UsedByNative
    external fun newBrushFromProto(
        brushDirectByteBuffer: ByteBuffer?,
        brushByteArray: ByteArray?,
        offset: Int,
        length: Int,
    ): Long

    /**
     * Constructs an unowned heap-allocated native `BrushFamily` from a serialized proto, which can
     * be passed in as either a direct [ByteBuffer] or a [ByteArray]. The callback is called for
     * each client texture ID in the BrushFamily proto.
     */
    @UsedByNative
    private external fun newBrushFamilyFromProtoInternal(
        brushFamilyDirectByteBuffer: ByteBuffer?,
        brushFamilyByteArray: ByteArray?,
        offset: Int,
        length: Int,
        callback: TextureDecodeCallback,
    ): Long

    /**
     * Constructs an unowned heap-allocated native `BrushCoat` from a serialized proto, which can be
     * passed in as either a direct [ByteBuffer] or a [ByteArray].
     */
    @UsedByNative
    external fun newBrushCoatFromProto(
        brushCoatDirectByteBuffer: ByteBuffer?,
        brushCoatByteArray: ByteArray?,
        offset: Int,
        length: Int,
    ): Long

    /**
     * Constructs an unowned heap-allocated native `BrushTip` from a serialized proto, which can be
     * passed in as either a direct [ByteBuffer] or a [ByteArray].
     */
    @UsedByNative
    external fun newBrushTipFromProto(
        brushTipDirectByteBuffer: ByteBuffer?,
        brushTipByteArray: ByteArray?,
        offset: Int,
        length: Int,
    ): Long

    /**
     * Constructs an unowned heap-allocated native `BrushPaint` from a serialized proto, which can
     * be passed in as either a direct [ByteBuffer] or a [ByteArray].
     */
    @UsedByNative
    external fun newBrushPaintFromProto(
        brushPaintDirectByteBuffer: ByteBuffer?,
        brushPaintByteArray: ByteArray?,
        offset: Int,
        length: Int,
    ): Long
}
