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

package androidx.camera.video.internal

import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.video.MediaConstants.MIME_TYPE_UNSPECIFIED

/** An [EncoderProfilesProvider] that filters profiles by a specific MIME type. */
internal class MimeMatchedEncoderProfilesProvider(
    private val baseProvider: EncoderProfilesProvider,
    private val videoMime: String = MIME_TYPE_UNSPECIFIED,
    private val audioMime: String = MIME_TYPE_UNSPECIFIED,
) : EncoderProfilesProvider {

    private val profilesCache = mutableMapOf<Int, EncoderProfilesProxy?>()

    override fun hasProfile(quality: Int): Boolean = getAll(quality) != null

    override fun getAll(quality: Int): EncoderProfilesProxy? {
        return synchronized(profilesCache) {
            profilesCache.getOrPut(quality) {
                baseProvider.getAll(quality)?.let { filterProfiles(it) }
            }
        }
    }

    private fun filterProfiles(profiles: EncoderProfilesProxy): EncoderProfilesProxy? {
        // If both are UNSPECIFIED, return original to avoid unnecessary object creation
        if (videoMime == MIME_TYPE_UNSPECIFIED && audioMime == MIME_TYPE_UNSPECIFIED) {
            return profiles
        }

        val matchedVideo =
            profiles.videoProfiles.filter {
                videoMime == MIME_TYPE_UNSPECIFIED || it.mediaType == videoMime
            }

        val matchedAudio =
            profiles.audioProfiles.filter {
                audioMime == MIME_TYPE_UNSPECIFIED || it.mediaType == audioMime
            }

        // For optimization, if the filtered video and audio profiles remain unchanged, the
        // original profiles is returned.
        if (
            matchedVideo.size == profiles.videoProfiles.size &&
                matchedAudio.size == profiles.audioProfiles.size
        ) {
            return profiles
        }

        if (matchedVideo.isEmpty() && matchedAudio.isEmpty()) {
            return null
        }

        return EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
            profiles.defaultDurationSeconds,
            profiles.recommendedFileFormat,
            matchedAudio,
            matchedVideo,
        )
    }
}
