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

package androidx.photopicker.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.ApplicationMediaCapabilities
import android.media.MediaFeature.HdrType.DOLBY_VISION
import android.media.MediaFeature.HdrType.HDR10
import android.media.MediaFeature.HdrType.HDR10_PLUS
import android.media.MediaFeature.HdrType.HLG
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions.getExtensionVersion
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * An [ActivityResultContract] to use the
 * [Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker) to select
 * a single image, video, or other type of visual media.
 *
 * This contract always prefers the system framework provided Photo Picker available via
 * [MediaStore.ACTION_PICK_IMAGES] when it is available, but will also provide a fallback on devices
 * that it is not available to provide a consistent API surface across all Android API 19 or higher
 * devices.
 *
 * The priority order for handling the Photo Picker is:
 * 1. The system framework provided [MediaStore.ACTION_PICK_IMAGES].
 * - An OEM can provide a system app that implements [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] to provide
 *   a consistent Photo Picker to older devices.
 * - [Intent.ACTION_OPEN_DOCUMENT] is used as a final fallback on all Android API 19 or higher
 *   devices.
 *
 * The input is a [PickVisualMediaRequest].
 *
 * The output is a `Uri` when the user has selected a media or `null` when the user hasn't selected
 * any item. Keep in mind that `Uri` returned by the photo picker isn't writable.
 *
 * This can be extended to override [createIntent] if you wish to pass additional extras to the
 * Intent created by `super.createIntent()`.
 */
