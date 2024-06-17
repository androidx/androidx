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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.geometry.Rect

/**
 * The extension of [FocusManager] to manage focus within a [ComposeScene].
 */
@InternalComposeUiApi
class ComposeSceneFocusManager internal constructor(
    private val focusOwner: () -> FocusOwner
) {
    /**
     * `true` if any child has focus
     */
    val hasFocus: Boolean get() = focusOwner().rootState.hasFocus

    /**
     * Searches for the currently focused item, and returns its coordinates as a rect.
     */
    fun getFocusRect(): Rect? = focusOwner().getFocusRect()

    /**
     * Take focus to ComposeScene in specified [focusDirection].
     *
     * Returns false if there are no focusable elements in this direction:
     * - the scene is empty
     * - we are in the end of the scene and move forward
     * - we are in the beginning of the scene and move backward
     */
    fun takeFocus(focusDirection: FocusDirection): Boolean {
        return focusOwner().takeFocus(focusDirection, previouslyFocusedRect = null)
    }

    /**
     * Release focus from ComposeScene
     */
    fun releaseFocus() {
        focusOwner().releaseFocus()
    }
}
