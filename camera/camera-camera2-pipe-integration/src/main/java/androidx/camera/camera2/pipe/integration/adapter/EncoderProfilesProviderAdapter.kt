/*
 * Copyright 2023 The Android Open Source Project
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
import android.media.EncoderProfiles
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.camera.core.Logger
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.compat.EncoderProfilesProxyCompat

/**
 * Adapt the [EncoderProfilesProvider] interface to [CameraPipe].
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class EncoderProfilesProviderAdapter(private val cameraIdString: String) : EncoderProfilesProvider {
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
                TAG, "Camera id is not an integer:  $cameraIdString, unable to create" +
                    " EncoderProfilesProviderAdapter."
            )
        }
        this.hasValidCameraId = hasValidCameraId
        cameraId = intCameraId

        // TODO(b/241296464): CamcorderProfileResolutionQuirk
        // TODO(b/265613005): InvalidVideoProfilesQuirk
    }

    override fun hasProfile(quality: Int): Boolean {
        if (!hasValidCameraId) {
            return false
        }
        return CamcorderProfile.hasProfile(cameraId, quality)
    }

    override fun getAll(quality: Int): EncoderProfilesProxy? {
        if (!hasValidCameraId) {
            return null
        }
        if (!CamcorderProfile.hasProfile(cameraId, quality)) {
             return null
        }
        return getProfilesInternal(quality)
    }

    @Nullable
    @Suppress("DEPRECATION")
    private fun getProfilesInternal(quality: Int): EncoderProfilesProxy? {
        return if (Build.VERSION.SDK_INT >= 31) {
            val profiles: EncoderProfiles? = Api31Impl.getAll(cameraIdString, quality)
            if (profiles != null) EncoderProfilesProxyCompat.from(profiles) else null
        } else {
            var profile: CamcorderProfile? = null
            try {
                profile = CamcorderProfile.get(cameraId, quality)
            } catch (e: RuntimeException) {
                // CamcorderProfile.get() will throw
                // - RuntimeException if not able to retrieve camcorder profile params.
                // - IllegalArgumentException if quality is not valid.
                Logger.w(TAG, "Unable to get CamcorderProfile by quality: $quality", e)
            }
            if (profile != null) EncoderProfilesProxyCompat.from(profile) else null
        }
    }

    @RequiresApi(31)
    internal object Api31Impl {
        @DoNotInline
        fun getAll(cameraId: String, quality: Int): EncoderProfiles? {
            return CamcorderProfile.getAll(cameraId, quality)
        }
    }

    companion object {
        private const val TAG = "EncoderProfilesProviderAdapter"
    }
}