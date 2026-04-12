/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.pager

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/**
 * This is used to determine how Pages are laid out in [Pager]. By changing the size of the pages
 * one can change how many pages are shown.
 *
 * Please refer to the sample to learn how to use this API.
 *
 * @sample androidx.compose.foundation.samples.CustomPageSizeSample
 */
@Stable
interface PageSize {

    /**
     * Based on [availableSpace] pick a size for the pages
     *
     * @param availableSpace The amount of space in pixels the pages in this Pager can use.
     * @param pageSpacing The amount of space in pixels used to separate pages.
     */
    fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int

    /** Pages take up the whole Pager size. */
    object Fill : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return availableSpace
        }
    }

    /**
     * Multiple pages in a viewport
     *
     * @param pageSize A fixed size for pages
     */
    class Fixed(val pageSize: Dp) : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return pageSize.roundToPx()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Fixed) return false
            return pageSize == other.pageSize
        }

        override fun hashCode(): Int {
            return pageSize.hashCode()
        }
    }
}
