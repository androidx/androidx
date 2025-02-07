/*
 * Copyright (C) 2024 The Android Open Source Project
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
internal object StrokeInputBatchSerializationJni {
    init {
        NativeLoader.load()
    }

    /**
     * Constructs a StrokeInputBatch from a serialized [CodedStrokeInputBatch], which can be passed
     * in as either a direct [ByteBuffer] or as an array of bytes. This returns the address of a
     * new-allocated StrokeInputBatch, which can be passed to and managed by
     * [ImmutableStrokeInputBatch].
     */
    @UsedByNative
    external fun nativeNewStrokeInputBatchFromProto(
        strokeInputBatchDirectBuffer: ByteBuffer?,
        strokeInputBatchBytes: ByteArray?,
        strokeInputBatchOffset: Int,
        strokeInputBatchLength: Int,
        throwOnParseError: Boolean,
    ): Long

    @UsedByNative
    external fun nativeSerializeStrokeInputBatch(nativeStrokeInputBatchPointer: Long): ByteArray
}
