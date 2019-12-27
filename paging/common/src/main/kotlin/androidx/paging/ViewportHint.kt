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

package androidx.paging

/**
 * Load access pair - page, and index inside (or adjacent)
 */
internal data class ViewportHint(
    /**
     * Index of the accessed page relative to initial load = 0
     */
    val sourcePageIndex: Int,

    /**
     * Index either inside, or (in the case of placeholders) outside of page bounds, reflecting
     * how close access was.
     *
     * Note: It is valid for this field to be a negative number, indicating access of an element
     * before the page referenced by [sourcePageIndex].
     */
    val indexInPage: Int
) : Comparable<ViewportHint> {
    override operator fun compareTo(other: ViewportHint): Int {
        if (sourcePageIndex != other.sourcePageIndex) return sourcePageIndex - other.sourcePageIndex
        return indexInPage - other.indexInPage
    }

    companion object {
        val MIN_VALUE = ViewportHint(Int.MIN_VALUE, 0)
        val MAX_VALUE = ViewportHint(Int.MAX_VALUE, Int.MAX_VALUE)
        val DUMMY_VALUE = MAX_VALUE
    }
}
