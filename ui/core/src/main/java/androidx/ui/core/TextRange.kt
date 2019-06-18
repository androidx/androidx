/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

fun CharSequence.substring(range: TextRange): String = this.substring(range.start, range.end)

/**
 * An immutable text range class
 * @param start the inclusive starting offset of the range.
 * @param end the exclusive end offset of the range
 */
data class TextRange(val start: Int, val end: Int) {

    init {
        if (start > end) {
            // TODO(nona): Handle reversed range?
            throw IllegalArgumentException("Reversed range is not supported (yet)")
        }
    }

    /**
     * Returns true if the range is collapsed
     */
    val collapsed: Boolean get() = start == end

    /**
     * Returns the length of the range.
     */
    val length: Int get() = end - start

    /**
     * Returns true if the given range has intersection with this range
     */
    fun intersects(other: TextRange): Boolean = start < other.end && other.start < end

    /**
     * Returns true if this range covers including equals with the given range.
     */
    fun contains(other: TextRange): Boolean = start <= other.start && other.end <= end

    /**
     * Returns true if the given offset is a part of this range.
     */
    fun contains(offset: Int): Boolean = start <= offset && offset < end
}