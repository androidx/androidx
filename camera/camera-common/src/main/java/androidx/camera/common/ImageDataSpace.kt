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

package androidx.camera.common

import android.hardware.DataSpace
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Represents the data space of an image.
 *
 * @see [android.hardware.DataSpace]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    flag = false,
    value =
        [
            DataSpace.DATASPACE_DEPTH,
            DataSpace.DATASPACE_DYNAMIC_DEPTH,
            DataSpace.DATASPACE_HEIF,
            DataSpace.DATASPACE_JPEG_R,
            DataSpace.DATASPACE_UNKNOWN,
            DataSpace.DATASPACE_SCRGB_LINEAR,
            DataSpace.DATASPACE_SRGB,
            DataSpace.DATASPACE_SCRGB,
            DataSpace.DATASPACE_DISPLAY_P3,
            DataSpace.DATASPACE_BT2020_HLG,
            DataSpace.DATASPACE_BT2020_PQ,
            DataSpace.DATASPACE_ADOBE_RGB,
            DataSpace.DATASPACE_JFIF,
            DataSpace.DATASPACE_BT601_625,
            DataSpace.DATASPACE_BT601_525,
            DataSpace.DATASPACE_BT2020,
            DataSpace.DATASPACE_BT709,
            DataSpace.DATASPACE_DCI_P3,
            DataSpace.DATASPACE_SRGB_LINEAR,
        ],
)
public annotation class ImageDataSpace
