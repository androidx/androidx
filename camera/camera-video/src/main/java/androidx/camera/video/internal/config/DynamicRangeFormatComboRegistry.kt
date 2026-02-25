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

import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_AUDIO_AMR_NB
import android.media.MediaFormat.MIMETYPE_AUDIO_AMR_WB
import android.media.MediaFormat.MIMETYPE_AUDIO_OPUS
import android.media.MediaFormat.MIMETYPE_AUDIO_VORBIS
import android.media.MediaFormat.MIMETYPE_VIDEO_APV
import android.media.MediaFormat.MIMETYPE_VIDEO_AV1
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION
import android.media.MediaFormat.MIMETYPE_VIDEO_H263
import android.media.MediaFormat.MIMETYPE_VIDEO_HEVC
import android.media.MediaFormat.MIMETYPE_VIDEO_MPEG4
import android.media.MediaFormat.MIMETYPE_VIDEO_VP8
import android.media.MediaFormat.MIMETYPE_VIDEO_VP9
import android.os.Build
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.DOLBY_VISION_8_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_MPEG_4
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_WEBM

/**
 * A provider that maps [DynamicRange] profiles to their respective [FormatComboRegistry].
 *
 * This registry is primarily used by the internal **auto-selection mechanism** to resolve default
 * configurations (container, video codec, and audio codec) when a developer has not explicitly
 * specified them in the video capture settings.
 *
 * **Note on Requirement:** The combinations defined here are **not hard requirements**. They
 * represent industry-standard, highly-compatible "safe" defaults. This registry should **not** be
 * used to validate or reject explicit user configurations; if a developer provides a specific
 * combination that falls outside of these definitions, the system should attempt to honor it based
 * on hardware capabilities.
 *
 * **Note on API Levels:** High Dynamic Range (HDR) profiles are only included in this registry for
 * **API 33+**. While individual codecs may exist on older versions, the formal 10-bit
 * camera-to-codec pipeline (managed via `DynamicRangeProfiles`) was introduced in Android 13 (API
 * 33).
 */
public object DynamicRangeFormatComboRegistry {

    // --- OS Gated Codec Constants ---
    private val MIMETYPE_VIDEO_HEVC_GATED = MIMETYPE_VIDEO_HEVC.takeIf(minSdk = 24)
    private val MIMETYPE_VIDEO_VP9_GATED = MIMETYPE_VIDEO_VP9.takeIf(minSdk = 24)
    private val MIMETYPE_AUDIO_OPUS_GATED = MIMETYPE_AUDIO_OPUS.takeIf(minSdk = 29)
    private val MIMETYPE_VIDEO_DOLBY_VISION_GATED = MIMETYPE_VIDEO_DOLBY_VISION.takeIf(minSdk = 33)
    private val MIMETYPE_VIDEO_AV1_GATED = MIMETYPE_VIDEO_AV1.takeIf(minSdk = 34)
    private val MIMETYPE_VIDEO_APV_GATED = MIMETYPE_VIDEO_APV.takeIf(minSdk = 36)

    private val registries: Map<DynamicRange, FormatComboRegistry> by lazy {
        mutableMapOf(
            SDR to buildSdrRegistry(),
            HLG_10_BIT to buildHlgRegistry(),
            HDR10_10_BIT to buildHdr10Registry(),
            HDR10_PLUS_10_BIT to buildHdr10PlusRegistry(),
            DOLBY_VISION_8_BIT to buildDolbyVisionRegistry(),
            DOLBY_VISION_10_BIT to buildDolbyVisionRegistry(),
        )
    }

    private val standardMp4Audios by lazy {
        listOfNotNull(MIMETYPE_AUDIO_AAC, MIMETYPE_AUDIO_AMR_NB, MIMETYPE_AUDIO_AMR_WB)
    }

    private val standardWebmAudios by lazy {
        listOfNotNull(MIMETYPE_AUDIO_VORBIS, MIMETYPE_AUDIO_OPUS_GATED)
    }

    /**
     * Resolves the [FormatComboRegistry] for a given [DynamicRange].
     *
     * @param dynamicRange The profile to look up (e.g., SDR, HLG_10_BIT).
     * @return The corresponding registry, or `null` if the profile has no defined capabilities.
     */
    public fun getRegistry(dynamicRange: DynamicRange): FormatComboRegistry? {
        return registries[dynamicRange]
    }

