/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.content

import android.graphics.RectF

/**
 * Represents the content associated with a goto link on a page in the PDF document. Goto Link is an
 * internal navigation link which directs the user to a different location within the same pdf
 * document
 *
 * @param bounds: A list of rectangles defining the clickable area of the link.
 * @param destination: [Destination] where the goto link is directing.
 */
public class PdfPageGotoLinkContent(bounds: List<RectF>, public val destination: Destination) :
    PdfPageContent(bounds) {
    /**
     * Creates a new instance of PdfPageGotoLinkContent.Destination using the page number, x
     * coordinate, and y coordinate of the destination where goto link is directing, and the zoom
     * factor of the page when goto link takes to the destination
     *
     * Note: Here (0,0) represents top-left corner of the page.
     *
     * @param pageNumber: Page number of the goto link Destination
     * @param xCoordinate: X coordinate of the goto link Destination in points (1/ 72")
     * @param yCoordinate: Y coordinate of the goto link Destination in points (1/ 72")
     * @param zoom: Zoom factor of the page when goto link takes to the destination
     */
    public class Destination(
        public val pageNumber: Int,
        public val xCoordinate: Float,
        public val yCoordinate: Float,
        public val zoom: Float,
    )
}
