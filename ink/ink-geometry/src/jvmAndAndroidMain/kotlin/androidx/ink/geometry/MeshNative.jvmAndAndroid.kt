/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ink.geometry

import androidx.annotation.VisibleForTesting
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object MeshNative {
    init {
        NativeLoader.load()
    }

    @VisibleForTesting @UsedByNative actual external fun createEmpty(): Long

    @UsedByNative actual external fun newCopy(otherNativePointer: Long): Long

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative actual external fun getVertexCount(nativePointer: Long): Int

    @UsedByNative actual external fun getVertexStride(nativePointer: Long): Int

    @UsedByNative actual external fun getTriangleCount(nativePointer: Long): Int

    @UsedByNative actual external fun getAttributeCount(nativePointer: Long): Int

    /**
     * Returns a new [ImmutableBox] with the bounding box of the mesh at [nativePointer] if
     * non-empty, or null if the mesh is empty.
     */
    @UsedByNative actual external fun createBounds(nativePointer: Long): ImmutableBox?

    /**
     * Set the given [offsets] and [scales] arrays (each of which must have at least
     * [Mesh.MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS] elements) to the unpacking transform offsets
     * and scales for the attribute with the given [attributeIndex].
     *
     * @return The number of elements of [offsets] and [scales] that have been filled. This is the
     *   number of [MeshAttributeUnpackingParams.ComponentUnpackingParams] in the
     *   [MeshAttributeUnpackingParams] that should be created for this attribute.
     */
    @UsedByNative
    actual external fun fillAttributeUnpackingParams(
        nativePointer: Long,
        attributeIndex: Int,
        offsets: FloatArray,
        scales: FloatArray,
    ): Int

    /**
     * Return the address of a newly allocated copy of the `ink::MeshFormat` belonging to this mesh.
     */
    @UsedByNative actual external fun newCopyOfFormat(nativePointer: Long): Long

    @UsedByNative
    actual external fun fillPosition(nativePointer: Long, vertexIndex: Int, outPosition: MutableVec)
}
