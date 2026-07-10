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

import androidx.ink.nativeloader.cinterop.IntersectionNative_boxBoxIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_boxParallelogramIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_parallelogramParallelogramIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_partitionedMeshBoxIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_partitionedMeshParallelogramIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_partitionedMeshPartitionedMeshIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_partitionedMeshSegmentIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_partitionedMeshTriangleIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_partitionedMeshVecIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_segmentBoxIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_segmentParallelogramIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_segmentSegmentIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_segmentTriangleIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_triangleBoxIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_triangleParallelogramIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_triangleTriangleIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_vecBoxIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_vecParallelogramIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_vecSegmentIntersects
import androidx.ink.nativeloader.cinterop.IntersectionNative_vecTriangleIntersects
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual internal object IntersectionNative {

    actual fun vecSegmentIntersects(
        vecX: Float,
        vecY: Float,
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
    ): Boolean =
        IntersectionNative_vecSegmentIntersects(
            vecX,
            vecY,
            segmentStartX,
            segmentStartY,
            segmentEndX,
            segmentEndY,
        )

    actual fun vecTriangleIntersects(
        vecX: Float,
        vecY: Float,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
    ): Boolean =
        IntersectionNative_vecTriangleIntersects(
            vecX,
            vecY,
            triangleP0X,
            triangleP0Y,
            triangleP1X,
            triangleP1Y,
            triangleP2X,
            triangleP2Y,
        )

    actual fun vecParallelogramIntersects(
        vecX: Float,
        vecY: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
    ): Boolean =
        IntersectionNative_vecParallelogramIntersects(
            vecX,
            vecY,
            parallelogramCenterX,
            parallelogramCenterY,
            parallelogramWidth,
            parallelogramHeight,
            parallelogramAngleInRadian,
            parallelogramShearFactor,
        )

    actual fun vecBoxIntersects(
        vecX: Float,
        vecY: Float,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
    ): Boolean = IntersectionNative_vecBoxIntersects(vecX, vecY, boxXMin, boxYMin, boxXMax, boxYMax)

    actual fun segmentSegmentIntersects(
        segment1StartX: Float,
        segment1StartY: Float,
        segment1EndX: Float,
        segment1EndY: Float,
        segment2StartX: Float,
        segment2StartY: Float,
        segment2EndX: Float,
        segment2EndY: Float,
    ): Boolean =
        IntersectionNative_segmentSegmentIntersects(
            segment1StartX,
            segment1StartY,
            segment1EndX,
            segment1EndY,
            segment2StartX,
            segment2StartY,
            segment2EndX,
            segment2EndY,
        )

    actual fun segmentTriangleIntersects(
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
    ): Boolean =
        IntersectionNative_segmentTriangleIntersects(
            segmentStartX,
            segmentStartY,
            segmentEndX,
            segmentEndY,
            triangleP0X,
            triangleP0Y,
            triangleP1X,
            triangleP1Y,
            triangleP2X,
            triangleP2Y,
        )

    actual fun segmentBoxIntersects(
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
    ): Boolean =
        IntersectionNative_segmentBoxIntersects(
            segmentStartX,
            segmentStartY,
            segmentEndX,
            segmentEndY,
            boxXMin,
            boxYMin,
            boxXMax,
            boxYMax,
        )

    actual fun segmentParallelogramIntersects(
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
    ): Boolean =
        IntersectionNative_segmentParallelogramIntersects(
            segmentStartX,
            segmentStartY,
            segmentEndX,
            segmentEndY,
            parallelogramCenterX,
            parallelogramCenterY,
            parallelogramWidth,
            parallelogramHeight,
            parallelogramAngleInRadian,
            parallelogramShearFactor,
        )

    actual fun triangleTriangleIntersects(
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
    ): Boolean =
        IntersectionNative_triangleTriangleIntersects(
            triangle1P0X,
            triangle1P0Y,
            triangle1P1X,
            triangle1P1Y,
            triangle1P2X,
            triangle1P2Y,
            triangle2P0X,
            triangle2P0Y,
            triangle2P1X,
            triangle2P1Y,
            triangle2P2X,
            triangle2P2Y,
        )

    actual fun triangleBoxIntersects(
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
    ): Boolean =
        IntersectionNative_triangleBoxIntersects(
            triangleP0X,
            triangleP0Y,
            triangleP1X,
            triangleP1Y,
            triangleP2X,
            triangleP2Y,
            boxXMin,
            boxYMin,
            boxXMax,
            boxYMax,
        )

    actual fun triangleParallelogramIntersects(
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
    ): Boolean =
        IntersectionNative_triangleParallelogramIntersects(
            triangleP0X,
            triangleP0Y,
            triangleP1X,
            triangleP1Y,
            triangleP2X,
            triangleP2Y,
            parallelogramCenterX,
            parallelogramCenterY,
            parallelogramWidth,
            parallelogramHeight,
            parallelogramAngleInRadian,
            parallelogramShearFactor,
        )

    actual fun boxBoxIntersects(
        box1XMin: Float,
        box1YMin: Float,
        box1XMax: Float,
        box1YMax: Float,
        box2XMin: Float,
        box2YMin: Float,
        box2XMax: Float,
        box2YMax: Float,
    ): Boolean =
        IntersectionNative_boxBoxIntersects(
            box1XMin,
            box1YMin,
            box1XMax,
            box1YMax,
            box2XMin,
            box2YMin,
            box2XMax,
            box2YMax,
        )

    actual fun boxParallelogramIntersects(
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
    ): Boolean =
        IntersectionNative_boxParallelogramIntersects(
            boxXMin,
            boxYMin,
            boxXMax,
            boxYMax,
            parallelogramCenterX,
            parallelogramCenterY,
            parallelogramWidth,
            parallelogramHeight,
            parallelogramAngleInRadian,
            parallelogramShearFactor,
        )

    actual fun parallelogramParallelogramIntersects(
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
    ): Boolean =
        IntersectionNative_parallelogramParallelogramIntersects(
            parallelogram1CenterX,
            parallelogram1CenterY,
            parallelogram1Width,
            parallelogram1Height,
            parallelogram1AngleInRadian,
            parallelogram1ShearFactor,
            parallelogram2CenterX,
            parallelogram2CenterY,
            parallelogram2Width,
            parallelogram2Height,
            parallelogram2AngleInRadian,
            parallelogram2ShearFactor,
        )

    actual fun partitionedMeshVecIntersects(
        partitionedMeshNativePointer: Long,
        vecX: Float,
        vecY: Float,
        meshToVecA: Float,
        meshToVecB: Float,
        meshToVecC: Float,
        meshToVecD: Float,
        meshToVecE: Float,
        meshToVecF: Float,
    ): Boolean =
        IntersectionNative_partitionedMeshVecIntersects(
            partitionedMeshNativePointer,
            vecX,
            vecY,
            meshToVecA,
            meshToVecB,
            meshToVecC,
            meshToVecD,
            meshToVecE,
            meshToVecF,
        )

    actual fun partitionedMeshSegmentIntersects(
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
    ): Boolean =
        IntersectionNative_partitionedMeshSegmentIntersects(
            partitionedMeshNativePointer,
            segmentStartX,
            segmentStartY,
            segmentEndX,
            segmentEndY,
            meshToSegmentA,
            meshToSegmentB,
            meshToSegmentC,
            meshToSegmentD,
            meshToSegmentE,
            meshToSegmentF,
        )

    actual fun partitionedMeshTriangleIntersects(
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
    ): Boolean =
        IntersectionNative_partitionedMeshTriangleIntersects(
            partitionedMeshNativePointer,
            triangleP0X,
            triangleP0Y,
            triangleP1X,
            triangleP1Y,
            triangleP2X,
            triangleP2Y,
            meshToTriangleA,
            meshToTriangleB,
            meshToTriangleC,
            meshToTriangleD,
            meshToTriangleE,
            meshToTriangleF,
        )

    actual fun partitionedMeshBoxIntersects(
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
    ): Boolean =
        IntersectionNative_partitionedMeshBoxIntersects(
            partitionedMeshNativePointer,
            boxXMin,
            boxYMin,
            boxXMax,
            boxYMax,
            meshToBoxA,
            meshToBoxB,
            meshToBoxC,
            meshToBoxD,
            meshToBoxE,
            meshToBoxF,
        )

    actual fun partitionedMeshParallelogramIntersects(
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
    ): Boolean =
        IntersectionNative_partitionedMeshParallelogramIntersects(
            partitionedMeshNativePointer,
            parallelogramCenterX,
            parallelogramCenterY,
            parallelogramWidth,
            parallelogramHeight,
            parallelogramAngleInRadian,
            parallelogramShearFactor,
            meshToParallelogramA,
            meshToParallelogramB,
            meshToParallelogramC,
            meshToParallelogramD,
            meshToParallelogramE,
            meshToParallelogramF,
        )

    actual fun partitionedMeshPartitionedMeshIntersects(
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
    ): Boolean =
        IntersectionNative_partitionedMeshPartitionedMeshIntersects(
            thisPartitionedMeshNativePointer,
            otherPartitionedMeshNativePointer,
            thisToCommonTransformA,
            thisToCommonTransformB,
            thisToCommonTransformC,
            thisToCommonTransformD,
            thisToCommonTransformE,
            thisToCommonTransformF,
            otherToCommonTransformA,
            otherToCommonTransformB,
            otherToCommonTransformC,
            otherToCommonTransformD,
            otherToCommonTransformE,
            otherToCommonTransformF,
        )
}
