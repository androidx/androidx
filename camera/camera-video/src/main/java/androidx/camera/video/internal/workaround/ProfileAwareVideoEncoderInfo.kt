/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.camera.video.internal.workaround

import android.media.CamcorderProfile
import android.media.EncoderProfiles
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.annotation.VisibleForTesting
import androidx.camera.core.Logger
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.video.Quality
import androidx.camera.video.internal.compat.quirk.MediaCodecInfoReportIncorrectInfoQuirk
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import androidx.camera.video.internal.workaround.ProfileAwareVideoEncoderInfo.Companion.profileCache

/**
 * A [VideoEncoderInfo] wrapper that harmonizes [MediaCodec] capabilities with real-world device
 * recording capabilities defined in [CamcorderProfile].
 *
 * ### The Problem: MediaCodec Under-Reporting
 * On many Android devices (specified in [MediaCodecInfoReportIncorrectInfoQuirk]), the
 * [MediaCodecInfo] query returns conservative or incorrect resolution limits. For example, an
 * encoder might report a maximum width of 1072px, even though the device hardware successfully
 * records 1080p video. This "under-reporting" causes CameraX to reject valid high-resolution
 * configurations because [VideoEncoderInfo.isSizeSupported] returns false based on the faulty codec
 * metadata.
 *
 * ### The Solution: Profile-Based Validation
 * This class applies a "trust but verify" strategy:
 * 1. **Trust MediaCodec:** It honors all capabilities natively reported by the encoder.
 * 2. **Verify via CamcorderProfile:** It scans the device's [CamcorderProfile] (or
 *    [android.media.EncoderProfiles] on API 31+) to find hardware-validated recording
 *    configurations for the specific MIME type.
 * 3. **Merge Capabilities:** If a resolution is found in a CamcorderProfile that exceeds the
 *    encoder's reported limits, this class "stretches" the supported ranges and includes those
 *    specific sizes in [isSizeSupported].
 *
 * ### Key Features
 * * **MIME-Specific Caching:** Profile discovery is an expensive system call; results are cached in
 *   a static [profileCache] to ensure subsequent encoder lookups are near-instant.
 * * **Alignment Awareness:** Even when injecting "extra" sizes from profiles, the class still
 *   enforces [widthAlignment] and [heightAlignment] to ensure the resulting format is compatible
 *   with the underlying hardware buffers.
 * * **Multi-Camera Support:** Scans profiles for both primary (back) and secondary (front) cameras
 *   to build a comprehensive map of what the silicon is actually capable of encoding.
 *
 * @see MediaCodecInfoReportIncorrectInfoQuirk
 */
