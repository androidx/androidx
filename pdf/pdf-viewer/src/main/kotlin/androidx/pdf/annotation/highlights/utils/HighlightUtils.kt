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

package androidx.pdf.annotation.highlights.utils

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PathPdfObject.PathInput
import androidx.pdf.exceptions.RequestFailedException
import androidx.pdf.exceptions.RequestMetadata
import androidx.pdf.util.TEXT_BOUNDS_REQUEST_NAME

/** Applies a [Matrix] transformation to this point, returning a new [PointF]. */
internal fun PointF.applyTransform(transform: Matrix): PointF {
    val pointArr = floatArrayOf(this.x, this.y)
    transform.mapPoints(pointArr)
    return PointF(pointArr[0], pointArr[1])
}

/** Calculates the union bounding box of all rectangles. */
internal fun List<RectF>.computeBoundingBox(): RectF {
    if (isEmpty()) return RectF()

    val boundingBox = RectF(this[0])
    for (i in 1 until size) {
        boundingBox.union(this[i])
    }
    return boundingBox
}

/** Converts a list of [RectF] bounds into [PathPdfObject]s. */
internal fun List<RectF>.toPathPdfObjects(color: Int): List<PathPdfObject> {
    return map { rect ->
        PathPdfObject(
            brushColor = color,
            brushWidth = 0f,
            inputs =
                listOf(
                    PathInput(rect.left, rect.top, PathInput.MOVE_TO),
                    PathInput(rect.right, rect.top, PathInput.LINE_TO),
                    PathInput(rect.right, rect.bottom, PathInput.LINE_TO),
                    PathInput(rect.left, rect.bottom, PathInput.LINE_TO),
                    PathInput(rect.left, rect.top, PathInput.LINE_TO),
                ),
        )
    }
}

internal fun createTextBoundsRequestFailedException(
    pageNum: Int,
    throwable: Throwable,
): RequestFailedException {
    return RequestFailedException(
        requestMetadata =
            RequestMetadata(
                requestName = TEXT_BOUNDS_REQUEST_NAME,
                pageRange = IntRange(pageNum, pageNum),
            ),
        throwable = throwable,
        // Non-critical failure, user can retry the operation.
        showError = false,
    )
}
