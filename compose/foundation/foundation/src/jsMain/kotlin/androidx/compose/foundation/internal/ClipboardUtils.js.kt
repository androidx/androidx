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
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import kotlin.js.Promise
import kotlinx.coroutines.await
import org.w3c.files.Blob

private const val MIME_TYPE_PLAIN_TEXT = "text/plain"

@OptIn(InternalComposeUiApi::class)
internal actual suspend fun ClipEntry.readText(): String? {
    if (!this.hasText()) return null
    if (this.fallbackPlainText != null) return this.fallbackPlainText

    if (this.clipboardItems.isEmpty()) return null
    val blob = clipboardItems[0].getType(MIME_TYPE_PLAIN_TEXT).await<Blob>()
    return getTextFromBlob(blob).await<String>().toString()
}

internal actual suspend fun ClipEntry.readAnnotatedString(): AnnotatedString? {
    val text = readText() ?: return null
    return AnnotatedString(text)
}

internal actual fun AnnotatedString?.toClipEntry(): ClipEntry? {
    if (this == null) return null
    return ClipEntry.withPlainText(this.text)
}

@OptIn(InternalComposeUiApi::class)
internal actual fun ClipEntry?.hasText(): Boolean {
    if (this == null) return false
    if (this.fallbackPlainText != null) return true
    if (this.clipboardItems.isEmpty()) return false
    return doesJsArrayContainValue(this.clipboardItems[0].types, MIME_TYPE_PLAIN_TEXT)
}

internal actual fun Clipboard.isReadSupported(): Boolean =
    js("Boolean(window.navigator.clipboard && window.navigator.clipboard.read)")

internal actual fun Clipboard.isWriteSupported(): Boolean =
    js("Boolean(window.navigator.clipboard && (window.navigator.clipboard.write || window.navigator.clipboard.writeText))")

@Suppress("UNUSED_PARAMETER")
private fun doesJsArrayContainValue(jsArray: Array<*>, value: Any): Boolean =
    js("jsArray.includes(value)")

@Suppress("UNUSED_PARAMETER")
private fun getTextFromBlob(blob: Blob): Promise<String> =
    js("blob.text()")