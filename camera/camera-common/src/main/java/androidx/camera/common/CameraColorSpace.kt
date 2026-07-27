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

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef

/**
 * Represents the color space of a camera stream.
 *
 * Valid values are defined in [CameraColorSpaces].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            CameraColorSpaces.UNKNOWN,
            CameraColorSpaces.SRGB,
            CameraColorSpaces.LINEAR_SRGB,
            CameraColorSpaces.EXTENDED_SRGB,
            CameraColorSpaces.LINEAR_EXTENDED_SRGB,
            CameraColorSpaces.BT709,
            CameraColorSpaces.BT2020,
            CameraColorSpaces.DCI_P3,
            CameraColorSpaces.DISPLAY_P3,
            CameraColorSpaces.NTSC_1953,
            CameraColorSpaces.SMPTE_C,
            CameraColorSpaces.ADOBE_RGB,
            CameraColorSpaces.PRO_PHOTO_RGB,
            CameraColorSpaces.ACES,
            CameraColorSpaces.ACESCG,
            CameraColorSpaces.CIE_XYZ,
            CameraColorSpaces.CIE_LAB,
            CameraColorSpaces.BT2020_HLG,
            CameraColorSpaces.BT2020_PQ,
        ]
)
public annotation class CameraColorSpace
