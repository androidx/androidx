/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround

import android.media.CamcorderProfile.QUALITY_HIGH
import android.media.CamcorderProfile.QUALITY_LOW
import android.util.Size
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProvider.QUALITY_HIGH_TO_LOW
import androidx.camera.core.impl.EncoderProfilesProxy

/**
 * An [EncoderProfilesProvider] that filters profiles based on supported sizes.
 *
 * This class wraps another [EncoderProfilesProvider] and filters its output to only include
 * profiles with resolutions that are supported by the camera, as indicated by the provided list of
 * [supportedSizes].
 */
public class SizeFilteredEncoderProfilesProvider(
    /** The original [EncoderProfilesProvider] to wrap. */
    private val provider: EncoderProfilesProvider,

    /** The list of supported sizes. */
    private val supportedSizes: List<Size>,
) : EncoderProfilesProvider {

    private val encoderProfilesCache = mutableMapOf<Int, EncoderProfilesProxy?>()

    override fun hasProfile(quality: Int): Boolean {
        return getAll(quality) != null
    }

    override fun getAll(quality: Int): EncoderProfilesProxy? {
        if (!provider.hasProfile(quality)) {
            return null
        }

        if (encoderProfilesCache.containsKey(quality)) {
            return encoderProfilesCache[quality]
        }

        var profiles = provider.getAll(quality)
        if (profiles != null && !isResolutionSupported(profiles)) {
            profiles =
                when (quality) {
                    QUALITY_HIGH -> findFirstAvailableProfile(QUALITY_HIGH_TO_LOW)
                    QUALITY_LOW -> findFirstAvailableProfile(QUALITY_HIGH_TO_LOW.reversed())
                    else -> null
                }
        }

        encoderProfilesCache[quality] = profiles
        return profiles
    }

    /**
     * Checks if the resolution of the given [EncoderProfilesProxy] is supported.
     *
     * @param profiles The [EncoderProfilesProxy] to check.
     * @return `true` if the resolution is supported, `false` otherwise.
     */
    private fun isResolutionSupported(profiles: EncoderProfilesProxy): Boolean {
        if (supportedSizes.isEmpty() || profiles.videoProfiles.isEmpty()) {
            return false
        }

        // cts/CamcorderProfileTest.java ensures all video profiles have the same size so we just
        // need to check the first video profile.
        val videoProfile = profiles.videoProfiles[0]
        return supportedSizes.contains(videoProfile.resolution)
    }

    /**
     * Finds the first available profile based on the given quality order.
     *
     * This method iterates through the provided [qualityOrder] and returns the first profile that
     * is available.
     *
     * @param qualityOrder The order of qualities to search.
     * @return The first available [EncoderProfilesProxy], or `null` if no suitable profile is
     *   found.
     */
    private fun findFirstAvailableProfile(qualityOrder: List<Int>): EncoderProfilesProxy? {
        for (quality in qualityOrder) {
            getAll(quality)?.let {
                return it
            }
        }
        return null
    }
}
