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

package androidx.compose.ui.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.AnnotatedString
import platform.AppKit.NSPasteboard
import platform.AppKit.NSPasteboardTypeString

actual typealias NativeClipboard = NSPasteboard

@Suppress("DEPRECATION")
private class NSPasteboardPlatformClipboardManager : ClipboardManager {
    override fun getText(): AnnotatedString? =
        getClipboardText()?.let { AnnotatedString(it) }

    override fun setText(annotatedString: AnnotatedString) {
        setClipboardText(annotatedString.text)
    }

    override fun hasText(): Boolean = !getClipboardText().isNullOrEmpty()

    override fun getClip(): ClipEntry? = null

    @Suppress("GetterSetterNames")
    override fun setClip(clipEntry: ClipEntry?) = Unit

    private fun setClipboardText(text: String) {
        NSPasteboard.generalPasteboard.clearContents()
        NSPasteboard.generalPasteboard.setString(string = text, forType = NSPasteboardTypeString)
    }

    private fun getClipboardText(): String? {
        return NSPasteboard.generalPasteboard.stringForType(dataType = NSPasteboardTypeString)
    }
}

internal class NSPasteboardPlatformClipboard : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? {
        if (nativeClipboard.pasteboardItems == null) return null
        if (nativeClipboard.pasteboardItems!!.isEmpty()) return null

        val str = nativeClipboard.stringForType(NSPasteboardTypeString)
        if (str.isNullOrEmpty()) return null

        return ClipEntry.withPlainText(str)
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (clipEntry?.plainText == null) {
            nativeClipboard.clearContents()
            return
        }
        val plainText = clipEntry.plainText ?: return
        nativeClipboard.setString(plainText, NSPasteboardTypeString)
    }

    override val nativeClipboard: NativeClipboard
        get() = NSPasteboard.generalPasteboard
}

@Suppress("DEPRECATION")
internal actual fun createPlatformClipboardManager(): ClipboardManager = NSPasteboardPlatformClipboardManager()

internal actual fun createPlatformClipboard(): Clipboard = NSPasteboardPlatformClipboard()

/**
 * A wrapper for [platform.AppKit.NSPasteboard] items.
 * Currently, it operates only with string(s) - [platform.AppKit.NSPasteboardTypeString].
 * To access or set other data items, consider using [Clipboard.nativeClipboard].
 */
actual class ClipEntry internal constructor() {

    // TODO: https://youtrack.jetbrains.com/issue/CMP-1260
    actual val clipMetadata: ClipMetadata
        get() = TODO("ClipMetadata is not implemented. Consider using nativeClipboard")

    internal var plainText: String? = null

    @ExperimentalComposeUiApi
    fun getPlainText(): String? = plainText

    companion object {
        @ExperimentalComposeUiApi
        fun withPlainText(text: String): ClipEntry = ClipEntry().apply {
            plainText = text
        }
    }
}
