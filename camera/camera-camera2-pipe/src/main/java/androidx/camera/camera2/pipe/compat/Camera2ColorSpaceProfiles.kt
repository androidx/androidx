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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.params.ColorSpaceProfiles
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraColorSpace
import androidx.camera.camera2.pipe.CameraColorSpaceProfiles
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.core.Log
import kotlin.reflect.KClass

/**
 * Implementation of the color space profile interface using Camera2 library.
 *
 * @see CameraColorSpaceProfiles
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class Camera2ColorSpaceProfiles(private val colorSpaceProfiles: ColorSpaceProfiles) :
    CameraColorSpaceProfiles {

    override fun getSupportedColorSpaces(imageFormat: StreamFormat): Set<CameraColorSpace> =
        colorSpaceProfiles
            .getSupportedColorSpaces(imageFormat.value)
            .mapNotNull { colorSpaceNamed ->
                val cameraColorSpace = CameraColorSpace.fromColorSpaceNamed(colorSpaceNamed)
                if (cameraColorSpace == null) {
                    Log.warn { "Unsupported color space: ${colorSpaceNamed.name}" }
                }
                cameraColorSpace
            }
            .toSet()

    override fun getSupportedImageFormatsForColorSpace(
        cameraColorSpace: CameraColorSpace
    ): Set<StreamFormat> {
        val colorSpaceNamed = cameraColorSpace.toColorSpaceNamed()
        if (colorSpaceNamed == null) {
            Log.warn { "Unsupported color space: ${cameraColorSpace.colorSpaceName}" }
            return emptySet()
        }
        return colorSpaceProfiles
            .getSupportedImageFormatsForColorSpace(colorSpaceNamed)
            .map { StreamFormat(it) }
            .toSet()
    }

    override fun getSupportedDynamicRangeProfiles(
        cameraColorSpace: CameraColorSpace,
        imageFormat: StreamFormat,
    ): Set<OutputStream.DynamicRangeProfile> {
        val colorSpaceNamed = cameraColorSpace.toColorSpaceNamed()
        if (colorSpaceNamed == null) {
            Log.warn { "Unsupported color space: ${cameraColorSpace.colorSpaceName}" }
            return emptySet()
        }
        return colorSpaceProfiles
            .getSupportedDynamicRangeProfiles(colorSpaceNamed, imageFormat.value)
            .map { OutputStream.DynamicRangeProfile(it) }
            .toSet()
    }

    override fun getSupportedColorSpacesForDynamicRange(
        imageFormat: StreamFormat,
        dynamicRangeProfile: OutputStream.DynamicRangeProfile,
    ): Set<CameraColorSpace> =
        colorSpaceProfiles
            .getSupportedColorSpacesForDynamicRange(imageFormat.value, dynamicRangeProfile.value)
            .mapNotNull { colorSpaceNamed ->
                val cameraColorSpace = CameraColorSpace.fromColorSpaceNamed(colorSpaceNamed)
                if (cameraColorSpace == null) {
                    Log.warn { "Unsupported color space: ${colorSpaceNamed.name}" }
                }
                cameraColorSpace
            }
            .toSet()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        return when (type) {
            ColorSpaceProfiles::class -> colorSpaceProfiles as T
            Camera2ColorSpaceProfiles::class -> this as T
            else -> null
        }
    }
}
