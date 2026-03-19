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

package androidx.compose.ui.events

import org.w3c.dom.events.UIEvent

private external interface InputEventInit {
    val data: String
    val inputType: String
}

private  fun InputEventInit(inputType: String, data: String?, isComposing: Boolean): InputEventInit = js("({data: data, inputType: inputType, isComposing: isComposing})")

private  external class InputEvent(type: String, options: InputEventInit) : UIEvent

internal fun beforeInput(inputType: String, data: String?, isComposing: Boolean = false): UIEvent =
    InputEvent("beforeinput", InputEventInit(inputType = inputType, data = data, isComposing = isComposing))
