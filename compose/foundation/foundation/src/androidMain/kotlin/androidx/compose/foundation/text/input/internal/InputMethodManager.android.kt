/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.ExtractedText
import androidx.core.view.SoftwareKeyboardControllerCompat

internal interface InputMethodManager {
    fun isActive(): Boolean

    fun restartInput()

    fun showSoftInput()

    fun hideSoftInput()

    fun updateExtractedText(
        token: Int,
        extractedText: ExtractedText
    )

    fun updateSelection(
        selectionStart: Int,
        selectionEnd: Int,
        compositionStart: Int,
        compositionEnd: Int
    )

    fun updateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo)
}

/**
 * Wrapper class to prevent depending on getSystemService and final InputMethodManager.
 * Let's us test TextInputServiceAndroid class.
 */
internal class InputMethodManagerImpl(private val view: View) : InputMethodManager {

    private val imm by lazy(LazyThreadSafetyMode.NONE) {
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
    }

    private val softwareKeyboardControllerCompat =
        SoftwareKeyboardControllerCompat(view)

    override fun isActive(): Boolean = imm.isActive(view)

    override fun restartInput() {
        imm.restartInput(view)
    }

    override fun showSoftInput() {
        if (DEBUG && !view.hasWindowFocus()) {
            Log.d(TAG, "InputMethodManagerImpl: requesting soft input on non focused field")
        }

        softwareKeyboardControllerCompat.show()
    }

    override fun hideSoftInput() {
        softwareKeyboardControllerCompat.hide()
    }

    override fun updateExtractedText(
        token: Int,
        extractedText: ExtractedText
    ) {
        imm.updateExtractedText(view, token, extractedText)
    }

    override fun updateSelection(
        selectionStart: Int,
        selectionEnd: Int,
        compositionStart: Int,
        compositionEnd: Int
    ) {
        imm.updateSelection(view, selectionStart, selectionEnd, compositionStart, compositionEnd)
    }

    override fun updateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {
        imm.updateCursorAnchorInfo(view, cursorAnchorInfo)
    }
}
