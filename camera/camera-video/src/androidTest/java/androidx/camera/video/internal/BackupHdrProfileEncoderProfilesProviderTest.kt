/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.video.internal

import android.content.Context
import android.media.CamcorderProfile
import android.media.EncoderProfiles.VideoProfile.HDR_HLG
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.Camera2EncoderProfilesProvider
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.adapter.EncoderProfilesProviderAdapter
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.video.internal.BackupHdrProfileEncoderProfilesProvider.validateOrAdapt
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.MediaCodecInfoReportIncorrectInfoQuirk
import androidx.camera.video.internal.encoder.VideoEncoderInfoImpl
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class BackupHdrProfileEncoderProfilesProviderTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
    private val quality: Int
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule val labTest: LabTestRule = LabTestRule()

    companion object {

        // Reference to the available values listed in Quality.
        @JvmStatic
        private val qualities =
            arrayOf(
                CamcorderProfile.QUALITY_480P,
                CamcorderProfile.QUALITY_720P,
                CamcorderProfile.QUALITY_1080P,
                CamcorderProfile.QUALITY_2160P,
            )

        @JvmStatic
        private val cameraXConfigs =
            listOf(Camera2Config::class.simpleName, CameraPipeConfig::class.simpleName)

        @JvmStatic
        @Parameterized.Parameters(name = "config={0}, quality={2}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                cameraXConfigs.forEach { configImplName ->
                    qualities.forEach { quality ->
                        add(
                            arrayOf(
                                configImplName,
                                when (configImplName) {
                                    CameraPipeConfig::class.simpleName ->
                                        CameraPipeConfig.defaultConfig()
                                    Camera2Config::class.simpleName -> Camera2Config.defaultConfig()
                                    else -> Camera2Config.defaultConfig()
                                },
                                quality
                            )
                        )
                    }
                }
            }

        private fun hasMediaCodecIncorrectInfoQuirk(): Boolean {
            return DeviceQuirks.get(MediaCodecInfoReportIncorrectInfoQuirk::class.java) != null
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private lateinit var cameraId: String
    private lateinit var cameraInfo: CameraInfoInternal
    private lateinit var baseProvider: EncoderProfilesProvider

    @Before
    fun setup() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )

        CameraXUtil.initialize(context, cameraConfig).get()

        cameraId = CameraUtil.getCameraIdWithLensFacing(cameraSelector.lensFacing!!)!!
        cameraInfo =
            CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
                as CameraInfoInternal
        baseProvider =
            if (implName == CameraPipeConfig::class.simpleName) {
                EncoderProfilesProviderAdapter(cameraId, cameraInfo.cameraQuirks)
            } else {
                Camera2EncoderProfilesProvider(cameraId, cameraInfo.cameraQuirks)
            }
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun validateOrAdaptWithDefaultEncoderInfoFinder_returnNonNull_whenProfileIsFromCamcorder() {
        // Arrange.
        assumeFalse(hasMediaCodecIncorrectInfoQuirk())
        assumeTrue(baseProvider.hasProfile(quality))
        val encoderProfiles = baseProvider.getAll(quality)
        val baseVideoProfile = encoderProfiles!!.videoProfiles[0]
        // Due to a known issue where VideoProfile might be null, see InvalidVideoProfilesQuirk,
        // skip the test if VideoProfile is null.
        assumeNotNull(baseVideoProfile)

        // Act.
        val resultVideoProfile = validateOrAdapt(baseVideoProfile, VideoEncoderInfoImpl.FINDER)

        // Verify.
        assertWithMessage("Video profile validation failed.").that(resultVideoProfile).isNotNull()
    }

    @LabTestRule.LabTestOnly
    @SdkSuppress(minSdkVersion = 33)
    @Test
    fun providerWithDefaultEncoderInfoFinder_provideHdrBackupProfile_whenBaseSdrProfileIsValid() {
        // Pre-arrange.
        assumeTrue(cameraInfo.supportedDynamicRanges.containsAll(setOf(SDR, HLG_10_BIT)))

        // Arrange.
        assumeFalse(hasMediaCodecIncorrectInfoQuirk())
        assumeTrue(baseProvider.hasProfile(quality))
        val baseVideoProfilesSize = baseProvider.getAll(quality)!!.videoProfiles.size

        // Act.
        val backupProvider =
            BackupHdrProfileEncoderProfilesProvider(baseProvider, VideoEncoderInfoImpl.FINDER)
        assertThat(backupProvider.hasProfile(quality)).isTrue()
        val backupVideoProfiles = backupProvider.getAll(quality)!!.videoProfiles

        // Verify.
        assertThat(backupVideoProfiles.size).isEqualTo(baseVideoProfilesSize + 1)
        assertThat(backupVideoProfiles.last().hdrFormat).isEqualTo(HDR_HLG)
        assertThat(backupVideoProfiles.last().bitDepth).isEqualTo(BIT_DEPTH_10)
    }
}
