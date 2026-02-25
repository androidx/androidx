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
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.config.AudioConfigUtil.outputFormatToAudioMime
import androidx.camera.video.internal.config.VideoConfigUtil.getDynamicRangeDefaultMime
import androidx.camera.video.internal.config.VideoConfigUtil.outputFormatToVideoMime
import androidx.camera.video.internal.muxer.Muxer
import androidx.camera.video.internal.utils.CodecUtil

public object MediaConfigUtil {
    private const val TAG = "MediaConfigUtil"

    private const val OUTPUT_FORMAT_DEFAULT = OUTPUT_FORMAT_MPEG_4

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
    ): MediaInfo {
        Logger.d(
            TAG,
            "Resolving MediaInfo for MediaSpec: $mediaSpec, " +
                "DynamicRange: $dynamicRange, EncoderProfiles: $encoderProfiles",
        )
        val outputFormat = mediaSpec.outputFormat
        val videoMime = mediaSpec.videoSpec.mimeType
        val audioMime = mediaSpec.audioSpec.mimeType

        // Step 1: Trust EncoderProfiles if it fully matches requirements
        // If all formats are UNSPECIFIED and encoderProfiles exists, it is definitely fully
        // compatible.
        resolveByEncoderProfiles(
                encoderProfiles = encoderProfiles,
                dynamicRange = dynamicRange,
                outputFormat = outputFormat,
                videoMime = videoMime,
                audioMime = audioMime,
            )
            ?.let {
                return it
            }

        // Step 2: Fall back to the FormatComboRegistry mapping
        resolveByFormatCombo(
                encoderProfiles = encoderProfiles,
                dynamicRange = dynamicRange,
                outputFormat = outputFormat,
                videoMime = videoMime,
                audioMime = audioMime,
            )
            ?.let {
                return it
            }

        // Step 3: Fallback to the default
        return resolveByDefault(
            encoderProfiles = encoderProfiles,
            dynamicRange = dynamicRange,
            outputFormat = outputFormat,
            videoMime = videoMime,
            audioMime = audioMime,
        )
    }

    private fun resolveByEncoderProfiles(
        encoderProfiles: EncoderProfilesProxy?,
        dynamicRange: DynamicRange,
        @OutputFormat outputFormat: Int,
        videoMime: String,
        audioMime: String,
    ): MediaInfo? {
        val compatibleProfiles =
            resolveCompatibleProfiles(
                    encoderProfiles = encoderProfiles,
                    dynamicRange = dynamicRange,
                    outputFormat = outputFormat,
                    videoMime = videoMime,
                    audioMime = audioMime,
                )
                .also { Logger.d(TAG, "Resolved CompatibleProfiles: $it") }
                .takeIf { it.isFullyCompatible } ?: return null

        return compatibleProfiles.toMediaInfo().also {
            Logger.d(TAG, "Resolved MediaInfo by CompatibleProfiles: $it")
        }
    }

    private fun resolveByFormatCombo(
        encoderProfiles: EncoderProfilesProxy?,
        dynamicRange: DynamicRange,
        @OutputFormat outputFormat: Int,
        videoMime: String,
        audioMime: String,
    ): MediaInfo? {
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

        val compatibleProfiles =
            resolveCompatibleProfiles(
                encoderProfiles = encoderProfiles,
                dynamicRange = dynamicRange,
                outputFormat = formatCombo.container,
                videoMime = formatCombo.videoMime!!,
                audioMime = formatCombo.audioMime ?: AudioSpec.MIME_TYPE_UNSPECIFIED,
            )

        return MediaInfo(
                containerInfo =
                    ContainerInfo(
                        outputFormat = formatCombo.container,
                        compatibleEncoderProfiles = compatibleProfiles.encoderProfiles,
                    ),
                videoMimeInfo =
                    VideoMimeInfo(
                        mimeType = formatCombo.videoMime,
                        compatibleVideoProfile = compatibleProfiles.videoProfile,
                    ),
                audioMimeInfo =
                    formatCombo.audioMime?.let {
                        AudioMimeInfo(
                            mimeType = it,
                            profile = AudioConfigUtil.audioMimeToAudioProfile(it),
                            compatibleAudioProfile = compatibleProfiles.audioProfile,
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
        Logger.d(
            TAG,
            "resolveFormatCombo - supportedVideoEncoderMimes: $supportedVideoEncoderMimes" +
                ", supportedAudioEncoderMimes: $supportedAudioEncoderMimes",
        )
        val registry = DynamicRangeFormatComboRegistry.getRegistry(dynamicRange) ?: return null

        val eligibleFormatCombos =
            registry
                .getCombos(
                    outputFormat = outputFormat,
                    videoMime = videoMime,
                    audioMime = audioMime,
                )
                .filter { combo ->
                    // Remove Audio-Only combos since currently CameraX doesn't support Audio-Only
                    // recording
                    combo.videoMime != null
                }
                .also { Logger.d(TAG, "eligibleFormatCombos: $it") }

        if (eligibleFormatCombos.isEmpty()) return null

        // Define selection priority
        return eligibleFormatCombos.firstOrNull { combo ->
            // Priority 1: Both Video and Audio encoders are supported
            supportedVideoEncoderMimes.contains(combo.videoMime) &&
                supportedAudioEncoderMimes.contains(combo.audioMime)
        }
            ?: eligibleFormatCombos.firstOrNull { combo ->
                // Priority 2: Video is supported and combo is Video-Only
                supportedVideoEncoderMimes.contains(combo.videoMime) && combo.audioMime == null
            }
            ?: eligibleFormatCombos.firstOrNull() // Priority 3: Fallback to first available
    }

    private fun resolveByDefault(
        encoderProfiles: EncoderProfilesProxy?,
        dynamicRange: DynamicRange,
        @OutputFormat outputFormat: Int,
        videoMime: String,
        audioMime: String,
    ): MediaInfo {
        val resolvedOutputFormat =
            outputFormat.takeIf { it != OUTPUT_FORMAT_UNSPECIFIED } ?: OUTPUT_FORMAT_DEFAULT

        val resolvedVideoMime =
            videoMime.takeIf { it != VideoSpec.MIME_TYPE_UNSPECIFIED }
                ?: getDynamicRangeDefaultMime(dynamicRange)
                ?: outputFormatToVideoMime(outputFormat)

        val resolvedAudioMime =
            audioMime.takeIf { it != AudioSpec.MIME_TYPE_UNSPECIFIED }
                ?: outputFormatToAudioMime(outputFormat)

        val compatibleProfiles =
            resolveCompatibleProfiles(
                encoderProfiles = encoderProfiles,
                dynamicRange = dynamicRange,
                outputFormat = resolvedOutputFormat,
                videoMime = resolvedVideoMime,
                audioMime = resolvedAudioMime,
            )

        return MediaInfo(
                containerInfo =
                    ContainerInfo(
                        outputFormat = resolvedOutputFormat,
                        compatibleEncoderProfiles = compatibleProfiles.encoderProfiles,
                    ),
                videoMimeInfo =
                    VideoMimeInfo(
                        mimeType = resolvedVideoMime,
                        compatibleVideoProfile = compatibleProfiles.videoProfile,
                    ),
                audioMimeInfo =
                    AudioMimeInfo(
                        mimeType = resolvedAudioMime,
                        profile = AudioConfigUtil.audioMimeToAudioProfile(resolvedAudioMime),
                        compatibleAudioProfile = compatibleProfiles.audioProfile,
                    ),
            )
            .also { Logger.d(TAG, "Resolved MediaInfo by Default: $it") }
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

    @JvmStatic
    public fun outputFormatToMuxerFormat(@OutputFormat outputFormat: Int): Int {
        return when (outputFormat) {
            OUTPUT_FORMAT_WEBM -> Muxer.MUXER_FORMAT_WEBM
            else -> Muxer.MUXER_FORMAT_MPEG_4
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
