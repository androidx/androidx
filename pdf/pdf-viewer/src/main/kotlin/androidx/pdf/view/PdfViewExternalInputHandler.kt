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

/**
 * This abstract class provides common functionality for handling external input shortcuts.
 *
 * Handlers specific to different devices should extend this class.
 *
 * @param pdfView The view on which the actions are performed.
 */
internal abstract class PdfViewExternalInputHandler(val pdfView: PdfView) {
    abstract val verticalScrollFactor: Int
    abstract val horizontalScrollFactor: Int

    fun scrollUp() {
        val scrollAmount =
            ExternalInputUtils.calculateScroll(pdfView.viewportHeight, verticalScrollFactor)
        pdfView.scrollBy(0, -scrollAmount)
    }

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
}
