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

package androidx.camera.core.featurecombination.impl.resolver

import android.graphics.ImageFormat
import android.util.Range
import androidx.camera.core.CameraUseCaseAdapterProvider
import androidx.camera.core.CompositionSettings
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featurecombination.Feature.Companion.FPS_60
import androidx.camera.core.featurecombination.Feature.Companion.HDR_HLG10
import androidx.camera.core.featurecombination.Feature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featurecombination.Feature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.featurecombination.impl.feature.DynamicRangeFeature
import androidx.camera.core.featurecombination.impl.feature.FpsRangeFeature
import androidx.camera.core.featurecombination.impl.feature.VideoStabilizationFeature
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.Supported
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.Unsupported
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.UseCaseMissing
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.stabilization.StabilizationMode.OFF
import androidx.camera.core.impl.stabilization.StabilizationMode.ON
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.StreamSpecQueryResult
import androidx.camera.core.internal.StreamSpecsCalculator
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.FakeStreamSpecsCalculator
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalSessionConfig::class)
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class DefaultFeatureCombinationResolverTest {
    private val fakeStreamSpecsCalculator = FakeStreamSpecsCalculator()

    private val cameraUseCaseAdapter =
        CameraUseCaseAdapter(
            FakeCamera(),
            FakeCameraCoordinator(),
            fakeStreamSpecsCalculator,
            FakeUseCaseConfigFactory(),
        )

    private val fakeCameraInfo =
        FakeCameraInfoInternal(fakeStreamSpecsCalculator).apply {
            setCameraUseCaseAdapterProvider(
                object : CameraUseCaseAdapterProvider {
                    override fun provide(cameraId: String): CameraUseCaseAdapter {
                        return cameraUseCaseAdapter
                    }

                    override fun provide(
                        camera: CameraInternal,
                        secondaryCamera: CameraInternal?,
                        adapterCameraInfo: AdapterCameraInfo,
                        secondaryAdapterCameraInfo: AdapterCameraInfo?,
                        compositionSettings: CompositionSettings,
                        secondaryCompositionSettings: CompositionSettings,
                    ): CameraUseCaseAdapter {
                        return cameraUseCaseAdapter
                    }
                }
            )
        }

    private val defaultResolver = DefaultFeatureCombinationResolver(fakeCameraInfo)

    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture.Builder().build()
    private val unsupportedUseCase = FakeUseCase()

    private val defaultUseCases =
        listOf(preview, imageCapture, VideoCapture.withOutput(Recorder.Builder().build()))

    private val defaultPrivStreamSpec =
        FakeStreamSpecsCalculator.ExtendedStreamSpec(
            dynamicRange = DynamicRangeFeature.DEFAULT_DYNAMIC_RANGE,
            expectedFrameRateRange = FpsRangeFeature.DEFAULT_FPS_RANGE,
            imageFormat = INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            previewStabilizationMode =
                VideoStabilizationFeature.DEFAULT_STABILIZATION_MODE.toPreviewStabilizationMode(),
        )

    private val defaultJpegStreamSpec =
        FakeStreamSpecsCalculator.ExtendedStreamSpec(
            dynamicRange = DynamicRange.UNSPECIFIED,
            expectedFrameRateRange = FpsRangeFeature.DEFAULT_FPS_RANGE,
            imageFormat = ImageFormat.JPEG,
            previewStabilizationMode =
                VideoStabilizationFeature.DEFAULT_STABILIZATION_MODE.toPreviewStabilizationMode(),
        )

    @Test
    fun resolveFeatureCombination_useCaseCombinationNotSupportedByDevice_returnsUnsupported() {
        // Arrange - JPEG stream spec support not added, but ImageCapture added
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(defaultPrivStreamSpec)

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, preferredFeatures = listOf(HDR_HLG10))
            )

        assertThat(result).isInstanceOf(Unsupported::class.java)
    }

    @Test
    fun resolveFeatureCombination_noFeature_throwsException() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec,
        )

        // Act & assert
        assertThrows<IllegalArgumentException> {
            defaultResolver.resolveFeatureCombination(SessionConfig(defaultUseCases))
        }
    }

    @Test
    fun resolveFeatureCombination_ultraHdrRequiredAndSupported_returnsSupportedResult() {
        // Arrange: Ultra HDR is required and supported
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec.copy(imageFormat = ImageFormat.JPEG_R),
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, requiredFeatures = setOf(IMAGE_ULTRA_HDR))
            )

        // Assert: Ensure result is supported and contains Ultra HDR feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).contains(IMAGE_ULTRA_HDR)
    }

    @Test
    fun resolveFeatureCombination_ultraHdrRequiredButNoImageCapture_returnsUseCaseMissing() {
        // Arrange: Ultra HDR is required and supported, but no ImageCapture
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec.copy(imageFormat = ImageFormat.JPEG_R),
        )
        val useCases = listOf(preview) // No ImageCapture

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(useCases, requiredFeatures = setOf(IMAGE_ULTRA_HDR))
            )

        // Assert: Ensure result is UseCaseMissing and reports ImageCapture as the missing use case
        // and Ultra HDR as the requiring feature.
        assertThat(result).isInstanceOf(UseCaseMissing::class.java)
        val useCaseMissingResult = result as UseCaseMissing
        assertThat(useCaseMissingResult.requiredUseCases).isEqualTo("ImageCapture")
        assertThat(useCaseMissingResult.featureRequiring).isEqualTo(IMAGE_ULTRA_HDR)
    }

    @Test
    fun resolveFeatureCombination_ultraHdrRequiredButNotSupported_returnsUnsupported() {
        // Arrange: Ultra HDR is required, but not supported, only JPEG capture is supported.
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec,
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, requiredFeatures = setOf(IMAGE_ULTRA_HDR))
            )

        // Assert
        assertThat(result).isInstanceOf(Unsupported::class.java)
    }

    @Test
    fun resolveFeatureCombination_ultraHdrPreferredAndSupported_supportedWithUltraHdr() {
        // Arrange: Ultra HDR is preferred and supported
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec.copy(imageFormat = ImageFormat.JPEG_R),
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, preferredFeatures = listOf(IMAGE_ULTRA_HDR))
            )

        // Assert: Ensure result is supported and contains Ultra HDR feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).contains(IMAGE_ULTRA_HDR)
    }

    @Test
    fun resolveFeatureCombination_ultraHdrPreferredButNoImageCapture_supportedWithoutUltraHdr() {
        // Arrange: Ultra HDR is preferred and supported, but no ImageCapture
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec.copy(imageFormat = ImageFormat.JPEG_R),
        )
        val useCases = listOf(preview) // No ImageCapture

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(useCases, preferredFeatures = listOf(IMAGE_ULTRA_HDR))
            )

        // Assert: Ensure result is supported and does not contain Ultra HDR feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).doesNotContain(IMAGE_ULTRA_HDR)
    }

    @Test
    fun resolveFeatureCombination_ultraHdrPreferredButNotSupported_returnsSupported() {
        // Arrange: Ultra HDR is preferred, but not supported, only JPEG capture is supported
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec,
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, preferredFeatures = listOf(IMAGE_ULTRA_HDR))
            )

        // Assert
        // Assert: Ensure result is supported and does not contain Ultra HDR feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).doesNotContain(IMAGE_ULTRA_HDR)
    }

    @Test
    fun resolveFeatureCombination_hlg10RequiredAndSupported_returnsSupportedIncludingHlg10() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(dynamicRange = DynamicRange.HLG_10_BIT),
            defaultJpegStreamSpec,
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, requiredFeatures = setOf(HDR_HLG10))
            )

        // Assert: Returns Supported with HDR_HLG10 included as resolved feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).contains(HDR_HLG10)
    }

    @Test
    fun resolveFeatureCombination_hlg10RequiredButOnlyImageCaptureUseCase_returnsUseCaseMissing() {
        // Arrange: Ultra HDR is required and supported, but no ImageCapture
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(dynamicRange = DynamicRange.HLG_10_BIT),
            defaultJpegStreamSpec,
        )
        val useCases = listOf(imageCapture) // Only ImageCapture, no Preview or VideoCapture

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(useCases, requiredFeatures = setOf(HDR_HLG10))
            )

        // Assert: Ensure result is UseCaseMissing and reports ImageCapture as the missing use case
        // and Ultra HDR as the requiring feature.
        assertThat(result).isInstanceOf(UseCaseMissing::class.java)
        val useCaseMissingResult = result as UseCaseMissing
        assertThat(useCaseMissingResult.requiredUseCases).isEqualTo("Preview or VideoCapture")
        assertThat(useCaseMissingResult.featureRequiring).isEqualTo(HDR_HLG10)
    }

    @Test
    fun resolveFeatureCombination_hlg10RequiredButUnsupported_returnsUnsupported() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec,
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, requiredFeatures = setOf(HDR_HLG10))
            )

        // Assert
        assertThat(result).isInstanceOf(Unsupported::class.java)
    }

    @Test
    fun resolveFeatureCombination_hlg10PreferredAndSupported_returnsSupportedIncludingHlg10() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(dynamicRange = DynamicRange.HLG_10_BIT),
            defaultJpegStreamSpec,
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, preferredFeatures = listOf(HDR_HLG10))
            )

        // Assert: Returns Supported with HDR_HLG10 included as resolved feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).contains(HDR_HLG10)
    }

    @Test
    fun resolveFeatureCombination_hlg10PreferredButUnsupported_returnsSupportedWithoutHlg10() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec,
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, preferredFeatures = listOf(HDR_HLG10))
            )

        // Assert: Returns Supported without HDR_HLG10 included as resolved feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).doesNotContain(HDR_HLG10)
    }

    @Test
    fun resolveFeatureCombination_previewStabilizationRequiredAndSupported_returnsSupported() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(previewStabilizationMode = ON),
            defaultJpegStreamSpec.copy(previewStabilizationMode = ON),
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, requiredFeatures = setOf(PREVIEW_STABILIZATION))
            )

        // Assert: Returns Supported with PREVIEW_STABILIZATION included as resolved feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).contains(PREVIEW_STABILIZATION)
    }

    @Test
    fun resolveFeatureCombination_prevStabRequiredButOnlyImgCaptureUseCase_returnsUseCaseMissing() {
        // Arrange: Ultra HDR is required and supported, but no ImageCapture
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(previewStabilizationMode = ON),
            defaultJpegStreamSpec.copy(previewStabilizationMode = ON),
        )
        val useCases = listOf(imageCapture) // Only ImageCapture, no Preview or VideoCapture

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(useCases, requiredFeatures = setOf(PREVIEW_STABILIZATION))
            )

        // Assert: Ensure result is UseCaseMissing and reports ImageCapture as the missing use case
        // and Ultra HDR as the requiring feature.
        assertThat(result).isInstanceOf(UseCaseMissing::class.java)
        val useCaseMissingResult = result as UseCaseMissing
        assertThat(useCaseMissingResult.requiredUseCases).isEqualTo("Preview or VideoCapture")
        assertThat(useCaseMissingResult.featureRequiring).isEqualTo(PREVIEW_STABILIZATION)
    }

    @Test
    fun resolveFeatureCombination_previewStabilizationRequiredButUnsupported_returnsUnsupported() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec,
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, requiredFeatures = setOf(PREVIEW_STABILIZATION))
            )

        // Assert
        assertThat(result).isInstanceOf(Unsupported::class.java)
    }

    @Test
    fun resolveFeatureCombination_prevStabPreferredAndSupported_returnsSupportedIncludingPrvStab() {
        // Arrange: StabilizationMode.ON will be set to Preview only while UNSPECIFIED will be set
        // to all other use cases for proper merging.
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(previewStabilizationMode = ON),
            defaultJpegStreamSpec.copy(previewStabilizationMode = ON),
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, preferredFeatures = listOf(PREVIEW_STABILIZATION))
            )

        // Assert: Returns Supported with PREVIEW_STABILIZATION included as resolved feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).contains(PREVIEW_STABILIZATION)
    }

    @Test
    fun resolveFeatureCombination_prevStabPreferredButUnsupported_returnsSupportedWithoutPrvStab() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec,
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, preferredFeatures = listOf(PREVIEW_STABILIZATION))
            )

        // Assert: Returns Supported without PREVIEW_STABILIZATION included as resolved feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).doesNotContain(PREVIEW_STABILIZATION)
    }

    @Test
    fun resolveFeatureCombination_fps60RequiredAndSupported_returnsSupported() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(expectedFrameRateRange = Range(60, 60)),
            defaultJpegStreamSpec.copy(expectedFrameRateRange = Range(60, 60)),
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, requiredFeatures = setOf(FPS_60))
            )

        // Assert: Returns Supported with FPS_60 included as resolved feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).contains(FPS_60)
    }

    @Test
    fun resolveFeatureCombination_fps60RequiredButOnlyImgCaptureUseCase_returnsUseCaseMissing() {
        // Arrange: Ultra HDR is required and supported, but no ImageCapture
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(expectedFrameRateRange = Range(60, 60)),
            defaultJpegStreamSpec.copy(expectedFrameRateRange = Range(60, 60)),
        )
        val useCases = listOf(imageCapture) // Only ImageCapture, no Preview or VideoCapture

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(useCases, requiredFeatures = setOf(FPS_60))
            )

        // Assert: Ensure result is UseCaseMissing and reports ImageCapture as the missing use case
        // and Ultra HDR as the requiring feature.
        assertThat(result).isInstanceOf(UseCaseMissing::class.java)
        val useCaseMissingResult = result as UseCaseMissing
        assertThat(useCaseMissingResult.requiredUseCases).isEqualTo("Preview or VideoCapture")
        assertThat(useCaseMissingResult.featureRequiring).isEqualTo(FPS_60)
    }

    @Test
    fun resolveFeatureCombination_fps60RequiredButUnsupported_returnsUnsupported() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec,
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, requiredFeatures = setOf(FPS_60))
            )

        // Assert
        assertThat(result).isInstanceOf(Unsupported::class.java)
    }

    @Test
    fun resolveFeatureCombination_fps60PreferredAndSupported_returnsSupportedIncludingFps60() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(expectedFrameRateRange = Range(60, 60)),
            defaultJpegStreamSpec.copy(expectedFrameRateRange = Range(60, 60)),
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, preferredFeatures = listOf(FPS_60))
            )

        // Assert: Returns Supported with FPS_60 included as resolved feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).contains(FPS_60)
    }

    @Test
    fun resolveFeatureCombination_fps60PreferredButUnsupported_returnsSupportedWithoutFps60() {
        // Arrange
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec,
            defaultJpegStreamSpec,
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(defaultUseCases, preferredFeatures = listOf(FPS_60))
            )

        // Assert: Returns Supported without FPS_60 included as resolved feature.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).doesNotContain(FPS_60)
    }

    @Test
    fun resolveFeatureCombination_allFeaturesPreferred_returnsSupportedWithCorrectFeatures() {
        // Arrange: Support HDR_HLG10 and IMAGE_ULTRA_HDR while not supporting others
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(dynamicRange = DynamicRange.HLG_10_BIT),
            defaultJpegStreamSpec.copy(imageFormat = ImageFormat.JPEG_R),
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(
                    defaultUseCases,
                    preferredFeatures =
                        listOf(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR),
                )
            )

        // Assert: Returns Supported with HDR_HLG10, IMAGE_ULTRA_HDR included as resolved features.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features).containsExactly(HDR_HLG10, IMAGE_ULTRA_HDR)
    }

    @Test
    fun resolve_allPreferredButHlg10Plus60FpsUnsupported_returnsSupportedWithPrioritizedFeatures() {
        // Arrange: Support HDR + UltraHDR + Stabilization and 60FPS + UltraHDR + Stabilization
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(
                dynamicRange = DynamicRange.HLG_10_BIT,
                previewStabilizationMode = ON,
            ),
            defaultJpegStreamSpec.copy(
                imageFormat = ImageFormat.JPEG_R,
                previewStabilizationMode = ON,
            ),
            defaultPrivStreamSpec.copy(
                expectedFrameRateRange = Range(60, 60),
                previewStabilizationMode = ON,
            ),
            defaultJpegStreamSpec.copy(
                imageFormat = ImageFormat.JPEG_R,
                expectedFrameRateRange = Range(60, 60),
                previewStabilizationMode = ON,
            ),
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(
                    defaultUseCases,
                    preferredFeatures =
                        listOf(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR),
                )
            )

        // Assert: Returns Supported with HLG10, PrevStab, UltraHDR included as resolved features.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features)
            .containsExactly(HDR_HLG10, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR)
    }

    @Test
    fun resolve_highestPriorityPreferredIsUnsupportedWithRequired_returnsSupportedCorrectly() {
        // Arrange: Support HDR + UltraHDR + Stabilization and 60FPS + UltraHDR + Stabilization
        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            defaultPrivStreamSpec.copy(
                dynamicRange = DynamicRange.HLG_10_BIT,
                previewStabilizationMode = ON,
            ),
            defaultJpegStreamSpec.copy(
                imageFormat = ImageFormat.JPEG_R,
                previewStabilizationMode = ON,
            ),
            defaultPrivStreamSpec.copy(
                expectedFrameRateRange = Range(60, 60),
                previewStabilizationMode = ON,
            ),
            defaultJpegStreamSpec.copy(
                imageFormat = ImageFormat.JPEG_R,
                expectedFrameRateRange = Range(60, 60),
                previewStabilizationMode = ON,
            ),
        )

        // Act
        val result =
            defaultResolver.resolveFeatureCombination(
                SessionConfig(
                    defaultUseCases,
                    requiredFeatures = setOf(FPS_60),
                    preferredFeatures = listOf(HDR_HLG10, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR),
                )
            )

        // Assert: Returns Supported with 60FPS, PrevStab, UltraHDR included as resolved features.
        // Although HLG10 is prioritized higher, it's not supported with 60FPS which is required.
        assertThat(result).isInstanceOf(Supported::class.java)
        val resolvedFeatureCombination = (result as Supported).resolvedFeatureCombination
        assertThat(resolvedFeatureCombination.features)
            .containsExactly(FPS_60, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR)
    }

    @Test
    fun resolveFeatureCombination_containsRequiredFeature_passesTrueForIsFeatureComboInvocation() {
        val defaultResolver =
            DefaultFeatureCombinationResolver(
                FakeCameraInfoInternal(
                    object : StreamSpecsCalculator {
                        override fun calculateSuggestedStreamSpecs(
                            cameraMode: Int,
                            cameraInfoInternal: CameraInfoInternal,
                            newUseCases: List<UseCase>,
                            attachedUseCases: List<UseCase>,
                            cameraConfig: CameraConfig,
                            sessionType: Int,
                            targetFrameRate: Range<Int>,
                            isFeatureComboInvocation: Boolean,
                            findMaxSupportedFrameRate: Boolean,
                        ): StreamSpecQueryResult {
                            assertThat(isFeatureComboInvocation).isEqualTo(true)
                            return StreamSpecQueryResult()
                        }
                    }
                )
            )

        defaultResolver.resolveFeatureCombination(
            SessionConfig(defaultUseCases, requiredFeatures = setOf(HDR_HLG10))
        )
    }

    @Test
    fun resolveFeatureCombination_containsPreferredFeature_passesTrueForIsFeatureComboInvocation() {
        val defaultResolver =
            DefaultFeatureCombinationResolver(
                FakeCameraInfoInternal(
                    object : StreamSpecsCalculator {
                        override fun calculateSuggestedStreamSpecs(
                            cameraMode: Int,
                            cameraInfoInternal: CameraInfoInternal,
                            newUseCases: List<UseCase>,
                            attachedUseCases: List<UseCase>,
                            cameraConfig: CameraConfig,
                            sessionType: Int,
                            targetFrameRate: Range<Int>,
                            isFeatureComboInvocation: Boolean,
                            findMaxSupportedFrameRate: Boolean,
                        ): StreamSpecQueryResult {
                            assertThat(isFeatureComboInvocation).isEqualTo(true)
                            return StreamSpecQueryResult()
                        }
                    }
                )
            )

        defaultResolver.resolveFeatureCombination(
            SessionConfig(defaultUseCases, preferredFeatures = listOf(HDR_HLG10))
        )
    }

    private fun VideoStabilizationFeature.StabilizationMode.toPreviewStabilizationMode(): Int {
        return when (this) {
            VideoStabilizationFeature.StabilizationMode.OFF -> OFF
            VideoStabilizationFeature.StabilizationMode.ON -> OFF
            VideoStabilizationFeature.StabilizationMode.PREVIEW -> ON
        }
    }
}
