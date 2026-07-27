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

/**
 * A compatibility wrapper for color space profiles.
 *
 * @see android.hardware.camera2.params.ColorSpaceProfiles
 */
public interface ColorSpaceProfilesWrapper : UnsafeWrapper {

    /**
     * Get the supported color spaces for a given image format.
     *
     * @param imageFormat The image format to query.
     * @return The set of supported [CameraColorSpace]s.
     */
    public fun getSupportedColorSpaces(@ImageFormat imageFormat: Int): Set<@CameraColorSpace String>

    /**
     * Get the supported image formats for a given color space.
     *
     * @param cameraColorSpace The [CameraColorSpace] to query.
     * @return The set of supported image formats.
     */
    public fun getSupportedImageFormatsForColorSpace(
        @CameraColorSpace cameraColorSpace: String
    ): Set<@ImageFormat Int>

    /**
     * Get the supported dynamic range profiles for a given color space and image format.
     *
     * @param cameraColorSpace The [CameraColorSpace] to query.
     * @param imageFormat The image format to query.
     * @return A set of framework dynamic range profiles (e.g.,
     *   [android.hardware.camera2.params.DynamicRangeProfiles.HDR10])
     */
    public fun getSupportedDynamicRangeProfiles(
        @CameraColorSpace cameraColorSpace: String,
        @ImageFormat imageFormat: Int,
    ): Set<@DynamicRangeProfile Long>

    /**
     * Get the supported color spaces for a given image format and dynamic range profile.
     *
     * @param imageFormat The image format to query.
     * @param dynamicRangeProfile The dynamic range profile to query.
     * @return The set of supported [CameraColorSpace]s.
     */
    public fun getSupportedColorSpacesForDynamicRange(
        @ImageFormat imageFormat: Int,
        @DynamicRangeProfile dynamicRangeProfile: Long,
    ): Set<@CameraColorSpace String>
}

/**
 * An implementation of [ColorSpaceProfilesWrapper] that reports no support for any color spaces.
 */
internal object UnsupportedColorSpaceProfiles : ColorSpaceProfilesWrapper {
    override fun getSupportedColorSpaces(
        @ImageFormat imageFormat: Int
    ): Set<@CameraColorSpace String> = emptySet()

    override fun getSupportedImageFormatsForColorSpace(
        @CameraColorSpace cameraColorSpace: String
    ): Set<@ImageFormat Int> = emptySet()

    override fun getSupportedDynamicRangeProfiles(
        @CameraColorSpace cameraColorSpace: String,
        @ImageFormat imageFormat: Int,
    ): Set<@DynamicRangeProfile Long> = emptySet()

    override fun getSupportedColorSpacesForDynamicRange(
        @ImageFormat imageFormat: Int,
        @DynamicRangeProfile dynamicRangeProfile: Long,
    ): Set<@CameraColorSpace String> = emptySet()

    override fun <T : Any> unwrapAs(type: Class<T>): T? = null
}
