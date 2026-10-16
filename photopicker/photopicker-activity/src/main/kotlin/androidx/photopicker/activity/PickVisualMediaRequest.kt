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

@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released

package androidx.photopicker.activity

import android.os.Build
import android.provider.MediaStore
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.photopicker.activity.PickVisualMedia.DefaultTab
import androidx.photopicker.activity.PickVisualMedia.ImageAndVideo
import androidx.photopicker.activity.PickVisualMedia.MediaCapabilities
import androidx.photopicker.activity.PickVisualMedia.VisualMediaType
import androidx.photopicker.activity.params.HighlightAlbumParams
import androidx.photopicker.activity.params.HighlightSearchResultsParams
import androidx.photopicker.activity.params.SelectionParams
import androidx.photopicker.activity.params.UiCustomizationParams

/**
 * Creates a request for a [PickMultipleVisualMedia] or [PickVisualMedia] Activity Contract.
 *
 * @param mediaType type to go into the PickVisualMediaRequest
 * @return a PickVisualMediaRequest that contains the given input
 */
@Deprecated(
    "Superseded by PickVisualMediaRequest that takes an optional maxItems",
    level = DeprecationLevel.HIDDEN,
) // Binary API compatibility.
public fun PickVisualMediaRequest(
    mediaType: VisualMediaType = ImageAndVideo
): PickVisualMediaRequest = PickVisualMediaRequest.Builder().setMediaType(mediaType).build()

/**
 * Creates a request for a [PickMultipleVisualMedia] or [PickVisualMedia] Activity Contract.
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
public fun PickVisualMediaRequest(
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
): PickVisualMediaRequest =
    PickVisualMediaRequest.Builder().setMediaType(mediaType).setMaxItems(maxItems).build()

/**
 * Creates a request for a [PickMultipleVisualMedia] or [PickVisualMedia] Activity Contract.
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
public fun PickVisualMediaRequest(
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
    isOrderedSelection: Boolean = false,
    defaultTab: DefaultTab = DefaultTab.PhotosTab,
): PickVisualMediaRequest =
    PickVisualMediaRequest.Builder()
        .setMediaType(mediaType)
        .setMaxItems(maxItems)
        .setOrderedSelection(isOrderedSelection)
        .setDefaultTab(defaultTab)
        .build()

/**
 * Creates a request for a [PickMultipleVisualMedia] or [PickVisualMedia] Activity Contract.
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
public fun PickVisualMediaRequest(
    accentColor: Long,
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
    isOrderedSelection: Boolean = false,
    defaultTab: DefaultTab = DefaultTab.PhotosTab,
): PickVisualMediaRequest =
    PickVisualMediaRequest.Builder()
        .setMediaType(mediaType)
        .setMaxItems(maxItems)
        .setOrderedSelection(isOrderedSelection)
        .setDefaultTab(defaultTab)
        .setAccentColor(accentColor)
        .build()

/**
 * Creates a request for a [PickMultipleVisualMedia] or [PickVisualMedia] Activity Contract.
 *
 * @param mediaCapabilitiesForTranscoding the [MediaCapabilities] that the application can handle.
 * @param mediaType type to go into the PickVisualMediaRequest
 * @param maxItems limit the number of selectable items when using [PickMultipleVisualMedia]
 * @param isOrderedSelection whether the user can control the order of selected media when using
 *   [PickMultipleVisualMedia] (defaults to false)
 * @param defaultTab the tab to initially open the picker in (defaults to [DefaultTab.PhotosTab])
 * @return a PickVisualMediaRequest that contains the given input
 */
@Suppress("MissingJvmstatic")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public fun PickVisualMediaRequest(
    mediaCapabilitiesForTranscoding: MediaCapabilities?,
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
    isOrderedSelection: Boolean = false,
    defaultTab: DefaultTab = DefaultTab.PhotosTab,
): PickVisualMediaRequest =
    PickVisualMediaRequest.Builder()
        .setMediaType(mediaType)
        .setMaxItems(maxItems)
        .setOrderedSelection(isOrderedSelection)
        .setDefaultTab(defaultTab)
        .setMediaCapabilitiesForTranscoding(mediaCapabilitiesForTranscoding)
        .build()

/**
 * Creates a request for a [PickMultipleVisualMedia] or [PickVisualMedia] Activity Contract.
 *
 * @param mediaCapabilitiesForTranscoding the [MediaCapabilities] that the application can handle.
 * @param accentColor color long to customize picker accent color
 * @param mediaType type to go into the PickVisualMediaRequest
 * @param maxItems limit the number of selectable items when using [PickMultipleVisualMedia]
 * @param isOrderedSelection whether the user can control the order of selected media when using
 *   [PickMultipleVisualMedia] (defaults to false)
 * @param defaultTab the tab to initially open the picker in (defaults to [DefaultTab.PhotosTab])
 * @return a PickVisualMediaRequest that contains the given input
 */
