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
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import kotlin.math.min

/**
 * An [ActivityResultContract] to use the
 * [Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker) to select
 * multiple images, videos, or other types of visual media.
 *
 * This contract always prefers the system framework provided Photo Picker available via
 * [MediaStore.ACTION_PICK_IMAGES] when it is available, but will also provide a fallback on devices
 * that it is not available to provide a consistent API surface across all Android API 19 or higher
 * devices.
 *
 * The priority order for handling the Photo Picker is:
 * 1. The system framework provided [MediaStore.ACTION_PICK_IMAGES].
 * - An OEM can provide a system app that implements
 *   [PickVisualMedia.ACTION_SYSTEM_FALLBACK_PICK_IMAGES] to provide a consistent Photo Picker to
 *   older devices. These system apps may handle the
 *   [PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX] extra to respect the [maxItems] passed
 *   to this contract.
 * - [Intent.ACTION_OPEN_DOCUMENT] is used as a final fallback on all Android API 19 or higher
 *   devices. This Intent does not allow limiting the max items the user selects.
 *
 * The constructor accepts one parameter [maxItems] to limit the number of selectable items when
 * using the photo picker to return. When launching the activity, the minimum of [maxItems] and
 * input [PickVisualMediaRequest.maxItems] is set as the limit.
 *
 * The input is a [PickVisualMediaRequest].
 *
 * The output is a list `Uri` of the selected media. It can be empty if the user hasn't selected any
 * items. Keep in mind that `Uri` returned by the photo picker aren't writable.
 *
 * This can be extended to override [createIntent] if you wish to pass additional extras to the
 * Intent created by `super.createIntent()`.
 */
public open class PickMultipleVisualMedia(private val maxItems: Int = getMaxItems()) :
    ActivityResultContract<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>() {

    init {
        require(maxItems > 1) { "Max items must be higher than 1" }
    }

    @CallSuper
    @SuppressLint("NewApi")
    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
        // Check to see if the photo picker is available
        return if (PickVisualMedia.isSystemPickerAvailable()) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = PickVisualMedia.getVisualMimeType(input.mediaType)
                val currentMaxItems = min(maxItems, input.maxItems)

                require(
                    currentMaxItems > 1 && currentMaxItems <= MediaStore.getPickImagesMaxLimit()
                ) {
                    "Max items must be greater than 1 and lesser than or equal to " +
                        "MediaStore.getPickImagesMaxLimit()"
                }

                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, currentMaxItems)
                putExtra(MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB, input.defaultTab.value)
                putExtra(MediaStore.EXTRA_PICK_IMAGES_IN_ORDER, input.isOrderedSelection)

                if (input.isCustomAccentColorApplied) {
                    putExtra(MediaStore.EXTRA_PICK_IMAGES_ACCENT_COLOR, input.accentColor)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    input.mediaCapabilitiesForTranscoding?.let { capabilities ->
                        putExtra(
                            MediaStore.EXTRA_MEDIA_CAPABILITIES,
                            capabilities.toApplicationMediaCapabilities(),
                        )
                    }
                }
            }
        } else if (PickVisualMedia.isSystemFallbackPickerAvailable(context)) {
            val fallbackPicker =
                checkNotNull(PickVisualMedia.getSystemFallbackPicker(context)).activityInfo
            Intent(PickVisualMedia.ACTION_SYSTEM_FALLBACK_PICK_IMAGES).apply {
                setClassName(fallbackPicker.applicationInfo.packageName, fallbackPicker.name)
                type = PickVisualMedia.getVisualMimeType(input.mediaType)

                val currentMaxItems = min(maxItems, input.maxItems)
                require(currentMaxItems > 1) { "Max items must be greater than 1" }

                putExtra(PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX, currentMaxItems)
                putExtra(
                    PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_LAUNCH_TAB,
                    input.defaultTab.value,
                )
                putExtra(
                    PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_IN_ORDER,
                    input.isOrderedSelection,
                )

                if (input.isCustomAccentColorApplied) {
                    putExtra(
                        PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_ACCENT_COLOR,
                        input.accentColor,
                    )
                }
            }
        } else {
            // For older devices running KitKat and higher and devices running Android 12
            // and 13 without the SDK extension that includes the Photo Picker, rely on the
            // ACTION_OPEN_DOCUMENT intent
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = PickVisualMedia.getVisualMimeType(input.mediaType)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

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
    ): SynchronousResult<List<@JvmSuppressWildcards Uri>>? = null

    final override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.getClipDataUris() ?: emptyList()
    }

    internal companion object {
        /**
         * The system photo picker has a maximum limit of selectable items returned by
         * [MediaStore.getPickImagesMaxLimit()] On devices supporting picker provided via
         * [PickVisualMedia.ACTION_SYSTEM_FALLBACK_PICK_IMAGES], the limit may be ignored if it's
         * higher than the allowed limit. On devices not supporting the photo picker, the limit is
         * ignored.
         *
         * @see MediaStore.EXTRA_PICK_IMAGES_MAX
         */
        @SuppressLint("NewApi")
        internal fun getMaxItems() =
            if (PickVisualMedia.isSystemPickerAvailable()) {
                MediaStore.getPickImagesMaxLimit()
            } else {
                Integer.MAX_VALUE
            }
    }
}
