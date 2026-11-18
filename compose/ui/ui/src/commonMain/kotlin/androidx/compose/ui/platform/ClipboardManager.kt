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
@Deprecated(
    "Use Clipboard instead, which supports suspend functions.",
    ReplaceWith("Clipboard", "androidx.compose.ui.platform.Clipboard"),
)
public interface ClipboardManager {
    /**
     * This method put the text into the Clipboard.
     *
     * @param annotatedString The [AnnotatedString] to be put into Clipboard.
     */
    @Suppress("GetterSetterNames") public fun setText(annotatedString: AnnotatedString)

    /**
     * This method get the text from the Clipboard.
     *
     * @return The text in the Clipboard. It could be null due to 2 reasons: 1. Clipboard is
     *   empty; 2. Cannot convert the [CharSequence] text in Clipboard to [AnnotatedString].
     */
    public fun getText(): AnnotatedString?

    /** This method returns true if there is a text in the Clipboard, false otherwise. */
    public fun hasText(): Boolean = getText()?.isNotEmpty() == true

    /**
     * Returns the clipboard entry that's provided by the platform's ClipboardManager.
     *
     * This item can include arbitrary content like images, videos, or any data that may be provided
     * through a mediator. Returned entry may contain multiple items with different types.
     *
     * It's safe to call this function without triggering Clipboard access warnings on mobile
     * platforms.
     */
    public fun getClip(): ClipEntry? = null

    /**
     * Puts the given [clipEntry] in platform's ClipboardManager.
     *
     * @param clipEntry Platform specific clip object that either holds data or links to it. Pass
     *   null to clear the clipboard.
     */
    @Suppress("GetterSetterNames") public fun setClip(clipEntry: ClipEntry?): Unit = Unit

    /**
     * Returns the native clipboard that exposes the full functionality of platform clipboard.
     *
     * @throws UnsupportedOperationException If the current platform does not offer a native
     *   Clipboard interface.
     */
    @Suppress("DEPRECATION")
    public val nativeClipboard: NativeClipboard
        get() {
            throw UnsupportedOperationException("This platform does not offer a native Clipboard")
        }
}

/** Platform specific protocol that expresses an item in the native Clipboard. */
public expect class ClipEntry {

    /**
     * Returns a [ClipMetadata] which describes the contents of this [ClipEntry]. This is an ideal
     * way to check whether to accept or reject what may be pasted from the clipboard without
     * explicitly reading the content.
     *
     * Calling this function does not trigger any content access warnings on any platform.
     */
    public val clipMetadata: ClipMetadata

    /**
     * Reads the first available text representation of this clip.
     *
     * Use readText() when you want the best-effort text value from a clip: it prefers plain text,
     * then falls back to HTML or a URL. Use [readPlainText], [readHtml], or [readUrl] to request a
     * specific representation.
     *
     * @return plain text, HTML markup, or URL text, or null if none can be read
     * @sample androidx.compose.ui.samples.ReadTextFromClipboardSample
     */
    public suspend fun readText(): String?

    /**
     * Reads the plain-text representation.
     *
     * Unlike [readText], returns null when plain text is unavailable instead of trying other
     * representations.
     *
     * Use readPlainText() when the caller accepts only plain text and should get null rather than
     * HTML or a URL if plain text is unavailable.
     *
     * @return plain text, or null if unavailable or reading fails
     * @sample androidx.compose.ui.samples.ReadDistinctRepresentationsFromClipboardSample
     */
    public suspend fun readPlainText(): String?

    /**
     * Reads the HTML representation. If there is no HTML representation available, this method
     * doesn't try to read other text values.
     *
     * @return HTML markup, or null if unavailable or reading fails
     * @sample androidx.compose.ui.samples.ReadDistinctRepresentationsFromClipboardSample
     */
    public suspend fun readHtml(): String?

    /**
     * Reads the URL representation. If there is no URL representation available, this method
     * doesn't try to read other text values.
     *
     * @return URL string, or null if unavailable or reading fails
     * @sample androidx.compose.ui.samples.ReadDistinctRepresentationsFromClipboardSample
     */
    public suspend fun readUrl(): String?

    public companion object {
        /**
         * Creates an entry with [plainText] and optional [html] representations.
         *
         * @param plainText plain-text representation of the item
         * @param html optional HTML representation of the same text content
         * @sample androidx.compose.ui.samples.WritePlainTextToClipboard
         * @sample androidx.compose.ui.samples.WriteHtmlToClipboard
         */
        public fun withText(plainText: String, html: String? = null): ClipEntry

        /**
         * Creates an entry with URL, plain-text and optional HTML representation.
         *
         * @param url valid URL string to write; this function does not validate [url]
         * @param plainText plain-text fallback, such as a label; defaults to [url]
         * @param html optional HTML link for the same URL
         * @sample androidx.compose.ui.samples.WriteUrlToClipboard
         */
        public fun withUrl(url: String, plainText: String = url, html: String? = null): ClipEntry
    }
}

/**
 * Describes the representations a [ClipEntry] advertises without reading its content.
 *
 * This is useful for checking whether the clipboard contains compatible data (for example, to
 * determine whether to show or enable a "Paste" button) without triggering platform clipboard
 * access warning.
 */
public expect class ClipMetadata {
    /**
     * Reports whether the clip advertises a text representation. Use [ClipEntry.readText] to read
     * an available text representation.
     */
    public fun hasText(): Boolean

    /** Reports whether the clip advertises plain text. Use [ClipEntry.readPlainText] to read it. */
    public fun hasPlainText(): Boolean

    /**
     * Reports whether the clip advertises an HTML representation. Use [ClipEntry.readHtml] to read
     * it.
     */
    public fun hasHtml(): Boolean

    /**
     * Reports whether the clip advertises a URL representation. Use [ClipEntry.readUrl] to read it.
     */
    public fun hasUrl(): Boolean
}

/** Native Clipboard specific to each platform. */
@Deprecated("Use direct reference to platform type instead of typealias")
public expect class NativeClipboard