@Suppress("MissingJvmstatic")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public fun PickVisualMediaRequest(
    mediaCapabilitiesForTranscoding: MediaCapabilities?,
    accentColor: Long,
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems(),
    isOrderedSelection: Boolean = false,
    defaultTab: DefaultTab = DefaultTab.PhotosTab,
): PickVisualMediaRequest =
    PickVisualMediaRequest.Builder()
        .setMediaType(mediaType)
        .setMaxItems(maxItems)
        .setOrderedSelection(isOrderedSelection)
        .setDefaultTab(defaultTab)
        .setAccentColor(accentColor)
        .setMediaCapabilitiesForTranscoding(mediaCapabilitiesForTranscoding)
        .build()

/** A request for a [PickMultipleVisualMedia] or [PickVisualMedia] Activity Contract. */
public class PickVisualMediaRequest internal constructor() {

    public var mediaType: VisualMediaType = ImageAndVideo
        internal set

    public var maxItems: Int = PickMultipleVisualMedia.getMaxItems()
        internal set

    public var isOrderedSelection: Boolean = false
        internal set

    public var defaultTab: DefaultTab = DefaultTab.PhotosTab
        internal set

    public var isCustomDefaultTabApplied: Boolean = false
        internal set

    public var isCustomAccentColorApplied: Boolean = false
        internal set

    public var accentColor: Long = 0
        internal set

    public var mediaCapabilitiesForTranscoding: MediaCapabilities? = null
        internal set

    public var isLocationMetadataAccessRequested: Boolean = false
        internal set

    public var highlightSearchResultsParams: HighlightSearchResultsParams? = null
        internal set

    public var highlightAlbumParams: HighlightAlbumParams? = null
        internal set

