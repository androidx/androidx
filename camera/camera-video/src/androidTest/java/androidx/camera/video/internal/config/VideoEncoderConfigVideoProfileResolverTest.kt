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
import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.Timebase
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.video.EncoderProfilesResolver
import androidx.camera.video.EncoderProfilesResolverFactory
import androidx.camera.video.VideoSpec
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

@RunWith(Parameterized::class)
@SmallTest
class VideoEncoderConfigVideoProfileResolverTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val defaultVideoSpec = VideoSpec.builder().build()
    private val timebase = Timebase.UPTIME

    private lateinit var dynamicRanges: Set<DynamicRange>
    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var profilesResolver: EncoderProfilesResolver

    @Before
    fun setUp() {
        val cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )

        CameraXUtil.initialize(context, cameraConfig).get()

        val cameraInfo = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
        profilesResolver = EncoderProfilesResolverFactory.getResolver(cameraInfo)
        dynamicRanges = profilesResolver.supportedDynamicRanges
        dynamicRanges.forEach {
            Assume.assumeTrue(profilesResolver.getSupportedQualities(it).isNotEmpty())
        }
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
    fun defaultVideoSpecProducesValidSettings_forSurfaceSizeEquivalentToQuality() {
        dynamicRanges.forEach { dynamicRange ->
            val supportedProfiles =
                profilesResolver.getSupportedQualities(dynamicRange).map {
                    profilesResolver.getProfiles(it, dynamicRange)!!
                }

            supportedProfiles.forEach {
                val videoProfile = it.defaultVideoProfile
                val config =
                    VideoEncoderConfigVideoProfileResolver(
                            videoProfile.mediaType,
                            timebase,
                            defaultVideoSpec,
                            videoProfile.resolution,
                            videoProfile,
                            dynamicRange,
                            Range(videoProfile.frameRate, videoProfile.frameRate),
                        )
                        .get()

                assertThat(config.mimeType).isEqualTo(videoProfile.mediaType)
                assertThat(config.bitrate).isEqualTo(videoProfile.bitrate)
                assertThat(config.resolution).isEqualTo(videoProfile.resolution)
                assertThat(config.captureFrameRate).isEqualTo(videoProfile.frameRate)
                assertThat(config.encodeFrameRate).isEqualTo(videoProfile.frameRate)
            }
        }
    }
}
