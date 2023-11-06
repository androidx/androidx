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

package androidx.compose.foundation.text2

import android.view.KeyEvent
import android.view.inputmethod.ExtractedText
import androidx.compose.foundation.text2.input.internal.ComposeInputMethodManager
import com.google.common.truth.Truth.assertThat

internal class FakeInputMethodManager : ComposeInputMethodManager {
    private val calls = mutableListOf<String>()

    fun expectCall(description: String) {
        assertThat(calls.removeFirst()).isEqualTo(description)
    }

    fun expectNoMoreCalls() {
        assertThat(calls).isEmpty()
    }

    fun resetCalls() {
        calls.clear()
    }

    override fun restartInput() {
        calls += "restartInput"
    }

    override fun showSoftInput() {
        calls += "showSoftInput"
    }

    override fun hideSoftInput() {
        calls += "hideSoftInput"
    }

    override fun updateExtractedText(token: Int, extractedText: ExtractedText) {
        calls += "updateExtractedText"
    }

    override fun updateSelection(
        selectionStart: Int,
        selectionEnd: Int,
        compositionStart: Int,
        compositionEnd: Int
    ) {
        calls += "updateSelection($selectionStart, $selectionEnd, " +
            "$compositionStart, $compositionEnd)"
    }

    override fun sendKeyEvent(event: KeyEvent) {
        calls += "sendKeyEvent"
    }
}
