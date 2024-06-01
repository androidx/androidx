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

package androidx.compose.foundation.text.input

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
fun interface KeyboardActionHandler {

    /**
     * This is run when an IME action is performed by the software keyboard, or enter key is pressed
     * on a hardware keyboard when the associated TextField is configured as single line. This
     * callback replaces the default behavior for certain actions such as closing the keyboard or
     * moving the focus to next field. If you also want to execute the default behavior, invoke
     * [performDefaultAction].
     *
     * If you do not this callback to trigger when enter key is pressed on a single line TextField,
     * refer to [Modifier.onPreviewKeyEvent] on how to intercept key events.
     */
    fun onKeyboardAction(performDefaultAction: () -> Unit)
}
