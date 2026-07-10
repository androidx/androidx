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

import androidx.ink.nativeloader.cinterop.ParallelogramNative_computeBoundingBox
import androidx.ink.nativeloader.cinterop.ParallelogramNative_computeCorners
import androidx.ink.nativeloader.cinterop.ParallelogramNative_computeSemiAxes
import androidx.ink.nativeloader.cinterop.ParallelogramNative_contains
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

/** Native helper functions for Parallelogram. */
@OptIn(ExperimentalForeignApi::class)
actual internal object ParallelogramNative {

    actual fun createBoundingBox(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
    ): ImmutableBox {
        ParallelogramNative_computeBoundingBox(
                centerX,
                centerY,
                width,
                height,
                rotationDegrees,
                skew,
            )
            .useContents {
                return ImmutableBox.fromTwoPoints(xmin, ymin, xmax, ymax)
            }
    }

    actual fun populateBoundingBox(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        outBox: MutableBox,
    ) {
        ParallelogramNative_computeBoundingBox(
                centerX,
                centerY,
                width,
                height,
                rotationDegrees,
                skew,
            )
            .useContents {
                outBox.setXBounds(xmin, xmax)
                outBox.setYBounds(ymin, ymax)
            }
    }

    actual fun createSemiAxes(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
    ): Array<ImmutableVec> {
        ParallelogramNative_computeSemiAxes(centerX, centerY, width, height, rotationDegrees, skew)
            .useContents {
                return arrayOf(ImmutableVec(first.x, first.y), ImmutableVec(second.x, second.y))
            }
    }

    actual fun populateSemiAxes(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        outAxis1: MutableVec,
        outAxis2: MutableVec,
    ) {
        ParallelogramNative_computeSemiAxes(centerX, centerY, width, height, rotationDegrees, skew)
            .useContents {
                outAxis1.x = first.x
                outAxis1.y = first.y
                outAxis2.x = second.x
                outAxis2.y = second.y
            }
    }

    actual fun createCorners(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
    ): Array<ImmutableVec> {
        ParallelogramNative_computeCorners(centerX, centerY, width, height, rotationDegrees, skew)
            .useContents {
                return arrayOf(
                    ImmutableVec(corner1.x, corner1.y),
                    ImmutableVec(corner2.x, corner2.y),
                    ImmutableVec(corner3.x, corner3.y),
                    ImmutableVec(corner4.x, corner4.y),
                )
            }
    }

    actual fun populateCorners(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        outCorner1: MutableVec,
        outCorner2: MutableVec,
        outCorner3: MutableVec,
        outCorner4: MutableVec,
    ) {
        ParallelogramNative_computeCorners(centerX, centerY, width, height, rotationDegrees, skew)
            .useContents {
                outCorner1.x = corner1.x
                outCorner1.y = corner1.y
                outCorner2.x = corner2.x
                outCorner2.y = corner2.y
                outCorner3.x = corner3.x
                outCorner3.y = corner3.y
                outCorner4.x = corner4.x
                outCorner4.y = corner4.y
            }
    }

    actual fun contains(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        pointX: Float,
        pointY: Float,
    ): Boolean {
        return ParallelogramNative_contains(
            centerX,
            centerY,
            width,
            height,
            rotationDegrees,
            skew,
            pointX,
            pointY,
        )
    }
}
