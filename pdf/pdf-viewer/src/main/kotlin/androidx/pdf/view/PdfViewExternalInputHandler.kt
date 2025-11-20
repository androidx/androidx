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

package androidx.pdf.view

import androidx.pdf.util.MathUtils

/**
 * This abstract class provides common functionality for handling external input shortcuts.
 *
 * Handlers specific to different devices should extend this class.
 *
 * @param pdfView The view on which the actions are performed.
 */
internal abstract class PdfViewExternalInputHandler(val pdfView: PdfView) {
    abstract val horizontalScrollFactor: Int
    abstract val verticalScrollFactor: Int

    fun scrollDown() {
        val scrollAmount =
            ExternalInputUtils.calculateScroll(pdfView.viewportHeight, verticalScrollFactor)
        pdfView.scrollBy(0, scrollAmount)
    }

    fun scrollLeft() {
        val scrollAmount =
            ExternalInputUtils.calculateScroll(pdfView.viewportWidth, horizontalScrollFactor)
        pdfView.scrollBy(-scrollAmount, 0)
    }

    fun scrollRight() {
        val scrollAmount =
            ExternalInputUtils.calculateScroll(pdfView.viewportWidth, horizontalScrollFactor)
        pdfView.scrollBy(scrollAmount, 0)
    }

    fun scrollUp() {
        val scrollAmount =
            ExternalInputUtils.calculateScroll(pdfView.viewportHeight, verticalScrollFactor)
        pdfView.scrollBy(0, -scrollAmount)
    }

    fun zoomIn(x: Float, y: Float) {
        val newZoom =
            ExternalInputUtils.calculateGreaterZoom(
                pdfView.zoom,
                pdfView.getFitToWidthZoom(),
                ZOOM_LEVELS,
                pdfView.maxZoom,
            )
        applyZoom(newZoom, x, y)
    }

    fun zoomOut(x: Float, y: Float) {
        val newZoom =
            ExternalInputUtils.calculateSmallerZoom(
                pdfView.zoom,
                pdfView.getFitToWidthZoom(),
                ZOOM_LEVELS,
                pdfView.minZoom,
            )
        applyZoom(newZoom, x, y)
    }

    protected fun applyZoom(newZoom: Float, x: Float, y: Float) {
        val effectiveNewZoom = MathUtils.clamp(newZoom, pdfView.minZoom, pdfView.maxZoom)
        pdfView.zoomTo(effectiveNewZoom, x, y)
    }

    private companion object {
        val ZOOM_LEVELS =
            listOf(
                0.25f,
                0.33f,
                0.50f,
                0.67f,
                0.75f,
                0.80f,
                0.90f,
                1.0f,
                1.10f,
                1.25f,
                1.50f,
                1.75f,
                2.0f,
                2.50f,
                3.0f,
                4.0f,
                5.0f,
                6.0f,
                7.0f,
                8.0f,
                10.0f,
                12.0f,
                16.0f,
                20.0f,
                25.0f,
            )
    }
}