    /**
     * Returns the supported [DynamicRange]s for the given video MIME type.
     *
     * @param videoMime The video MIME type to query.
     * @return A [Set] of [DynamicRange] compatible with the video MIME type.
     */
    public fun getDynamicRangesForVideoMime(videoMime: String): Set<DynamicRange> {
        val supportedRanges = mutableSetOf<DynamicRange>()

        for ((range, registry) in registries) {
            // Check only for video support in this registry
            if (registry.getCombosForVideo(videoMime).isNotEmpty()) {
                supportedRanges.add(range)
            }
        }

        return supportedRanges
    }

    /**
     * Reference:
     * https://developer.android.com/reference/android/media/MediaMuxer#addTrack(android.media.MediaFormat)
     */
    private fun buildSdrRegistry(): FormatComboRegistry =
        FormatComboRegistry.Builder()
            .apply {
                container(OUTPUT_FORMAT_MPEG_4) {
                    support(
                        videoMimes =
                            listOfNotNull(
                                MIMETYPE_VIDEO_AVC,
                                MIMETYPE_VIDEO_MPEG4,
                                MIMETYPE_VIDEO_H263,
                                MIMETYPE_VIDEO_HEVC_GATED,
                                MIMETYPE_VIDEO_DOLBY_VISION_GATED,
                                MIMETYPE_VIDEO_AV1_GATED,
                                MIMETYPE_VIDEO_APV_GATED,
                            ),
                        audioMimes = standardMp4Audios,
                    )
                }

                container(OUTPUT_FORMAT_WEBM) {
                    support(
                        videoMimes = listOfNotNull(MIMETYPE_VIDEO_VP8, MIMETYPE_VIDEO_VP9_GATED),
                        audioMimes = standardWebmAudios,
                    )
                }
            }
            .build()

    private fun buildHlgRegistry(): FormatComboRegistry =
        FormatComboRegistry.Builder()
            .apply {
                // HLG is a broadcast standard primarily used with HEVC in MP4.
                container(OUTPUT_FORMAT_MPEG_4) {
                    support(
                        videoMimes =
                            listOfNotNull(
                                MIMETYPE_VIDEO_HEVC_GATED,
                                MIMETYPE_VIDEO_AV1_GATED,
                                MIMETYPE_VIDEO_APV_GATED,
                            ),
                        audioMimes = standardMp4Audios,
                    )
                }

                // WebM support for HLG is not widely standardized.
            }
            .build()

    private fun buildHdr10Registry(): FormatComboRegistry =
        FormatComboRegistry.Builder()
            .apply {
                container(OUTPUT_FORMAT_MPEG_4) {
                    support(
                        videoMimes =
                            listOfNotNull(
                                MIMETYPE_VIDEO_HEVC_GATED,
                                MIMETYPE_VIDEO_AV1_GATED,
                                MIMETYPE_VIDEO_APV_GATED,
                            ),
                        audioMimes = standardMp4Audios,
                    )
                }

                // HDR10 is supported in WebM because VP9 Profile 2 (10-bit) was specifically
                // designed to carry HDR10 static metadata in WebM/Matroska.
                container(OUTPUT_FORMAT_WEBM) {
                    support(
                        videoMimes = listOfNotNull(MIMETYPE_VIDEO_VP9_GATED),
                        audioMimes = standardWebmAudios,
                    )
                }
            }
            .build()

    private fun buildHdr10PlusRegistry(): FormatComboRegistry =
        FormatComboRegistry.Builder()
            .apply {
                // HDR10+ are standardized for MP4 via HEVC/AV1.
                container(OUTPUT_FORMAT_MPEG_4) {
                    support(
                        videoMimes =
                            listOfNotNull(MIMETYPE_VIDEO_HEVC_GATED, MIMETYPE_VIDEO_AV1_GATED),
                        audioMimes = standardMp4Audios,
                    )
                }

                // WebM is excluded due to lack of standardized dynamic metadata transport in VP9.
            }
            .build()

    private fun buildDolbyVisionRegistry(): FormatComboRegistry =
        FormatComboRegistry.Builder()
            .apply {
                container(OUTPUT_FORMAT_MPEG_4) {
                    support(
                        videoMimes = listOfNotNull(MIMETYPE_VIDEO_DOLBY_VISION_GATED),
                        audioMimes = standardMp4Audios,
                    )
                }
            }
            .build()

    private fun String.takeIf(minSdk: Int): String? = takeIf { Build.VERSION.SDK_INT >= minSdk }
}
