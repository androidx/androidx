/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.ui.geometry.Rect

/**
 * An interface that represents a platform-specific focus owner. This interface provides
 * focus-related methods to request, clear, and move focus within the owner's hierarchy.
 */
internal interface PlatformFocusOwner {
    /**
     * Ask the system to provide focus to this owner.
     *
     * @return true if the system granted focus to this owner. False otherwise.
     */
    fun requestOwnerFocus(focusDirection: FocusDirection?, previouslyFocusedRect: Rect?): Boolean

    /** Clear the focus from this owner. */
    fun clearOwnerFocus()

    /** Move focus in the specified direction. */
    fun moveFocusInChildren(focusDirection: FocusDirection): Boolean

    /**
     * When compose is not within the compose hierarchy, but within an embedded view, the bounds of
     * the interop focus target are not the correct bounds. This function fetches the bounds of the
     * view that has focus.
     */
    fun getEmbeddedViewFocusRect(): Rect?

    // TODO(b/438567275) Add a default implementation that could be used for non-android devices.
    /**
     * Let's the owner know that a new focus target is available. The owner can use this as a signal
     * to run initial focus.
     */
    fun focusTargetAvailable() {}
}
