/*
 * Copyright (C) 2019 The Android Open Source Project
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
package androidx.camera.core

import android.graphics.ImageFormat
import android.graphics.Rect
import android.util.LayoutDirection
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.MirrorMode.MIRROR_MODE_OFF
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
import androidx.camera.core.MirrorMode.MIRROR_MODE_UNSPECIFIED
import androidx.camera.core.UseCase.snapToSurfaceRotation
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.featurecombination.Feature.Companion.FPS_60
import androidx.camera.core.featurecombination.Feature.Companion.HDR_HLG10
import androidx.camera.core.featurecombination.Feature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featurecombination.Feature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.StreamSpecsCalculatorImpl
import androidx.camera.core.internal.TargetConfig.OPTION_TARGET_NAME
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
@OptIn(ExperimentalSessionConfig::class)
class UseCaseTest {
    private lateinit var fakeCamera: FakeCamera
    private lateinit var fakeFrontCamera: FakeCamera

    @Before
    fun setup() {
        fakeCamera = FakeCamera()
        fakeFrontCamera = FakeCamera(null, FakeCameraInfoInternal(0, LENS_FACING_FRONT))
    }

    @Test
    fun noCameraTransform_rotationMirrored() {
        // Arrange.
        val testUseCase = createFakeUseCase(targetRotation = Surface.ROTATION_90)
        val fakeCamera = FakeCamera()
        fakeCamera.hasTransform = false
        // Act/Assert:
        assertThat(testUseCase.getRelativeRotation(fakeCamera, true)).isEqualTo(90)
        assertThat(testUseCase.getRelativeRotation(fakeCamera, false)).isEqualTo(270)
    }

    @Test
    fun hasCameraTransform_rotationNotMirrored() {
        // Arrange.
        val testUseCase = createFakeUseCase(targetRotation = Surface.ROTATION_90)
        val fakeCamera = FakeCamera()
        // Act/Assert:
        assertThat(testUseCase.getRelativeRotation(fakeCamera, true)).isEqualTo(270)
        assertThat(testUseCase.getRelativeRotation(fakeCamera, false)).isEqualTo(270)
    }

    @Test
    fun getAttachedSessionConfig() {
        val testUseCase = createFakeUseCase()
        val sessionToAttach = SessionConfig.Builder().build()
        testUseCase.updateSessionConfig(listOf(sessionToAttach))
        val attachedSession = testUseCase.sessionConfig
        assertThat(attachedSession).isEqualTo(sessionToAttach)
    }

    @Test
    fun removeListener() {
        val testUseCase = createFakeUseCase()
        testUseCase.bindToCamera(fakeCamera, null, null, null)
        testUseCase.unbindFromCamera(fakeCamera)
        testUseCase.notifyActive()
        assertThat(fakeCamera.useCaseActiveHistory).isEmpty()
    }

    @Test
    fun notifyActiveState() {
        val testUseCase = createFakeUseCase()
        testUseCase.bindToCamera(fakeCamera, null, null, null)
        testUseCase.notifyActive()
        assertThat(fakeCamera.useCaseActiveHistory[0]).isEqualTo(testUseCase)
    }

    @Test
    fun notifyInactiveState() {
        val testUseCase = createFakeUseCase()
        testUseCase.bindToCamera(fakeCamera, null, null, null)
        testUseCase.notifyInactive()
        assertThat(fakeCamera.useCaseInactiveHistory[0]).isEqualTo(testUseCase)
    }

    @Test
    fun notifyUpdatedSettings() {
        val testUseCase = FakeUseCase()
        testUseCase.bindToCamera(fakeCamera, null, null, null)
        testUseCase.notifyUpdated()
        assertThat(fakeCamera.useCaseUpdateHistory[0]).isEqualTo(testUseCase)
    }

    @Test
    fun notifyResetUseCase() {
        val testUseCase = FakeUseCase()
        testUseCase.bindToCamera(fakeCamera, null, null, null)
        testUseCase.notifyReset()
        assertThat(fakeCamera.useCaseResetHistory[0]).isEqualTo(testUseCase)
    }

    @Test
    fun useCaseConfig_keepOptionPriority() {
        val builder = FakeUseCaseConfig.Builder()
        val opt = Config.Option.create<Int>("OPT1", Int::class.java)
        builder.mutableConfig.insertOption(opt, Config.OptionPriority.ALWAYS_OVERRIDE, 1)
        val fakeUseCase = builder.build()
        val useCaseConfig = fakeUseCase.currentConfig
        assertThat(useCaseConfig.getOptionPriority(opt))
            .isEqualTo(Config.OptionPriority.ALWAYS_OVERRIDE)
    }

    @Test
    fun attachedSurfaceResolutionCanBeReset_whenOnDetach() {
        val testUseCase = FakeUseCase()
        testUseCase.updateSuggestedStreamSpec(TEST_STREAM_SPEC, null)
        assertThat(testUseCase.attachedSurfaceResolution).isNotNull()
        testUseCase.bindToCamera(fakeCamera, null, null, null)
        testUseCase.unbindFromCamera(fakeCamera)
        assertThat(testUseCase.attachedSurfaceResolution).isNull()
    }

    @Test
    fun attachedStreamSpecCanBeReset_whenOnDetach() {
        val testUseCase = FakeUseCase()
        testUseCase.updateSuggestedStreamSpec(TEST_STREAM_SPEC, null)
        assertThat(testUseCase.attachedStreamSpec).isNotNull()
        testUseCase.bindToCamera(fakeCamera, null, null, null)
        testUseCase.unbindFromCamera(fakeCamera)
        assertThat(testUseCase.attachedStreamSpec).isNull()
    }

    @Test
    fun viewPortCropRectCanBeReset_whenOnDetach() {
        val testUseCase = FakeUseCase()
        testUseCase.setViewPortCropRect(Rect(0, 0, 640, 480))
        assertThat(testUseCase.viewPortCropRect).isNotNull()
        testUseCase.bindToCamera(fakeCamera, null, null, null)
        testUseCase.unbindFromCamera(fakeCamera)
        assertThat(testUseCase.viewPortCropRect).isNull()
    }

    @Test
    fun mergeConfigs() {
        val cameraDefaultPriority = 4
        val defaultConfig =
            FakeUseCaseConfig.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .setBufferFormat(ImageFormat.RAW10)
                .setSurfaceOccupancyPriority(cameraDefaultPriority)
                .useCaseConfig
        val useCaseImageFormat = ImageFormat.YUV_420_888
        val useCaseConfig =
            FakeUseCaseConfig.Builder()
                .setTargetRotation(Surface.ROTATION_90)
                .setBufferFormat(useCaseImageFormat)
                .useCaseConfig
        val extendedConfig =
            FakeUseCaseConfig.Builder().setTargetRotation(Surface.ROTATION_180).useCaseConfig
        val testUseCase = FakeUseCase(useCaseConfig)
        val cameraInfo = FakeCameraInfoInternal()
        val mergedConfig = testUseCase.mergeConfigs(cameraInfo, extendedConfig, defaultConfig)
        assertThat(mergedConfig.surfaceOccupancyPriority).isEqualTo(cameraDefaultPriority)
        assertThat(mergedConfig.inputFormat).isEqualTo(useCaseImageFormat)
        val imageOutputConfig = mergedConfig as ImageOutputConfig
        assertThat(imageOutputConfig.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun returnNullResolutionInfo_beforeAddingToCameraUseCaseAdapter() {
        val fakeUseCase = FakeUseCase()
        assertThat(fakeUseCase.resolutionInfoInternal).isNull()
    }

    @Test
    @Throws(CameraUseCaseAdapter.CameraException::class)
    fun returnResolutionInfo_afterAddingToCameraUseCaseAdapter() {
        val fakeUseCase = FakeUseCase()
        val cameraUseCaseAdapter = createCameraUseCaseAdapter()
        cameraUseCaseAdapter.addUseCases(listOf<UseCase>(fakeUseCase))
        val resolutionInfo = fakeUseCase.resolutionInfoInternal
        assertThat(resolutionInfo).isNotNull()
        assertThat(resolutionInfo!!.resolution).isEqualTo(SURFACE_RESOLUTION)
        assertThat(resolutionInfo.cropRect)
            .isEqualTo(Rect(0, 0, SURFACE_RESOLUTION.width, SURFACE_RESOLUTION.height))
        assertThat(resolutionInfo.rotationDegrees).isEqualTo(0)
    }

    @Test
    @Throws(CameraUseCaseAdapter.CameraException::class)
    fun returnNullResolutionInfo_afterRemovedFromCameraUseCaseAdapter() {
        val fakeUseCase = FakeUseCase()
        val cameraUseCaseAdapter = createCameraUseCaseAdapter()
        cameraUseCaseAdapter.addUseCases(listOf<UseCase>(fakeUseCase))
        cameraUseCaseAdapter.removeUseCases(listOf<UseCase>(fakeUseCase))
        val resolutionInfo = fakeUseCase.resolutionInfoInternal
        assertThat(resolutionInfo).isNull()
    }

    @Test
    @Throws(CameraUseCaseAdapter.CameraException::class)
    fun correctRotationDegreesInResolutionInfo() {
        val fakeUseCase = FakeUseCase()
        fakeUseCase.targetRotationInternal = Surface.ROTATION_90
        val cameraUseCaseAdapter = createCameraUseCaseAdapter()
        cameraUseCaseAdapter.addUseCases(listOf<UseCase>(fakeUseCase))
        val resolutionInfo = fakeUseCase.resolutionInfoInternal
        assertThat(resolutionInfo!!.rotationDegrees).isEqualTo(270)
    }

    @Test
    @Throws(CameraUseCaseAdapter.CameraException::class)
    fun correctViewPortRectInResolutionInfo() {
        val fakeUseCase = FakeUseCase()
        val cameraUseCaseAdapter = createCameraUseCaseAdapter()
        cameraUseCaseAdapter.setViewPort(
            ViewPort(ViewPort.FILL_CENTER, Rational(16, 9), Surface.ROTATION_0, LayoutDirection.LTR)
        )
        cameraUseCaseAdapter.addUseCases(listOf<UseCase>(fakeUseCase))
        val resolutionInfo = fakeUseCase.resolutionInfoInternal
        assertThat(resolutionInfo!!.cropRect).isEqualTo(Rect(0, 60, 640, 420))
    }

    @Test
    fun defaultMirrorModeIsOff() {
        val fakeUseCase = createFakeUseCase()
        assertThat(fakeUseCase.mirrorModeInternal).isEqualTo(MIRROR_MODE_UNSPECIFIED)
    }

    @Test
    fun canGetSetMirrorMode() {
        val fakeUseCase = createFakeUseCase(mirrorMode = MIRROR_MODE_ON)
        assertThat(fakeUseCase.mirrorModeInternal).isEqualTo(MIRROR_MODE_ON)
    }

    @Test
    fun setMirrorModeOff_isMirroringRequiredIsFalse() {
        val fakeUseCase = createFakeUseCase(mirrorMode = MIRROR_MODE_OFF)
        assertThat(fakeUseCase.isMirroringRequired(fakeCamera)).isFalse()
        assertThat(fakeUseCase.isMirroringRequired(fakeFrontCamera)).isFalse()
    }

    @Test
    fun setMirrorModeOn_isMirroringRequiredIsTrue() {
        val fakeUseCase = createFakeUseCase(mirrorMode = MIRROR_MODE_ON)
        assertThat(fakeUseCase.isMirroringRequired(fakeCamera)).isTrue()
        assertThat(fakeUseCase.isMirroringRequired(fakeFrontCamera)).isTrue()
    }

    @Test
    fun setMirrorModeOnFrontOnly_isMirroringRequiredDependsOnCamera() {
        val fakeUseCase = createFakeUseCase(mirrorMode = MIRROR_MODE_ON_FRONT_ONLY)
        assertThat(fakeUseCase.isMirroringRequired(fakeCamera)).isFalse()
        assertThat(fakeUseCase.isMirroringRequired(fakeFrontCamera)).isTrue()
    }

    @Test
    fun snapToSurfaceRotation_toCorrectValue() {
        assertThat(snapToSurfaceRotation(45)).isEqualTo(Surface.ROTATION_270)
        assertThat(snapToSurfaceRotation(135)).isEqualTo(Surface.ROTATION_180)
        assertThat(snapToSurfaceRotation(225)).isEqualTo(Surface.ROTATION_90)
        assertThat(snapToSurfaceRotation(315)).isEqualTo(Surface.ROTATION_0)
    }

    @Test
    fun snapToSurfaceRotation_invalidInput() {
        assertThrows<IllegalArgumentException> { snapToSurfaceRotation(-1) }
        assertThrows<IllegalArgumentException> { snapToSurfaceRotation(360) }
    }

    @Test
    fun keepUseCaseTargetName_whenMergingConfigs() {
        val targetName = "Fake-UseCase-TargetName"
        val fakeUseCase = FakeUseCaseConfig.Builder().setTargetName(targetName).build()
        val extendedConfig =
            FakeUseCaseConfig.Builder()
                .apply {
                    mutableConfig.insertOption(OPTION_TARGET_NAME, "Extended-Config-TargetName")
                }
                .useCaseConfig
        val defaultConfig =
            FakeUseCaseConfig.Builder()
                .apply {
                    mutableConfig.insertOption(OPTION_TARGET_NAME, "Default-Config-TargetName")
                }
                .useCaseConfig
        val mergedConfig =
            fakeUseCase.mergeConfigs(
                FakeCameraInfoInternal(0, CameraSelector.LENS_FACING_BACK),
                extendedConfig,
                defaultConfig,
            )
        assertThat(mergedConfig.targetName).isEqualTo(targetName)
    }

    @Test
    fun bindToCamera_whenHlg10FeatureIsSet_getDynamicRangeReturnsHlg10() {
        val preview = Preview.Builder().build()
        assumeFalse(preview.dynamicRange == DynamicRange.HLG_10_BIT)

        preview.setFeatureCombination(setOf(HDR_HLG10))
        preview.bindToCamera(fakeCamera, null, null, null)

        assertThat(preview.dynamicRange).isEqualTo(DynamicRange.HLG_10_BIT)
    }

    @Test
    fun bindToCamera_whenOtherFeatureIsSet_getDynamicRangeReturnsSdr() {
        val preview = Preview.Builder().build()

        preview.setFeatureCombination(setOf(FPS_60))
        preview.bindToCamera(fakeCamera, null, null, null)

        assertThat(preview.dynamicRange).isEqualTo(DynamicRange.SDR)
    }

    @Test
    fun bindToCamera_whenNoFeatureCombinationIsSet_getDynamicRangeReturnsUnspecified() {
        val preview = Preview.Builder().build()

        preview.bindToCamera(fakeCamera, null, null, null)

        assertThat(preview.dynamicRange).isEqualTo(DynamicRange.UNSPECIFIED)
    }

    @Test
    fun bindToCamera_whenEmptyFeatureCombinationIsSet_getDynamicRangeReturnsSdr() {
        val preview = Preview.Builder().build()

        preview.setFeatureCombination(emptySet())
        preview.bindToCamera(fakeCamera, null, null, null)

        assertThat(preview.dynamicRange).isEqualTo(DynamicRange.SDR)
    }

    @Test
    fun bindToCamera_when60FpsFeatureIsSet_getTargetFrameRateReturns60() {
        val preview = Preview.Builder().build()
        assumeFalse(preview.targetFrameRate == Range(60, 60))

        preview.setFeatureCombination(setOf(FPS_60))
        preview.bindToCamera(fakeCamera, null, null, null)

        assertThat(preview.targetFrameRate).isEqualTo(Range(60, 60))
    }

    @Test
    fun bindToCamera_whenOtherFeatureIsSet_getTargetFrameRateReturns30() {
        val preview = Preview.Builder().build()

        preview.setFeatureCombination(setOf(HDR_HLG10))
        preview.bindToCamera(fakeCamera, null, null, null)

        assertThat(preview.targetFrameRate).isEqualTo(Range(30, 30))
    }

    @Test
    fun bindToCamera_whenNoFeatureCombinationIsSet_getTargetFrameRateReturnsUnspecified() {
        val preview = Preview.Builder().build()

        preview.bindToCamera(fakeCamera, null, null, null)

        assertThat(preview.targetFrameRate).isEqualTo(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)
    }

    @Test
    fun bindToCamera_whenEmptyFeatureCombinationIsSet_getTargetFrameRateReturns30() {
        val preview = Preview.Builder().build()

        preview.setFeatureCombination(emptySet())
        preview.bindToCamera(fakeCamera, null, null, null)

        assertThat(preview.targetFrameRate).isEqualTo(Range(30, 30))
    }

    @Test
    fun bindToCamera_whenUltraHdrFeatureIsSet_getImageFormatReturnsJpegUltraHdr() {
        val imageCapture = ImageCapture.Builder().build()
        assumeFalse(imageCapture.imageFormat == ImageFormat.JPEG_R)

        imageCapture.setFeatureCombination(setOf(IMAGE_ULTRA_HDR))
        imageCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(imageCapture.imageFormat).isEqualTo(ImageFormat.JPEG_R)
    }

    @Test
    fun bindToCamera_whenOtherFeatureIsSet_getImageFormatReturnsJpeg() {
        val imageCapture = ImageCapture.Builder().build()

        imageCapture.setFeatureCombination(setOf(FPS_60))
        imageCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(imageCapture.imageFormat).isEqualTo(ImageFormat.JPEG)
    }

    @Test
    fun bindToCamera_whenNoFeatureCombinationIsSet_getImageFormatReturnsJpeg() {
        val imageCapture = ImageCapture.Builder().build()

        imageCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(imageCapture.imageFormat).isEqualTo(ImageFormat.JPEG)
    }

    @Test
    fun bindToCamera_whenEmptyFeatureCombinationIsSet_getImageFormatReturnsJpeg() {
        val imageCapture = ImageCapture.Builder().build()

        imageCapture.setFeatureCombination(emptySet())
        imageCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(imageCapture.imageFormat).isEqualTo(ImageFormat.JPEG)
    }

    @Test
    fun bindToCamera_whenUltraHdrFeatureIsSet_getOutputFormatReturnsJpegUltraHdr() {
        val imageCapture = ImageCapture.Builder().build()
        assumeFalse(imageCapture.outputFormat == ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)

        imageCapture.setFeatureCombination(setOf(IMAGE_ULTRA_HDR))
        imageCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(imageCapture.outputFormat).isEqualTo(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
    }

    @Test
    fun bindToCamera_whenOtherFeatureIsSet_getOutputFormatReturnsJpeg() {
        val imageCapture = ImageCapture.Builder().build()

        imageCapture.setFeatureCombination(setOf(FPS_60))
        imageCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(imageCapture.outputFormat).isEqualTo(ImageCapture.OUTPUT_FORMAT_JPEG)
    }

    @Test
    fun bindToCamera_whenNoFeatureCombinationIsSet_getOutputFormatReturnsJpeg() {
        val imageCapture = ImageCapture.Builder().build()

        imageCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(imageCapture.outputFormat).isEqualTo(ImageCapture.OUTPUT_FORMAT_JPEG)
    }

    @Test
    fun bindToCamera_whenEmptyFeatureCombinationIsSet_getOutputFormatReturnsJpeg() {
        val imageCapture = ImageCapture.Builder().build()

        imageCapture.setFeatureCombination(emptySet())
        imageCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(imageCapture.outputFormat).isEqualTo(ImageCapture.OUTPUT_FORMAT_JPEG)
    }

    @Test
    fun bindToCamera_whenPreviewStabilizationFeatureIsSet_isPreviewStabilizationReturnsEnabled() {
        val preview = Preview.Builder().build()
        assumeFalse(preview.isPreviewStabilizationEnabled)

        preview.setFeatureCombination(setOf(PREVIEW_STABILIZATION))
        preview.bindToCamera(fakeCamera, null, null, null)

        assertThat(preview.isPreviewStabilizationEnabled).isTrue()
    }

    @Test
    fun bindToCamera_whenPreviewStabilizationFeatureIsSet_isVideoStabilizationReturnsDisabled() {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        videoCapture.setFeatureCombination(setOf(PREVIEW_STABILIZATION))
        videoCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(videoCapture.isVideoStabilizationEnabled).isFalse()
    }

    @Test
    fun bindToCamera_whenOtherFeatureIsSet_isVideoStabilizationReturnsDisabled() {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        videoCapture.setFeatureCombination(setOf(FPS_60))
        videoCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(videoCapture.isVideoStabilizationEnabled).isFalse()
    }

    @Test
    fun bindToCamera_whenNoFeatureCombinationIsSet_isVideoStabilizationReturnsDisabled() {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        videoCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(videoCapture.isVideoStabilizationEnabled).isFalse()
    }

    @Test
    fun bindToCamera_whenEmptyFeatureCombinationIsSet_isVideoStabilizationReturnsDisabled() {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        videoCapture.setFeatureCombination(emptySet())
        videoCapture.bindToCamera(fakeCamera, null, null, null)

        assertThat(videoCapture.isVideoStabilizationEnabled).isFalse()
    }

    private fun createFakeUseCase(
        targetRotation: Int = Surface.ROTATION_0,
        mirrorMode: Int? = null,
    ): FakeUseCase {
        return FakeUseCase(
            FakeUseCaseConfig.Builder()
                .setTargetName("UseCase")
                .setTargetRotation(targetRotation)
                .apply { mirrorMode?.let { setMirrorMode(it) } }
                .useCaseConfig
        )
    }

    private fun createCameraUseCaseAdapter(): CameraUseCaseAdapter {
        val cameraId = "fakeCameraId"
        val fakeCamera = FakeCamera(cameraId, null, FakeCameraInfoInternal(cameraId))
        val fakeCameraDeviceSurfaceManager = FakeCameraDeviceSurfaceManager()
        fakeCameraDeviceSurfaceManager.setSuggestedStreamSpec(
            cameraId,
            FakeUseCaseConfig::class.java,
            TEST_STREAM_SPEC,
        )
        val useCaseConfigFactory: UseCaseConfigFactory = FakeUseCaseConfigFactory()
        val cameraCoordinator: CameraCoordinator = FakeCameraCoordinator()
        return CameraUseCaseAdapter(
            fakeCamera,
            cameraCoordinator,
            StreamSpecsCalculatorImpl(useCaseConfigFactory, fakeCameraDeviceSurfaceManager),
            useCaseConfigFactory,
        )
    }

    companion object {
        private val SURFACE_RESOLUTION: Size by lazy { Size(640, 480) }
        private val TEST_STREAM_SPEC: StreamSpec by lazy {
            StreamSpec.builder(SURFACE_RESOLUTION).build()
        }
    }
}
