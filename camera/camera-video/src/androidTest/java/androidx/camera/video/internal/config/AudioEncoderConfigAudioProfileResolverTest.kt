/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Context
import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.impl.Timebase
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.video.AudioSpec
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test used to verify AudioEncoderConfigAudioProfileResolver works as expected.
 *
 * Only standard dynamic range is checked, since video and audio should be independent.
 */
@RunWith(Parameterized::class)
@SmallTest
@SdkSuppress(minSdkVersion = 21)
class AudioEncoderConfigAudioProfileResolverTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val defaultAudioSpec = AudioSpec.builder().build()
    private val timebase = Timebase.UPTIME

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var videoCapabilities: VideoCapabilities

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        CameraXUtil.initialize(
            context,
            cameraConfig
        ).get()

        val cameraInfo = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
        videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        Assume.assumeTrue(videoCapabilities.getSupportedQualities(SDR).isNotEmpty())
    }

    @After
    fun tearDown() {
        if (this::cameraUseCaseAdapter.isInitialized) {
            runBlocking(Dispatchers.Main) {
                cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
            }
        }

        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun defaultAudioSpecAndAudioSourceProducesValidSettings() {
        val supportedProfiles = videoCapabilities.getSupportedQualities(SDR).map {
            videoCapabilities.getProfiles(it, SDR)!!
        }

        for (encoderProfiles in supportedProfiles) {
            val audioProfile = encoderProfiles.defaultAudioProfile ?: continue

            val audioSettings =
                AudioSettingsAudioProfileResolver(defaultAudioSpec, audioProfile).get()
            val config = AudioEncoderConfigAudioProfileResolver(
                audioProfile.mediaType,
                audioProfile.profile,
                timebase,
                defaultAudioSpec,
                audioSettings,
                audioProfile
            ).get()

            assertThat(config.mimeType).isEqualTo(audioProfile.mediaType)
            assertThat(config.bitrate).isEqualTo(audioProfile.bitrate)
            assertThat(config.sampleRate).isEqualTo(audioProfile.sampleRate)
            assertThat(config.channelCount).isEqualTo(audioProfile.channels)
        }
    }

    @Test
    fun increasedChannelCountIncreasesBitrate() {
        val encoderProfiles = videoCapabilities.getProfiles(Quality.HIGHEST, SDR)!!
        val profile = encoderProfiles.defaultAudioProfile
        Assume.assumeTrue(profile != null)

        // Get default channel count
        val defaultAudioSettings =
            AudioSettingsAudioProfileResolver(
                defaultAudioSpec,
                profile!!
            ).get()
        val defaultConfig =
            AudioEncoderConfigAudioProfileResolver(
                profile.mediaType,
                profile.profile,
                timebase,
                defaultAudioSpec,
                defaultAudioSettings,
                profile
            ).get()
        val defaultChannelCount = defaultConfig.channelCount

        val higherChannelCountAudioSettings =
            defaultAudioSettings.toBuilder().setChannelCount(defaultChannelCount * 2).build()

        val higherChannelCountConfig = AudioEncoderConfigAudioProfileResolver(
            profile.mediaType,
            profile.profile,
            timebase,
            defaultAudioSpec,
            higherChannelCountAudioSettings,
            profile
        ).get()

        assertThat(higherChannelCountConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun increasedSampleRateIncreasesBitrate() {
        val encoderProfiles = videoCapabilities.getProfiles(Quality.HIGHEST, SDR)!!
        val profile = encoderProfiles.defaultAudioProfile
        Assume.assumeTrue(profile != null)

        // Get default sample rate
        val defaultAudioSettings =
            AudioSettingsAudioProfileResolver(
                defaultAudioSpec,
                profile!!
            ).get()
        val defaultConfig =
            AudioEncoderConfigAudioProfileResolver(
                profile.mediaType,
                profile.profile,
                timebase,
                defaultAudioSpec,
                defaultAudioSettings,
                profile
            ).get()
        val defaultSampleRate = defaultConfig.sampleRate

        val higherSampleRateAudioSettings =
            defaultAudioSettings.toBuilder().setChannelCount(defaultSampleRate * 2).build()

        val higherSampleRateConfig = AudioEncoderConfigAudioProfileResolver(
            profile.mediaType,
            profile.profile,
            timebase,
            defaultAudioSpec,
            higherSampleRateAudioSettings,
            profile
        ).get()

        assertThat(higherSampleRateConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun bitrateRangeInVideoSpecClampsBitrate() {
        val encoderProfiles = videoCapabilities.getProfiles(Quality.HIGHEST, SDR)!!
        val profile = encoderProfiles.defaultAudioProfile
        Assume.assumeTrue(profile != null)

        val defaultAudioSettings =
            AudioSettingsAudioProfileResolver(
                defaultAudioSpec,
                profile!!
            ).get()

        val defaultBitrate = profile.bitrate

        // Create audio spec with limit 20% higher than default.
        val higherBitrate = (defaultBitrate * 1.2).toInt()
        val higherAudioSpec =
            AudioSpec.builder().setBitrate(Range(higherBitrate, Int.MAX_VALUE)).build()

        // Create audio spec with limit 20% lower than default.
        val lowerBitrate = (defaultBitrate * 0.8).toInt()
        val lowerAudioSpec = AudioSpec.builder().setBitrate(Range(0, lowerBitrate)).build()

        assertThat(
            AudioEncoderConfigAudioProfileResolver(
                profile.mediaType,
                profile.profile,
                timebase,
                higherAudioSpec,
                defaultAudioSettings,
                profile
            ).get().bitrate
        ).isEqualTo(higherBitrate)

        assertThat(
            AudioEncoderConfigAudioProfileResolver(
                profile.mediaType,
                profile.profile,
                timebase,
                lowerAudioSpec,
                defaultAudioSettings,
                profile
            ).get().bitrate
        ).isEqualTo(lowerBitrate)
    }
}