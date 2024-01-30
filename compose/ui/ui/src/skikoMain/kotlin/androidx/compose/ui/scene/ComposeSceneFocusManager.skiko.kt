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

package androidx.compose.ui.scene

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Rect

/**
 * The extension of [FocusManager] to manage focus within a [ComposeScene].
 */
@InternalComposeUiApi
interface ComposeSceneFocusManager : FocusManager {
    /**
     * Call this function to propagate focus to one of the focus modifiers
     * in the component hierarchy.
     */
    fun requestFocus()

    /**
     * Call this function to clear focus from the currently focused component, and set the focus to
     * the root focus modifier.
     */
    fun releaseFocus()

    /**
     * Searches for the currently focused item, and returns its coordinates as a rect.
     */
    fun getFocusRect(): Rect?
}
