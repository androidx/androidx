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
import android.widget.photopicker.PhotoPickerSelectionParams
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import java.time.Duration

/**
 * Represents selection constraints for the photo picker.
 *
 * This class allows specifying constraints that the selected media items must satisfy. By providing
 * this information to [androidx.photopicker.activity.PickVisualMediaRequest], the photo picker will
 * disable selection of items in the picker grid that violate these constraints.
 *
 * Callers should use [Builder] to construct an instance of this class.
 *
 * @property maxMediaItemSizeInBytes The maximum allowed size, in bytes, for an individual media
 *   item, or `-1` if unrestricted (default).
 * @property maxVideoDuration The maximum allowed duration for a video media item, or `null` if
 *   unrestricted (default).
 * @property minVideoDuration The minimum required duration for a video media item, or `null` if
 *   unrestricted (default).
 * @property maxMediaItemResolutionInPixels The maximum allowed resolution in total pixels, or `-1`
 *   if unrestricted (default).
 * @property minMediaItemResolutionInPixels The minimum required resolution in total pixels, or `-1`
 *   if unrestricted (default).
 * @property mimeTypes The list of allowed mime types that items must match to be selectable. If
 *   empty, all mime types are allowed (default).
 * @property maxSelectionBatchSizeInBytes The maximum allowed cumulative size, in bytes, for the
 *   entire batch of selected media items, or `-1` if unrestricted (default).
 * @see android.provider.MediaStore.EXTRA_PICK_IMAGES_SELECTION_PARAMS
 * @see android.widget.photopicker.PhotoPickerSelectionParams
 */
