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
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE
import androidx.camera.video.Recorder.VIDEO_RECORDING_TYPE_REGULAR
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import androidx.camera.video.internal.encoder.VideoEncoderInfoImpl

/** Factory for creating and caching [EncoderProfilesResolver] instances. */
internal object EncoderProfilesResolverFactory {
    @GuardedBy("cache") private val cache = LruCache<CacheKey, EncoderProfilesResolver>(16)

    /** Gets or creates [VideoCapabilities] for the given camera and configuration. */
    @JvmStatic
    fun getResolver(
        cameraInfo: CameraInfo,
        @Recorder.VideoRecordingType videoRecordingType: Int = VIDEO_RECORDING_TYPE_REGULAR,
        @Recorder.VideoCapabilitiesSource
        videoCapabilitiesSource: Int = VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
        videoEncoderInfoFinder: VideoEncoderInfo.Finder = VideoEncoderInfoImpl.FINDER,
    ): EncoderProfilesResolver {
        val createResolver by lazy {
            createResolver(
                cameraInfo,
                videoRecordingType,
                videoCapabilitiesSource,
                videoEncoderInfoFinder,
            )
        }

        if (shouldSkipCache(cameraInfo)) {
            return createResolver
        }

        val adapterInfo = cameraInfo as AdapterCameraInfo
        val key =
            CacheKey(
                adapterInfo.cameraId,
                adapterInfo.cameraConfig,
                videoRecordingType,
                videoCapabilitiesSource,
                videoEncoderInfoFinder,
            )

        synchronized(cache) {
            return cache.get(key) ?: createResolver.also { cache.put(key, it) }
        }
    }

    private fun createResolver(
        cameraInfo: CameraInfo,
        videoRecordingType: Int,
        videoCapabilitiesSource: Int,
        videoEncoderInfoFinder: VideoEncoderInfo.Finder,
    ): EncoderProfilesResolver {
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
                videoEncoderInfoFinder = videoEncoderInfoFinder,
            )

        return EncoderProfilesResolver(
            resolvedProvider,
            qualitySource,
            cameraInfoInternal.supportedDynamicRanges,
        )
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
        val videoEncoderInfoFinder: VideoEncoderInfo.Finder,
    )
}
