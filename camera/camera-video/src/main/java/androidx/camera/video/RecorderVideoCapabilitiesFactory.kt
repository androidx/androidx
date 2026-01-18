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

package androidx.camera.video

import android.util.LruCache
import androidx.annotation.GuardedBy
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.video.internal.encoder.VideoEncoderInfoImpl

/** Factory for creating and caching [VideoCapabilities] instances. */
internal object RecorderVideoCapabilitiesFactory {
    @GuardedBy("capabilitiesCache")
    private val capabilitiesCache = LruCache<CacheKey, VideoCapabilities>(16)

    /** Gets or creates [VideoCapabilities] for the given camera and configuration. */
    @JvmStatic
    fun getCapabilities(
        cameraInfo: CameraInfo,
        @Recorder.VideoRecordingType videoRecordingType: Int,
        @Recorder.VideoCapabilitiesSource videoCapabilitiesSource: Int,
    ): VideoCapabilities {
        if (shouldSkipCache(cameraInfo)) {
            return createCapabilities(cameraInfo, videoRecordingType, videoCapabilitiesSource)
        }

        val adapterInfo = cameraInfo as AdapterCameraInfo
        val key =
            CacheKey(
                adapterInfo.cameraId,
                adapterInfo.cameraConfig,
                videoRecordingType,
                videoCapabilitiesSource,
            )

        synchronized(capabilitiesCache) {
            return capabilitiesCache.get(key)
                ?: createCapabilities(cameraInfo, videoRecordingType, videoCapabilitiesSource)
                    .also { capabilitiesCache.put(key, it) }
        }
    }

    private fun createCapabilities(
        cameraInfo: CameraInfo,
        videoRecordingType: Int,
        videoCapabilitiesSource: Int,
    ): VideoCapabilities {
        val cameraInfoInternal = cameraInfo as CameraInfoInternal
        val qualitySource =
            if (videoRecordingType == Recorder.VIDEO_RECORDING_TYPE_HIGH_SPEED) {
                Quality.QUALITY_SOURCE_HIGH_SPEED
            } else {
                Quality.QUALITY_SOURCE_REGULAR
            }

        val resolvedProvider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo = cameraInfoInternal,
                videoCapabilitiesSource = videoCapabilitiesSource,
                qualitySource = qualitySource,
                videoEncoderInfoFinder = VideoEncoderInfoImpl.FINDER,
            )

        return RecorderVideoCapabilities(resolvedProvider, videoRecordingType, cameraInfoInternal)
    }

    /**
     * Checks whether the video capabilities for a given camera should be cached.
     *
     * Caching is skipped for external cameras or cameras with an unknown lens facing, as their
     * properties may not be stable across device reboots or during camera hot-plugging.
     */
    private fun shouldSkipCache(cameraInfo: CameraInfo): Boolean {
        return if (cameraInfo is AdapterCameraInfo) {
            cameraInfo.isExternalCamera ||
                cameraInfo.lensFacing == CameraSelector.LENS_FACING_UNKNOWN
        } else {
            // If we can't determine the camera properties (e.g., not an AdapterCameraInfo),
            // it's safer to skip caching.
            true
        }
    }

    private data class CacheKey(
        val cameraId: String,
        val cameraConfig: Any,
        val videoRecordingType: Int,
        val videoCapabilitiesSource: Int,
    )
}
