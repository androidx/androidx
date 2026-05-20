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

package androidx.ink.geometry

import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.ink.nativeloader.UsedByNative
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.WeakHashMap

/**
 * Hold onto [Mesh] instances referenced by the returned buffers so those are not GCed while a live
 * buffer points at the underlying native memory.
 */
@VisibleForTesting
@GuardedBy("meshesReferencedByBuffers")
internal val meshesReferencedByBuffers = WeakHashMap<ByteBuffer, Mesh>()

/**
 * Read-only access to the raw data of the vertices of the [Mesh]. Every [Mesh.vertexStride] bytes
 * in this data represents another vertex. This is a direct buffer, so it is a reference to native
 * data rather than JVM-managed data, in order to avoid copying for performance reasons. The data is
 * exposed this way (direct buffer, packed) primarily for efficient rendering - most non-rendering
 * data access should go through other methods on [Mesh], which more cleanly hide details of the
 * packed format.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Mesh.getRawVertexBuffer(): ByteBuffer =
    (JvmMeshNative.createUnsafelyMutableMeshOwnedRawVertexBuffer(nativePointer)
            ?: ByteBuffer.allocateDirect(0))
        // Preserve a weak reference to the original buffer returned by NewDirectByteBuffer.
        // DirectByteBuffer methods that copy or slice the buffer preserve a reference to the
        // originally
        // allocated buffer for this purpose. But each duplicate points back to the original, not to
        // intermediate entries in the chain.
        .also {
            check(it.isDirect()) { "This must return a direct buffer." }
            synchronized(meshesReferencedByBuffers) { meshesReferencedByBuffers.put(it, this) }
        }
        .asReadOnlyBuffer()

/**
 * Read-only access to the raw data of the triangle indices of the [Mesh]. Every element in this
 * buffer represents another triangle index, with 3 triangle indices making up each triangle. This
 * is a direct buffer, so it is a reference to native data rather than JVM-managed data, in order to
 * avoid copying for performance reasons. The data is exposed as a direct buffer primarily for
 * efficient rendering - most non-rendering data access should go through other methods on this
 * class.
 *
 * The data type of each triangle index is **unsigned** 16-bit [UShort].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Mesh.getRawTriangleIndexBuffer(): ShortBuffer =
    (JvmMeshNative.createUnsafelyMutableMeshOwnedRawTriangleIndexBuffer(nativePointer)
            ?: ByteBuffer.allocateDirect(0))
        // Preserve a weak reference to the original buffer returned by NewDirectByteBuffer.
        // DirectByteBuffer methods that copy or slice the buffer preserve a reference to the
        // originally
        // allocated buffer for this purpose. But each duplicate points back to the original, not to
        // intermediate entries in the chain.
        .also {
            check(it.isDirect()) { "This must return a direct buffer." }
            synchronized(meshesReferencedByBuffers) { meshesReferencedByBuffers.put(it, this) }
        }
        // Note that the order of operations seems to be important: asShortBuffer() must be called
        // immediately after order(ByteOrder.nativeOrder()).
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
        .asReadOnlyBuffer()

@UsedByNative
private object JvmMeshNative {
    @UsedByNative
    external fun createUnsafelyMutableMeshOwnedRawVertexBuffer(nativePointer: Long): ByteBuffer?

    @UsedByNative
    external fun createUnsafelyMutableMeshOwnedRawTriangleIndexBuffer(
        nativePointer: Long
    ): ByteBuffer?
}
