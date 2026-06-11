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

import androidx.ink.nativeloader.cinterop.StrokeInputBatchSerializationNative_createFromProto
import androidx.ink.nativeloader.cinterop.StrokeInputBatchSerializationNative_encode
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned

actual internal object StrokeInputBatchSerializationNative {

    actual fun createFromProto(decompressedBytes: ByteArray, size: Int): Long =
        decompressedBytes.usePinned {
            StrokeInputBatchSerializationNative_createFromProto(
                jni_env_pass_through = null,
                if (decompressedBytes.isEmpty()) null else it.addressOf(0),
                size,
                throwForNonOkStatusCallback,
            )
        }

    actual fun encode(nativeStrokeInputBatchPointer: Long): ByteArray = memScoped {
        ByteArrayAlloc(this).let { byteArrayAlloc ->
            byteArrayAlloc.get(
                StrokeInputBatchSerializationNative_encode(
                    byteArrayAlloc.scopedStableRef.asCPointer(),
                    nativeStrokeInputBatchPointer,
                    allocByteArrayCallback,
                )
            )
        }
    }
}
