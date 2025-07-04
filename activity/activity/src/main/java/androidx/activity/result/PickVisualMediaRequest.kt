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

import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.DefaultTab
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.MediaCapabilities
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi

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
    level = DeprecationLevel.HIDDEN,
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
    level = DeprecationLevel.HIDDEN,
) // Binary API compatibility.
@Suppress("MissingJvmstatic")
fun PickVisualMediaRequest(
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
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
 * @param defaultTab the tab to initially open the picker in (defaults to [DefaultTab.PhotosTab]).
 *   Note that the support for this parameter was added in API level 35 / R ext 12 and applies the
 *   default behavior for older versions. Also see
 *   [android.provider.MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB]
 * @return a PickVisualMediaRequest that contains the given input
 */
@Suppress("MissingJvmstatic")
fun PickVisualMediaRequest(
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
    isOrderedSelection: Boolean = false,
    defaultTab: DefaultTab = DefaultTab.PhotosTab,
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
 * @param accentColor color long to customize picker accent color. Note that the support for this
 *   parameter was added in API level 35 / R ext 12 and applies the default behavior for older
 *   versions. Also see [android.provider.MediaStore.EXTRA_PICK_IMAGES_ACCENT_COLOR]
 * @param mediaType type to go into the PickVisualMediaRequest
 * @param maxItems limit the number of selectable items when using [PickMultipleVisualMedia]
 * @param isOrderedSelection whether the user can control the order of selected media when using
 *   [PickMultipleVisualMedia] (defaults to false)
 * @param defaultTab the tab to initially open the picker in (defaults to [DefaultTab.PhotosTab]).
 *   Note that the support for this parameter was added in API level 35 / R ext 12 and applies the
 *   default behavior for older versions. Also see
 *   [android.provider.MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB]
 * @return a PickVisualMediaRequest that contains the given input
 */
@Suppress("MissingJvmstatic")
fun PickVisualMediaRequest(
    accentColor: Long,
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
    isOrderedSelection: Boolean = false,
    defaultTab: DefaultTab = DefaultTab.PhotosTab,
) =
    PickVisualMediaRequest.Builder()
        .setMediaType(mediaType)
        .setMaxItems(maxItems)
        .setOrderedSelection(isOrderedSelection)
        .setDefaultTab(defaultTab)
        .setAccentColor(accentColor)
        .build()

/**
 * Creates a request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 *
 * @param mediaCapabilitiesForTranscoding the [MediaCapabilities] that the application can handle.
 * @param mediaType type to go into the PickVisualMediaRequest
 * @param maxItems limit the number of selectable items when using [PickMultipleVisualMedia]
 * @param isOrderedSelection whether the user can control the order of selected media when using
 *   [PickMultipleVisualMedia] (defaults to false)
 * @param defaultTab the tab to initially open in the picker (defaults to [DefaultTab.PhotosTab])
 * @return a PickVisualMediaRequest that contains the given input
 */
@Suppress("MissingJvmstatic")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun PickVisualMediaRequest(
    mediaCapabilitiesForTranscoding: MediaCapabilities?,
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
    isOrderedSelection: Boolean = false,
    defaultTab: DefaultTab = DefaultTab.PhotosTab,
) =
    PickVisualMediaRequest.Builder()
        .setMediaType(mediaType)
        .setMaxItems(maxItems)
        .setOrderedSelection(isOrderedSelection)
        .setDefaultTab(defaultTab)
        .setMediaCapabilitiesForTranscoding(mediaCapabilitiesForTranscoding)
        .build()

/**
 * Creates a request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 *
 * @param mediaCapabilitiesForTranscoding the [MediaCapabilities] that the application can handle.
 * @param accentColor color long to customize picker accent color
 * @param mediaType type to go into the PickVisualMediaRequest
 * @param maxItems limit the number of selectable items when using [PickMultipleVisualMedia]
 * @param isOrderedSelection whether the user can control the order of selected media when using
 *   [PickMultipleVisualMedia] (defaults to false)
 * @param defaultTab the tab to initially open in the picker (defaults to [DefaultTab.PhotosTab])
 * @return a PickVisualMediaRequest that contains the given input
 */
@Suppress("MissingJvmstatic")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun PickVisualMediaRequest(
    mediaCapabilitiesForTranscoding: MediaCapabilities?,
    accentColor: Long,
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
    isOrderedSelection: Boolean = false,
    defaultTab: DefaultTab = DefaultTab.PhotosTab,
) =
    PickVisualMediaRequest.Builder()
        .setMediaType(mediaType)
        .setMaxItems(maxItems)
        .setOrderedSelection(isOrderedSelection)
        .setDefaultTab(defaultTab)
        .setAccentColor(accentColor)
        .setMediaCapabilitiesForTranscoding(mediaCapabilitiesForTranscoding)
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

    var mediaCapabilitiesForTranscoding: MediaCapabilities? = null
        internal set

    /** A builder for constructing [PickVisualMediaRequest] instances. */
    class Builder {

        private var mediaType: VisualMediaType = ImageAndVideo
        private var maxItems: Int = PickMultipleVisualMedia.getMaxItems()
        private var isOrderedSelection: Boolean = false
        private var defaultTab: DefaultTab = DefaultTab.PhotosTab
        private var isCustomAccentColorApplied: Boolean = false
        private var accentColor: Long = 0
        private var mediaCapabilitiesForTranscoding: MediaCapabilities? = null

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
         * as, e.g. [DefaultTab.PhotosTab], [DefaultTab.AlbumsTab]. This feature was added in API
         * level 35 / R ext 12 and applies the default behavior for older versions.
         *
         * @param defaultTab the tab to launch the picker in (defaults to [DefaultTab.PhotosTab])
         * @return This builder.
         * @see android.provider.MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB
         */
        fun setDefaultTab(defaultTab: DefaultTab): Builder {
            this.defaultTab = defaultTab
            return this
        }

        /**
         * Set the accent color for the [PickVisualMediaRequest].
         *
         * The accent color is used to change the main color in the photo picker. This feature was
         * added in API level 35 / R ext 12 and applies the default behavior for older versions.
         *
         * @param accentColor color long to apply as accent to the main color in the picker
         * @return This builder.
         * @see android.provider.MediaStore.EXTRA_PICK_IMAGES_ACCENT_COLOR
         */
        fun setAccentColor(accentColor: Long): Builder {
            this.accentColor = accentColor
            this.isCustomAccentColorApplied = true
            return this
        }

        /**
         * Set the media capabilities for the [PickVisualMediaRequest].
         *
         * This parameter allows you to specify the media capabilities that your application can
         * handle, such as the HDR type of the media. This parameter might be not supported by the
         * underlying photo picker implementation.
         *
         * When the requested video format does not match the capabilities specified by the calling
         * app and the video duration is within the range that photo picker can handle, photo picker
         * will transcode the video into a default supported format, otherwise, the calling app will
         * receive the original file.
         *
         * @param mediaCapabilities the [MediaCapabilities] to apply to the media selection.
         * @return This builder.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun setMediaCapabilitiesForTranscoding(mediaCapabilities: MediaCapabilities?): Builder {
            this.mediaCapabilitiesForTranscoding = mediaCapabilities
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
                this.mediaCapabilitiesForTranscoding = this@Builder.mediaCapabilitiesForTranscoding
            }
    }
}
