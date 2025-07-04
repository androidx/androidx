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

import androidx.compose.foundation.internal.hasText
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.Clipboard

internal actual class ClipboardPasteState actual constructor(private val clipboard: Clipboard) {
    private var _hasClip = false
    private var _hasText = false

    actual val hasText: Boolean get() = _hasText
    actual val hasClip: Boolean get() = _hasClip

    // TODO: replace the experimental API usage when a common ClipEntry API is ready - https://youtrack.jetbrains.com/issue/CMP-7624
    @OptIn(ExperimentalComposeUiApi::class)
    actual suspend fun update() {
        val entry = clipboard.getClipEntry()
        val itemsSize = entry?.clipboardItems?.length ?: 0
        _hasClip = itemsSize > 0
        _hasText = entry?.hasText() ?: false
    }
}