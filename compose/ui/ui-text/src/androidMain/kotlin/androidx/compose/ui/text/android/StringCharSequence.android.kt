/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.text.android

import android.text.GetChars

/**
 * A [CharSequence] backed by a [String] that is not itself a [String].
 *
 * [android.text.Layout] draws each line with `Canvas.drawText(CharSequence, start, end, ...)`. When
 * the text is a [String] (or a `SpannedString` / `SpannableString`), the platform passes the whole
 * [String] to native code, which reads all of it with JNI `GetStringChars` (copying it, and
 * inflating it to UTF-16 when it is stored compressed) on every call, even though only the range
 * from start to end is drawn. Drawing a paragraph therefore costs O(line count * text length). For
 * other [CharSequence]s, the platform copies only the drawn range into a temporary buffer through
 * [android.text.TextUtils.getChars], which uses [GetChars] when available.
 *
 * Wrapping a [String] this way does not change the laid out or drawn result: the native String and
 * char array paths receive the same characters and the same context range, and the platform text
 * classes ([android.text.StaticLayout], `MeasuredParagraph`, `TextLine`) read the text through
 * [android.text.TextUtils] without special handling for [String].
 */
internal class StringCharSequence(private val string: String) : CharSequence, GetChars {
    override val length: Int
        get() = string.length

    override fun get(index: Int): Char = string[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        string.subSequence(startIndex, endIndex)

    override fun getChars(start: Int, end: Int, dest: CharArray, destoff: Int) {
        string.toCharArray(dest, destoff, start, end)
    }

    override fun toString(): String = string
}
