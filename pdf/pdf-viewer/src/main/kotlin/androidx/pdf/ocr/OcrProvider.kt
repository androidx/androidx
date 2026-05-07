/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.ocr

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.RestrictTo
import java.io.Closeable

/**
 * Interface for processing images to recognize text.
 *
 * Implementations should handle the complexity of OCR processing and return a structured
 * [OcrResult] that allows for spatial queries.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface OcrProvider : Closeable {
    /**
     * Processes the given [image] and returns recognized text results.
     *
     * @param image The bitmap to process for text recognition.
     * @return An [OcrResult] containing the recognized text and its visual mapping, or `null` if no
     *   text was recognized or an error occurred.
     */
    public suspend fun recognizeText(image: Bitmap): OcrResult?

    override fun close()
}

/**
 * Represents a segment of recognized text and its visual mapping in an image.
 *
 * @property text The recognized string content.
 * @property bounds A list of [Rect] objects representing the visual bounding boxes of the text. The
 *   bounds are typically aggregated by line to optimize for rendering and selection.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OcrText(public val text: String, public val bounds: List<Rect>)

/**
 * Represents the complete result of an OCR process, providing methods to query text spatially.
 *
 * This interface manages the mapping between the raw recognized text and its visual coordinates in
 * the original image.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface OcrResult {
    /** All recognized text and its corresponding bounding boxes. */
    public val allText: OcrText

    /** Whether the primary direction of the recognized text is Right-to-Left. */
    public val isRtl: Boolean

    /**
     * Returns the text and its bounding boxes within the selection range defined by two points.
     *
     * The selection range is determined by finding the closest characters to the provided
     * [startPoint] and [endPoint].
     *
     * @param startPoint The starting coordinate of the selection, relative to image dimensions.
     * @param endPoint The ending coordinate of the selection, relative to image dimensions.
     * @return An [OcrText] object containing the selected text and its bounds.
     */
    public fun getText(startPoint: Point, endPoint: Point): OcrText

    /**
     * Returns the word and its bounding boxes at the specified coordinate.
     *
     * If the [point] lies within a word, that entire word is returned. If the point is on
     * whitespace or outside any recognized text, `null` is returned.
     *
     * @param point The coordinate to query, relative to image dimensions.
     * @return An [OcrText] object containing the word at the point, or `null` if no word is found.
     */
    public fun getWordAt(point: Point): OcrText?

    /**
     * Searches for occurrences of the [searchTerm] and returns their bounding boxes.
     *
     * @param searchTerm The string to search for.
     * @return A list of lists of [Rect] objects, where each inner list represents the visual
     *   bounding boxes for one occurrence of the search term.
     */
    public fun getSearchBounds(searchTerm: String): List<List<Rect>>
}