@RequiresApi(Build.VERSION_CODES.O)
public class SelectionParams
internal constructor(
    public val maxMediaItemSizeInBytes: Long = -1L,
    public val maxVideoDuration: Duration? = null,
    public val minVideoDuration: Duration? = null,
    public val maxMediaItemResolutionInPixels: Long = -1L,
    public val minMediaItemResolutionInPixels: Long = -1L,
    public val mimeTypes: List<String> = emptyList(),
    public val maxSelectionBatchSizeInBytes: Long = -1L,
) {
    /**
     * Converts this [SelectionParams] instance into a platform [PhotoPickerSelectionParams]
     * parcelable object.
     *
     * @return The platform [PhotoPickerSelectionParams] instance.
     */
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 22)
    internal fun toPhotoPickerSelectionParams(): PhotoPickerSelectionParams {
        val builder = PhotoPickerSelectionParams.Builder()

        if (maxMediaItemSizeInBytes > 0) {
            builder.setMaxMediaItemSizeInBytes(maxMediaItemSizeInBytes)
        }
        maxVideoDuration?.let { builder.setMaxVideoDuration(it) }
        minVideoDuration?.let { builder.setMinVideoDuration(it) }
        if (maxMediaItemResolutionInPixels > 0) {
            builder.setMaxMediaItemResolutionInPixels(maxMediaItemResolutionInPixels)
        }
        if (minMediaItemResolutionInPixels >= 0) {
            builder.setMinMediaItemResolutionInPixels(minMediaItemResolutionInPixels)
        }
        if (mimeTypes.isNotEmpty()) {
            builder.setMimeTypes(mimeTypes)
        }
        if (maxSelectionBatchSizeInBytes > 0) {
            builder.setMaxSelectionBatchSizeInBytes(maxSelectionBatchSizeInBytes)
        }

        return builder.build()
    }

    /** A builder for constructing [SelectionParams] instances. */
    @RequiresApi(Build.VERSION_CODES.O)
    public class Builder {
        private var maxMediaItemSizeInBytes: Long = -1L
        private var maxVideoDuration: Duration? = null
        private var minVideoDuration: Duration? = null
        private var maxMediaItemResolutionInPixels: Long = -1L
        private var minMediaItemResolutionInPixels: Long = -1L
        private var mimeTypes: List<String> = emptyList()
        private var maxSelectionBatchSizeInBytes: Long = -1L

        /**
         * Sets the maximum allowed size, in bytes, for any individual media item to be selectable.
         *
         * Media items exceeding this size will be disabled and cannot be selected.
         *
         * If the maximum media item size is not set by the caller app using
         * [setMaxMediaItemSizeInBytes], there will be no restriction on the maximum media item
         * size.
         *
         * @param maxMediaItemSizeInBytes The maximum size in bytes.
         * @return This builder.
         * @throws IllegalArgumentException if [maxMediaItemSizeInBytes] is not positive.
         */
        public fun setMaxMediaItemSizeInBytes(maxMediaItemSizeInBytes: Long): Builder {
            require(maxMediaItemSizeInBytes > 0) { "Maximum media item size must be positive." }
            this.maxMediaItemSizeInBytes = maxMediaItemSizeInBytes
            return this
        }

        /**
         * Clears the maximum allowed size limit for individual media items.
         *
         * On calling this, the PhotoPicker will not enforce an upper limit on the size of
         * individual media items.
         *
         * @return This builder.
         */
        public fun clearMaxMediaItemSize(): Builder {
            this.maxMediaItemSizeInBytes = -1L
            return this
        }

        /**
         * Sets the maximum allowed duration for a video media item to be selectable.
         *
         * Video items exceeding this duration will be disabled and cannot be selected.
         *
         * If the maximum video duration is not set by the caller app using [setMaxVideoDuration],
         * there will be no restriction on the maximum video duration.
         *
         * @param maxVideoDuration The maximum video duration.
         * @return This builder.
         * @throws IllegalArgumentException if [maxVideoDuration] is not positive.
         */
        public fun setMaxVideoDuration(maxVideoDuration: Duration): Builder {
            require(maxVideoDuration > Duration.ZERO) {
                "Maximum video duration must be positive."
            }
            this.maxVideoDuration = maxVideoDuration
            return this
        }

        /**
         * Clears the maximum allowed duration limit for video media items.
         *
         * On calling this, the PhotoPicker will not enforce an upper limit on the duration of video
         * media items.
         *
         * @return This builder.
         */
        public fun clearMaxVideoDuration(): Builder {
            this.maxVideoDuration = null
            return this
        }

        /**
         * Sets the minimum allowed duration for a video media item to be selectable.
         *
         * Video items shorter than this duration will be disabled and cannot be selected.
         *
         * If the minimum video duration is not set by the caller app using [setMinVideoDuration],
         * there will be no restriction on the minimum video duration.
         *
         * @param minVideoDuration The minimum video duration.
         * @return This builder.
         * @throws IllegalArgumentException if [minVideoDuration] is not positive.
         */
        public fun setMinVideoDuration(minVideoDuration: Duration): Builder {
            require(minVideoDuration > Duration.ZERO) {
                "Minimum video duration must be positive."
            }
            this.minVideoDuration = minVideoDuration
            return this
        }

        /**
         * Clears the minimum allowed duration limit for video media items.
         *
         * On calling this, the PhotoPicker will not enforce a lower limit on the duration of video
         * media items.
         *
         * @return This builder.
         */
        public fun clearMinVideoDuration(): Builder {
            this.minVideoDuration = null
            return this
        }

        /**
         * Sets the maximum allowed resolution constraint for a media item to be selectable.
         *
         * Media items with total resolution (width * height) exceeding this limit will be disabled
         * and cannot be selected.
         *
         * If the maximum resolution is not set by the caller app using
         * [setMaxMediaItemResolutionInPixels], there will be no restriction on the maximum
         * resolution.
         *
         * @param maxMediaItemResolutionInPixels The maximum media item resolution in total pixels.
         * @return This builder.
         * @throws IllegalArgumentException if [maxMediaItemResolutionInPixels] is not positive.
         */
        public fun setMaxMediaItemResolutionInPixels(
            maxMediaItemResolutionInPixels: Long
        ): Builder {
            require(maxMediaItemResolutionInPixels > 0) {
                "Maximum media item resolution must be positive."
            }
            this.maxMediaItemResolutionInPixels = maxMediaItemResolutionInPixels
            return this
        }

        /**
         * Clears the maximum allowed resolution limit for media items.
         *
         * On calling this, the PhotoPicker will not enforce an upper limit on the resolution of
         * media items.
         *
         * @return This builder.
         */
        public fun clearMaxMediaItemResolution(): Builder {
            this.maxMediaItemResolutionInPixels = -1L
            return this
        }

        /**
         * Sets the minimum allowed resolution constraint for a media item to be selectable.
         *
         * Media items with total resolution (width * height) below this limit will be disabled and
         * cannot be selected.
         *
         * If the minimum resolution is not set by the caller app using
         * [setMinMediaItemResolutionInPixels], there will be no restriction on the minimum
         * resolution.
         *
         * @param minMediaItemResolutionInPixels The minimum allowed resolution in total pixels.
         * @return This builder.
         * @throws IllegalArgumentException if [minMediaItemResolutionInPixels] is negative.
         */
        public fun setMinMediaItemResolutionInPixels(
            minMediaItemResolutionInPixels: Long
        ): Builder {
            require(minMediaItemResolutionInPixels >= 0) {
                "Minimum media item resolution must be non-negative."
            }
            this.minMediaItemResolutionInPixels = minMediaItemResolutionInPixels
            return this
        }

        /**
         * Clears the minimum allowed resolution limit for media items.
         *
         * On calling this, the PhotoPicker will not enforce a lower limit on the resolution of
         * media items.
         *
         * @return This builder.
         */
        public fun clearMinMediaItemResolution(): Builder {
            this.minMediaItemResolutionInPixels = -1L
            return this
        }

        /**
         * Sets the list of mime types that are allowed for selection.
         *
         * Media items whose mime type is not in this list will be visible in the picker grid but
         * disabled for selection.
         *
         * If the allowed mime types are not set by the caller app using [setMimeTypes], all items
         * matching the requested media type filter will be selectable.
         *
         * This constraint is distinct from the media type filter specified via
         * [androidx.photopicker.activity.PickVisualMediaRequest.Builder.setMediaType]. The media
         * type in the request determines which items are queried and displayed in the PhotoPicker
         * grid in the first place, whereas [setMimeTypes] controls which displayed items are
         * enabled for selection.
         *
         * @param mimeTypes The list of allowed mime types.
         * @return This builder.
         * @throws IllegalArgumentException if [mimeTypes] is empty or contains non-image/video mime
         *   types.
         */
        public fun setMimeTypes(mimeTypes: List<String>): Builder {
            require(mimeTypes.isNotEmpty()) { "mime types list must not be empty." }
            for (mimeType in mimeTypes) {
                require(
                    mimeType.startsWith("image/", ignoreCase = true) ||
                        mimeType.startsWith("video/", ignoreCase = true)
                ) {
                    "Invalid mime type found: $mimeType. Only image/video mime types are supported."
                }
            }
            this.mimeTypes = mimeTypes.toList()
            return this
        }

        /**
         * Clears the allowed mime types selection constraint.
         *
         * On calling this, the PhotoPicker will not enforce allowed mime type constraints for
         * selection.
         *
         * @return This builder.
         */
        public fun clearMimeTypes(): Builder {
            this.mimeTypes = emptyList()
            return this
        }

        /**
         * Sets the maximum allowed cumulative size, in bytes, for the entire batch of selected
         * media items.
         *
         * This limits the aggregate size of all items selected in a single PhotoPicker session. If
         * selecting an item causes the total cumulative size to exceed this limit, the user will be
         * alerted that the maximum selection size has been reached and cannot add more items until
         * existing selections are removed.
         *
         * If the maximum selection batch size is not set by the caller app using
         * [setMaxSelectionBatchSizeInBytes], there will be no restriction on the cumulative size of
         * selected media items.
         *
         * @param maxSelectionBatchSizeInBytes The maximum batch size in bytes.
         * @return This builder.
         * @throws IllegalArgumentException if [maxSelectionBatchSizeInBytes] is not positive.
         */
        public fun setMaxSelectionBatchSizeInBytes(maxSelectionBatchSizeInBytes: Long): Builder {
            require(maxSelectionBatchSizeInBytes > 0) {
                "Maximum batch size limit must be positive."
            }
            this.maxSelectionBatchSizeInBytes = maxSelectionBatchSizeInBytes
            return this
        }

        /**
         * Clears the maximum allowed cumulative batch size limit.
         *
         * On calling this, the PhotoPicker will not enforce an upper limit on the cumulative size
         * of selected media items.
         *
         * @return This builder.
         */
        public fun clearMaxSelectionBatchSize(): Builder {
            this.maxSelectionBatchSizeInBytes = -1L
            return this
        }

        /**
         * Builds a new immutable [SelectionParams] object.
         *
         * @return The newly constructed SelectionParams.
         * @throws IllegalArgumentException if minVideoDuration is greater than maxVideoDuration, or
         *   minMediaItemResolutionInPixels is greater than maxMediaItemResolutionInPixels.
         */
        public fun build(): SelectionParams {
            val minDuration = minVideoDuration
            val maxDuration = maxVideoDuration
            if (minDuration != null && maxDuration != null) {
                require(minDuration <= maxDuration) {
                    "Minimum video duration cannot be greater than maximum video duration."
                }
            }

            val minRes = minMediaItemResolutionInPixels
            val maxRes = maxMediaItemResolutionInPixels
            if (minRes >= 0 && maxRes > 0) {
                require(minRes <= maxRes) {
                    "Minimum media item resolution cannot be greater than maximum media item resolution."
                }
            }

            return SelectionParams(
                maxMediaItemSizeInBytes = maxMediaItemSizeInBytes,
                maxVideoDuration = maxVideoDuration,
                minVideoDuration = minVideoDuration,
                maxMediaItemResolutionInPixels = maxMediaItemResolutionInPixels,
                minMediaItemResolutionInPixels = minMediaItemResolutionInPixels,
                mimeTypes = mimeTypes,
                maxSelectionBatchSizeInBytes = maxSelectionBatchSizeInBytes,
            )
        }
    }
}
