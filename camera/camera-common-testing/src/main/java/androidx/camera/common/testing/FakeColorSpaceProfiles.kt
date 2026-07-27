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

package androidx.camera.common.testing

import androidx.camera.common.CameraColorSpace
import androidx.camera.common.ColorSpaceProfilesWrapper
import androidx.camera.common.DynamicRangeProfile
import androidx.camera.common.ImageFormat
import java.lang.Class

/**
 * A fake implementation of [ColorSpaceProfilesWrapper] for testing.
 *
 * Allows mock values to be configured for supported color spaces, image formats, and dynamic range
 * profiles.
 */
public class FakeColorSpaceProfiles(
    private val supportedColorSpaces: Map<@ImageFormat Int, Set<@CameraColorSpace String>> =
        emptyMap(),
    private val supportedImageFormats: Map<@CameraColorSpace String, Set<@ImageFormat Int>> =
        emptyMap(),
    private val supportedDynamicRangeProfiles:
        Map<Pair<@CameraColorSpace String, @ImageFormat Int>, Set<@DynamicRangeProfile Long>> =
        emptyMap(),
    private val supportedColorSpacesForDynamicRange:
        Map<Pair<@ImageFormat Int, @DynamicRangeProfile Long>, Set<@CameraColorSpace String>> =
        emptyMap(),
) : ColorSpaceProfilesWrapper {

    override fun getSupportedColorSpaces(
        @ImageFormat imageFormat: Int
    ): Set<@CameraColorSpace String> = supportedColorSpaces[imageFormat].orEmpty()

    override fun getSupportedImageFormatsForColorSpace(
        @CameraColorSpace cameraColorSpace: String
    ): Set<@ImageFormat Int> = supportedImageFormats[cameraColorSpace].orEmpty()

    override fun getSupportedDynamicRangeProfiles(
        @CameraColorSpace cameraColorSpace: String,
        @ImageFormat imageFormat: Int,
    ): Set<@DynamicRangeProfile Long> =
        supportedDynamicRangeProfiles[Pair(cameraColorSpace, imageFormat)].orEmpty()

    override fun getSupportedColorSpacesForDynamicRange(
        @ImageFormat imageFormat: Int,
        @DynamicRangeProfile dynamicRangeProfile: Long,
    ): Set<@CameraColorSpace String> =
        supportedColorSpacesForDynamicRange[Pair(imageFormat, dynamicRangeProfile)].orEmpty()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            else -> null
        }
}
