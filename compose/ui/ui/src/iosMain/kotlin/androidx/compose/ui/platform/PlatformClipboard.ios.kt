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
import platform.UIKit.UIPasteboard

actual typealias NativeClipboard = UIPasteboard

internal class UiKitPlatformClipboard internal constructor() : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? {
        if (nativeClipboard.numberOfItems() == 0L) return null
        return ClipEntry().apply {
            getPlainTextLambda = {
                nativeClipboard.string
            }
            hasPlainText = nativeClipboard.hasStrings
        }
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (clipEntry == null) {
            nativeClipboard.items = emptyList<Map<String, Any>>()
        } else {
            nativeClipboard.string = clipEntry.getPlainText()
        }
    }

    /**
     * Provides the [platform.UIKit.UIPasteboard] instance.
     */
    override val nativeClipboard: NativeClipboard
        get() = UIPasteboard.generalPasteboard
}

internal actual fun createPlatformClipboard(): Clipboard {
    return UiKitPlatformClipboard()
}

/**
 * A wrapper for [UIPasteboard] items.
 * Currently, it operates only with string(s) - [UIPasteboard.string].
 * To access or set other data items, consider using [Clipboard.nativeClipboard].
 */
actual class ClipEntry internal constructor() {

    // TODO https://youtrack.jetbrains.com/issue/CMP-1260/ClipboardManager.-Implement-getClip-getClipMetadata-setClip
    actual val clipMetadata: ClipMetadata
        get() = TODO("ClipMetadata is not implemented. Consider using nativeClipboard")

    internal var getPlainTextLambda: () -> String? = { null }
    internal var hasPlainText: Boolean = false

    @ExperimentalComposeUiApi
    fun getPlainText(): String? = getPlainTextLambda.invoke()

    @ExperimentalComposeUiApi
    fun hasPlainText(): Boolean {
        return hasPlainText
    }

    companion object {
        @ExperimentalComposeUiApi
        fun withPlainText(text: String): ClipEntry = ClipEntry().apply {
            getPlainTextLambda = { text }
            hasPlainText = true
        }
    }
}