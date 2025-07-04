/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.pdf.util

import android.content.Context
import android.util.Range
import androidx.annotation.StringRes

/**
 * Formats a page indicator label.
 *
 * @param context The application context.
 * @param range The range of visible pages. An empty range (upper < lower) means no pages are fully
 *   visible.
 * @param pageCount The total number of pages.
 * @param singlePageResId The string resource ID for single pages AND accessibility.
 * @param rangePageResId The string resource ID for multiple pages (visual only).
 * @return The formatted page indicator string.
 */
internal fun buildPageIndicatorLabel(
    context: Context,
    range: Range<Int>,
    pageCount: Int,
    @StringRes singlePageResId: Int,
    @StringRes rangePageResId: Int,
): String {
    return when {
        range.upper < range.lower || range.lower == range.upper -> {
            // Use the single page resource, for no fully visible pages OR one fully visible page.
            val pageNumber = if (range.upper < range.lower) 0 else range.lower + 1
            context.getString(singlePageResId, pageNumber, pageCount)
        }
        else -> {
            val resId = if (singlePageResId == rangePageResId) singlePageResId else rangePageResId
            // If singleResId and rangeResId are the same, we're in accessibility mode,
            // and we should format as a single page.  Otherwise, we use the range format.
            if (resId == singlePageResId) {
                context.getString(resId, range.lower + 1, pageCount)
            } else {
                context.getString(resId, range.lower + 1, range.upper + 1, pageCount)
            }
        }
    }
}
