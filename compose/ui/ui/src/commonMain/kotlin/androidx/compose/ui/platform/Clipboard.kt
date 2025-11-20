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

interface Clipboard {

    /**
     * Returns the clipboard entry that's provided by the platform's ClipboardManager.
     *
     * This item can include arbitrary content like text, images, videos, or any data that may be
     * provided through a mediator. Returned entry may contain multiple items with different types.
     *
     * It returns null when the clipboard is empty.
     *
     * Calling this function on Android will access the Clipboard's contents, and the first time it
     * happens this will trigger a warning that says "App pasted from Clipboard". Use
     * [nativeClipboard] and `primaryClipDescription` on Android to circumvent this issue if you are
     * only interested in querying what is available in the clipboard.
     */
    suspend fun getClipEntry(): ClipEntry?

    /**
     * Puts the given [clipEntry] in platform's ClipboardManager.
     *
     * @param clipEntry Platform specific clip object that either holds data or links to it. Pass
     *   null to clear the clipboard.
     */
    suspend fun setClipEntry(clipEntry: ClipEntry?)

    /** Returns the native clipboard that exposes the full functionality of platform clipboard. */
    val nativeClipboard: NativeClipboard
}
