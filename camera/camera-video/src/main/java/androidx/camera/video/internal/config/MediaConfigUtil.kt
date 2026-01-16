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

package androidx.camera.video.internal.config

import android.media.MediaRecorder.OutputFormat.MPEG_4
import android.media.MediaRecorder.OutputFormat.THREE_GPP
import android.media.MediaRecorder.OutputFormat.WEBM
import androidx.annotation.VisibleForTesting
import androidx.camera.core.DynamicRange
import androidx.camera.core.Logger
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.video.AudioSpec
import androidx.camera.video.MediaSpec
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_MPEG_4
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_UNSPECIFIED
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_WEBM
import androidx.camera.video.MediaSpec.OutputFormat
import androidx.camera.video.internal.utils.CodecUtil

public object MediaConfigUtil {
    private const val TAG = "MediaConfigUtil"

    private var supportedVideoEncoderMimesOverride: List<String>? = null
    private var supportedAudioEncoderMimesOverride: List<String>? = null

    @VisibleForTesting
    public fun setSupportedEncoderMimeTypes(videoMimes: List<String>?, audioMimes: List<String>?) {
        supportedVideoEncoderMimesOverride = videoMimes
        supportedAudioEncoderMimesOverride = audioMimes
    }

    private fun getSupportedVideoEncoderMimes() =
        supportedVideoEncoderMimesOverride ?: CodecUtil.getVideoEncoderMimeTypes()

    private fun getSupportedAudioEncoderMimes() =
        supportedAudioEncoderMimesOverride ?: CodecUtil.getAudioEncoderMimeTypes()

    @JvmStatic
    public fun resolveMediaInfo(
        mediaSpec: MediaSpec,
        dynamicRange: DynamicRange,
        encoderProfiles: EncoderProfilesProxy?,
    ): MediaInfo? {
        Logger.d(
            TAG,
            "Resolving MediaInfo for MediaSpec: $mediaSpec, " +
                "DynamicRange: $dynamicRange, EncoderProfiles: $encoderProfiles",
        )
        val outputFormat = mediaSpec.outputFormat
        val videoMime = mediaSpec.videoSpec.mimeType
        val audioMime = mediaSpec.audioSpec.mimeType

        val compatibleProfiles =
            resolveCompatibleProfiles(
                    encoderProfiles = encoderProfiles,
                    dynamicRange = dynamicRange,
                    outputFormat = outputFormat,
                    videoMime = videoMime,
                    audioMime = audioMime,
                )
                .also { Logger.d(TAG, "Resolved CompatibleProfiles: $it") }

        // Step 1: Trust EncoderProfiles if it fully matches requirements
        // If all formats are UNSPECIFIED and encoderProfiles exists, it is definitely fully
        // compatible.
        if (compatibleProfiles.isFullyCompatible) {
            return compatibleProfiles.toMediaInfo().also {
                Logger.d(TAG, "Resolved MediaInfo by fully CompatibleProfiles: $it")
            }
        }

        // Step 2: Fall back to the FormatComboRegistry mapping
        val supportedVideoMimes = getSupportedVideoEncoderMimes()
        val supportedAudioMimes = getSupportedAudioEncoderMimes()
        val formatCombo =
            resolveFormatCombo(
                    dynamicRange = dynamicRange,
                    outputFormat = outputFormat,
                    videoMime = videoMime,
                    audioMime = audioMime,
                    supportedVideoEncoderMimes = supportedVideoMimes,
                    supportedAudioEncoderMimes = supportedAudioMimes,
                )
                .also { Logger.d(TAG, "Resolved FormatCombo: $it") } ?: return null

        // Step 3: Find compatible profiles based on resolved formatCombo
        val formatComboCompatibleProfiles =
            resolveCompatibleProfiles(
                    encoderProfiles = encoderProfiles,
                    dynamicRange = dynamicRange,
                    outputFormat = formatCombo.container,
                    videoMime = formatCombo.videoMime!!,
                    audioMime = formatCombo.audioMime ?: AudioSpec.MIME_TYPE_UNSPECIFIED,
                )
                .also { Logger.d(TAG, "Resolved FormatCombo CompatibleProfiles: $it") }

        return MediaInfo(
                containerInfo =
                    ContainerInfo(
                        outputFormat = formatCombo.container,
                        compatibleEncoderProfiles = formatComboCompatibleProfiles.encoderProfiles,
                    ),
                videoMimeInfo =
                    VideoMimeInfo(
                        mimeType = formatCombo.videoMime,
                        compatibleVideoProfile = formatComboCompatibleProfiles.videoProfile,
                    ),
                audioMimeInfo =
                    formatCombo.audioMime?.let {
                        AudioMimeInfo(
                            mimeType = it,
                            profile = AudioConfigUtil.audioMimeToAudioProfile(it),
                            compatibleAudioProfile = formatComboCompatibleProfiles.audioProfile,
                        )
                    },
            )
            .also { Logger.d(TAG, "Resolved MediaInfo by FormatCombo: $it") }
    }

