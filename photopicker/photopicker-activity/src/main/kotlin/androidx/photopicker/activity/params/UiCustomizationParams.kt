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

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.widget.photopicker.PhotoPickerUiCustomizationParams
import androidx.annotation.IntDef
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.photopicker.activity.PickVisualMedia

/**
 * Defines UI customization options for the photopicker.
 *
 * @property aspectRatio The requested [AspectRatio] for thumbnails in the picker grid.
 * @see android.provider.MediaStore.EXTRA_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS
 * @see android.widget.photopicker.PhotoPickerUiCustomizationParams
 */
public class UiCustomizationParams internal constructor() {
    public companion object {
        /** Aspect ratio options for thumbnails in [UiCustomizationParams]. */
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(ASPECT_RATIO_UNDEFINED, ASPECT_RATIO_SQUARE_1_1, ASPECT_RATIO_PORTRAIT_9_16)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Target(
            AnnotationTarget.TYPE,
            AnnotationTarget.PROPERTY,
            AnnotationTarget.VALUE_PARAMETER,
            AnnotationTarget.PROPERTY_GETTER,
        )
        public annotation class AspectRatio

        /** Undefined aspect ratio for the photopicker thumbnail sizing within the media grid. */
        public const val ASPECT_RATIO_UNDEFINED: Int = -1

        /**
         * Request the default square 1:1 ratio for the photopicker thumbnail sizing within the
         * media grid.
         *
         * @see PhotoPickerUiCustomizationParams.ASPECT_RATIO_SQUARE_1_1
         */
        public const val ASPECT_RATIO_SQUARE_1_1: Int = 0

        /**
         * Requests a portrait 9:16 aspect ratio for the photopicker thumbnail sizing within the
         * media grid.
         *
         * @see PhotoPickerUiCustomizationParams.ASPECT_RATIO_PORTRAIT_9_16
         */
        public const val ASPECT_RATIO_PORTRAIT_9_16: Int = 1
    }

    /** The set [AspectRatio] for this request, defaulting to [ASPECT_RATIO_UNDEFINED]. */
    @get:AspectRatio
    public var aspectRatio: Int = ASPECT_RATIO_UNDEFINED
        internal set

    /**
     * Converts this [UiCustomizationParams] instance into a platform
     * [PhotoPickerUiCustomizationParams] parcelable object.
     *
     * @return The platform [PhotoPickerUiCustomizationParams] instance.
     */
    @SuppressLint("WrongConstant")
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 22)
    internal fun toPhotoPickerUiCustomizationParams(): PhotoPickerUiCustomizationParams {
        val builder = PhotoPickerUiCustomizationParams.Builder()
        if (aspectRatio != ASPECT_RATIO_UNDEFINED) {
            builder.setAspectRatio(aspectRatio)
        }
        return builder.build()
    }

    /**
     * Converts this [UiCustomizationParams] into a [Bundle] containing UI customization key-value
     * pairs for the fallback photo picker.
     */
    internal fun createFallbackUiParamBundle(): Bundle {
        return Bundle().apply {
            if (aspectRatio != ASPECT_RATIO_UNDEFINED) {
                putInt(
                    PickVisualMedia
                        .EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_UI_CUSTOMIZATION_PARAMS_ASPECT_RATIO,
                    aspectRatio,
                )
            }
        }
    }

    /** A builder for constructing [UiCustomizationParams] instance. */
    public class Builder {
        @AspectRatio private var aspectRatio: Int = ASPECT_RATIO_UNDEFINED

        /**
         * Sets the desired aspect ratio for thumbnails in the media grid.
         *
         * @param aspectRatio The aspect ratio constant from [UiCustomizationParams].
         * @return This builder.
         */
        public fun setAspectRatio(@AspectRatio aspectRatio: Int): Builder {
            this.aspectRatio = aspectRatio
            return this
        }

        /**
         * Builds a new [UiCustomizationParams] object.
         *
         * @return The newly constructed UiCustomizationParams instance.
         */
        public fun build(): UiCustomizationParams =
            UiCustomizationParams().apply {
                this.aspectRatio = this@Builder.aspectRatio
            }
    }
}
