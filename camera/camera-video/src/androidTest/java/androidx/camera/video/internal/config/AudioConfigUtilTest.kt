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
import android.media.MediaRecorder
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.AudioUtil
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.IgnoreAudioProblematicDeviceRule
import androidx.camera.video.AudioSpec
import androidx.camera.video.EncoderProfilesResolver
import androidx.camera.video.EncoderProfilesResolverFactory
import androidx.camera.video.internal.audio.AudioSource
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test used to verify AudioConfigUtil works as expected.
 *
 * Only standard dynamic range is checked, since video and audio should be independent.
 */
@RunWith(Parameterized::class)
@SmallTest
class AudioConfigUtilTest(private val implName: String, private val cameraConfig: CameraXConfig) {

    // Ignore problematic device for b/277176784
    @get:Rule val ignoreProblematicDeviceRule = IgnoreAudioProblematicDeviceRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))
    }

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val defaultAudioSpec = AudioSpec.builder().build()

    private lateinit var profilesResolver: EncoderProfilesResolver

    @Before
    fun setUp() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )

        // Skip for b/399704074
        assumeFalse(
            "Emulator API 26 crashes running this test.",
            Build.VERSION.SDK_INT == 26 && isEmulator(),
        )

        val cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        // Skip for b/168175357
        Assume.assumeTrue(AudioUtil.canStartAudioRecord(MediaRecorder.AudioSource.CAMCORDER))

        CameraXUtil.initialize(context, cameraConfig).get()

        val cameraInfo = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
        profilesResolver = EncoderProfilesResolverFactory.getResolver(cameraInfo)
        Assume.assumeTrue(profilesResolver.getSupportedQualities(SDR).isNotEmpty())
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun resolveAudioSettings_defaultAudioSpecWithoutProfile_resolvesToSupportedSettings() {
        val audioSettings = AudioConfigUtil.resolveAudioSettings(defaultAudioSpec)
        assertThat(
                AudioSource.isSettingsSupported(
                    audioSettings.captureSampleRate,
                    audioSettings.channelCount,
                    audioSettings.audioFormat,
                )
            )
            .isTrue()
    }

    @Test
    fun resolveAudioSettings_defaultAudioSpecWithProfile_resolvesToSupportedSettings() {
        val resolvedSettings =
            profilesResolver.getSupportedQualities(SDR).mapNotNull {
                val encoderProfiles = profilesResolver.getProfiles(it, SDR)!!
                val audioProfile = encoderProfiles.defaultAudioProfile
                if (audioProfile == null) {
                    null
                } else {
                    AudioConfigUtil.resolveAudioSettings(defaultAudioSpec, audioProfile)
                }
            }

        resolvedSettings.forEach {
            assertThat(
                    AudioSource.isSettingsSupported(
                        it.captureSampleRate,
                        it.channelCount,
                        it.audioFormat,
                    )
                )
                .isTrue()
        }
    }

    @Test
    fun resolveAudioSettings_nonDefaultAudioSpec_resolvesToSupportedSettings() {
        val nonDefaultAudioSpec =
            AudioSpec.builder().setSampleRate(10000).setChannelCount(2).build()

        val resolvedSettings =
            profilesResolver.getSupportedQualities(SDR).mapNotNull { quality ->
                val encoderProfiles = profilesResolver.getProfiles(quality, SDR)!!
                val audioProfile = encoderProfiles.defaultAudioProfile
                if (audioProfile == null) {
                    null
                } else {
                    AudioConfigUtil.resolveAudioSettings(nonDefaultAudioSpec, audioProfile)
                }
            }

        resolvedSettings.forEach { settings ->
            assertThat(
                    AudioSource.isSettingsSupported(
                        settings.captureSampleRate,
                        settings.channelCount,
                        settings.audioFormat,
                    )
                )
                .isTrue()
        }
    }
}
