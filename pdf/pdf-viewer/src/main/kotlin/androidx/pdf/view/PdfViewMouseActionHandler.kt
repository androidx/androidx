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

import android.view.MotionEvent

/**
 * Performs actions in response to mouse shortcuts detected by [PdfViewExternalInputManager]
 *
 * @param pdfView The view on which the actions are to be taken.
 */
internal class PdfViewMouseActionHandler(pdfView: PdfView) : PdfViewExternalInputHandler(pdfView) {

    override val horizontalScrollFactor = HORIZONTAL_SCROLL_FACTOR
    override val verticalScrollFactor = VERTICAL_SCROLL_FACTOR

    fun dragSelection(event: MotionEvent): Boolean {
        return pdfView.maybeDragSelection(event, isSourceMouse = true)
    }

    private companion object {
        const val HORIZONTAL_SCROLL_FACTOR = 14
        const val VERTICAL_SCROLL_FACTOR = 14
    }
}
