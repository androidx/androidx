/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Context

/**
 * Returns an [android.content.ClipboardManager] that exposes the full functionality of platform
 * clipboard.
 */
val Clipboard.nativeClipboardManager: android.content.ClipboardManager
    @Suppress("DEPRECATION") get() = nativeClipboard

internal class AndroidClipboard
internal constructor(private val androidClipboardManager: AndroidClipboardManager) : Clipboard {

    internal constructor(context: Context) : this(AndroidClipboardManager(context))

    override suspend fun getClipEntry(): ClipEntry? {
        return androidClipboardManager.getClip()
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        androidClipboardManager.setClip(clipEntry)
    }

    // The new extension field [nativeClipboardManager] still delegates to this property.
    // Therefore, this deprecated field shall be used in tests to mock the backing
    // native ClipboardManager.
    @Deprecated(
        message = "Use [nativeClipboardManager] extension instead",
        replaceWith = ReplaceWith("nativeClipboardManager"),
    )
    override val nativeClipboard: android.content.ClipboardManager
        get() = androidClipboardManager.nativeClipboard
}