public open class PickVisualMedia : ActivityResultContract<PickVisualMediaRequest, Uri?>() {
    public companion object {
        /**
         * Check if the current device has support for the photo picker by checking the running
         * Android version or the SDK extension version.
         *
         * Note that this does not check for any Intent handled by
         * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES].
         */
        @SuppressLint("NewApi")
        @Deprecated(
            message =
                "This method is deprecated in favor of isPhotoPickerAvailable(context) " +
                    "to support the picker provided by updatable system apps",
            replaceWith = ReplaceWith("isPhotoPickerAvailable(context)"),
        )
        @JvmStatic
        public fun isPhotoPickerAvailable(): Boolean {
            return isSystemPickerAvailable()
        }

        /**
         * In cases where the system framework provided [MediaStore.ACTION_PICK_IMAGES] Photo Picker
         * cannot be implemented, OEMs or system apps can provide a consistent Photo Picker
         * experience to those devices by creating an Activity that handles this action. This app
         * must also include [Intent.CATEGORY_DEFAULT] in the activity's intent filter.
         *
         * Only system apps can implement this action - any non-system apps will be ignored when
         * searching for the activities that handle this Intent.
         *
         * Note: this should not be used directly, instead relying on the selection logic done by
         * [createIntent] to create the correct Intent for the current device.
         */
        @field:Suppress("ActionValue") /* Don't include SYSTEM_FALLBACK in the action */
        public const val ACTION_SYSTEM_FALLBACK_PICK_IMAGES: String =
            "androidx.activity.result.contract.action.PICK_IMAGES"

        /**
         * Extra that will be sent by [PickMultipleVisualMedia] to an Activity that handles
         * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates that maximum number of photos the
         * user should select.
         *
         * If this extra is not present, only a single photo should be selectable.
         *
         * If this extra is present but equal to [Int.MAX_VALUE], then no limit should be enforced.
         */
        @field:Suppress("ActionValue") /* Don't include SYSTEM_FALLBACK in the extra */
        public const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX: String =
            "androidx.activity.result.contract.extra.PICK_IMAGES_MAX"

        /**
         * Extra that will be sent by [PickVisualMedia] and [PickMultipleVisualMedia] to an Activity
         * that handles [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates the preferred default
         * tab of the picker.
         *
         * If this extra is not present, the default tab of the picker will be used.
         */
        @field:Suppress("ActionValue")
        /* Don't include SYSTEM_FALLBACK in the extra */
        public const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_LAUNCH_TAB: String =
            "androidx.activity.result.contract.extra.PICK_IMAGES_LAUNCH_TAB"

        /**
         * Extra that will be sent by [PickMultipleVisualMedia] to an Activity that handles
         * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates allowing the user to control the
         * order in which images are returned to the calling app.
         */
        @field:Suppress("ActionValue")
        /* Don't include SYSTEM_FALLBACK in the extra */
        public const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_IN_ORDER: String =
            "androidx.activity.result.contract.extra.PICK_IMAGES_IN_ORDER"

        /**
         * Extra that will be sent by [PickVisualMedia] and [PickMultipleVisualMedia] to an Activity
         * that handles [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates the preferred accent
         * color of the picker.
         *
         * If this extra is not present, the default accent color of the picker will be used.
         */
        @field:Suppress("ActionValue")
        /* Don't include SYSTEM_FALLBACK in the extra */
        public const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_ACCENT_COLOR: String =
            "androidx.activity.result.contract.extra.PICK_IMAGES_ACCENT_COLOR"

        /**
         * Extra that will be sent by [PickVisualMedia] and [PickMultipleVisualMedia] to an Activity
         * that handles [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates whether to request
         * location metadata access from the picker.
         *
         * If this extra is not present, location metadata should be redacted by default.
         */
        @field:Suppress("ActionValue")
        /* Don't include SYSTEM_FALLBACK in the extra */
        public const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS:
            String =
            "androidx.activity.result.contract.extra.PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS"

        /**
         * Extra that will be sent by [PickVisualMedia] and [PickMultipleVisualMedia] to an Activity
         * that handles [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates the search results to
         * highlight in the picker.
         */
        @field:Suppress("ActionValue")
        public const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_SEARCH_RESULTS: String =
            "androidx.activity.result.contract.extra.PICK_IMAGES_HIGHLIGHT_SEARCH_RESULTS"

        /**
         * Extra that will be sent by [PickVisualMedia] and [PickMultipleVisualMedia] to an Activity
         * that handles [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] to indicate the predefined album to
         * highlight in the picker.
         */
        @field:Suppress("ActionValue")
        public const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM: String =
            "androidx.activity.result.contract.extra.PICK_IMAGES_HIGHLIGHT_ALBUM"

        /**
         * Bundle key used in system fallback highlight extras to specify the value of the highlight
         * type.
         */
        public const val KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE: String =
            "androidx.activity.result.contract.key.PICK_IMAGES_HIGHLIGHT_TYPE"

        /**
         * Value for [KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE] specifying a collapsed
         * highlight section.
         */
        public const val SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE_COLLAPSED: Int = 0

        /**
         * Value for [KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE] specifying an expanded
         * highlight grid.
         */
        public const val SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_TYPE_EXPANDED: Int = 1

        /**
         * Bundle key used in system fallback search results highlight extras to specify the text
         * query.
         */
        public const val KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_SEARCH_TEXT_QUERY: String =
            "androidx.activity.result.contract.key.PICK_IMAGES_HIGHLIGHT_SEARCH_TEXT_QUERY"

        /**
         * Bundle key used in system fallback album highlight extras to specify the highlight album.
         */
        public const val KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_ID: String =
            "androidx.activity.result.contract.key.PICK_IMAGES_HIGHLIGHT_ALBUM_ID"

        /**
         * Value for [KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_ID] specifying the Favorites
         * album.
         */
        public const val SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_FAVORITES: String =
            "androidx.activity.result.contract.album.FAVORITES"

        /**
         * Value for [KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_ID] specifying the Camera
         * album.
         */
        public const val SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_CAMERA: String =
            "androidx.activity.result.contract.album.CAMERA"

        /**
         * Value for [KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_ID] specifying the Downloads
         * album.
         */
        public const val SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_DOWNLOADS: String =
            "androidx.activity.result.contract.album.DOWNLOADS"

        /**
         * Value for [KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_ID] specifying the Screenshots
         * album.
         */
        public const val SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_SCREENSHOTS: String =
            "androidx.activity.result.contract.album.SCREENSHOTS"

        /**
         * Value for [KEY_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_ID] specifying the Videos
         * album.
         */
        public const val SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM_VIDEOS: String =
            "androidx.activity.result.contract.album.VIDEOS"

        @JvmStatic
        internal fun areSystemHighlightParamsAvailable(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 19
        }

        /* Extra that will be sent by [PickVisualMedia] and [PickMultipleVisualMedia] to an Activity
         * that handles [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that specifies UI customization options
         * for the picker.
         *
         * The value of this intent extra should be of type [android.os.Bundle] containing UI
         * customization params for the fallback picker.
         *
         * The bundle may include the following keys:
         * - [EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO]: An [Int]
         *   specifying the thumbnail aspect ratio, one of
         *   [SYSTEM_FALLBACK_PICK_IMAGES_ASPECT_RATIO_SQUARE_1_1] or
         *   [SYSTEM_FALLBACK_PICK_IMAGES_ASPECT_RATIO_PORTRAIT_9_16].
         */
        @field:Suppress("ActionValue")
        /* Don't include SYSTEM_FALLBACK in the extra */
        public const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS: String =
            "androidx.activity.result.contract.extra.PICK_IMAGES_UI_CUSTOMIZATION_PARAMS"

        /**
         * Key within the [EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS] bundle
         * specifying the thumbnail aspect ratio.
         *
         * Expected value is an integer: [SYSTEM_FALLBACK_PICK_IMAGES_ASPECT_RATIO_SQUARE_1_1] or
         * [SYSTEM_FALLBACK_PICK_IMAGES_ASPECT_RATIO_PORTRAIT_9_16].
         */
        @field:Suppress("ActionValue")
        /* Don't include SYSTEM_FALLBACK in the extra */
        public const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO:
            String =
            "androidx.activity.result.contract.extra.PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO"

        /**
         * Value for [EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO]
         * specifying a square 1:1 aspect ratio thumbnail sizing within the media grid.
         */
        public const val SYSTEM_FALLBACK_PICK_IMAGES_ASPECT_RATIO_SQUARE_1_1: Int = 0

        /**
         * Value for [EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO]
         * specifying a portrait 9:16 aspect ratio thumbnail sizing within the media grid.
         */
        public const val SYSTEM_FALLBACK_PICK_IMAGES_ASPECT_RATIO_PORTRAIT_9_16: Int = 1

        @JvmStatic
        internal fun arePhotoPickerUiCustomizationParamsAvailable(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 22
        }

        /**
         * Check if the current device has support for the photo picker by checking the running
         * Android version, the SDK extension version or the picker provided by a system app
         * implementing [ACTION_SYSTEM_FALLBACK_PICK_IMAGES].
         */
        @SuppressLint("NewApi")
        @JvmStatic
        public fun isPhotoPickerAvailable(context: Context): Boolean {
            return isSystemPickerAvailable() || isSystemFallbackPickerAvailable(context)
        }

        /**
         * Check if the current device has support for the system framework provided photo picker by
         * checking the running Android version or the SDK extension version.
         *
         * Note that this does not check for any Intent handled by
         * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES].
         */
        @SuppressLint("NewApi")
        @JvmStatic
        internal fun isSystemPickerAvailable(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // getExtension is seen as part of Android Tiramisu only while the SdkExtensions
                // have been added on Android R
                getExtensionVersion(Build.VERSION_CODES.R) >= 2
            } else {
                false
            }
        }

