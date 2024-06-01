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

package androidx.compose.ui.platform

import androidx.compose.ui.text.AnnotatedString

/** Interface for managing the Clipboard. */
interface ClipboardManager {
    /**
     * This method put the text into the Clipboard.
     *
     * @param annotatedString The [AnnotatedString] to be put into Clipboard.
     */
    @Suppress("GetterSetterNames") fun setText(annotatedString: AnnotatedString)

    /**
     * This method get the text from the Clipboard.
     *
     * @return The text in the Clipboard. It could be null due to 2 reasons: 1. Clipboard is
     *   empty; 2. Cannot convert the [CharSequence] text in Clipboard to [AnnotatedString].
     */
    fun getText(): AnnotatedString?

    /** This method returns true if there is a text in the Clipboard, false otherwise. */
    fun hasText(): Boolean = getText()?.isNotEmpty() == true

    /**
     * Returns the clipboard entry that's provided by the platform's ClipboardManager.
     *
     * This item can include arbitrary content like images, videos, or any data that may be provided
     * through a mediator. Returned entry may contain multiple items with different types.
     *
     * It's safe to call this function without triggering Clipboard access warnings on mobile
     * platforms.
     */
    fun getClip(): ClipEntry? = null

    /**
     * Puts the given [clipEntry] in platform's ClipboardManager.
     *
     * @param clipEntry Platform specific clip object that either holds data or links to it. Pass
     *   null to clear the clipboard.
     */
    @Suppress("GetterSetterNames") fun setClip(clipEntry: ClipEntry?) = Unit

    /**
     * Returns the native clipboard that exposes the full functionality of platform clipboard.
     *
     * @throws UnsupportedOperationException If the current platform does not offer a native
     *   Clipboard interface.
     */
    val nativeClipboard: NativeClipboard
        get() {
            throw UnsupportedOperationException("This platform does not offer a native Clipboard")
        }
}

/** Platform specific protocol that expresses an item in the native Clipboard. */
expect class ClipEntry {

    /**
     * Returns a [ClipMetadata] which describes the contents of this [ClipEntry]. This is an ideal
     * way to check whether to accept or reject what may be pasted from the clipboard without
     * explicitly reading the content.
     *
     * Calling this function does not trigger any content access warnings on any platform.
     */
    val clipMetadata: ClipMetadata
}

/**
 * Platform specific protocol that describes an item in the native Clipboard. This object should not
 * contain any actual piece of data.
 */
expect class ClipMetadata

/** Native Clipboard specific to each platform. */
expect class NativeClipboard
