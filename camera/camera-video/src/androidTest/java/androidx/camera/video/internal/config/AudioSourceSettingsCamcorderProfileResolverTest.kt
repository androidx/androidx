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
import android.media.AudioFormat
import android.media.MediaRecorder
import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.AudioUtil
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.video.AudioSpec
import androidx.camera.video.Quality
import androidx.camera.video.VideoCapabilities
import androidx.camera.video.internal.AudioSource
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
class AudioSourceSettingsCamcorderProfileResolverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val defaultAudioSpec = AudioSpec.builder().build()

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var videoCapabilities: VideoCapabilities

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        // Skip for b/168175357
        Assume.assumeTrue(AudioUtil.canStartAudioRecord(MediaRecorder.AudioSource.CAMCORDER))

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
    fun defaultAudioSpecResolvesToSupportedSettings() {
        val resolvedSettings = videoCapabilities.supportedQualities.map {
            val camcorderProfile = videoCapabilities.getProfile(it)
            AudioSourceSettingsCamcorderProfileResolver(defaultAudioSpec, camcorderProfile!!).get()
        }

        resolvedSettings.forEach {
            assertThat(
                AudioSource.isSettingsSupported(
                    it.sampleRate,
                    it.channelCount,
                    it.audioFormat
                )
            )
        }
    }

    @Test
    fun nonDefaultAudioSpecResolvesToSupportedSampleRate() {
        val audioSpecs = listOf(
            AudioSpec.builder().setSampleRate(Range(0, 1000)).build(),
            AudioSpec.builder().setSampleRate(Range(1000, 10000)).build(),
            AudioSpec.builder().setSampleRate(Range(10000, 100000)).build()
        )

        val resolvedSettings = videoCapabilities.supportedQualities.flatMap { quality ->
            val camcorderProfile = videoCapabilities.getProfile(quality)
            audioSpecs.map {
                AudioSourceSettingsCamcorderProfileResolver(it, camcorderProfile!!).get()
            }
        }

        resolvedSettings.forEach {
            assertThat(
                AudioSource.isSettingsSupported(
                    it.sampleRate,
                    it.channelCount,
                    it.audioFormat
                )
            )
        }
    }

    @Test
    fun sampleRateCanOverrideCamcorderProfile_ifSupported() {
        val profile = videoCapabilities.getProfile(Quality.HIGHEST)
        // Get a config using the default audio spec to retrieve the source format
        // Note: This relies on resolution of sample rate and source format being independent.
        // If a dependency between the two is introduced, this will stop working and will
        // need to be rewritten.
        val autoCamcorderProfileConfig =
            AudioSourceSettingsCamcorderProfileResolver(defaultAudioSpec, profile!!).get()
        // Try to find a sample rate that is supported, but not the
        // sample rate advertised by CamcorderProfile
        val nonReportedSampleRate = AudioSource.COMMON_SAMPLE_RATES.firstOrNull {
            it != profile.audioSampleRate && AudioSource.isSettingsSupported(
                it,
                profile.audioChannels,
                autoCamcorderProfileConfig.audioFormat
            )
        }
        Assume.assumeTrue(
            "Device does not support any other common sample rates. Cannot override.",
            nonReportedSampleRate != null
        )

        // Create an audio spec that overrides the auto sample rate behavior
        val audioSpec =
            AudioSpec.builder().setSampleRate(Range(nonReportedSampleRate!!, nonReportedSampleRate))
                .build()
        val resolvedSampleRate =
            AudioSourceSettingsCamcorderProfileResolver(audioSpec, profile).get().sampleRate

        assertThat(resolvedSampleRate).isNotEqualTo(profile.audioSampleRate)
        assertThat(resolvedSampleRate).isEqualTo(nonReportedSampleRate)
    }

    @Test
    fun audioSpecDefaultProducesValidSourceEnum() {
        val profile = videoCapabilities.getProfile(Quality.HIGHEST)
        val audioSpec = AudioSpec.builder().build()
        val resolvedAudioSourceEnum =
            AudioSourceSettingsCamcorderProfileResolver(
                audioSpec,
                profile!!
            ).get().audioSource

        assertThat(resolvedAudioSourceEnum).isAnyOf(
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.MIC
        )
    }

    @Test
    fun audioSpecDefaultProducesValidSourceFormat() {
        val profile = videoCapabilities.getProfile(Quality.HIGHEST)
        val audioSpec = AudioSpec.builder().build()
        val resolvedAudioSourceFormat =
            AudioSourceSettingsCamcorderProfileResolver(
                audioSpec,
                profile!!
            ).get().audioFormat

        assertThat(resolvedAudioSourceFormat).isNotEqualTo(AudioFormat.ENCODING_INVALID)
    }
}