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

import android.graphics.Path
import android.os.ParcelFileDescriptor
import androidx.annotation.RestrictTo
import androidx.pdf.annotation.models.PathPdfObject.PathInput
import androidx.pdf.annotation.models.PdfAnnotationData
import java.io.IOException

/** Tolerance level for path approximation. */
private const val ACCEPTABLE_TOLERANCE_IN_PATH = 0.5f

/**
 * Writes a list of [PdfAnnotationData] objects to a [ParcelFileDescriptor].
 *
 * @param pfd The [ParcelFileDescriptor] to write to.
 * @param annotations The list of [PdfAnnotationData] objects to write.
 * @throws IOException If there is an error writing to the file.
 */
internal fun writeAnnotationsToFile(
    pfd: ParcelFileDescriptor,
    annotations: List<PdfAnnotationData>,
) {
    // TODO: Read and write annotations to file with org.json
}

/**
 * Reads a list of [PdfAnnotationData] objects from a [ParcelFileDescriptor].
 *
 * @param pfd The [ParcelFileDescriptor] to read from.
 * @return A list of [PdfAnnotationData] objects read from the file.
 */
internal fun readAnnotationsFromPfd(pfd: ParcelFileDescriptor): List<PdfAnnotationData> {
    // TODO: Read and write annotations to file with org.json
    val jsonString = readFromPfd(pfd)
    return listOf()
}

/**
 * Creates a [Path] object from a list of [PathInput] points.
 *
 * @return A [Path] object constructed from the input points. Returns an empty Path if the input
 *   list is empty.
 */
internal fun List<PathInput>.getPathFromPathInputs(): Path {
    val pathInputs = this
    if (pathInputs.isEmpty()) return Path()

    val path = Path()
    path.moveTo(pathInputs[0].x, pathInputs[0].y)
    for (i in 1 until pathInputs.size) {
        path.lineTo(pathInputs[i].x, pathInputs[i].y)
    }
    return path
}

/**
 * Creates a a list of [PathInput] points from [Path] object.
 *
 * @return A list of [PathInput] constructed from the path object. Returns an empty list if the path
 *   is empty.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun Path.getPathInputsFromPath(): List<PathInput> {
    val pathInputs = mutableListOf<PathInput>()
    val approx: FloatArray = this.approximate(ACCEPTABLE_TOLERANCE_IN_PATH)

    for (i in 0 until approx.size step 3) {
        pathInputs.add(PathInput(approx[i + 1], approx[i + 2]))
    }
    return pathInputs
}
