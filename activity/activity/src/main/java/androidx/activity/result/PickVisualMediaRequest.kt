/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.activity.result

import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.DefaultTab
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.annotation.IntRange

/**
 * Creates a request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 *
 * @param mediaType type to go into the PickVisualMediaRequest
 * @return a PickVisualMediaRequest that contains the given input
 */
@Deprecated(
    "Superseded by PickVisualMediaRequest that takes an optional maxItems",
    level = DeprecationLevel.HIDDEN
) // Binary API compatibility.
fun PickVisualMediaRequest(mediaType: VisualMediaType = ImageAndVideo) =
    PickVisualMediaRequest.Builder().setMediaType(mediaType).build()

/**
 * Creates a request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 *
 * @param mediaType type to go into the PickVisualMediaRequest
 * @param maxItems limit the number of selectable items when using [PickMultipleVisualMedia]
 * @return a PickVisualMediaRequest that contains the given input
 */
@Deprecated(
    "Superseded by PickVisualMediaRequest that take optional isOrderedSelection and defaultTab",
    level = DeprecationLevel.HIDDEN
) // Binary API compatibility.
@Suppress("MissingJvmstatic")
fun PickVisualMediaRequest(
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems()
) = PickVisualMediaRequest.Builder().setMediaType(mediaType).setMaxItems(maxItems).build()

/**
 * Creates a request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 *
 * @param mediaType type to go into the PickVisualMediaRequest
 * @param maxItems limit the number of selectable items when using [PickMultipleVisualMedia]
 * @param isOrderedSelection whether the user can control the order of selected media when using
 *   [PickMultipleVisualMedia] (defaults to false)
 * @param defaultTab the tab to initially open in the picker (defaults to [DefaultTab.PhotosTab])
 * @return a PickVisualMediaRequest that contains the given input
 */
@Suppress("MissingJvmstatic")
fun PickVisualMediaRequest(
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
    isOrderedSelection: Boolean = false,
    defaultTab: DefaultTab = DefaultTab.PhotosTab
) =
    PickVisualMediaRequest.Builder()
        .setMediaType(mediaType)
        .setMaxItems(maxItems)
        .setOrderedSelection(isOrderedSelection)
        .setDefaultTab(defaultTab)
        .build()

/**
 * Creates a request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 *
 * @param accentColor color long to customize picker accent color
 * @param mediaType type to go into the PickVisualMediaRequest
 * @param maxItems limit the number of selectable items when using [PickMultipleVisualMedia]
 * @param isOrderedSelection whether the user can control the order of selected media when using
 *   [PickMultipleVisualMedia] (defaults to false)
 * @param defaultTab the tab to initially open in the picker (defaults to [DefaultTab.PhotosTab])
 * @return a PickVisualMediaRequest that contains the given input
 */
@Suppress("MissingJvmstatic")
fun PickVisualMediaRequest(
    accentColor: Long,
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
    isOrderedSelection: Boolean = false,
    defaultTab: DefaultTab = DefaultTab.PhotosTab
) =
    PickVisualMediaRequest.Builder()
        .setMediaType(mediaType)
        .setMaxItems(maxItems)
        .setOrderedSelection(isOrderedSelection)
        .setDefaultTab(defaultTab)
        .setAccentColor(accentColor)
        .build()

/**
 * A request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 */
class PickVisualMediaRequest internal constructor() {

    var mediaType: VisualMediaType = ImageAndVideo
        internal set

    var maxItems: Int = PickMultipleVisualMedia.getMaxItems()
        internal set

    var isOrderedSelection: Boolean = false
        internal set

    var defaultTab: DefaultTab = DefaultTab.PhotosTab
        internal set

    var isCustomAccentColorApplied: Boolean = false
        internal set

    var accentColor: Long = 0
        internal set

    /** A builder for constructing [PickVisualMediaRequest] instances. */
    class Builder {

        private var mediaType: VisualMediaType = ImageAndVideo
        private var maxItems: Int = PickMultipleVisualMedia.getMaxItems()
        private var isOrderedSelection: Boolean = false
        private var defaultTab: DefaultTab = DefaultTab.PhotosTab
        private var isCustomAccentColorApplied: Boolean = false
        private var accentColor: Long = 0

        /**
         * Set the media type for the [PickVisualMediaRequest].
         *
         * The type is the mime type to filter by, e.g. `PickVisualMedia.ImageOnly`,
         * `PickVisualMedia.ImageAndVideo`, `PickVisualMedia.SingleMimeType("image/gif")`
         *
         * @param mediaType type to go into the PickVisualMediaRequest
         * @return This builder.
         */
        fun setMediaType(mediaType: VisualMediaType): Builder {
            this.mediaType = mediaType
            return this
        }

        /**
         * Limit the number of selectable items in the photo picker when using
         * [PickMultipleVisualMedia]
         *
         * @param maxItems int type limiting the number of selectable items
         * @return This builder.
         */
        fun setMaxItems(@IntRange(from = 2) maxItems: Int): Builder {
            this.maxItems = maxItems
            return this
        }

        /**
         * Set the ordered selection for the [PickVisualMediaRequest].
         *
         * Allow the user to control the order in which images are returned to the calling app. This
         * parameter might be not supported by the underlying photo picker implementation.
         *
         * @param isOrderedSelection boolean to enable customisable selection order in the picker
         * @return This builder.
         */
        fun setOrderedSelection(isOrderedSelection: Boolean): Builder {
            this.isOrderedSelection = isOrderedSelection
            return this
        }

        /**
         * Set the default tab for the [PickVisualMediaRequest].
         *
         * The default tab is used to open the preferred view inside the photo picker at first such
         * as, e.g. [DefaultTab.PhotosTab], [DefaultTab.AlbumsTab]. This parameter might be not
         * supported by the underlying photo picker implementation.
         *
         * @param defaultTab the tab to launch the picker in
         * @return This builder.
         */
        fun setDefaultTab(defaultTab: DefaultTab): Builder {
            this.defaultTab = defaultTab
            return this
        }

        /**
         * Set the accent color for the [PickVisualMediaRequest].
         *
         * The accent color is used to change the main color in the photo picker. This parameter
         * might be not supported by the underlying photo picker implementation.
         *
         * @param accentColor color long to apply as accent to the main color in the picker
         * @return This builder.
         */
        fun setAccentColor(accentColor: Long): Builder {
            this.accentColor = accentColor
            this.isCustomAccentColorApplied = true
            return this
        }

        /**
         * Build the PickVisualMediaRequest specified by this builder.
         *
         * @return the newly constructed PickVisualMediaRequest.
         */
        fun build(): PickVisualMediaRequest =
            PickVisualMediaRequest().apply {
                this.mediaType = this@Builder.mediaType
                this.maxItems = this@Builder.maxItems
                this.isOrderedSelection = this@Builder.isOrderedSelection
                this.defaultTab = this@Builder.defaultTab
                this.isCustomAccentColorApplied = this@Builder.isCustomAccentColorApplied
                this.accentColor = this@Builder.accentColor
            }
    }
}
