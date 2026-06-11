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

package androidx.ink.strokes

import androidx.annotation.GuardedBy
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.WeakHashMap

/**
 * Hold onto [InProgressStroke] instances referenced by the returned buffers so those are not GCed
 * while a live buffer points at the underlying native memory.
 */
@VisibleForTesting
@GuardedBy("inProgressStrokesReferencedByBuffers")
internal val inProgressStrokesReferencedByBuffers = WeakHashMap<ByteBuffer, InProgressStroke>()

/**
 * Gets the vertices of the mesh at [partitionIndex] for brush coat [coatIndex] which must be less
 * than that coat's [InProgressStroke.getMeshPartitionCount].
 *
 * Note that the returned direct [ByteBuffer] ceases to be valid after the next call to
 * [InProgressStroke.updateShape]. Continuing to use it after that point will result in incorrect
 * and possibly undefined behavior.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun InProgressStroke.getRawVertexBuffer(
    @IntRange(from = 0) coatIndex: Int,
    partitionIndex: Int,
): ByteBuffer {
    require(partitionIndex >= 0 && partitionIndex < getMeshPartitionCount(coatIndex)) {
        "Cannot get raw vertex buffer at partitionIndex $partitionIndex out of range " +
            "[0, ${getMeshPartitionCount(coatIndex)})."
    }
    return (JvmInProgressStrokeNative.getUnsafelyMutableInProgressStrokeOwnedRawVertexData(
            nativePointer,
            coatIndex,
            partitionIndex,
        ) ?: ByteBuffer.allocateDirect(0))
        .also {
            check(it.isDirect) {
                "getUnsafelyMutableInProgressStrokeOwnedRawVertexData returned a non-direct buffer."
            }
            synchronized(inProgressStrokesReferencedByBuffers) {
                inProgressStrokesReferencedByBuffers.put(it, this)
            }
        }
        .asReadOnlyBuffer()
}

/**
 * Gets the triangle indices of the mesh at [partitionIndex] for brush coat [coatIndex] which must
 * be less than that coat's [InProgressStroke.getMeshPartitionCount].
 *
 * Note that the returned direct [ShortBuffer] ceases to be valid after the next call to
 * [InProgressStroke.updateShape]. Continuing to use it after that point will result in incorrect
 * and possibly undefined behavior.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun InProgressStroke.getRawTriangleIndexBuffer(
    @IntRange(from = 0) coatIndex: Int,
    partitionIndex: Int,
): ShortBuffer {
    require(partitionIndex >= 0 && partitionIndex < getMeshPartitionCount(coatIndex)) {
        "Cannot get raw triangle index buffer at partitionIndex $partitionIndex out of range " +
            "[0, ${getMeshPartitionCount(coatIndex)})."
    }
    // The resulting buffer is writeable, so first make it readonly. Then, because Java ByteBuffers
    // defaults to a fixed endianness instead of using the endianness of the device, insist on
    // ByteOrder.nativeOrder. Note that the order of operations seems to be important:
    // asShortBuffer() must be called immediately after order(ByteOrder.nativeOrder()).
    return (JvmInProgressStrokeNative.getUnsafelyMutableInProgressStrokeOwnedRawTriangleIndexData(
            nativePointer,
            coatIndex,
            partitionIndex,
        ) ?: ByteBuffer.allocateDirect(0))
        .also {
            check(it.isDirect) {
                "getUnsafelyMutableInProgressStrokeOwnedRawTriangleIndexData returned a non-direct buffer."
            }
            synchronized(inProgressStrokesReferencedByBuffers) {
                inProgressStrokesReferencedByBuffers.put(it, this)
            }
        }
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
        .asReadOnlyBuffer()
}

@UsedByNative
private object JvmInProgressStrokeNative {
    init {
        NativeLoader.load()
    }

    /**
     * Returns a direct [ByteBuffer] wrapped around the contents of `RawVertexData` for the mesh at
     * [partitionIndex]. Code using this should only expose a read-only wrapper of that.
     */
    @UsedByNative
    external fun getUnsafelyMutableInProgressStrokeOwnedRawVertexData(
        nativePointer: Long,
        coatIndex: Int,
        partitionIndex: Int,
    ): ByteBuffer?

    /**
     * Returns a direct [ByteBuffer] wrapped around the contents of `RawTriangleIndexData` for the
     * mesh at [partitionIndex]. Code using this should only expose a read-only wrapper of that.
     */
    @UsedByNative
    external fun getUnsafelyMutableInProgressStrokeOwnedRawTriangleIndexData(
        nativePointer: Long,
        coatIndex: Int,
        partitionIndex: Int,
    ): ByteBuffer?
}