    @get:RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 22)
    public var selectionParams: SelectionParams? = null
        internal set

    public var uiCustomizationParams: UiCustomizationParams? = null
        internal set

    /** A builder for constructing [PickVisualMediaRequest] instances. */
    public class Builder {

        private var mediaType: VisualMediaType = ImageAndVideo
        private var maxItems: Int = PickMultipleVisualMedia.getMaxItems()
        private var isOrderedSelection: Boolean = false
        private var defaultTab: DefaultTab = DefaultTab.PhotosTab
        private var isCustomDefaultTabApplied: Boolean = false
        private var isCustomAccentColorApplied: Boolean = false
        private var accentColor: Long = 0
        private var mediaCapabilitiesForTranscoding: MediaCapabilities? = null
        private var isLocationMetadataAccessRequested: Boolean = false
        private var highlightSearchResultsParams: HighlightSearchResultsParams? = null
        private var highlightAlbumParams: HighlightAlbumParams? = null
        private var selectionParams: SelectionParams? = null
        private var uiCustomizationParams: UiCustomizationParams? = null

        /**
         * Set the media type for the [PickVisualMediaRequest].
         *
         * The type is the mime type to filter by, e.g. `PickVisualMedia.ImageOnly`,
         * `PickVisualMedia.ImageAndVideo`, `PickVisualMedia.SingleMimeType("image/gif")`
         *
         * @param mediaType type to go into the PickVisualMediaRequest
         * @return This builder.
         */
        public fun setMediaType(mediaType: VisualMediaType): Builder {
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
        public fun setMaxItems(@IntRange(from = 2) maxItems: Int): Builder {
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
        public fun setOrderedSelection(isOrderedSelection: Boolean): Builder {
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
        public fun setDefaultTab(defaultTab: DefaultTab): Builder {
            this.defaultTab = defaultTab
            this.isCustomDefaultTabApplied = true
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
        public fun setAccentColor(accentColor: Long): Builder {
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
        public fun setMediaCapabilitiesForTranscoding(
            mediaCapabilities: MediaCapabilities?
        ): Builder {
            this.mediaCapabilitiesForTranscoding = mediaCapabilities
            return this
        }

        /**
         * Sets whether the caller requests access to location metadata for the selected media
         * items.
         *
         * When set to `true`, this informs the Photopicker that the app is requesting location
         * information (such as EXIF GPS data) for the media items selected by the user. The default
         * value is `false`, meaning location metadata is redacted by default.
         *
         * Requesting location metadata does not guarantee that the calling app will receive it. The
         * Photopicker may prompt the user via the picker UI, and the user's choice to allow or deny
         * location sharing is final. If granted, calling apps can access this metadata when the
         * selected media files are opened via the returned URIs.
         *
         * This feature was added in SDK extension 23 on Android Tiramisu (API level 33) and higher.
         * If the underlying Photopicker implementation or device does not support this feature,
         * this request is safely ignored.
         *
         * @param isLocationMetadataAccessRequested boolean whether to request location metadata
         *   access from the Photopicker
         * @return This builder.
         */
        public fun setLocationMetadataAccessRequested(
            isLocationMetadataAccessRequested: Boolean
        ): Builder {
            this.isLocationMetadataAccessRequested = isLocationMetadataAccessRequested
            return this
        }

        /**
         * Sets parameters for highlighting search results in the photo picker. This allows apps to
         * highlight search results in the photopicker UI whenever feasible based on the given input
         * text query. Only one of search results highlight or album highlight can be specified to
         * either show a highlighted media section when the photopicker launches or to open the
         * photopicker to show a highlighted media results grid based on the given input query.
         * Specifying both album and search highlight will result in throwing
         * [IllegalArgumentException].
         *
         * This feature was added in Android Tiramisu (API level 33 / T ext 19) and will be ignored
         * on Android R and S.
         *
         * @param highlightSearchResultsParams The search result highlight configuration, or `null`
         *   to clear.
         * @return This builder.
         * @see MediaStore.EXTRA_PICK_IMAGES_HIGHLIGHT_SEARCH_RESULTS
         */
        public fun setHighlightSearchResultsParams(
            highlightSearchResultsParams: HighlightSearchResultsParams?
        ): Builder {
            this.highlightSearchResultsParams = highlightSearchResultsParams
            return this
        }

        /**
         * Sets parameters for highlighting a specific album's media in the photo picker.
         *
         * This allows apps to highlight media results of an album in the photo picker UI whenever
         * feasible based on the given input album (one of [HighlightAlbumParams.HighlightAlbum]).
         * Only one of search results highlight or album highlight can be specified to either show a
         * highlighted media section when the photopicker launches or to open the photopicker to
         * show a highlighted media results grid based on the given input album. Specifying both
         * album and search highlight will result in throwing [IllegalArgumentException].
         *
         * This feature was added in Android Tiramisu (API level 33 / T ext 19) and will be ignored
         * on Android R and S.
         *
         * @param highlightAlbumParams The album highlight configuration, or `null` to clear.
         * @return This builder.
         * @see MediaStore.EXTRA_PICK_IMAGES_HIGHLIGHT_ALBUM
         */
        public fun setHighlightAlbumParams(highlightAlbumParams: HighlightAlbumParams?): Builder {
            this.highlightAlbumParams = highlightAlbumParams
            return this
        }

        /**
         * Sets selection constraints for media items in the photo picker. The items that do not
         * satisfy these input constraints will not be selectable in the photopicker grid.
         *
         * This feature was added for the system PhotoPicker in SDK extension version 22 (API level
         * 34 / U ext 22) and is available starting from the same and higher. If the platform
         * conditions are not met, this cannot be used. Callers of this must add this check before
         * using.
         *
         * @param selectionParams The selection constraint parameters, or `null` to clear.
         * @return This builder.
         * @see android.provider.MediaStore.EXTRA_PICK_IMAGES_SELECTION_PARAMS
         */
        @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 22)
        public fun setSelectionParams(selectionParams: SelectionParams?): Builder {
            this.selectionParams = selectionParams
            return this
        }

        /* Sets UI customization options in the PhotoPicker. Not passing this implies the
         * photopicker will use its default UI.
         *
         * This feature was added for the system PhotoPicker in SDK extension version 22 on Android
         * UpsideDownCake (API level 34) and higher. For devices running a system fallback photo
         * picker, this preference is forwarded via
         * [PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS]. If the
         * underlying platform conditions or fallback picker implementation do not support this
         * feature, this request is safely ignored.
         *
         * @param uiCustomizationParams The UI customization preferences, or `null` to clear.
         * @return This builder.
         * @see android.provider.MediaStore.EXTRA_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS
         */
        public fun setUiCustomizationParams(
            uiCustomizationParams: UiCustomizationParams?
        ): Builder {
            this.uiCustomizationParams = uiCustomizationParams
            return this
        }

        /**
         * Build the PickVisualMediaRequest specified by this builder.
         *
         * @return the newly constructed PickVisualMediaRequest.
         * @throws IllegalArgumentException if both search results highlight and album highlight
         *   parameters are set.
         */
        public fun build(): PickVisualMediaRequest {
            if (highlightSearchResultsParams != null && highlightAlbumParams != null) {
                throw IllegalArgumentException(
                    "Cannot set both highlightSearchResultsParams and highlightAlbumParams. " +
                        "Only one highlight parameter may be configured per request."
                )
            }
            return PickVisualMediaRequest().apply {
                this.mediaType = this@Builder.mediaType
                this.maxItems = this@Builder.maxItems
                this.isOrderedSelection = this@Builder.isOrderedSelection
                this.defaultTab = this@Builder.defaultTab
                this.isCustomDefaultTabApplied = this@Builder.isCustomDefaultTabApplied
                this.isCustomAccentColorApplied = this@Builder.isCustomAccentColorApplied
                this.accentColor = this@Builder.accentColor
                this.mediaCapabilitiesForTranscoding = this@Builder.mediaCapabilitiesForTranscoding
                this.isLocationMetadataAccessRequested =
                    this@Builder.isLocationMetadataAccessRequested
                this.highlightSearchResultsParams = this@Builder.highlightSearchResultsParams
                this.highlightAlbumParams = this@Builder.highlightAlbumParams
                this.selectionParams = this@Builder.selectionParams
                this.uiCustomizationParams = this@Builder.uiCustomizationParams
            }
        }
    }
}
