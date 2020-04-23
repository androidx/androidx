/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.texttoolbar

import androidx.ui.geometry.Rect
import androidx.ui.text.AnnotatedString

/**
 * Interface for text-related toolbar.
 */
interface TextToolbar {
    /**
     * Show the floating toolbar(post-M) or primary toolbar(pre-M) for copying text.
     *
     * @param rect region of interest. The selected region around which the floating toolbar
     * should show.
     * @param text selected text. For copying to the clipboard manager.
     */
    fun showCopyMenu(rect: Rect, text: AnnotatedString)
}