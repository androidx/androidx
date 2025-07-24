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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.ui.platform.Clipboard

// the paste state is needed to show or hide the "paste" context menu item.
// in browsers we don't want to bother users by a permission request,
// so we return unconditionally true
internal actual class ClipboardPasteState actual constructor(private val clipboard: Clipboard) {
    actual val hasText = true
    actual val hasClip = true
    actual suspend fun update() {}
}