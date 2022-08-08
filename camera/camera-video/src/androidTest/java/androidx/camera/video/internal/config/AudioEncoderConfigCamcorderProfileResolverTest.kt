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
import androidx.camera.core.CameraSelector
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.video.AudioSpec
import androidx.camera.video.Quality
import androidx.camera.video.VideoCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 21)
class AudioEncoderConfigCamcorderProfileResolverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val defaultAudioSpec = AudioSpec.builder().build()

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var videoCapabilities: VideoCapabilities

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        CameraXUtil.initialize(
            context,
            Camera2Config.defaultConfig()
        ).get()

        val cameraInfo = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
        videoCapabilities = VideoCapabilities.from(cameraInfo)
        Assume.assumeTrue(videoCapabilities.supportedQualities.isNotEmpty())
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
        val supportedProfiles = videoCapabilities.supportedQualities.map {
            videoCapabilities.getProfile(it)!!
        }

        supportedProfiles.forEach {
            val sourceSettings =
                AudioSourceSettingsCamcorderProfileResolver(defaultAudioSpec, it).get()
            val config = AudioEncoderConfigCamcorderProfileResolver(
                it.audioCodecMimeType!!,
                it.requiredAudioProfile,
                defaultAudioSpec,
                sourceSettings,
                it
            ).get()

            assertThat(config.mimeType).isEqualTo(it.audioCodecMimeType)
            assertThat(config.bitrate).isEqualTo(it.audioBitRate)
            assertThat(config.sampleRate).isEqualTo(it.audioSampleRate)
            assertThat(config.channelCount).isEqualTo(it.audioChannels)
        }
    }

    @Test
    fun increasedChannelCountIncreasesBitrate() {
        // Get default channel count
        val profile = videoCapabilities.getProfile(Quality.HIGHEST)!!
        val defaultSourceSettings =
            AudioSourceSettingsCamcorderProfileResolver(defaultAudioSpec, profile).get()
        val defaultConfig =
            AudioEncoderConfigCamcorderProfileResolver(
                profile.audioCodecMimeType!!,
                profile.requiredAudioProfile,
                defaultAudioSpec,
                defaultSourceSettings,
                profile
            ).get()
        val defaultChannelCount = defaultConfig.channelCount

        val higherChannelCountSourceSettings =
            defaultSourceSettings.toBuilder().setChannelCount(defaultChannelCount * 2).build()

        val higherChannelCountConfig = AudioEncoderConfigCamcorderProfileResolver(
            profile.audioCodecMimeType!!,
            profile.requiredAudioProfile,
            defaultAudioSpec,
            higherChannelCountSourceSettings,
            profile
        ).get()

        assertThat(higherChannelCountConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun increasedSampleRateIncreasesBitrate() {
        // Get default sample rate
        val profile = videoCapabilities.getProfile(Quality.HIGHEST)!!
        val defaultSourceSettings =
            AudioSourceSettingsCamcorderProfileResolver(defaultAudioSpec, profile).get()
        val defaultConfig =
            AudioEncoderConfigCamcorderProfileResolver(
                profile.audioCodecMimeType!!,
                profile.requiredAudioProfile,
                defaultAudioSpec,
                defaultSourceSettings,
                profile
            ).get()
        val defaultSampleRate = defaultConfig.sampleRate

        val higherSampleRateSourceSettings =
            defaultSourceSettings.toBuilder().setChannelCount(defaultSampleRate * 2).build()

        val higherSampleRateConfig = AudioEncoderConfigCamcorderProfileResolver(
            profile.audioCodecMimeType!!,
            profile.requiredAudioProfile,
            defaultAudioSpec,
            higherSampleRateSourceSettings,
            profile
        ).get()

        assertThat(higherSampleRateConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun bitrateRangeInVideoSpecClampsBitrate() {
        val profile = videoCapabilities.getProfile(Quality.HIGHEST)!!
        val defaultSourceSettings =
            AudioSourceSettingsCamcorderProfileResolver(defaultAudioSpec, profile).get()

        val defaultBitrate = profile.audioBitRate

        // Create audio spec with limit 20% higher than default.
        val higherBitrate = (defaultBitrate * 1.2).toInt()
        val higherAudioSpec =
            AudioSpec.builder().setBitrate(Range(higherBitrate, Int.MAX_VALUE)).build()

        // Create audio spec with limit 20% lower than default.
        val lowerBitrate = (defaultBitrate * 0.8).toInt()
        val lowerAudioSpec = AudioSpec.builder().setBitrate(Range(0, lowerBitrate)).build()

        assertThat(
            AudioEncoderConfigCamcorderProfileResolver(
                profile.audioCodecMimeType!!,
                profile.requiredAudioProfile,
                higherAudioSpec,
                defaultSourceSettings,
                profile
            ).get().bitrate
        ).isEqualTo(higherBitrate)

        assertThat(
            AudioEncoderConfigCamcorderProfileResolver(
                profile.audioCodecMimeType!!,
                profile.requiredAudioProfile,
                lowerAudioSpec,
                defaultSourceSettings,
                profile
            ).get().bitrate
        ).isEqualTo(lowerBitrate)
    }
}