/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.sample

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button

/**
 * A test IME that currently provides a minimal UI containing a "Close" button. To use this, go to
 * "Settings > System > Languages & Input > On-screen keyboard" and enable "Test IME". Remember you
 * may still need to switch to this IME after the default on-screen keyboard pops up.
 */
internal class TestIme : InputMethodService() {

    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.test_ime, null).apply {
            findViewById<Button>(R.id.button_close).setOnClickListener {
                requestHideSelf(InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }
}