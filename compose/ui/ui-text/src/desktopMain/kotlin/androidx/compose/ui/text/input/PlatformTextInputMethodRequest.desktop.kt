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

package androidx.compose.ui.text.input

import java.awt.Component
import java.awt.event.InputMethodListener
import java.awt.im.InputMethodRequests

/**
 * Represents a request to open a desktop text input session via
 * `PlatformTextInputModifierNode.textInputSession`.
 */
actual interface PlatformTextInputMethodRequest {
    /**
     * The [InputMethodListener] that will be registered via [Component.addInputMethodListener]
     * while the session is active.
     */
    val inputMethodListener: InputMethodListener

    /**
     * The [InputMethodRequests] that will be returned from the Compose host's
     * [Component.getInputMethodRequests] while the session is active.
     */
    val inputMethodRequests: InputMethodRequests
}
