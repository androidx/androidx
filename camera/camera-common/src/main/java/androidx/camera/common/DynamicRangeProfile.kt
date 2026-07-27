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

import android.hardware.camera2.params.DynamicRangeProfiles as Camera2DynamicRangeProfiles
import androidx.annotation.LongDef
import androidx.annotation.RestrictTo

/**
 * The dynamic range profiles supported by the camera device.
 *
 * These profiles are used to configure 10-bit or 8-bit HDR capture streams.
 *
 * Values are defined in [android.hardware.camera2.params.DynamicRangeProfiles] (e.g.,
 * [Camera2DynamicRangeProfiles.STANDARD]).
 */
@LongDef(
    Camera2DynamicRangeProfiles.STANDARD,
    Camera2DynamicRangeProfiles.HLG10,
    Camera2DynamicRangeProfiles.HDR10,
    Camera2DynamicRangeProfiles.HDR10_PLUS,
    Camera2DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF,
    Camera2DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF_PO,
    Camera2DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM,
    Camera2DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM_PO,
    Camera2DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF,
    Camera2DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF_PO,
    Camera2DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM,
    Camera2DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM_PO,
    Camera2DynamicRangeProfiles.PUBLIC_MAX,
)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public annotation class DynamicRangeProfile