        @JvmStatic
        internal fun arePhotoPickerSelectionParamsAvailable(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 22
        }

        @JvmStatic
        internal fun isLocationMetadataAvailable(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 23
        }

        @JvmStatic
        internal fun isSystemFallbackPickerAvailable(context: Context): Boolean {
            return getSystemFallbackPicker(context) != null
        }

        @Suppress("DEPRECATION")
        @JvmStatic
        internal fun getSystemFallbackPicker(context: Context): ResolveInfo? {
            return context.packageManager.resolveActivity(
                Intent(ACTION_SYSTEM_FALLBACK_PICK_IMAGES),
                PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_SYSTEM_ONLY,
            )
        }

        internal fun getVisualMimeType(input: VisualMediaType): String? {
            return when (input) {
                is ImageOnly -> "image/*"
                is VideoOnly -> "video/*"
                is SingleMimeType -> input.mimeType
                is ImageAndVideo -> null
            }
        }
    }

    /** Represents filter input type accepted by the photo picker. */
    public sealed interface VisualMediaType

    /** [VisualMediaType] object used to filter images only when using the photo picker. */
    public object ImageOnly : VisualMediaType

    /** [VisualMediaType] object used to filter video only when using the photo picker. */
    public object VideoOnly : VisualMediaType

    /** [VisualMediaType] object used to filter images and video when using the photo picker. */
    public object ImageAndVideo : VisualMediaType

    /**
     * [VisualMediaType] class used to filter a single mime type only when using the photo picker.
     */
    public class SingleMimeType(public val mimeType: String) : VisualMediaType

    /**
     * Represents the media capabilities of an application.
     *
     * This class allows you to specify the media capabilities that your application can handle,
     * such as the HDR type of the media. By providing this information to [PickVisualMediaRequest],
     * the photo picker can provide a more appropriate media format when possible.
     *
     * @see PickVisualMediaRequest.Builder.setMediaCapabilitiesForTranscoding
     */
    public class MediaCapabilities internal constructor() {

        public companion object {
            /** Defines the type of HDR (high dynamic range). */
            @Retention(AnnotationRetention.SOURCE)
            @IntDef(TYPE_HLG10, TYPE_HDR10, TYPE_HDR10_PLUS, TYPE_DOLBY_VISION)
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @Target(
                AnnotationTarget.TYPE,
                AnnotationTarget.PROPERTY,
                AnnotationTarget.VALUE_PARAMETER,
            )
            public annotation class HdrType

            /** HDR type for HLG10. */
            public const val TYPE_HLG10: Int = 0
            /** HDR type for HDR10. */
            public const val TYPE_HDR10: Int = 1
            /** HDR type for HDR10+. */
            public const val TYPE_HDR10_PLUS: Int = 2
            /** HDR type for Dolby-Vision. */
            public const val TYPE_DOLBY_VISION: Int = 3
        }

        public var supportedHdrTypes: Set<@HdrType Int> = emptySet()
            internal set

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        internal fun toApplicationMediaCapabilities(): ApplicationMediaCapabilities {
            return ApplicationMediaCapabilities.Builder()
                .apply {
                    addSupportedVideoMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC)
                    supportedHdrTypes.forEach {
                        when (it) {
                            TYPE_HLG10 -> addSupportedHdrType(HLG)
                            TYPE_HDR10 -> addSupportedHdrType(HDR10)
                            TYPE_HDR10_PLUS -> addSupportedHdrType(HDR10_PLUS)
                            TYPE_DOLBY_VISION -> addSupportedHdrType(DOLBY_VISION)
                        }
                    }
                }
                .build()
        }

        /** A builder for constructing [MediaCapabilities] instances. */
        public class Builder {

            private var supportedHdrTypes: MutableSet<@HdrType Int> = mutableSetOf()

            /**
             * Adds the supported HDR (High Dynamic Range) types for media capabilities.
             *
             * @param hdrType A supported HDR type from the [HdrType].
             * @return This Builder.
             * @throws IllegalArgumentException if an invalid hdrType is provided.
             */
            public fun addSupportedHdrType(hdrType: @HdrType Int): Builder {
                this.supportedHdrTypes.add(hdrType)
                return this
            }

            /**
             * Build the MediaCapabilities specified by this builder.
             *
             * @return the newly constructed MediaCapabilities.
             */
            public fun build(): MediaCapabilities =
                MediaCapabilities().apply {
                    this.supportedHdrTypes = this@Builder.supportedHdrTypes
                }
        }
    }

    /** Represents filter input type accepted by the photo picker. */
    public abstract class DefaultTab private constructor() {
        public abstract val value: Int

        /**
         * [DefaultTab] object used to open the picker in Photos tab (also the default if no value
         * is provided).
         */
        public object PhotosTab : DefaultTab() {
            override val value: Int = MediaStore.PICK_IMAGES_TAB_IMAGES
        }

        /** [DefaultTab] object used to open the picker in Albums tab. */
        public object AlbumsTab : DefaultTab() {
            override val value: Int = MediaStore.PICK_IMAGES_TAB_ALBUMS
        }
    }

    @CallSuper
    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
        // Check if Photo Picker is available on the device
        return if (isSystemPickerAvailable()) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = getVisualMimeType(input.mediaType)

                if (input.isCustomDefaultTabApplied) {
                    putExtra(MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB, input.defaultTab.value)
                }

                if (input.isCustomAccentColorApplied) {
                    putExtra(MediaStore.EXTRA_PICK_IMAGES_ACCENT_COLOR, input.accentColor)
                }

                if (areSystemHighlightParamsAvailable()) {
                    input.highlightSearchResultsParams?.let {
                        putExtra(
                            MediaStore.EXTRA_PICK_IMAGES_HIGHLIGHT_SEARCH_RESULTS,
                            it.createHighlightSearchResultsBundle(),
                        )
                    }

                    input.highlightAlbumParams?.let {
                        putExtra(
                            MediaStore.EXTRA_PICK_IMAGES_HIGHLIGHT_ALBUM,
                            it.createHighlightAlbumBundle(),
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    input.mediaCapabilitiesForTranscoding?.let { capabilities ->
                        putExtra(
                            MediaStore.EXTRA_MEDIA_CAPABILITIES,
                            capabilities.toApplicationMediaCapabilities(),
                        )
                    }
                }

                if (isLocationMetadataAvailable()) {
                    if (input.isLocationMetadataAccessRequested) {
                        putExtra(MediaStore.EXTRA_REQUEST_LOCATION_METADATA_ACCESS, true)
                    }
                }

                if (arePhotoPickerSelectionParamsAvailable()) {
                    input.selectionParams?.toPhotoPickerSelectionParams()?.let { selectionParams ->
                        putExtra(MediaStore.EXTRA_PICK_IMAGES_SELECTION_PARAMS, selectionParams)
                    }
                }

                if (arePhotoPickerUiCustomizationParamsAvailable()) {
                    input.uiCustomizationParams?.let { uiCustomizationParams ->
                        putExtra(
                            MediaStore.EXTRA_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS,
                            uiCustomizationParams.toPhotoPickerUiCustomizationParams(),
                        )
                    }
                }
            }
        } else if (isSystemFallbackPickerAvailable(context)) {
            val fallbackPicker = checkNotNull(getSystemFallbackPicker(context)).activityInfo
            Intent(ACTION_SYSTEM_FALLBACK_PICK_IMAGES).apply {
                setClassName(fallbackPicker.applicationInfo.packageName, fallbackPicker.name)
                type = getVisualMimeType(input.mediaType)

                if (input.isCustomDefaultTabApplied) {
                    putExtra(EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_LAUNCH_TAB, input.defaultTab.value)
                }

                if (input.isCustomAccentColorApplied) {
                    putExtra(EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_ACCENT_COLOR, input.accentColor)
                }

                if (input.isLocationMetadataAccessRequested) {
                    putExtra(
                        EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_REQUEST_LOCATION_METADATA_ACCESS,
                        true,
                    )
                }

                input.highlightSearchResultsParams?.let {
                    putExtra(
                        EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_SEARCH_RESULTS,
                        it.createHighlightSearchResultsFallbackBundle(),
                    )
                }

                input.highlightAlbumParams?.let {
                    putExtra(
                        EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_HIGHLIGHT_ALBUM,
                        it.createHighlightAlbumFallbackBundle(),
                    )
                }

                input.uiCustomizationParams?.createFallbackUiParamBundle()?.let { bundle ->
                    putExtra(
                        EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS,
                        bundle,
                    )
                }
            }
        } else {
            // For older devices running KitKat and higher and devices running Android 12
            // and 13 without the SDK extension that includes the Photo Picker, rely on the
            // ACTION_OPEN_DOCUMENT intent
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = getVisualMimeType(input.mediaType)

                if (type == null) {
                    // ACTION_OPEN_DOCUMENT requires to set this parameter when launching the
                    // intent with multiple mime types
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                }
            }
        }
    }

    @Suppress("InvalidNullabilityOverride")
    final override fun getSynchronousResult(
        context: Context,
        input: PickVisualMediaRequest,
    ): SynchronousResult<Uri?>? = null

    final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent
            .takeIf { resultCode == Activity.RESULT_OK }
            ?.run {
                // Check both the data URI and ClipData since the fallback picker
                // may only return results through getClipDataUris()
                data ?: getClipDataUris().firstOrNull()
            }
    }
}

internal fun Intent.getClipDataUris(): List<Uri> {
    // Use a LinkedHashSet to maintain any ordering that may be
    // present in the ClipData
    val resultSet = LinkedHashSet<Uri>()
    data?.let { data -> resultSet.add(data) }
    val clipData = clipData
    if (clipData == null && resultSet.isEmpty()) {
        return emptyList()
    } else if (clipData != null) {
        for (i in 0 until clipData.itemCount) {
            val uri = clipData.getItemAt(i).uri
            if (uri != null) {
                resultSet.add(uri)
            }
        }
    }
    return ArrayList(resultSet)
}
