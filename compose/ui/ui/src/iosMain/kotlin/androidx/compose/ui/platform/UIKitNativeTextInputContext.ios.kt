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

package androidx.compose.ui.platform

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Color

/**
 * Interface for providing the required information for the iOS to use native text editing
 * experience.
 *
 * The main difference between this approach and the default compose one is that iOS handles
 * the caret, selection handles, selection area, related gestures and context menu appearance
 * and behavior itself.
 */
@InternalComposeUiApi
interface UIKitNativeTextInputContext {
    fun usingNativeTextInput(): Boolean

    fun updateNativeTextInputEditMenuState(
        copy: (() -> Unit)?,
        paste: (() -> Unit)?,
        cut: (() -> Unit)?,
        selectAll: (() -> Unit)?,
        customActions: List<UIKitNativeTextInputContextMenuCustomAction>?
    )

    fun updateNativeTextInputTintColor(color: Color?)
}

@InternalComposeUiApi
class UIKitNativeTextInputContextMenuCustomAction(
    val title: String,
    val action: () -> Unit
)
