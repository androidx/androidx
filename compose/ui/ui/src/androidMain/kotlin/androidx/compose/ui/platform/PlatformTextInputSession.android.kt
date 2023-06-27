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

package androidx.compose.ui.platform

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.text.input.PlatformTextInputMethodRequest

actual interface PlatformTextInputSession {
    /**
     * The [View] this input session is bound to. This view should be used to obtain and interact
     * with the [InputMethodManager].
     */
    val view: View

    actual suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing
}