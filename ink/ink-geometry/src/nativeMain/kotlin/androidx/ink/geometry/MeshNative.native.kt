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
import androidx.ink.nativeloader.cinterop.MeshNative_createEmpty
import androidx.ink.nativeloader.cinterop.MeshNative_fillAttributeUnpackingParams
import androidx.ink.nativeloader.cinterop.MeshNative_free
import androidx.ink.nativeloader.cinterop.MeshNative_getAttributeCount
import androidx.ink.nativeloader.cinterop.MeshNative_getBounds
import androidx.ink.nativeloader.cinterop.MeshNative_getTriangleCount
import androidx.ink.nativeloader.cinterop.MeshNative_getVertexCount
import androidx.ink.nativeloader.cinterop.MeshNative_getVertexPosition
import androidx.ink.nativeloader.cinterop.MeshNative_getVertexStride
import androidx.ink.nativeloader.cinterop.MeshNative_isEmpty
import androidx.ink.nativeloader.cinterop.MeshNative_newCopy
import androidx.ink.nativeloader.cinterop.MeshNative_newCopyOfFormat
import kotlin.math.min
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual internal object MeshNative {

    @VisibleForTesting actual fun createEmpty(): Long = MeshNative_createEmpty()

    actual fun newCopy(otherNativePointer: Long): Long = MeshNative_newCopy(otherNativePointer)

    actual fun free(nativePointer: Long) = MeshNative_free(nativePointer)

    actual fun getVertexCount(nativePointer: Long): Int = MeshNative_getVertexCount(nativePointer)

    actual fun getVertexStride(nativePointer: Long): Int = MeshNative_getVertexStride(nativePointer)

    actual fun getTriangleCount(nativePointer: Long): Int =
        MeshNative_getTriangleCount(nativePointer)

    actual fun getAttributeCount(nativePointer: Long): Int =
        MeshNative_getAttributeCount(nativePointer)

    /**
     * Returns a new [ImmutableBox] with the bounding box of the mesh at [nativePointer] if
     * non-empty, or null if the mesh is empty.
     */
    actual fun createBounds(nativePointer: Long): ImmutableBox? {
        if (MeshNative_isEmpty(nativePointer)) {
            return null
        }
        MeshNative_getBounds(nativePointer).useContents {
            return ImmutableBox.fromTwoPoints(x_min, y_min, x_max, y_max)
        }
    }

    /**
     * Set the given [offsets] and [scales] arrays (each of which must have at least
     * [Mesh.MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS] elements) to the unpacking transform offsets
     * and scales for the attribute with the given [attributeIndex].
     *
     * @return The number of elements of [offsets] and [scales] that have been filled. This is the
     *   number of [MeshAttributeUnpackingParams.ComponentUnpackingParams] in the
     *   [MeshAttributeUnpackingParams] that should be created for this attribute.
     */
    actual fun fillAttributeUnpackingParams(
        nativePointer: Long,
        attributeIndex: Int,
        offsets: FloatArray,
        scales: FloatArray,
    ): Int {
        check(offsets.size >= Mesh.MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS) {
            "offsets must have at least ${Mesh.MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS} elements."
        }
        check(scales.size >= Mesh.MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS) {
            "scales must have at least ${Mesh.MAX_ATTRIBUTE_UNPACKING_PARAM_COMPONENTS} elements."
        }
        check(attributeIndex >= 0 && attributeIndex < min(offsets.size, scales.size)) {
            "attributeIndex $attributeIndex must be in the range [0, ${min(offsets.size, scales.size)})"
        }
        offsets.usePinned { offsetsPinned ->
            scales.usePinned { scalesPinned ->
                // addressOf(0) is safe because offsetsPinned and scalesPinned are non-empty.
                return MeshNative_fillAttributeUnpackingParams(
                    nativePointer,
                    attributeIndex,
                    offsetsPinned.addressOf(0),
                    scalesPinned.addressOf(0),
                )
            }
        }
    }

    /**
     * Return the address of a newly allocated copy of the `ink::MeshFormat` belonging to this mesh.
     */
    actual fun newCopyOfFormat(nativePointer: Long): Long =
        MeshNative_newCopyOfFormat(nativePointer)

    actual fun fillPosition(nativePointer: Long, vertexIndex: Int, outPosition: MutableVec) {
        MeshNative_getVertexPosition(nativePointer, vertexIndex).useContents {
            outPosition.x = x
            outPosition.y = y
        }
    }
}