public class ProfileAwareVideoEncoderInfo
private constructor(private val videoEncoderInfo: VideoEncoderInfo) : VideoEncoderInfo {

    // Determine if we found any hardware profiles that exceed MediaCodec capabilities
    private val profileData: ProfileData? by lazy { getOrComputeProfileData(videoEncoderInfo.mime) }

    private val isWidthStretchApplied: Boolean by lazy {
        profileData != null && profileData!!.maxWidth > videoEncoderInfo.getSupportedWidths().upper
    }

    private val isHeightStretchApplied: Boolean by lazy {
        profileData != null &&
            profileData!!.maxHeight > videoEncoderInfo.getSupportedHeights().upper
    }

    private val _supportedWidths: Range<Int> by lazy {
        val baseRange = videoEncoderInfo.getSupportedWidths()
        if (isWidthStretchApplied) Range.create(baseRange.lower, profileData!!.maxWidth)
        else baseRange
    }

    private val _supportedHeights: Range<Int> by lazy {
        val baseRange = videoEncoderInfo.getSupportedHeights()
        if (isHeightStretchApplied) Range.create(baseRange.lower, profileData!!.maxHeight)
        else baseRange
    }

    override val mime: String
        get() = videoEncoderInfo.mime

    override fun getName(): String = videoEncoderInfo.getName()

    override fun canSwapWidthHeight(): Boolean = videoEncoderInfo.canSwapWidthHeight()

    override fun isSizeSupported(width: Int, height: Int): Boolean {
        val isExtraSizeSupported = profileData?.extraSizes?.contains(Size(width, height)) ?: false

        return isExtraSizeSupported || videoEncoderInfo.isSizeSupported(width, height)
    }

    override fun getSupportedWidths(): Range<Int> = _supportedWidths

    override fun getSupportedHeights(): Range<Int> = _supportedHeights

    override fun getSupportedWidthsFor(height: Int): Range<Int> {
        return try {
            // Try to get the base range (honors dynamic constraints/load limits)
            videoEncoderInfo.getSupportedWidthsFor(height)
        } catch (e: IllegalArgumentException) {
            // If rejected, check if our profiles actually support this height.
            // If they do, return the global stretched range to allow the width search.
            val isHeightInProfiles = profileData?.extraSizes?.any { it.height == height } ?: false
            if (isHeightInProfiles) {
                _supportedWidths
            } else {
                throw e // Unsupported by both encoder and profiles
            }
        }
    }

    override fun getSupportedHeightsFor(width: Int): Range<Int> {
        return try {
            // Try to get the base range (honors dynamic constraints/load limits)
            videoEncoderInfo.getSupportedHeightsFor(width)
        } catch (e: IllegalArgumentException) {
            // If rejected, check if our profiles actually support this width.
            // If they do, return the global stretched range to allow the height search.
            val isWidthInProfiles = profileData?.extraSizes?.any { it.width == width } ?: false
            if (isWidthInProfiles) {
                _supportedHeights
            } else {
                throw e // Unsupported by both encoder and profiles
            }
        }
    }

    override val widthAlignment: Int
        get() = videoEncoderInfo.widthAlignment

    override val heightAlignment: Int
        get() = videoEncoderInfo.heightAlignment

    override val supportedBitrateRange: Range<Int>
        get() = videoEncoderInfo.supportedBitrateRange

    private data class ProfileData(val extraSizes: Set<Size>, val maxWidth: Int, val maxHeight: Int)

    public companion object {
        private const val TAG: String = "ProfileAwareEncoderInfo"
        private val profileCache = mutableMapOf<String, ProfileData?>()

        @JvmStatic
        public fun from(videoEncoderInfo: VideoEncoderInfo): VideoEncoderInfo {
            return ProfileAwareVideoEncoderInfo(videoEncoderInfo)
        }

        @VisibleForTesting
        internal fun clearCache() {
            synchronized(profileCache) { profileCache.clear() }
        }

        private fun getOrComputeProfileData(mime: String): ProfileData? {
            synchronized(profileCache) {
                if (profileCache.containsKey(mime)) {
                    return profileCache[mime]
                }

                val extraSizes = mutableSetOf<Size>()
                var maxWidth = 0
                var maxHeight = 0

                // Limit to primary cameras (0 = Back, 1 = Front). Most devices are defined on
                // these primary IDs. Scanning all logical/physical IDs is avoided to minimize IPC
                // latency during initialization.
                for (cameraId in listOf(0, 1)) {
                    for (quality in Quality.getSortedQualities()) {
                        val qualityValue =
                            (quality as Quality.ConstantQuality).getQualityValue(
                                Quality.QUALITY_SOURCE_REGULAR
                            )

                        findMatchingSizeFromProfile(cameraId, qualityValue, mime)?.let { size ->
                            extraSizes.add(size)
                            maxWidth = maxOf(maxWidth, size.width)
                            maxHeight = maxOf(maxHeight, size.height)
                        }
                    }
                }

                // Return null if no profile data was discovered for this MIME
                val result =
                    if (extraSizes.isEmpty()) {
                        null
                    } else {
                        ProfileData(extraSizes, maxWidth, maxHeight)
                    }
                Logger.d(TAG, "Add ProfileData for $mime: $result")

                profileCache[mime] = result
                return result
            }
        }

        private fun findMatchingSizeFromProfile(cameraId: Int, quality: Int, mime: String): Size? {
            if (!CamcorderProfile.hasProfile(cameraId, quality)) {
                return null
            }

            if (Build.VERSION.SDK_INT >= 31) {
                val profiles = CamcorderProfile.getAll(cameraId.toString(), quality)!!
                val videoProfiles: List<EncoderProfiles.VideoProfile?> = profiles.videoProfiles

                if (videoProfiles.any { it != null }) {
                    return videoProfiles
                        .find { it?.mediaType.equals(mime, ignoreCase = true) }
                        ?.let { Size(it.width, it.height) }
                }

                // If we reach here, it means the VideoProfile list was null-filled. This happens
                // on certain API 33 devices. Proceed to fallback.
            }

            // API < 31 or fallback
            @Suppress("DEPRECATION") val profile = CamcorderProfile.get(cameraId, quality)
            return if (isMimeMatch(profile.videoCodec, mime)) {
                Size(profile.videoFrameWidth, profile.videoFrameHeight)
            } else {
                null
            }
        }

        private fun isMimeMatch(codec: Int, mimeType: String): Boolean {
            val codecMime = EncoderProfilesProxy.getVideoCodecMimeType(codec)
            return codecMime.equals(mimeType, ignoreCase = true)
        }
    }
}
