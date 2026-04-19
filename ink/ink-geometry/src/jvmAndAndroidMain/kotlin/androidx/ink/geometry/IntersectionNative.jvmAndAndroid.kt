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

package androidx.ink.geometry

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object IntersectionNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun vecSegmentIntersects(
        vecX: Float,
        vecY: Float,
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
    ): Boolean

    @UsedByNative
    actual external fun vecTriangleIntersects(
        vecX: Float,
        vecY: Float,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
    ): Boolean

    @UsedByNative
    actual external fun vecParallelogramIntersects(
        vecX: Float,
        vecY: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
    ): Boolean

    @UsedByNative
    actual external fun vecBoxIntersects(
        vecX: Float,
        vecY: Float,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
    ): Boolean

    @UsedByNative
    actual external fun segmentSegmentIntersects(
        segment1StartX: Float,
        segment1StartY: Float,
        segment1EndX: Float,
        segment1EndY: Float,
        segment2StartX: Float,
        segment2StartY: Float,
        segment2EndX: Float,
        segment2EndY: Float,
    ): Boolean

    @UsedByNative
    actual external fun segmentTriangleIntersects(
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
    ): Boolean

    @UsedByNative
    actual external fun segmentBoxIntersects(
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
    ): Boolean

    @UsedByNative
    actual external fun segmentParallelogramIntersects(
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
    ): Boolean

    @UsedByNative
    actual external fun triangleTriangleIntersects(
        triangle1P0X: Float,
        triangle1P0Y: Float,
        triangle1P1X: Float,
        triangle1P1Y: Float,
        triangle1P2X: Float,
        triangle1P2Y: Float,
        triangle2P0X: Float,
        triangle2P0Y: Float,
        triangle2P1X: Float,
        triangle2P1Y: Float,
        triangle2P2X: Float,
        triangle2P2Y: Float,
    ): Boolean

    @UsedByNative
    actual external fun triangleBoxIntersects(
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
    ): Boolean

    @UsedByNative
    actual external fun triangleParallelogramIntersects(
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
    ): Boolean

    @UsedByNative
    actual external fun boxBoxIntersects(
        box1XMin: Float,
        box1YMin: Float,
        box1XMax: Float,
        box1YMax: Float,
        box2XMin: Float,
        box2YMin: Float,
        box2XMax: Float,
        box2YMax: Float,
    ): Boolean

    @UsedByNative
    actual external fun boxParallelogramIntersects(
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
    ): Boolean

    @UsedByNative
    actual external fun parallelogramParallelogramIntersects(
        parallelogram1CenterX: Float,
        parallelogram1CenterY: Float,
        parallelogram1Width: Float,
        parallelogram1Height: Float,
        parallelogram1AngleInRadian: Float,
        parallelogram1ShearFactor: Float,
        parallelogram2CenterX: Float,
        parallelogram2CenterY: Float,
        parallelogram2Width: Float,
        parallelogram2Height: Float,
        parallelogram2AngleInRadian: Float,
        parallelogram2ShearFactor: Float,
    ): Boolean

    @UsedByNative
    actual external fun partitionedMeshVecIntersects(
        partitionedMeshNativePointer: Long,
        vecX: Float,
        vecY: Float,
        meshToVecA: Float,
        meshToVecB: Float,
        meshToVecC: Float,
        meshToVecD: Float,
        meshToVecE: Float,
        meshToVecF: Float,
    ): Boolean

    @UsedByNative
    actual external fun partitionedMeshSegmentIntersects(
        partitionedMeshNativePointer: Long,
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
        meshToSegmentA: Float,
        meshToSegmentB: Float,
        meshToSegmentC: Float,
        meshToSegmentD: Float,
        meshToSegmentE: Float,
        meshToSegmentF: Float,
    ): Boolean

    @UsedByNative
    actual external fun partitionedMeshTriangleIntersects(
        partitionedMeshNativePointer: Long,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        meshToTriangleA: Float,
        meshToTriangleB: Float,
        meshToTriangleC: Float,
        meshToTriangleD: Float,
        meshToTriangleE: Float,
        meshToTriangleF: Float,
    ): Boolean

    @UsedByNative
    actual external fun partitionedMeshBoxIntersects(
        partitionedMeshNativePointer: Long,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
        meshToBoxA: Float,
        meshToBoxB: Float,
        meshToBoxC: Float,
        meshToBoxD: Float,
        meshToBoxE: Float,
        meshToBoxF: Float,
    ): Boolean

    @UsedByNative
    actual external fun partitionedMeshParallelogramIntersects(
        partitionedMeshNativePointer: Long,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
        meshToParallelogramA: Float,
        meshToParallelogramB: Float,
        meshToParallelogramC: Float,
        meshToParallelogramD: Float,
        meshToParallelogramE: Float,
        meshToParallelogramF: Float,
    ): Boolean

    @UsedByNative
    actual external fun partitionedMeshPartitionedMeshIntersects(
        thisPartitionedMeshNativePointer: Long,
        otherPartitionedMeshNativePointer: Long,
        thisToCommonTransformA: Float,
        thisToCommonTransformB: Float,
        thisToCommonTransformC: Float,
        thisToCommonTransformD: Float,
        thisToCommonTransformE: Float,
        thisToCommonTransformF: Float,
        otherToCommonTransformA: Float,
        otherToCommonTransformB: Float,
        otherToCommonTransformC: Float,
        otherToCommonTransformD: Float,
        otherToCommonTransformE: Float,
        otherToCommonTransformF: Float,
    ): Boolean
}
