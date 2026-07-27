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

package androidx.camera.common.compat

import android.hardware.camera2.params.ColorSpaceProfiles
import androidx.annotation.RequiresApi
import androidx.camera.common.CameraColorSpace
import androidx.camera.common.CameraColorSpaces
import androidx.camera.common.ColorSpaceProfilesWrapper
import androidx.camera.common.DynamicRangeProfile
import androidx.camera.common.ImageFormat
import java.lang.Class

@RequiresApi(34)
@Suppress("WrongConstant")
internal class AndroidColorSpaceProfiles(private val colorSpaceProfiles: ColorSpaceProfiles) :
    ColorSpaceProfilesWrapper {

    override fun getSupportedColorSpaces(
        @ImageFormat imageFormat: Int
    ): Set<@CameraColorSpace String> {
        return colorSpaceProfiles
            .getSupportedColorSpaces(imageFormat)
            .mapNotNull { colorSpaceNamed ->
                val cameraColorSpace = CameraColorSpaces.fromColorSpaceNamed(colorSpaceNamed)
                if (cameraColorSpace == CameraColorSpaces.UNKNOWN) {
                    null
                } else {
                    cameraColorSpace
                }
            }
            .toSet()
    }

    override fun getSupportedImageFormatsForColorSpace(
        @CameraColorSpace cameraColorSpace: String
    ): Set<@ImageFormat Int> {
        val colorSpaceNamed =
            CameraColorSpaces.toColorSpaceNamed(cameraColorSpace) ?: return emptySet()
        return colorSpaceProfiles
            .getSupportedImageFormatsForColorSpace(colorSpaceNamed)
            .map { it }
            .toSet()
    }

    override fun getSupportedDynamicRangeProfiles(
        @CameraColorSpace cameraColorSpace: String,
        @ImageFormat imageFormat: Int,
    ): Set<@DynamicRangeProfile Long> {
        val colorSpaceNamed =
            CameraColorSpaces.toColorSpaceNamed(cameraColorSpace) ?: return emptySet()
        return colorSpaceProfiles
            .getSupportedDynamicRangeProfiles(colorSpaceNamed, imageFormat)
            .map { it }
            .toSet()
    }

    override fun getSupportedColorSpacesForDynamicRange(
        @ImageFormat imageFormat: Int,
        @DynamicRangeProfile dynamicRangeProfile: Long,
    ): Set<@CameraColorSpace String> {
        return colorSpaceProfiles
            .getSupportedColorSpacesForDynamicRange(imageFormat, dynamicRangeProfile)
            .mapNotNull { colorSpaceNamed ->
                val cameraColorSpace = CameraColorSpaces.fromColorSpaceNamed(colorSpaceNamed)
                if (cameraColorSpace == CameraColorSpaces.UNKNOWN) {
                    null
                } else {
                    cameraColorSpace
                }
            }
            .toSet()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type.isInstance(colorSpaceProfiles) -> colorSpaceProfiles as T
            else -> null
        }
}
