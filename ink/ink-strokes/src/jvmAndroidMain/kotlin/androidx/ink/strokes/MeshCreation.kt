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

@file:JvmName("MeshCreation")

package androidx.ink.strokes

import androidx.ink.geometry.PartitionedMesh
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/**
 * Creates a [PartitionedMesh] of the shape enclosed by the given [StrokeInputBatch] input points. A
 * typical use case is selecting a region of the scene and performing hit testing with the resulting
 * [PartitionedMesh].
 *
 * For a given stroke this algorithm aims to:
 * 1. Identify and create any connections that the user may have intended to make but did not fully
 *    connect.
 * 2. Trim any extra end points that the user did not intend to be part of the selected area.
 *
 * Example usage:
 * ```
 * fun onStrokeFinished(stroke: Stroke) {
 *    val selectionRegion = stroke.inputs.createClosedShape()
 *    for (stroke in myScene.strokes) {
 *        if (stroke.shape.intersects(selectionRegion)) {
 *            myScene.setSelected(stroke, true)
 *        }
 *    }
 * }
 * ```
 *
 * @return The [PartitionedMesh] of the closed shape. If there are fewer than 3 input points, or if
 *   there are fewer than 3 points remaining after removing points with the same (x,y) coordinates
 *   as the previous point, this function will return a [PartitionedMesh] that is point-like (1
 *   point remaining) or segment-like (2 points remaining). The resulting mesh will have an area of
 *   0 but can still be used for hit testing via intersection.
 * @receiver The [StrokeInputBatch] to create a closed shape from.
 */
public fun StrokeInputBatch.createClosedShape(): PartitionedMesh {
    return PartitionedMesh(
        MeshCreationNative.nativeCreateClosedShapeFromStrokeInputBatch(this.nativePointer)
    )
}

@UsedByNative
private object MeshCreationNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    public external fun nativeCreateClosedShapeFromStrokeInputBatch(
        strokeInputBatchNativePointer: Long
    ): Long
}
