/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.ink.util

import android.graphics.Path
import android.graphics.RectF
import androidx.annotation.VisibleForTesting
import androidx.ink.geometry.PartitionedMesh
import androidx.ink.geometry.outlinesToPath
import androidx.ink.strokes.Stroke
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.StampAnnotation
import androidx.pdf.utils.getPathInputsFromPath

/**
 * Converts this [Stroke] into a [StampAnnotation].
 *
 * The outline of the stroke is extracted from its shape's mesh and used to create [PathPdfObject]s.
 *
 * @param pageNum The page number where this annotation will be placed.
 * @return A [StampAnnotation] representing this stroke.
 */
internal fun Stroke.toStampAnnotation(pageNum: Int): StampAnnotation {
    val strokeMesh: PartitionedMesh = shape
    val renderGroupPaths: List<Path> =
        (0 until strokeMesh.getRenderGroupCount()).map { groupIndex ->
            strokeMesh.outlinesToPath(groupIndex)
        }
    val pathInputs: List<PathPdfObject.PathInput> =
        renderGroupPaths.flatMap { it.getPathInputsFromPath() }

    val pathPdfObject =
        PathPdfObject(brushColor = brush.colorIntArgb, brushWidth = brush.size, inputs = pathInputs)
    val bounds = getBounds()
    return StampAnnotation(pageNum = pageNum, bounds = bounds, pdfObjects = listOf(pathPdfObject))
}

/**
 * Computes the bounds of this [Stroke].
 *
 * The bounding box is determined by the minimum and maximum x and y coordinates of the stroke's
 * shape.
 *
 * @return A [RectF] representing the bounding box of the stroke.
 */
@VisibleForTesting
internal fun Stroke.getBounds(): RectF {
    return shape.computeBoundingBox()?.let { RectF(it.xMin, it.yMin, it.xMax, it.yMax) } ?: RectF()
}
