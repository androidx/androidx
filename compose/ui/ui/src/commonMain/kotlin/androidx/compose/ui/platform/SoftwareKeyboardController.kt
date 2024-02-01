/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextInputService

/**
 * Provide software keyboard control.
 */
@Stable
interface SoftwareKeyboardController {
    /**
     * Request that the system show a software keyboard.
     *
     * This request is best effort. If the system can currently show a software keyboard, it
     * will be shown. However, there is no guarantee that the system will be able to show a
     * software keyboard. If the system cannot show a software keyboard currently,
     * this call will be silently ignored.
     *
     * The software keyboard will never show if there is no composable that will accept text input,
     * such as a [TextField][androidx.compose.foundation.text.BasicTextField] when it is focused.
     * You may find it useful to ensure focus when calling this function.
     *
     * You do not need to call this function unless you also call [hide], as the
     * keyboard is automatically shown and hidden by focus events in the BasicTextField.
     *
     * Calling this function is considered a side-effect and should not be called directly from
     * recomposition.
     *
     * @sample androidx.compose.ui.samples.SoftwareKeyboardControllerSample
     */
    fun show()

    /**
     * Hide the software keyboard.
     *
     * This request is best effort, if the system cannot hide the software keyboard this call
     * will silently be ignored.
     *
     * Calling this function is considered a side-effect and should not be called directly from
     * recomposition.
     *
     * @sample androidx.compose.ui.samples.SoftwareKeyboardControllerSample
     */
    fun hide()
}

internal class DelegatingSoftwareKeyboardController(
    val textInputService: TextInputService
) : SoftwareKeyboardController {
    override fun show() {
        @Suppress("DEPRECATION")
        textInputService.showSoftwareKeyboard()
    }

    override fun hide() {
        @Suppress("DEPRECATION")
        textInputService.hideSoftwareKeyboard()
    }
}
