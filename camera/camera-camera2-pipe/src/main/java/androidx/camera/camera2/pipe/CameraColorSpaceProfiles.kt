/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo
import kotlin.reflect.KClass

/**
 * A compatibility wrapper for color space profiles.
 *
 * @see android.hardware.camera2.params.ColorSpaceProfiles
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraColorSpaceProfiles : UnsafeWrapper {
    public fun getSupportedColorSpaces(imageFormat: StreamFormat): Set<CameraColorSpace>

    public fun getSupportedImageFormatsForColorSpace(
        cameraColorSpace: CameraColorSpace
    ): Set<StreamFormat>

    public fun getSupportedDynamicRangeProfiles(
        cameraColorSpace: CameraColorSpace,
        imageFormat: StreamFormat,
    ): Set<OutputStream.DynamicRangeProfile>

    public fun getSupportedColorSpacesForDynamicRange(
        imageFormat: StreamFormat,
        dynamicRangeProfile: OutputStream.DynamicRangeProfile,
    ): Set<CameraColorSpace>
}

/** Returned for [CameraColorSpaceProfiles] on devices that do not support them. */
internal object UnsupportedCameraColorSpaceProfiles : CameraColorSpaceProfiles {
    override fun getSupportedColorSpaces(imageFormat: StreamFormat): Set<CameraColorSpace> =
        emptySet()

    override fun getSupportedImageFormatsForColorSpace(
        cameraColorSpace: CameraColorSpace
    ): Set<StreamFormat> = emptySet()

    override fun getSupportedDynamicRangeProfiles(
        cameraColorSpace: CameraColorSpace,
        imageFormat: StreamFormat,
    ): Set<OutputStream.DynamicRangeProfile> = emptySet()

    override fun getSupportedColorSpacesForDynamicRange(
        imageFormat: StreamFormat,
        dynamicRangeProfile: OutputStream.DynamicRangeProfile,
    ): Set<CameraColorSpace> = emptySet()

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = null
}
