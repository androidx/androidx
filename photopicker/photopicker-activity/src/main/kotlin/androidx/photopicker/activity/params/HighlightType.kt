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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Represents the supported highlight types that can be used to highlight album media or search
 * media in the photo picker.
 */
public class HighlightType private constructor() {
    public companion object {
        /** Defines the type of highlight presentation in the photo picker. */
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(COLLAPSED, EXPANDED)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Target(
            AnnotationTarget.TYPE,
            AnnotationTarget.PROPERTY,
            AnnotationTarget.VALUE_PARAMETER,
            AnnotationTarget.PROPERTY_GETTER,
        )
        public annotation class HighlightType

        /**
         * Highlights media results as a highlighted media section in the photo picker based on the
         * search query or the predefined album.
         *
         * @see android.provider.MediaStore.PICK_IMAGES_HIGHLIGHT_TYPE_COLLAPSED
         */
        public const val COLLAPSED: Int = 0

        /**
         * Opens the photo picker to show a full highlighted media results grid based on the
         * requested search query or predefined album.
         *
         * @see android.provider.MediaStore.PICK_IMAGES_HIGHLIGHT_TYPE_EXPANDED
         */
        public const val EXPANDED: Int = 1
    }
}
