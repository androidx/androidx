/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import org.w3c.dom.events.MouseEvent

internal val MouseEvent.composeButton get(): PointerButton? {
    // `MouseEvent.button` property only guarantees to indicate which buttons are pressed during
    // events caused by pressing or releasing one or multiple buttons
    when (type) {
        "mousedown", "mouseup" -> Unit
        else -> return null
    }
    // https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/button
    return when (val buttonIndex = button.toInt()) {
        // Main button pressed, usually the left button or the un-initialized state
        0 -> PointerButton.Primary
        // Auxiliary button pressed, usually the wheel button or the middle button (if present)
        1 -> PointerButton.Tertiary
        // Secondary button pressed, usually the right button
        2 -> PointerButton.Secondary
        // Fourth button, typically the Browser Back button
        3 -> PointerButton.Back
        // Fifth button, typically the Browser Forward button
        4 -> PointerButton.Forward
        else -> PointerButton(buttonIndex)
    }
}

internal val MouseEvent.composeButtons get() =
    // https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/buttons
    // The bit mask matches as-is with Compose's [ButtonMasks]
    PointerButtons(buttons.toInt())
