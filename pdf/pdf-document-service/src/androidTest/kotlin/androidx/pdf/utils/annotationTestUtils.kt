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
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.models.StampAnnotation
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlin.Float.Companion.MAX_VALUE
import kotlin.Float.Companion.MIN_VALUE
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Returns a sample [PathPdfObject] for testing purposes. */
private fun getSamplePathObject(): PathPdfObject {
    return PathPdfObject(
        255,
        10f,
        listOf(
            PathPdfObject.PathInput(10f, 10f),
            PathPdfObject.PathInput(20f, 20f),
            PathPdfObject.PathInput(30f, 30f),
            PathPdfObject.PathInput(40f, 40f),
            PathPdfObject.PathInput(50f, 50f),
        ),
    )
}

/**
 * Returns a sample [StampAnnotation] for testing purposes.
 *
 * @param pageNum The page number for the annotation.
 * @param bounds The bounds of the annotation.
 * @return A sample [StampAnnotation].
 */
fun getSampleStampAnnotation(
    pageNum: Int,
    bounds: RectF = RectF(0f, 0f, 100f, 100f),
): StampAnnotation {
    return StampAnnotation(pageNum, bounds, listOf(getSamplePathObject()))
}

/**
 * Asserts that two [StampAnnotation] objects are equal for testing purposes.
 *
 * @param expectedAnnotation The expected [StampAnnotation].
 * @param actualAnnotation The actual [StampAnnotation].
 */
fun assertStampAnnotationEquals(
    expectedAnnotation: StampAnnotation,
    actualAnnotation: StampAnnotation,
) {
    assertThat(actualAnnotation.bounds).isEqualTo(expectedAnnotation.bounds)
    assertThat(actualAnnotation.pageNum).isEqualTo(expectedAnnotation.pageNum)

    // Assert that the list of PdfObjects within the annotation is the same
    assertThat(actualAnnotation.pdfObjects.size).isEqualTo(expectedAnnotation.pdfObjects.size)
    for (i in expectedAnnotation.pdfObjects.indices) {
        val actualPathPdfObject = actualAnnotation.pdfObjects[i] as PathPdfObject
        val expectedPathPdfObject = expectedAnnotation.pdfObjects[i] as PathPdfObject
        assertThat(actualPathPdfObject.brushColor).isEqualTo(expectedPathPdfObject.brushColor)
        assertThat(actualPathPdfObject.brushWidth).isEqualTo(expectedPathPdfObject.brushWidth)

        // Assert that the list of PathInputs within the PathPdfObject is the same
        assertThat(actualPathPdfObject.inputs.size).isEqualTo(expectedPathPdfObject.inputs.size)
        for (j in actualPathPdfObject.inputs.indices) {
            val actualPathInput = actualPathPdfObject.inputs[j]
            val expectedPathInput = expectedPathPdfObject.inputs[j]
            assertThat(actualPathInput.x).isEqualTo(expectedPathInput.x)
            assertThat(actualPathInput.y).isEqualTo(expectedPathInput.y)
        }
    }
}

fun createPdfAnnotationDataList(
    numAnnots: Int,
    pathLength: Int,
    invalidRatio: Float = 0f,
): List<PdfAnnotationData> {
    require(invalidRatio >= 0 && invalidRatio <= 1) { "Ratio should be between 0 and 1" }

    val invalidCount = (numAnnots * invalidRatio).toInt()
    return List(numAnnots) { index ->
        val isInvalid = index < invalidCount
        val pageNum = if (isInvalid) -1 else 0

        createPdfAnnotationData(pageNum, pathLength)
    }
}

fun createPdfAnnotationData(pageNum: Int, pathLength: Int): PdfAnnotationData =
    PdfAnnotationData(
        editId = EditId(pageNum, value = UUID.randomUUID().toString()),
        annotation = createStampAnnotationWithPath(pageNum, pathLength),
    )

fun createStampAnnotationWithPath(pageNum: Int, pathSize: Int): StampAnnotation {
    val randomPathInputs = createPathPdfObjectList(pathSize)
    return StampAnnotation(
        pageNum,
        bounds = randomPathInputs.computeBoundsForPath(),
        pdfObjects = randomPathInputs,
    )
}

fun createPathPdfObjectList(size: Int): List<PathPdfObject> {
    return IntArray(size).map { randomizePathPdfObject(pathLength = 10) }
}

fun randomizePathPdfObject(pathLength: Int): PathPdfObject =
    PathPdfObject(brushColor = 0, brushWidth = 0f, inputs = randomizePathInputs(pathLength))

fun randomizePathInputs(pathLength: Int): List<PathPdfObject.PathInput> =
    IntArray(pathLength).map {
        PathPdfObject.PathInput(
            x = abs(Random.nextInt(100, 1000).toFloat()),
            y = abs(Random.nextInt(100, 1000).toFloat()),
        )
    }

fun List<PathPdfObject.PathInput>.computeBounds(): RectF {
    val left = this.fold(MAX_VALUE) { acc, input -> min(acc, input.x) }
    val top = this.fold(MAX_VALUE) { acc, input -> min(acc, input.y) }
    val right = this.fold(MIN_VALUE) { acc, input -> max(acc, input.x) }
    val bottom = this.fold(MIN_VALUE) { acc, input -> max(acc, input.y) }
    return RectF(left, top, right, bottom)
}

fun List<PathPdfObject>.computeBoundsForPath(): RectF {
    val emptyRect = RectF(MAX_VALUE, MAX_VALUE, MIN_VALUE, MIN_VALUE)
    return this.fold(emptyRect) { acc, pathObject -> acc.merge(pathObject.inputs.computeBounds()) }
}

fun RectF.merge(other: RectF): RectF =
    RectF(
        /* left = */ min(this.left, other.left),
        /* top = */ min(this.top, other.top),
        /* right = */ max(this.right, other.right),
        /* bottom = */ max(this.bottom, other.bottom),
    )
