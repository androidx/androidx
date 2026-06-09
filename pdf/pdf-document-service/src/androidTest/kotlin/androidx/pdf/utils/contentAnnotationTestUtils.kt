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

package androidx.pdf.utils

import android.graphics.RectF
import androidx.pdf.annotation.content.PathPdfObject
import androidx.pdf.annotation.content.PathPdfObject.PathInput
import androidx.pdf.annotation.content.StampAnnotation
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Returns a sample public [StampAnnotation] for testing purposes. */
fun getSampleContentStampAnnotation(
    pageNum: Int,
    bounds: RectF = RectF(0f, 0f, 100f, 100f),
): StampAnnotation {
    return StampAnnotation(pageNum, bounds, listOf(getSampleContentPathObject()))
}

private fun getSampleContentPathObject(): PathPdfObject {
    return PathPdfObject(
        255,
        10f,
        listOf(
            PathInput(10f, 10f, PathInput.MOVE_TO),
            PathInput(20f, 20f, PathInput.LINE_TO),
            PathInput(30f, 30f, PathInput.LINE_TO),
            PathInput(40f, 40f, PathInput.LINE_TO),
            PathInput(50f, 50f, PathInput.LINE_TO),
        ),
    )
}

fun createContentStampAnnotationWithPath(pageNum: Int, pathSize: Int): StampAnnotation {
    val randomPathInputs = createContentPathPdfObjectList(pathSize)
    return StampAnnotation(
        pageNum,
        bounds = randomPathInputs.computeContentBoundsForPath(),
        pdfObjects = randomPathInputs,
    )
}

fun createContentPathPdfObjectList(size: Int): List<PathPdfObject> {
    return IntArray(size).map { randomizeContentPathPdfObject(pathLength = 10) }
}

fun randomizeContentPathPdfObject(pathLength: Int): PathPdfObject =
    PathPdfObject(brushColor = 0, brushWidth = 0f, inputs = randomizeContentPathInputs(pathLength))

fun randomizeContentPathInputs(pathLength: Int): List<PathInput> =
    IntArray(pathLength).mapIndexed { index, _ ->
        val command = if (index == 0) PathInput.MOVE_TO else PathInput.LINE_TO
        PathInput(
            x = Random.nextInt(100, 1000).toFloat(),
            y = Random.nextInt(100, 1000).toFloat(),
            command = command,
        )
    }

fun List<PathInput>.computeContentBounds(): RectF {
    val left = this.fold(Float.MAX_VALUE) { acc, input -> min(acc, input.x) }
    val top = this.fold(Float.MAX_VALUE) { acc, input -> min(acc, input.y) }
    val right = this.fold(Float.MIN_VALUE) { acc, input -> max(acc, input.x) }
    val bottom = this.fold(Float.MIN_VALUE) { acc, input -> max(acc, input.y) }
    return RectF(left, top, right, bottom)
}

fun List<PathPdfObject>.computeContentBoundsForPath(): RectF {
    val emptyRect = RectF(Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
    return this.fold(emptyRect) { acc, pathObject ->
        acc.mergeContent(pathObject.inputs.computeContentBounds())
    }
}

fun RectF.mergeContent(other: RectF): RectF =
    RectF(
        /* left = */ min(this.left, other.left),
        /* top = */ min(this.top, other.top),
        /* right = */ max(this.right, other.right),
        /* bottom = */ max(this.bottom, other.bottom),
    )
