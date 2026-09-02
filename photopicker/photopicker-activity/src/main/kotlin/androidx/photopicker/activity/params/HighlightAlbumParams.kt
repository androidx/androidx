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
import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import androidx.photopicker.activity.PickVisualMedia
import androidx.photopicker.activity.params.HighlightType.Companion.HighlightType

/**
 * Parameters for launching the photopicker to highlight a specific album's media.
 *
 * @property highlightType The requested [HighlightType] specifying how the highlighted album is
 *   displayed.
 * @property highlightAlbum The requested predefined album to highlight.
 */
public class HighlightAlbumParams
internal constructor(
    @param:HighlightType @get:HighlightType public val highlightType: Int,
    @param:HighlightAlbum @get:HighlightAlbum public val highlightAlbum: String,
) {
    /** Predefined albums supported for highlighting media in the photopicker. */
    public companion object {
        @Retention(AnnotationRetention.SOURCE)
        @StringDef(
            ALBUM_FAVORITES,
            ALBUM_CAMERA,
            ALBUM_DOWNLOADS,
            ALBUM_SCREENSHOTS,
            ALBUM_VIDEOS,
        )
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Target(
            AnnotationTarget.TYPE,
            AnnotationTarget.PROPERTY,
            AnnotationTarget.VALUE_PARAMETER,
            AnnotationTarget.PROPERTY_GETTER,
        )
        public annotation class HighlightAlbum

        /**
         * Highlights media results from the Favorites album.
         *
         * @see android.provider.MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_FAVORITES
         */
        public const val ALBUM_FAVORITES: String =
            PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_FAVORITES

        /**
         * Highlights media results from the Camera album.
         *
         * @see android.provider.MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_CAMERA
         */
        public const val ALBUM_CAMERA: String =
            PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_CAMERA

        /**
         * Highlights media results from the Downloads album.
         *
         * @see android.provider.MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_DOWNLOADS
         */
        public const val ALBUM_DOWNLOADS: String =
            PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_DOWNLOADS

        /**
         * Highlights media results from the Screenshots album.
         *
         * @see android.provider.MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_SCREENSHOTS
         */
        public const val ALBUM_SCREENSHOTS: String =
            PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_SCREENSHOTS

        /**
         * Highlights media results from the Videos album.
         *
         * @see android.provider.MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_VIDEOS
         */
        public const val ALBUM_VIDEOS: String =
            PickVisualMedia.SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_VIDEOS
    }

    /**
     * Creates a [Bundle] containing parameters for highlighting a specific album using platform
     * [MediaStore] extra keys.
     *
     * @return A configured [Bundle] with highlight type and album ID extras.
     */
    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 19)
    internal fun createHighlightAlbumBundle(): Bundle =
        Bundle().apply {
            val platformAlbumId =
                when (highlightAlbum) {
                    ALBUM_FAVORITES -> MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_FAVORITES
                    ALBUM_CAMERA -> MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_CAMERA
                    ALBUM_DOWNLOADS -> MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_DOWNLOADS
                    ALBUM_SCREENSHOTS -> MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_SCREENSHOTS
                    ALBUM_VIDEOS -> MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_VIDEOS
                    else -> highlightAlbum
                }
            putInt(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_TYPE, highlightType)
            putString(MediaStore.KEY_PICK_IMAGES_HIGHLIGHT_ALBUM_ID, platformAlbumId)
        }

    /**
     * Creates a [Bundle] containing parameters for highlighting a specific album using system
     * fallback extra keys.
     *
     * @return A configured [Bundle] with system fallback highlight type and album ID extras.
     */
    internal fun createHighlightAlbumFallbackBundle(): Bundle =
        Bundle().apply {
            putInt(
                PickVisualMedia.KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE,
                highlightType,
            )
            putString(
                PickVisualMedia.KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_ID,
                highlightAlbum,
            )
        }

    /** A builder for constructing [HighlightAlbumParams] instances. */
    public class Builder {
        private var highlightType: Int? = null
        private var highlightAlbum: String? = null

        /**
         * Sets the requested highlight type specifying how the highlighted album will be displayed.
         *
         * @param highlightType A supported highlight type from [HighlightType].
         * @return This Builder.
         */
        public fun setHighlightType(@HighlightType highlightType: Int): Builder {
            this.highlightType = highlightType
            return this
        }

        /**
         * Sets the album to highlight. The value should be one of the predefined album constants:
         * [ALBUM_FAVORITES], [ALBUM_CAMERA], [ALBUM_DOWNLOADS], [ALBUM_SCREENSHOTS], or
         * [ALBUM_VIDEOS].
         *
         * @param highlightAlbum A supported predefined album string constant.
         * @return This Builder.
         */
        public fun setHighlightAlbum(@HighlightAlbum highlightAlbum: String): Builder {
            this.highlightAlbum = highlightAlbum
            return this
        }

        /**
         * Builds the [HighlightAlbumParams] object specified by this builder.
         *
         * @return The newly constructed [HighlightAlbumParams] instance.
         * @throws IllegalArgumentException if [highlightType] or [highlightAlbum] is not set.
         */
        public fun build(): HighlightAlbumParams {
            require(highlightType != null) { "HighlightType must be set" }
            require(highlightAlbum != null) { "HighlightAlbum must be set" }
            return HighlightAlbumParams(
                highlightType = highlightType!!,
                highlightAlbum = highlightAlbum!!,
            )
        }
    }
}
