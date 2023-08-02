/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.camera2.pipe.integration.adapter

import android.media.CamcorderProfile
import android.os.Build
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.camera.core.Logger
import androidx.camera.core.impl.CamcorderProfileProvider
import androidx.camera.core.impl.CamcorderProfileProxy

/**
 * Adapt the [CamcorderProfileProvider] interface to [CameraPipe].
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CamcorderProfileProviderAdapter(cameraIdString: String) : CamcorderProfileProvider {
    private val hasValidCameraId: Boolean
    private val cameraId: Int

    init {
        var hasValidCameraId = false
        var intCameraId = -1
        try {
            intCameraId = cameraIdString.toInt()
            hasValidCameraId = true
        } catch (e: NumberFormatException) {
            Logger.w(
                TAG,
                "Camera id is not an integer: " +
                    "$cameraIdString, unable to create CamcorderProfileProvider"
            )
        }
        this.hasValidCameraId = hasValidCameraId
        cameraId = intCameraId

        // TODO(b/241296464): CamcorderProfileResolutionQuirk and CamcorderProfileResolutionValidator
    }

    override fun hasProfile(quality: Int): Boolean {
        if (!hasValidCameraId) {
            return false
        }
        return CamcorderProfile.hasProfile(cameraId, quality)
        // TODO: b241296464 CamcorderProfileResolutionQuirk and
        // CamcorderProfileResolutionValidator. If has quick, check if the proxy profile has
        // valid video resolution
    }

    override fun get(quality: Int): CamcorderProfileProxy? {
        if (!hasValidCameraId) {
            return null
        }
        return if (!CamcorderProfile.hasProfile(cameraId, quality)) {
            null
        } else getProfileInternal(quality)
        // TODO: b241296464 CamcorderProfileResolutionQuirk and
        // CamcorderProfileResolutionValidator. If has quick, check if the proxy profile has
        // valid video resolution
    }

    @Nullable
    @Suppress("DEPRECATION")
    private fun getProfileInternal(quality: Int): CamcorderProfileProxy? {
        var profile: CamcorderProfile? = null
        try {
            profile = CamcorderProfile.get(cameraId, quality)
        } catch (e: RuntimeException) {
            // CamcorderProfile.get() will throw
            // - RuntimeException if not able to retrieve camcorder profile params.
            // - IllegalArgumentException if quality is not valid.
            Logger.w(TAG, "Unable to get CamcorderProfile by quality: $quality", e)
        }
        return if (profile != null) CamcorderProfileProxy.fromCamcorderProfile(profile) else null
    }

    companion object {
        private const val TAG = "CamcorderProfileProviderAdapter"
    }
}