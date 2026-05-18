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

package androidx.pdf.annotation

import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.RestrictTo

/** A provider interface that abstracts the retrieval of text boundary information. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface TextBoundsProvider {
    /**
     * Asynchronously obtains the rectangular bounds of text between two points on a specific page.
     *
     * The returned list contains [RectF] objects representing the boundaries of the text characters
     * found in the range. These rectangles should be provided in PDF coordinates to ensure
     * consistency across different zoom levels and transforms.
     *
     * @param pageNum The 0-based index of the page to query.
     * @param start The starting point of the selection in PDF coordinates.
     * @param end The end point of the selection in PDF coordinates.
     * @return A list of [RectF] representing the visual segments of the selected text. Returns an
     *   empty list if no text is found between the specified points.
     * @throws Exception if a failure occurs during extraction
     */
    public suspend fun getTextBoundsBetweenPoints(
        pageNum: Int,
        start: PointF,
        end: PointF,
    ): List<RectF>
}
