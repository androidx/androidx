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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.foundation.internal

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString


internal actual suspend fun ClipEntry.readText(): String? = getPlainText()

internal actual suspend fun ClipEntry.readAnnotatedString(): AnnotatedString? {
    val text = getPlainText() ?: return null
    return AnnotatedString(text)
}

internal actual fun AnnotatedString?.toClipEntry(): ClipEntry? {
    if (this == null) return null
    return ClipEntry.withPlainText(this.text)
}

internal actual fun ClipEntry?.hasText(): Boolean = this?.hasPlainText() ?: false

internal actual fun Clipboard.isReadSupported(): Boolean = true
internal actual fun Clipboard.isWriteSupported(): Boolean = true
