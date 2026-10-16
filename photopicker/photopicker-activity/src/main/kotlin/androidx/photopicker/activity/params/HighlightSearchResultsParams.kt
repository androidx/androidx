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

package androidx.photopicker.activity.params

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresExtension
import androidx.photopicker.activity.PickVisualMedia
import androidx.photopicker.activity.params.HighlightType.Companion.HighlightType

/**
 * Parameters for launching the photopicker with highlighted search results based on a text query.
 *
 * @property highlightType The requested [HighlightType] specifying how the highlighted search
 *   results are displayed.
 * @property highlightSearchTextQuery The search query string used to highlight media results.
 */
public class HighlightSearchResultsParams
internal constructor(
    @param:HighlightType @get:HighlightType public val highlightType: Int,
    public val highlightSearchTextQuery: String,
) {
    /**
     * Creates a [Bundle] containing parameters for highlighting search results using platform
     * [MediaStore] extra keys.
     *
     * @return A configured [Bundle] with highlight type and search query extras.
     */
    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 19)
    internal fun createHighlightSearchResultsBundle(): Bundle =
        Bundle().apply {
            putInt(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_TYPE, highlightType)
            putString(
                MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_SEARCH_TEXT_QUERY,
                highlightSearchTextQuery,
            )
        }

    /**
     * Creates a [Bundle] containing parameters for highlighting search results using system
     * fallback extra keys.
     *
     * @return A configured [Bundle] with system fallback highlight type and search query extras.
     */
    internal fun createHighlightSearchResultsFallbackBundle(): Bundle =
        Bundle().apply {
            putInt(
                PickVisualMedia.KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE,
                highlightType,
            )
            putString(
                PickVisualMedia.KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_SEARCH_TEXT_QUERY,
                highlightSearchTextQuery,
            )
        }

    /** A builder for constructing [HighlightSearchResultsParams] instances. */
    public class Builder {
        private var highlightType: Int? = null
        private var highlightSearchTextQuery: String? = null

        /**
         * Sets the requested highlight type specifying how the highlighted search results will be
         * displayed.
         *
         * @param highlightType A supported highlight type from [HighlightType].
         * @return This Builder.
         */
        public fun setHighlightType(@HighlightType highlightType: Int): Builder {
            this.highlightType = highlightType
            return this
        }

        /**
         * Sets the text query string used for highlighting media search results. It should not be
         * empty or null.
         *
         * @param highlightSearchTextQuery Input string query for highlighting media results.
         * @return This Builder.
         */
        public fun setHighlightSearchTextQuery(highlightSearchTextQuery: String): Builder {
            this.highlightSearchTextQuery = highlightSearchTextQuery
            return this
        }

        /**
         * Builds the [HighlightSearchResultsParams] object specified by this builder.
         *
         * @return The newly constructed [HighlightSearchResultsParams] instance.
         * @throws IllegalArgumentException if [highlightType] is not set, or if
         *   [highlightSearchTextQuery] is not set or blank.
         */
        public fun build(): HighlightSearchResultsParams {
            require(highlightType != null) { "HighlightType must be set" }
            require(highlightSearchTextQuery != null) { "Search text query must be set" }
            require(highlightSearchTextQuery!!.isNotBlank()) {
                "Search text query must not be blank"
            }
            return HighlightSearchResultsParams(
                highlightType = highlightType!!,
                highlightSearchTextQuery = highlightSearchTextQuery!!,
            )
        }
    }
}
