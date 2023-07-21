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
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.InvalidVideoProfilesQuirk
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.Logger
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.compat.EncoderProfilesProxyCompat
import javax.inject.Inject
import javax.inject.Named

/**
 * Adapt the [EncoderProfilesProvider] interface to [CameraPipe].
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@CameraScope
class EncoderProfilesProviderAdapter @Inject constructor(
    @Named("CameraId") private val cameraIdString: String,
) : EncoderProfilesProvider {
    private val hasValidCameraId: Boolean
    private val cameraId: Int
    private val mEncoderProfilesCache: MutableMap<Int, EncoderProfilesProxy?> = mutableMapOf()

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
    }

    override fun hasProfile(quality: Int): Boolean {
        if (!hasValidCameraId) {
            return false
        }

        if (!CamcorderProfile.hasProfile(cameraId, quality)) {
            return false
        }
        return true
    }

    override fun getAll(quality: Int): EncoderProfilesProxy? {
        if (!hasValidCameraId) {
            return null
        }
        if (!CamcorderProfile.hasProfile(cameraId, quality)) {
            return null
        }

        // Cache the value on first query, and reuse the result in subsequent queries.
        return if (mEncoderProfilesCache.containsKey(quality)) {
            mEncoderProfilesCache[quality]
        } else {
            val profiles = getProfilesInternal(quality)
            mEncoderProfilesCache[quality] = profiles
            profiles
        }
    }

    @Nullable
    private fun getProfilesInternal(quality: Int): EncoderProfilesProxy? {
        if (Build.VERSION.SDK_INT >= 31) {
            val profiles: EncoderProfiles = Api31Impl.getAll(cameraIdString, quality) ?: return null

            val isVideoProfilesInvalid = DeviceQuirks[InvalidVideoProfilesQuirk::class.java] != null
            if (isVideoProfilesInvalid) {
                Logger.d(
                    TAG, "EncoderProfiles contains invalid video profiles, use " +
                        "CamcorderProfile to create EncoderProfilesProxy."
                )
            } else {
                try {
                    return EncoderProfilesProxyCompat.from(profiles)
                } catch (e: NullPointerException) {
                    Logger.w(
                        TAG, "Failed to create EncoderProfilesProxy, EncoderProfiles might " +
                            "contain invalid video profiles. Use CamcorderProfile instead.", e
                    )
                }
            }
        }

        return createProfilesFromCamcorderProfile(quality)
    }

    @Nullable
    @Suppress("DEPRECATION")
    private fun createProfilesFromCamcorderProfile(quality: Int): EncoderProfilesProxy? {
        var profile: CamcorderProfile? = null
        try {
            profile = CamcorderProfile.get(cameraId, quality)
        } catch (e: RuntimeException) {
            // CamcorderProfile.get() will throw
            // - RuntimeException if not able to retrieve camcorder profile params.
            // - IllegalArgumentException if quality is not valid.
            Logger.w(TAG, "Unable to get CamcorderProfile by quality: $quality", e)
        }
        return if (profile != null) EncoderProfilesProxyCompat.from(profile) else null
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