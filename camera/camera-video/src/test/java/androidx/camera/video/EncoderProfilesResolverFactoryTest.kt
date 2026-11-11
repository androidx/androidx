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

import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.internal.encoder.FakeVideoEncoderFactory
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class EncoderProfilesResolverFactoryTest {

    private val videoEncoderInfoFinder: VideoEncoderInfo.Finder = VideoEncoderInfo.Finder {
        FakeVideoEncoderInfo()
    }

    @Test
    fun getResolver_returnsCachedInstanceForSameCamera() {
        val cameraInfo =
            AdapterCameraInfo(
                FakeCameraInfoInternal("0", CameraSelector.LENS_FACING_BACK),
                FakeCameraConfig(),
            )

        val capabilities1 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )
        val capabilities2 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )

        assertThat(capabilities1).isSameInstanceAs(capabilities2)
    }

    @Test
    fun getResolver_returnsNewInstanceForDifferentCamera() {
        val cameraInfo1 =
            AdapterCameraInfo(
                FakeCameraInfoInternal("0", CameraSelector.LENS_FACING_BACK),
                FakeCameraConfig(),
            )
        val cameraInfo2 =
            AdapterCameraInfo(
                FakeCameraInfoInternal("1", CameraSelector.LENS_FACING_BACK),
                FakeCameraConfig(),
            )

        val capabilities1 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo1,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )
        val capabilities2 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo2,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )

        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    @Test
    fun getResolver_returnsCachedInstanceForDifferentCameraInfoWithSameIdAndConfig() {
        val cameraConfig = FakeCameraConfig()
        val cameraInfo1 =
            AdapterCameraInfo(
                FakeCameraInfoInternal("0", CameraSelector.LENS_FACING_BACK),
                cameraConfig,
            )
        val cameraInfo2 =
            AdapterCameraInfo(
                FakeCameraInfoInternal("0", CameraSelector.LENS_FACING_BACK),
                cameraConfig,
            )

        val capabilities1 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo1,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )
        val capabilities2 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo2,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )

        assertThat(capabilities1).isSameInstanceAs(capabilities2)
    }

    @Test
    fun getResolver_returnsNewInstanceForDifferentRecordingType() {
        val cameraInfo =
            AdapterCameraInfo(
                FakeCameraInfoInternal("0", CameraSelector.LENS_FACING_BACK),
                FakeCameraConfig(),
            )

        val capabilities1 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )
        val capabilities2 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                Recorder.VIDEO_RECORDING_TYPE_HIGH_SPEED,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )

        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    @Test
    fun getResolver_returnsNewInstanceForDifferentSource() {
        val cameraInfo =
            AdapterCameraInfo(
                FakeCameraInfoInternal("0", CameraSelector.LENS_FACING_BACK),
                FakeCameraConfig(),
            )

        val capabilities1 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )
        val capabilities2 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
                videoEncoderInfoFinder,
            )

        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    @Test
    fun getResolver_doesNotCacheForExternalCamera() {
        val cameraInfo =
            AdapterCameraInfo(
                FakeCameraInfoInternal("external", CameraSelector.LENS_FACING_EXTERNAL),
                FakeCameraConfig(),
            )

        val capabilities1 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )
        val capabilities2 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )

        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    @Test
    fun getResolver_doesNotCacheForUnknownLensFacingCamera() {
        val cameraInfo =
            AdapterCameraInfo(
                FakeCameraInfoInternal("unknown", CameraSelector.LENS_FACING_UNKNOWN),
                FakeCameraConfig(),
            )

        val capabilities1 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )
        val capabilities2 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )

        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    @Test
    fun getResolver_returnsDifferentInstancesForDifferentCameraConfigs() {
        val cameraInfo1 = AdapterCameraInfo(FakeCameraInfoInternal("0"), FakeCameraConfig())
        val cameraInfo2 = AdapterCameraInfo(FakeCameraInfoInternal("0"), FakeCameraConfig())

        val capabilities1 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo1,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )
        val capabilities2 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo2,
                Recorder.VIDEO_RECORDING_TYPE_REGULAR,
                Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                videoEncoderInfoFinder,
            )

        // Assert
        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    @Test
    fun getResolver_findersFromSameFactory_returnsCachedInstance() {
        val cameraInfo = AdapterCameraInfo(FakeCameraInfoInternal("0"), FakeCameraConfig())
        val factory = createVideoEncoderFactory()

        val resolver1 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                videoEncoderInfoFinder = VideoEncoderInfo.Finder.from(factory),
            )
        val resolver2 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                videoEncoderInfoFinder = VideoEncoderInfo.Finder.from(factory),
            )

        assertThat(resolver1).isSameInstanceAs(resolver2)
    }

    @Test
    fun getResolver_findersFromDifferentFactories_returnsDifferentInstances() {
        val cameraInfo = AdapterCameraInfo(FakeCameraInfoInternal("0"), FakeCameraConfig())

        val resolver1 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                videoEncoderInfoFinder = VideoEncoderInfo.Finder.from(createVideoEncoderFactory()),
            )
        val resolver2 =
            EncoderProfilesResolverFactory.getResolver(
                cameraInfo,
                videoEncoderInfoFinder = VideoEncoderInfo.Finder.from(createVideoEncoderFactory()),
            )

        assertThat(resolver1).isNotSameInstanceAs(resolver2)
    }

    @Test
    fun recordersWithSameVideoEncoderFactory_shareCachedResolver() {
        val cameraInfo = AdapterCameraInfo(FakeCameraInfoInternal("0"), FakeCameraConfig())
        val factory = createVideoEncoderFactory()

        val resolver1 =
            Recorder.Builder()
                .setVideoEncoderFactory(factory)
                .build()
                .getEncoderProfilesResolver(cameraInfo, SESSION_TYPE_REGULAR)
        val resolver2 =
            Recorder.Builder()
                .setVideoEncoderFactory(factory)
                .build()
                .getEncoderProfilesResolver(cameraInfo, SESSION_TYPE_REGULAR)

        assertThat(resolver1).isSameInstanceAs(resolver2)
    }

    private fun createVideoEncoderFactory() = FakeVideoEncoderFactory { _, _ ->
        throw UnsupportedOperationException()
    }
}