    private fun resolveFormatCombo(
        @OutputFormat outputFormat: Int,
        videoMime: String,
        audioMime: String,
        dynamicRange: DynamicRange,
        supportedVideoEncoderMimes: List<String>,
        supportedAudioEncoderMimes: List<String>,
    ): FormatCombo? {
        val registry = DynamicRangeFormatComboRegistry.getRegistry(dynamicRange) ?: return null

        return registry
            .getCombos(outputFormat = outputFormat, videoMime = videoMime, audioMime = audioMime)
            .filter { combo ->
                // Remove Audio-Only combos since currently CameraX doesn't support Audio-Only
                // recording
                combo.videoMime != null
            }
            .firstOrNull { combo ->
                // Note: The registry appends the 'Video-Only' variant (i.e. audioMime == null)
                // after the mixed (Video + Audio) combos, which ensures that the mixed combo is
                // prioritized.
                supportedVideoEncoderMimes.contains(combo.videoMime) &&
                    (supportedAudioEncoderMimes.contains(combo.audioMime) ||
                        combo.audioMime == null)
            }
    }

    private fun resolveCompatibleProfiles(
        encoderProfiles: EncoderProfilesProxy?,
        dynamicRange: DynamicRange,
        @OutputFormat outputFormat: Int,
        videoMime: String,
        audioMime: String,
    ): CompatibleProfiles {
        encoderProfiles ?: return CompatibleProfiles.EMPTY

        val compatibleEncoderProfiles =
            resolveCompatibleEncoderProfiles(outputFormat = outputFormat, encoderProfiles)

        val compatibleVideoProfile =
            VideoConfigUtil.resolveCompatibleVideoProfile(
                videoMime = videoMime,
                dynamicRange = dynamicRange,
                videoProfiles = encoderProfiles.videoProfiles,
            )

        val compatibleAudioProfile =
            AudioConfigUtil.resolveCompatibleAudioProfile(
                audioMime = audioMime,
                audioProfiles = encoderProfiles.audioProfiles,
            )

        return CompatibleProfiles(
            encoderProfiles = compatibleEncoderProfiles,
            videoProfile = compatibleVideoProfile,
            audioProfile = compatibleAudioProfile,
        )
    }

    private fun resolveCompatibleEncoderProfiles(
        @OutputFormat outputFormat: Int,
        encoderProfiles: EncoderProfilesProxy?,
    ): EncoderProfilesProxy? {
        encoderProfiles ?: return null
        return encoderProfiles.takeIf {
            outputFormat == OUTPUT_FORMAT_UNSPECIFIED ||
                outputFormat == mediaRecorderFormatToOutputFormat(it.recommendedFileFormat)
        }
    }

    @OutputFormat
    private fun mediaRecorderFormatToOutputFormat(mediaRecorderFormat: Int): Int {
        return when (mediaRecorderFormat) {
            WEBM -> OUTPUT_FORMAT_WEBM
            MPEG_4,
            THREE_GPP -> OUTPUT_FORMAT_MPEG_4
            else -> OUTPUT_FORMAT_UNSPECIFIED
        }
    }

    private data class CompatibleProfiles(
        val encoderProfiles: EncoderProfilesProxy? = null,
        val videoProfile: VideoProfileProxy? = null,
        val audioProfile: AudioProfileProxy? = null,
    ) {
        companion object {
            val EMPTY = CompatibleProfiles()
        }

        val isFullyCompatible: Boolean
            get() = encoderProfiles != null && videoProfile != null && audioProfile != null

        fun toMediaInfo(): MediaInfo {
            check(isFullyCompatible)
            return MediaInfo(
                containerInfo =
                    ContainerInfo(
                        outputFormat =
                            mediaRecorderFormatToOutputFormat(
                                encoderProfiles!!.recommendedFileFormat
                            ),
                        compatibleEncoderProfiles = encoderProfiles,
                    ),
                videoMimeInfo =
                    VideoMimeInfo(
                        mimeType = videoProfile!!.mediaType,
                        compatibleVideoProfile = videoProfile,
                    ),
                audioMimeInfo =
                    AudioMimeInfo(
                        mimeType = audioProfile!!.mediaType,
                        profile = audioProfile.profile,
                        compatibleAudioProfile = audioProfile,
                    ),
            )
        }
    }
}
