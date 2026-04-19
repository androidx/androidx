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

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object PartitionedMeshNative {

    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun createEmptyForTesting(): Long

    @UsedByNative
    actual external fun createFromTriangleForTesting(
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
    ): Long

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative
    actual external fun getRenderGroupMeshPointers(nativePointer: Long, groupIndex: Int): LongArray

    @UsedByNative actual external fun getRenderGroupCount(nativePointer: Long): Int

    @UsedByNative
    actual external fun newCopyOfRenderGroupFormat(nativePointer: Long, groupIndex: Int): Long

    @UsedByNative actual external fun getOutlineCount(nativePointer: Long, groupIndex: Int): Int

    @UsedByNative
    actual external fun getOutlineVertexCount(
        nativePointer: Long,
        groupIndex: Int,
        outlineIndex: Int,
    ): Int

    @UsedByNative
    actual external fun populateOutlineVertexPosition(
        nativePointer: Long,
        groupIndex: Int,
        outlineIndex: Int,
        outlineVertexIndex: Int,
        outPosition: MutableVec,
    )

    /**
     * Returns a new [ImmutableBox] with the bounding box of the mesh at [nativePointer] if
     * non-empty, or null if the mesh is empty.
     */
    @UsedByNative actual external fun createBounds(nativePointer: Long): ImmutableBox?

    @UsedByNative
    actual external fun triangleCoverage(
        nativePointer: Long,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        triangleToThisTransformA: Float,
        triangleToThisTransformB: Float,
        triangleToThisTransformC: Float,
        triangleToThisTransformD: Float,
        triangleToThisTransformE: Float,
        triangleToThisTransformF: Float,
    ): Float

    /**
     * JNI method to construct C++ `PartitionedMesh` and `Triangle` objects and calculate coverage
     * using them.
     */
    @UsedByNative
    actual external fun boxCoverage(
        nativePointer: Long,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
        boxToThisTransformA: Float,
        boxToThisTransformB: Float,
        boxToThisTransformC: Float,
        boxToThisTransformD: Float,
        boxToThisTransformE: Float,
        boxToThisTransformF: Float,
    ): Float

    /**
     * JNI method to construct C++ `PartitionedMesh` and `Quad` objects and calculate coverage using
     * them.
     */
    @UsedByNative
    actual external fun parallelogramCoverage(
        nativePointer: Long,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
        parallelogramToThisTransformA: Float,
        parallelogramToThisTransformB: Float,
        parallelogramToThisTransformC: Float,
        parallelogramToThisTransformD: Float,
        parallelogramToThisTransformE: Float,
        parallelogramToThisTransformF: Float,
    ): Float

    /**
     * JNI method to construct C++ two `PartitionedMesh` objects and calculate coverage using them.
     */
    @UsedByNative
    actual external fun partitionedMeshCoverage(
        nativePointer: Long,
        otherShapeNativePointer: Long,
        otherShapeToThisTransformA: Float,
        otherShapeToThisTransformB: Float,
        otherShapeToThisTransformC: Float,
        otherShapeToThisTransformD: Float,
        otherShapeToThisTransformE: Float,
        otherShapeToThisTransformF: Float,
    ): Float

    /**
     * JNI method to construct C++ `PartitionedMesh` and `Triangle` objects and call native
     * `CoverageIsGreaterThan` on them.
     */
    @UsedByNative
    actual external fun triangleCoverageIsGreaterThan(
        nativePointer: Long,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        coverageThreshold: Float,
        triangleToThisTransformA: Float,
        triangleToThisTransformB: Float,
        triangleToThisTransformC: Float,
        triangleToThisTransformD: Float,
        triangleToThisTransformE: Float,
        triangleToThisTransformF: Float,
    ): Boolean

    /**
     * JNI method to construct C++ `PartitionedMesh` and `Rect` objects and call native
     * `CoverageIsGreaterThan` on them.
     */
    @UsedByNative
    actual external fun boxCoverageIsGreaterThan(
        nativePointer: Long,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
        coverageThreshold: Float,
        boxToThisTransformA: Float,
        boxToThisTransformB: Float,
        boxToThisTransformC: Float,
        boxToThisTransformD: Float,
        boxToThisTransformE: Float,
        boxToThisTransformF: Float,
    ): Boolean

    /**
     * JNI method to construct C++ `PartitionedMesh` and `Quad` objects and call native
     * `CoverageIsGreaterThan` on them.
     */
    @UsedByNative
    actual external fun parallelogramCoverageIsGreaterThan(
        nativePointer: Long,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
        coverageThreshold: Float,
        parallelogramToThisTransformA: Float,
        parallelogramToThisTransformB: Float,
        parallelogramToThisTransformC: Float,
        parallelogramToThisTransformD: Float,
        parallelogramToThisTransformE: Float,
        parallelogramToThisTransformF: Float,
    ): Boolean

    /**
     * JNI method to construct two C++ `PartitionedMesh` objects and call native
     * `CoverageIsGreaterThan` on them.
     */
    @UsedByNative
    actual external fun partitionedMeshCoverageIsGreaterThan(
        nativePointer: Long,
        otherShapeNativePointer: Long,
        coverageThreshold: Float,
        otherShapeToThisTransformA: Float,
        otherShapeToThisTransformB: Float,
        otherShapeToThisTransformC: Float,
        otherShapeToThisTransformD: Float,
        otherShapeToThisTransformE: Float,
        otherShapeToThisTransformF: Float,
    ): Boolean

    @UsedByNative actual external fun initializeSpatialIndex(nativePointer: Long)

    @UsedByNative actual external fun isSpatialIndexInitialized(nativePointer: Long): Boolean
}
