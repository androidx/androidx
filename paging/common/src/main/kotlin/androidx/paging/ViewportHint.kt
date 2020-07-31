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
 * Load access information blob, containing information from presenter.
 */
internal data class ViewportHint(
    /** Page index offset from initial load */
    val pageOffset: Int,
    /**
     * Distance from hint to first loaded item: `anchorPosition - firstLoadedItemPosition`
     *
     * Zero indicates access at boundary
     * Positive -> Within loaded range or in placeholders if greater than size of last page.
     * Negative -> placeholder access.
     */
    val indexInPage: Int,
    /**
     * Distance from hint to first loaded item: `anchorPosition - firstLoadedItemPosition`
     *
     * Zero indicates access at boundary
     * Positive -> Within loaded range or in placeholders if greater than size of last page.
     * Negative -> placeholder access.
     */
    val presentedItemsBefore: Int,
    /**
     * Distance from hint to first presented item: `anchorPosition - firstLoadedItemPosition`
     *
     * Zero indicates access at boundary
     * Positive -> Within loaded range or in placeholders if greater than size of last page.
     * Negative -> placeholder access.
     */
    val presentedItemsAfter: Int,
    val originalPageOffsetFirst: Int,
    val originalPageOffsetLast: Int
)
