/*
 * Copyright 2026 The Android Open Source Project
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

import android.util.Range
import android.view.Surface
import androidx.camera.core.impl.ImageInputConfig
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.internal.ThreadConfig
import androidx.camera.core.resolutionselector.ResolutionSelector
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class CameraXDslTest {

    @Test
    fun testPreviewDsl() {
        val preview = preview {
            targetName = "test_preview"
            targetRotation = Surface.ROTATION_90
            dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT

            assertThat(targetName).isEqualTo("test_preview")
            assertThat(targetRotation).isEqualTo(Surface.ROTATION_90)
            assertThat(dynamicRange).isEqualTo(DynamicRange.HDR_UNSPECIFIED_10_BIT)
        }

        val config = preview.currentConfig as ImageOutputConfig
        assertThat(preview.name).isEqualTo("test_preview")
        assertThat(config.getTargetRotation(Surface.ROTATION_0)).isEqualTo(Surface.ROTATION_90)

        val useCaseConfig = preview.currentConfig as UseCaseConfig<*>
        assertThat(useCaseConfig.retrieveOption(ImageInputConfig.OPTION_INPUT_DYNAMIC_RANGE, null))
            .isEqualTo(DynamicRange.HDR_UNSPECIFIED_10_BIT)
    }

    @Test
    fun testPreviewScope_additionalOptions() {
        val selector = ResolutionSelector.Builder().build()
        val preview = preview {
            mirrorMode = MirrorMode.MIRROR_MODE_ON
            resolutionSelector = selector
            isPreviewStabilizationEnabled = true
            targetFrameRate = Range(30, 30)

            assertThat(mirrorMode).isEqualTo(MirrorMode.MIRROR_MODE_ON)
            assertThat(resolutionSelector).isEqualTo(selector)
            assertThat(isPreviewStabilizationEnabled).isTrue()
            assertThat(targetFrameRate).isEqualTo(Range(30, 30))
        }

        val outputConfig = preview.currentConfig as ImageOutputConfig
        assertThat(outputConfig.retrieveOption(ImageOutputConfig.OPTION_MIRROR_MODE, null))
            .isEqualTo(MirrorMode.MIRROR_MODE_ON)
        assertThat(outputConfig.resolutionSelector).isEqualTo(selector)

        val useCaseConfig = preview.currentConfig as UseCaseConfig<*>
        assertThat(
                useCaseConfig.retrieveOption(UseCaseConfig.OPTION_PREVIEW_STABILIZATION_MODE, null)
            )
            .isEqualTo(StabilizationMode.ON)
        assertThat(useCaseConfig.retrieveOption(UseCaseConfig.OPTION_TARGET_FRAME_RATE, null))
            .isEqualTo(Range(30, 30))
    }

    @Test
    fun testImageCaptureDsl() {
        val selector = ResolutionSelector.Builder().build()
        val imageCapture = imageCapture {
            targetName = "test_image_capture"
            captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            flashMode = ImageCapture.FLASH_MODE_AUTO
            targetRotation = Surface.ROTATION_180
            resolutionSelector = selector
            isPostviewEnabled = true

            assertThat(targetName).isEqualTo("test_image_capture")
            assertThat(captureMode).isEqualTo(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            assertThat(flashMode).isEqualTo(ImageCapture.FLASH_MODE_AUTO)
            assertThat(targetRotation).isEqualTo(Surface.ROTATION_180)
            assertThat(resolutionSelector).isEqualTo(selector)
            assertThat(isPostviewEnabled).isTrue()
        }

        assertThat(imageCapture.name).isEqualTo("test_image_capture")
        assertThat(imageCapture.captureMode).isEqualTo(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        assertThat(imageCapture.flashMode).isEqualTo(ImageCapture.FLASH_MODE_AUTO)
        assertThat(imageCapture.isPostviewEnabled).isTrue()

        val config = imageCapture.currentConfig as ImageOutputConfig
        assertThat(config.getTargetRotation(Surface.ROTATION_0)).isEqualTo(Surface.ROTATION_180)
        assertThat(config.resolutionSelector).isEqualTo(selector)
    }

    @Test
    fun testImageCaptureDsl_screenFlash() {
        val dummyScreenFlash =
            object : ImageCapture.ScreenFlash {
                override fun apply(
                    expirationTimeMillis: Long,
                    screenFlashListener: ImageCapture.ScreenFlashListener,
                ) {}

                override fun clear() {}
            }

        val imageCapture = imageCapture {
            screenFlash = dummyScreenFlash
            assertThat(screenFlash).isEqualTo(dummyScreenFlash)
        }

        assertThat(imageCapture.screenFlash).isEqualTo(dummyScreenFlash)
    }

    @Test
    fun testImageAnalysisDsl() {
        val selector = ResolutionSelector.Builder().build()
        val executor = Executor { it.run() }
        val imageAnalysis = imageAnalysis {
            targetName = "test_image_analysis"
            backpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            imageQueueDepth = 5
            targetRotation = Surface.ROTATION_90
            resolutionSelector = selector
            backgroundExecutor = executor

            assertThat(targetName).isEqualTo("test_image_analysis")
            assertThat(backpressureStrategy).isEqualTo(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            assertThat(imageQueueDepth).isEqualTo(5)
            assertThat(targetRotation).isEqualTo(Surface.ROTATION_90)
            assertThat(resolutionSelector).isEqualTo(selector)
            assertThat(backgroundExecutor).isEqualTo(executor)
        }

        assertThat(imageAnalysis.name).isEqualTo("test_image_analysis")
        assertThat(imageAnalysis.backpressureStrategy)
            .isEqualTo(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        assertThat(imageAnalysis.imageQueueDepth).isEqualTo(5)

        val outputConfig = imageAnalysis.currentConfig as ImageOutputConfig
        assertThat(outputConfig.getTargetRotation(Surface.ROTATION_0))
            .isEqualTo(Surface.ROTATION_90)
        assertThat(outputConfig.resolutionSelector).isEqualTo(selector)

        val useCaseConfig = imageAnalysis.currentConfig as UseCaseConfig<*>
        assertThat(useCaseConfig.retrieveOption(ThreadConfig.OPTION_BACKGROUND_EXECUTOR, null))
            .isEqualTo(executor)
    }

    @Test
    fun testImageAnalysisScope_withResolutionSelector() {
        val selector = ResolutionSelector.Builder().build()
        val imageAnalysis = imageAnalysis {
            resolutionSelector = selector
            assertThat(resolutionSelector).isEqualTo(selector)
        }

        val outputConfig = imageAnalysis.currentConfig as ImageOutputConfig
        assertThat(outputConfig.resolutionSelector).isEqualTo(selector)
    }

    @Test
    fun testSessionConfigScope_properties() {
        val preview = Preview.Builder().build()
        val viewport = ViewPort.Builder(android.util.Rational(4, 3), Surface.ROTATION_0).build()
        val fpsRange = Range(30, 30)

        val config =
            sessionConfig(listOf(preview)) {
                viewPort = viewport
                frameRateRange = fpsRange
                isAutoRotationEnabled = true
                requiredFeatureGroup =
                    setOf(androidx.camera.core.featuregroup.GroupableFeature.FPS_60)
                preferredFeatureGroup =
                    listOf(
                        androidx.camera.core.featuregroup.GroupableFeature.HDR_HLG10,
                        androidx.camera.core.featuregroup.GroupableFeature.IMAGE_ULTRA_HDR,
                    )

                assertThat(viewPort).isEqualTo(viewport)
                assertThat(frameRateRange).isEqualTo(fpsRange)
                assertThat(isAutoRotationEnabled).isTrue()
                assertThat(requiredFeatureGroup)
                    .containsExactly(androidx.camera.core.featuregroup.GroupableFeature.FPS_60)
                assertThat(preferredFeatureGroup)
                    .containsExactly(
                        androidx.camera.core.featuregroup.GroupableFeature.HDR_HLG10,
                        androidx.camera.core.featuregroup.GroupableFeature.IMAGE_ULTRA_HDR,
                    )
                    .inOrder()
            }

        assertThat(config.viewPort).isEqualTo(viewport)
        assertThat(config.frameRateRange).isEqualTo(fpsRange)
        assertThat(config.isAutoRotationEnabled).isTrue()
        assertThat(config.requiredFeatureGroup)
            .containsExactly(androidx.camera.core.featuregroup.GroupableFeature.FPS_60)
        assertThat(config.preferredFeatureGroup)
            .containsExactly(
                androidx.camera.core.featuregroup.GroupableFeature.HDR_HLG10,
                androidx.camera.core.featuregroup.GroupableFeature.IMAGE_ULTRA_HDR,
            )
            .inOrder()
    }
}
