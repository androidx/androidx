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
     * how closs access was.
     */
    val indexInPage: Int
)