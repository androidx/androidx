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

package androidx.pdf.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.pdf.R

internal object ClipboardUtils {
    /**
     * Copies the given [text] to the Android clipboard.
     *
     * @param context The [Context] used to access system services and resources.
     * @param text The string to be copied to the clipboard.
     */
    internal fun copyToClipboard(context: Context, text: String) {
        val manager = context.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText(context.getString(R.string.clipboard_label), text)
        manager.setPrimaryClip(clip)
    }
}
