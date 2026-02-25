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
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.impl.Timebase
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.video.AudioSpec
import androidx.camera.video.EncoderProfilesResolver
import androidx.camera.video.EncoderProfilesResolverFactory
import androidx.camera.video.Quality
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeFalse
import org.junit.Before
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
class AudioEncoderConfigAudioProfileResolverTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val defaultAudioSpec = AudioSpec.builder().build()
    private val timebase = Timebase.UPTIME

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var profilesResolver: EncoderProfilesResolver

    @Before
    fun setUp() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        CameraXUtil.initialize(context, cameraConfig).get()

        val cameraInfo = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo

        profilesResolver = EncoderProfilesResolverFactory.getResolver(cameraInfo)

        Assume.assumeTrue(profilesResolver.getSupportedQualities(SDR).isNotEmpty())
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
        val supportedProfiles =
            profilesResolver.getSupportedQualities(SDR).map {
                profilesResolver.getProfiles(it, SDR)!!
            }

        for (encoderProfiles in supportedProfiles) {
            val audioProfile = encoderProfiles.defaultAudioProfile ?: continue

            val audioSettings =
                AudioSettingsAudioProfileResolver(defaultAudioSpec, audioProfile, null).get()
            val config =
                AudioEncoderConfigAudioProfileResolver(
                        audioProfile.mediaType,
                        audioProfile.profile,
                        timebase,
                        defaultAudioSpec,
                        audioSettings,
                        audioProfile,
                    )
                    .get()

            assertThat(config.mimeType).isEqualTo(audioProfile.mediaType)
            assertThat(config.bitrate).isEqualTo(audioProfile.bitrate)
            assertThat(config.captureSampleRate).isEqualTo(audioProfile.sampleRate)
            assertThat(config.encodeSampleRate).isEqualTo(audioProfile.sampleRate)
            assertThat(config.channelCount).isEqualTo(audioProfile.channels)
        }
    }

    @Test
    fun increasedChannelCountIncreasesBitrate() {
        val encoderProfiles = profilesResolver.getProfiles(Quality.HIGHEST, SDR)!!
        val profile = encoderProfiles.defaultAudioProfile
        Assume.assumeTrue(profile != null)

        // Get default channel count
        val defaultAudioSettings =
            AudioSettingsAudioProfileResolver(defaultAudioSpec, profile!!, null).get()
        val defaultConfig =
            AudioEncoderConfigAudioProfileResolver(
                    profile.mediaType,
                    profile.profile,
                    timebase,
                    defaultAudioSpec,
                    defaultAudioSettings,
                    profile,
                )
                .get()
        val defaultChannelCount = defaultConfig.channelCount

        val higherChannelCountAudioSettings =
            defaultAudioSettings.toBuilder().setChannelCount(defaultChannelCount * 2).build()

        val higherChannelCountConfig =
            AudioEncoderConfigAudioProfileResolver(
                    profile.mediaType,
                    profile.profile,
                    timebase,
                    defaultAudioSpec,
                    higherChannelCountAudioSettings,
                    profile,
                )
                .get()

        assertThat(higherChannelCountConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun increasedSampleRateIncreasesBitrate() {
        val encoderProfiles = profilesResolver.getProfiles(Quality.HIGHEST, SDR)!!
        val profile = encoderProfiles.defaultAudioProfile
        Assume.assumeTrue(profile != null)

        // Get default sample rate
        val defaultAudioSettings =
            AudioSettingsAudioProfileResolver(defaultAudioSpec, profile!!, null).get()
        val defaultConfig =
            AudioEncoderConfigAudioProfileResolver(
                    profile.mediaType,
                    profile.profile,
                    timebase,
                    defaultAudioSpec,
                    defaultAudioSettings,
                    profile,
                )
                .get()
        val defaultSampleRate = defaultConfig.captureSampleRate

        val higherSampleRateAudioSettings =
            defaultAudioSettings.toBuilder().setChannelCount(defaultSampleRate * 2).build()

        val higherSampleRateConfig =
            AudioEncoderConfigAudioProfileResolver(
                    profile.mediaType,
                    profile.profile,
                    timebase,
                    defaultAudioSpec,
                    higherSampleRateAudioSettings,
                    profile,
                )
                .get()

        assertThat(higherSampleRateConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }
}
