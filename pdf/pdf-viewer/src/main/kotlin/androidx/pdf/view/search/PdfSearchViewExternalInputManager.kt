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

package androidx.pdf.view.search

import android.view.KeyEvent

/**
 * This manager inspects each event to determine and execute any action that should be performed
 *
 * @param pdfSearchView The view on which the actions are to performed.
 */
internal class PdfSearchViewExternalInputManager(private val pdfSearchView: PdfSearchView) {

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> {
                pdfSearchView.onSearchCloseRequested?.invoke()
                true
            }
            else -> false
        }
    }
}
