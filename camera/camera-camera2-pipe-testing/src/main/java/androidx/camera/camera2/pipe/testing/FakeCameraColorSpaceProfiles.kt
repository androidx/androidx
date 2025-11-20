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

package androidx.camera.camera2.pipe.testing

import androidx.camera.camera2.pipe.CameraColorSpace
import androidx.camera.camera2.pipe.CameraColorSpaceProfiles
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import kotlin.reflect.KClass

/** A fake implementation of [CameraColorSpaceProfiles] for testing. */
public class FakeCameraColorSpaceProfiles(private val profileTable: List<Profile> = emptyList()) :
    CameraColorSpaceProfiles {

    override fun getSupportedColorSpaces(imageFormat: StreamFormat): Set<CameraColorSpace> {
        return profileTable.filter { it.format == imageFormat }.map { it.colorSpace }.toSet()
    }

    override fun getSupportedImageFormatsForColorSpace(
        cameraColorSpace: CameraColorSpace
    ): Set<StreamFormat> {
        return profileTable.filter { it.colorSpace == cameraColorSpace }.map { it.format }.toSet()
    }

    override fun getSupportedDynamicRangeProfiles(
        cameraColorSpace: CameraColorSpace,
        imageFormat: StreamFormat,
    ): Set<OutputStream.DynamicRangeProfile> {
        return profileTable
            .filter { it.colorSpace == cameraColorSpace && it.format == imageFormat }
            .map { it.profile }
            .toSet()
    }

    override fun getSupportedColorSpacesForDynamicRange(
        imageFormat: StreamFormat,
        dynamicRangeProfile: OutputStream.DynamicRangeProfile,
    ): Set<CameraColorSpace> {
        return profileTable
            .filter { it.format == imageFormat && it.profile == dynamicRangeProfile }
            .map { it.colorSpace }
            .toSet()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        return when (type) {
            FakeCameraColorSpaceProfiles::class -> this as T
            else -> null
        }
    }

    /** Defines a single entry in the [FakeCameraColorSpaceProfiles] table for testing purposes. */
    public data class Profile(
        internal val format: StreamFormat,
        internal val colorSpace: CameraColorSpace,
        internal val profile: OutputStream.DynamicRangeProfile,
    )
}
