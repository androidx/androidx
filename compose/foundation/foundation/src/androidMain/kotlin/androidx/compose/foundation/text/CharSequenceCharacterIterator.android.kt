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

package androidx.compose.foundation.text

import java.text.CharacterIterator

/**
 * An implementation of [java.text.CharacterIterator] that iterates over a given [CharSequence].
 *
 * Note: This class is an exact copy of
 * `androidx.compose.ui.text.android.CharSequenceCharacterIterator` in `:compose:ui:ui-text` (which
 * itself is copied from
 * [CharSequenceCharacterIterator.java](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/text/CharSequenceCharacterIterator.java)).
 *
 * @param charSequence The given char sequence.
 * @param start The index of the beginning of the range.
 * @param end The index of the end of the range.
 * @constructor Constructs the iterator given a CharSequence and a range. The position of the
 *   iterator index is set to the beginning of the range.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal class CharSequenceCharacterIterator(
    private val charSequence: CharSequence,
    private val start: Int,
    private val end: Int,
) : Object() /* see b/204328129 */, CharacterIterator {
    private var index: Int = start

    override fun first(): Char {
        index = start
        return current()
    }

    override fun last(): Char {
        return if (start == end) {
            index = end
            CharacterIterator.DONE
        } else {
            index = end - 1
            charSequence[index]
        }
    }

    override fun current(): Char {
        return if (index == end) CharacterIterator.DONE else charSequence[index]
    }

    override fun next(): Char {
        index++
        return if (index >= end) {
            index = end
            CharacterIterator.DONE
        } else {
            charSequence[index]
        }
    }

    override fun previous(): Char {
        return if (index <= start) {
            CharacterIterator.DONE
        } else {
            index--
            charSequence[index]
        }
    }

    override fun setIndex(position: Int): Char {
        return if (position in start..end) {
            index = position
            current()
        } else {
            throw IllegalArgumentException("invalid position")
        }
    }

    override fun getBeginIndex(): Int = start

    override fun getEndIndex(): Int = end

    override fun getIndex(): Int = index

    override fun clone(): Any {
        return try {
            super.clone()
        } catch (e: CloneNotSupportedException) {
            throw InternalError()
        }
    }
}
