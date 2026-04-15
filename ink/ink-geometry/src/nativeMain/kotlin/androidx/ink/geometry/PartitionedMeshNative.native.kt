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

import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_boxCoverage
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_boxCoverageIsGreaterThan
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_createEmptyForTesting
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_createFromTriangleForTesting
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_fillRenderGroupMeshPointers
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_free
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_getBounds
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_getOutlineCount
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_getOutlineVertexCount
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_getOutlineVertexPosition
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_getRenderGroupCount
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_getRenderGroupMeshCount
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_initializeSpatialIndex
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_isEmpty
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_isSpatialIndexInitialized
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_newCopyOfRenderGroupFormat
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_parallelogramCoverage
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_parallelogramCoverageIsGreaterThan
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_partitionedMeshCoverage
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_partitionedMeshCoverageIsGreaterThan
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_triangleCoverage
import androidx.ink.nativeloader.cinterop.PartitionedMeshNative_triangleCoverageIsGreaterThan
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual internal object PartitionedMeshNative {

    actual fun createEmptyForTesting(): Long = PartitionedMeshNative_createEmptyForTesting()

    actual fun createFromTriangleForTesting(
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
    ): Long =
        PartitionedMeshNative_createFromTriangleForTesting(
            triangleP0X,
            triangleP0Y,
            triangleP1X,
            triangleP1Y,
            triangleP2X,
            triangleP2Y,
        )

    actual fun free(nativePointer: Long) = PartitionedMeshNative_free(nativePointer)

    actual fun getRenderGroupMeshPointers(nativePointer: Long, groupIndex: Int): LongArray {
        val outPointers =
            LongArray(PartitionedMeshNative_getRenderGroupMeshCount(nativePointer, groupIndex))
        outPointers.usePinned {
            PartitionedMeshNative_fillRenderGroupMeshPointers(
                nativePointer,
                groupIndex,
                it.addressOf(0),
            )
        }
        return outPointers
    }

    actual fun getRenderGroupCount(nativePointer: Long): Int =
        PartitionedMeshNative_getRenderGroupCount(nativePointer)

    actual fun newCopyOfRenderGroupFormat(nativePointer: Long, groupIndex: Int): Long =
        PartitionedMeshNative_newCopyOfRenderGroupFormat(nativePointer, groupIndex)

    actual fun getOutlineCount(nativePointer: Long, groupIndex: Int): Int =
        PartitionedMeshNative_getOutlineCount(nativePointer, groupIndex)

    actual fun getOutlineVertexCount(nativePointer: Long, groupIndex: Int, outlineIndex: Int): Int =
        PartitionedMeshNative_getOutlineVertexCount(nativePointer, groupIndex, outlineIndex)

    actual fun populateOutlineVertexPosition(
        nativePointer: Long,
        groupIndex: Int,
        outlineIndex: Int,
        outlineVertexIndex: Int,
        outPosition: MutableVec,
    ) {
        PartitionedMeshNative_getOutlineVertexPosition(
                nativePointer,
                groupIndex,
                outlineIndex,
                outlineVertexIndex,
            )
            .useContents {
                outPosition.x = x
                outPosition.y = y
            }
    }

    actual fun createBounds(nativePointer: Long): ImmutableBox? {
        if (PartitionedMeshNative_isEmpty(nativePointer)) {
            return null
        }
        PartitionedMeshNative_getBounds(nativePointer).useContents {
            return ImmutableBox.fromTwoPoints(x_min, y_min, x_max, y_max)
        }
    }

    actual fun triangleCoverage(
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
    ): Float =
        PartitionedMeshNative_triangleCoverage(
            nativePointer,
            triangleP0X,
            triangleP0Y,
            triangleP1X,
            triangleP1Y,
            triangleP2X,
            triangleP2Y,
            triangleToThisTransformA,
            triangleToThisTransformB,
            triangleToThisTransformC,
            triangleToThisTransformD,
            triangleToThisTransformE,
            triangleToThisTransformF,
        )

    actual fun boxCoverage(
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
    ): Float =
        PartitionedMeshNative_boxCoverage(
            nativePointer,
            boxXMin,
            boxYMin,
            boxXMax,
            boxYMax,
            boxToThisTransformA,
            boxToThisTransformB,
            boxToThisTransformC,
            boxToThisTransformD,
            boxToThisTransformE,
            boxToThisTransformF,
        )

    actual fun parallelogramCoverage(
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
    ): Float =
        PartitionedMeshNative_parallelogramCoverage(
            nativePointer,
            parallelogramCenterX,
            parallelogramCenterY,
            parallelogramWidth,
            parallelogramHeight,
            parallelogramAngleInRadian,
            parallelogramShearFactor,
            parallelogramToThisTransformA,
            parallelogramToThisTransformB,
            parallelogramToThisTransformC,
            parallelogramToThisTransformD,
            parallelogramToThisTransformE,
            parallelogramToThisTransformF,
        )

    actual fun partitionedMeshCoverage(
        nativePointer: Long,
        otherShapeNativePointer: Long,
        otherShapeToThisTransformA: Float,
        otherShapeToThisTransformB: Float,
        otherShapeToThisTransformC: Float,
        otherShapeToThisTransformD: Float,
        otherShapeToThisTransformE: Float,
        otherShapeToThisTransformF: Float,
    ): Float =
        PartitionedMeshNative_partitionedMeshCoverage(
            nativePointer,
            otherShapeNativePointer,
            otherShapeToThisTransformA,
            otherShapeToThisTransformB,
            otherShapeToThisTransformC,
            otherShapeToThisTransformD,
            otherShapeToThisTransformE,
            otherShapeToThisTransformF,
        )

    actual fun triangleCoverageIsGreaterThan(
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
    ): Boolean =
        PartitionedMeshNative_triangleCoverageIsGreaterThan(
            nativePointer,
            triangleP0X,
            triangleP0Y,
            triangleP1X,
            triangleP1Y,
            triangleP2X,
            triangleP2Y,
            coverageThreshold,
            triangleToThisTransformA,
            triangleToThisTransformB,
            triangleToThisTransformC,
            triangleToThisTransformD,
            triangleToThisTransformE,
            triangleToThisTransformF,
        )

    actual fun boxCoverageIsGreaterThan(
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
    ): Boolean =
        PartitionedMeshNative_boxCoverageIsGreaterThan(
            nativePointer,
            boxXMin,
            boxYMin,
            boxXMax,
            boxYMax,
            coverageThreshold,
            boxToThisTransformA,
            boxToThisTransformB,
            boxToThisTransformC,
            boxToThisTransformD,
            boxToThisTransformE,
            boxToThisTransformF,
        )

    actual fun parallelogramCoverageIsGreaterThan(
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
    ): Boolean =
        PartitionedMeshNative_parallelogramCoverageIsGreaterThan(
            nativePointer,
            parallelogramCenterX,
            parallelogramCenterY,
            parallelogramWidth,
            parallelogramHeight,
            parallelogramAngleInRadian,
            parallelogramShearFactor,
            coverageThreshold,
            parallelogramToThisTransformA,
            parallelogramToThisTransformB,
            parallelogramToThisTransformC,
            parallelogramToThisTransformD,
            parallelogramToThisTransformE,
            parallelogramToThisTransformF,
        )

    actual fun partitionedMeshCoverageIsGreaterThan(
        nativePointer: Long,
        otherShapeNativePointer: Long,
        coverageThreshold: Float,
        otherShapeToThisTransformA: Float,
        otherShapeToThisTransformB: Float,
        otherShapeToThisTransformC: Float,
        otherShapeToThisTransformD: Float,
        otherShapeToThisTransformE: Float,
        otherShapeToThisTransformF: Float,
    ): Boolean =
        PartitionedMeshNative_partitionedMeshCoverageIsGreaterThan(
            nativePointer,
            otherShapeNativePointer,
            coverageThreshold,
            otherShapeToThisTransformA,
            otherShapeToThisTransformB,
            otherShapeToThisTransformC,
            otherShapeToThisTransformD,
            otherShapeToThisTransformE,
            otherShapeToThisTransformF,
        )

    actual fun initializeSpatialIndex(nativePointer: Long) =
        PartitionedMeshNative_initializeSpatialIndex(nativePointer)

    actual fun isSpatialIndexInitialized(nativePointer: Long): Boolean =
        PartitionedMeshNative_isSpatialIndexInitialized(nativePointer)
}
